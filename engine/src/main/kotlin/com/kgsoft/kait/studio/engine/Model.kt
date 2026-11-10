package com.kgsoft.kait.studio.engine

import java.io.InputStream
import kotlin.math.exp
import kotlin.math.sqrt

/** Thrown when an .npz file is missing data, inconsistent, or malformed. */
class ModelLoadException(message: String) : Exception(message)

/** Sampling knobs for [LoadedModel.generate]; defaults match TAIT's `generate`. */
data class GenerationConfig(
    val maxNewTokens: Int = 60,
    val temperature: Float = 0.7f,
    val topK: Int = 8,
    val topP: Float = 0.9f,
)

data class GeneratedText(
    /** Text between `<think>` and `</think>`, or "" if the model wrote no think tags. */
    val reasoning: String,
    /** The answer: text after `</think>` (or the whole text), inside `<answer>` tags if present. */
    val answer: String,
    /** The full generated text (prompt excluded, SEP/END stripped) before tag extraction. */
    val raw: String,
    /** Number of new tokens sampled (including END if it stopped the run). */
    val newTokens: Int,
    /** Wall time of the generation loop in milliseconds. */
    val millis: Long,
)

/** Row-major matrix: [rows] x [cols], data in row-major order. Internal. */
internal class Mat(val rows: Int, val cols: Int, val data: FloatArray) {
    operator fun get(r: Int, c: Int): Float = data[r * cols + c]
}

internal fun matmul(a: Mat, b: Mat): Mat {
    require(a.cols == b.rows) { "matmul shape mismatch: (${a.rows}x${a.cols}) x (${b.rows}x${b.cols})" }
    val out = FloatArray(a.rows * b.cols)
    for (i in 0 until a.rows) {
        for (k in 0 until a.cols) {
            val aik = a[i, k]
            if (aik == 0f) continue
            for (j in 0 until b.cols) {
                out[i * b.cols + j] += aik * b[k, j]
            }
        }
    }
    return Mat(a.rows, b.cols, out)
}

/** Numerically stable softmax: subtract the row max, divide by (sum + 1e-9). Matches TAIT. */
internal fun softmaxRow(row: FloatArray): FloatArray {
    val m = row.max()
    val e = FloatArray(row.size) { exp((row[it] - m).toDouble()).toFloat() }
    var sum = 0f
    for (v in e) sum += v
    val denom = sum + 1e-9f
    return FloatArray(row.size) { e[it] / denom }
}

/**
 * A loaded TAIT MiniGPT: one transformer block, no LayerNorm, no Dropout,
 * ReLU FFN — exactly the architecture in `tait/core/model.py`.
 *
 * Create via [KaitEngine.load]; all shapes are read from the file, never
 * hard-coded.
 */
class LoadedModel internal constructor(
    private val tokenizer: Tokenizer,
    private val wte: Mat,
    private val wpe: Mat,
    private val wq: Mat,
    private val wk: Mat,
    private val wv: Mat,
    private val wo: Mat,
    private val w1: Mat,
    private val b1: FloatArray,
    private val w2: Mat,
    private val b2: FloatArray,
    private val whead: Mat,
    private val bhead: FloatArray,
) {
    val vocabSize: Int get() = wte.rows
    val dModel: Int get() = wte.cols
    val dFf: Int get() = w1.cols
    val blockSize: Int get() = wpe.rows
    val sepId: Int get() = tokenizer.sepId
    val endId: Int get() = tokenizer.endId
    val padId: Int get() = tokenizer.padId

    fun encode(text: String): IntArray = tokenizer.encode(text)
    /** Decode ids to display text (SEP / END / pad ids are skipped). */
    fun decode(ids: IntArray): String = tokenizer.decode(ids)

    /**
     * Forward pass on [ids] with an all-true valid mask, returning the
     * next-token probabilities at the last position — TAIT's `forward_single`
     * (which first takes the last [blockSize] ids; that truncation is done
     * here too, so longer inputs are safe).
     */
    fun forwardProbs(ids: IntArray): FloatArray {
        require(ids.isNotEmpty()) { "forwardProbs needs at least one token id" }
        val cond = if (ids.size > blockSize) ids.copyOfRange(ids.size - blockSize, ids.size) else ids
        return forward(cond, BooleanArray(cond.size) { true })[cond.size - 1]
    }

    /**
     * Full forward pass, in TAIT's exact order:
     * h0 = wte[ids] + wpe[pos]; Q/K/V; scores / sqrt(d_model) with a causal
     * mask (j > i) plus a pad mask on invalid key positions (masked scores
     * become -1e9); stable softmax; attn @ Wo + residual; ReLU FFN + residual;
     * head + softmax. Returns per-position probabilities (T x vocab).
     */
    fun forward(ids: IntArray, validMask: BooleanArray): Array<FloatArray> {
        val t = ids.size
        require(validMask.size == t) { "validMask size ${validMask.size} != ids size $t" }
        require(t <= blockSize) { "sequence length $t exceeds block_size $blockSize" }
        val d = dModel
        for (id in ids) require(id in 0 until vocabSize) { "token id $id out of range [0, $vocabSize)" }

        // h0 = wte[ids] + wpe[pos]
        val h0data = FloatArray(t * d) { idx ->
            val row = idx / d
            val col = idx % d
            wte[ids[row], col] + wpe[row, col]
        }
        val h0 = Mat(t, d, h0data)
        val q = matmul(h0, wq)
        val k = matmul(h0, wk)
        val v = matmul(h0, wv)

        // scores = Q @ K^T / sqrt(d), causal + pad mask -> -1e9
        val scale = sqrt(d.toFloat())
        val attnData = FloatArray(t * t)
        for (i in 0 until t) {
            val row = FloatArray(t)
            for (j in 0 until t) {
                var s = 0f
                for (dd in 0 until d) s += q[i, dd] * k[j, dd]
                s /= scale
                if (j > i || !validMask[j]) s = -1e9f
                row[j] = s
            }
            val sm = softmaxRow(row)
            for (j in 0 until t) attnData[i * t + j] = sm[j]
        }
        val attn = matmul(Mat(t, t, attnData), v)
        val attnProj = matmul(attn, wo)
        val h1data = FloatArray(t * d) { h0data[it] + attnProj.data[it] }
        val h1 = Mat(t, d, h1data)

        // FFN: pre1 = h1 @ W1 + b1; ff1 = relu(pre1); ff2 = ff1 @ W2 + b2; h2 = h1 + ff2
        val pre1 = matmul(h1, w1)
        for (i in 0 until pre1.rows) {
            for (j in 0 until pre1.cols) pre1.data[i * pre1.cols + j] += b1[j]
        }
        val ff1data = FloatArray(pre1.data.size) { maxOf(0f, pre1.data[it]) }
        val ff2 = matmul(Mat(pre1.rows, pre1.cols, ff1data), w2)
        for (i in 0 until ff2.rows) {
            for (j in 0 until ff2.cols) ff2.data[i * ff2.cols + j] += b2[j]
        }
        val h2data = FloatArray(t * d) { h1data[it] + ff2.data[it] }
        val h2 = Mat(t, d, h2data)

        // logits = h2 @ Whead + bhead; probs = softmax(logits)
        val logits = matmul(h2, whead)
        for (i in 0 until logits.rows) {
            for (j in 0 until logits.cols) logits.data[i * logits.cols + j] += bhead[j]
        }
        return Array(t) { row ->
            softmaxRow(FloatArray(vocabSize) { logits[row, it] })
        }
    }

    /**
     * Generate a reply, step for step like TAIT's `MiniGPT.generate`:
     * ids = encode(prompt + SEP); each step takes the last [blockSize] ids,
     * gets the last-position probs, zeroes the pad probability and
     * renormalises, applies temperature (`p^(1/T)`, renormalised), keeps the
     * top-[config.topK] tokens, keeps the smallest set whose cumulative
     * probability reaches [config.topP], renormalises, and samples with
     * `Random(seed)` (a fresh non-seeded Random when [seed] is null).
     * Stops at END or after [config.maxNewTokens] new tokens.
     *
     * The returned [GeneratedText.raw] is the generated text with the prompt
     * and the SEP/END framing removed (like TAIT's `generate_reply`); the
     * `<think>` / `<answer>` tags inside it are split into [GeneratedText.reasoning]
     * and [GeneratedText.answer].
     */
    fun generate(prompt: String, config: GenerationConfig = GenerationConfig(), seed: Long? = null): GeneratedText {
        val start = System.currentTimeMillis()
        val rng = if (seed != null) kotlin.random.Random(seed) else kotlin.random.Random.Default
        val ids = ArrayList<Int>()
        for (id in tokenizer.encode(prompt)) ids.add(id)
        ids.add(sepId)
        var newTokens = 0
        for (step in 0 until config.maxNewTokens) {
            val cond = ids.takeLast(blockSize).toIntArray()
            val probs = forwardProbs(cond)
            // Sampling pipeline (Generation.kt), step for step like TAIT:
            // zero pad -> renormalise -> temperature -> top_k -> top_p -> renormalise.
            val p = sampleDistribution(probs, padId, config)
                ?: break
            val nextId = sampleToken(p, rng)
            ids.add(nextId)
            newTokens++
            if (nextId == endId) break
        }
        val generatedIds = ids.drop(ids.size - newTokens).toIntArray()
        val rawFull = tokenizer.decodeRaw(generatedIds)
        var raw = rawFull
        // Same framing removal as TAIT's generate_reply: split off SEP, cut at END.
        if ('\u0001' in raw) raw = raw.substringAfter('\u0001')
        if ('\u0002' in raw) raw = raw.substringBefore('\u0002')
        val (reasoning, answer) = extractReasoning(raw)
        return GeneratedText(
            reasoning = reasoning,
            answer = answer,
            raw = raw,
            newTokens = newTokens,
            millis = System.currentTimeMillis() - start,
        )
    }
}

/** Entry point for the engine: load a TAIT .npz model from any [InputStream]. */
object KaitEngine {
    fun load(input: InputStream): LoadedModel {
        val entries = readNpz(input)
        fun mat(name: String): Mat {
            val n = entries[name] ?: throw ModelLoadException("missing weight '$name' in npz")
            if (n.shape.size != 2) {
                throw ModelLoadException("weight '$name' must be 2-D, got shape ${n.shape}")
            }
            return Mat(n.shape[0], n.shape[1], n.requireFloats(name))
        }
        fun vec(name: String): FloatArray {
            val n = entries[name] ?: throw ModelLoadException("missing bias '$name' in npz")
            if (n.shape.size != 1) {
                throw ModelLoadException("bias '$name' must be 1-D, got shape ${n.shape}")
            }
            return n.requireFloats(name)
        }
        fun scalar(name: String): Long {
            val n = entries[name] ?: throw ModelLoadException("missing scalar '$name' in npz")
            return n.scalarLong()
        }

        val wte = mat("wte")
        val wpe = mat("wpe")
        val wq = mat("Wq")
        val wk = mat("Wk")
        val wv = mat("Wv")
        val wo = mat("Wo")
        val w1 = mat("W1")
        val b1 = vec("b1")
        val w2 = mat("W2")
        val b2 = vec("b2")
        val whead = mat("Whead")
        val bhead = vec("bhead")

        val dModel = wte.cols
        val dFf = w1.cols
        val blockSize = scalar("block_size").toInt()
        val sepId = scalar("sep_id").toInt()
        val endId = scalar("end_id").toInt()
        val tokPadId = if ("tok_pad_id" in entries) scalar("tok_pad_id").toInt() else null
        val padId = if ("pad_id" in entries) scalar("pad_id").toInt()
            else tokPadId ?: throw ModelLoadException("missing scalar 'pad_id'/'tok_pad_id' in npz")

        // Shape consistency, all read from the file.
        fun check(cond: Boolean, msg: String) {
            if (!cond) throw ModelLoadException(msg)
        }
        check(wpe.cols == dModel, "wpe cols ${wpe.cols} != d_model $dModel")
        check(wpe.rows == blockSize, "wpe rows ${wpe.rows} != block_size $blockSize")
        check(wq.rows == dModel && wq.cols == dModel, "Wq shape ${wq.rows}x${wq.cols} != ${dModel}x$dModel")
        check(wk.rows == dModel && wk.cols == dModel, "Wk shape ${wk.rows}x${wk.cols} != ${dModel}x$dModel")
        check(wv.rows == dModel && wv.cols == dModel, "Wv shape ${wv.rows}x${wv.cols} != ${dModel}x$dModel")
        check(wo.rows == dModel && wo.cols == dModel, "Wo shape ${wo.rows}x${wo.cols} != ${dModel}x$dModel")
        check(w1.rows == dModel, "W1 rows ${w1.rows} != d_model $dModel")
        check(b1.size == dFf, "b1 size ${b1.size} != d_ff $dFf")
        check(w2.rows == dFf && w2.cols == dModel, "W2 shape ${w2.rows}x${w2.cols} != ${dFf}x$dModel")
        check(b2.size == dModel, "b2 size ${b2.size} != d_model $dModel")
        check(whead.rows == dModel && whead.cols == wte.rows, "Whead shape ${whead.rows}x${whead.cols} != ${dModel}x${wte.rows}")
        check(bhead.size == wte.rows, "bhead size ${bhead.size} != vocab ${wte.rows}")
        check(sepId in 0 until wte.rows && endId in 0 until wte.rows && padId in 0 until wte.rows,
            "special ids out of range: sep=$sepId end=$endId pad=$padId vocab=${wte.rows}")
        if (tokPadId != null && tokPadId != padId) {
            throw ModelLoadException("inconsistent pad ids: tok_pad_id=$tokPadId but pad_id=$padId")
        }

        val mergeA = entries["merge_a"]?.requireInts("merge_a")
            ?: throw ModelLoadException("missing BPE merge data 'merge_a' in npz")
        val mergeB = entries["merge_b"]?.requireInts("merge_b")
            ?: throw ModelLoadException("missing BPE merge data 'merge_b' in npz")
        val mergeId = entries["merge_id"]?.requireInts("merge_id")
            ?: throw ModelLoadException("missing BPE merge data 'merge_id' in npz")
        val tokenizer = Tokenizer.fromNpz(mergeA, mergeB, mergeId, sepId, endId, padId, wte.rows)

        return LoadedModel(tokenizer, wte, wpe, wq, wk, wv, wo, w1, b1, w2, b2, whead, bhead)
    }
}
