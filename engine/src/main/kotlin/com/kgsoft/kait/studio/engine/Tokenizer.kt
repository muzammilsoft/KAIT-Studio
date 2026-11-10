package com.kgsoft.kait.studio.engine

/**
 * Byte-level BPE tokenizer rebuilt from a TAIT .npz file, exactly as TAIT does
 * in `tait/core/tokenizer.py` ([BPETokenizer.load] path):
 *
 * 1. vocab 0..255 are the single bytes 0..255.
 * 2. Walk the merge triples (merge_a, merge_b, merge_id) **in file order** and
 *    set `vocab[new_id] = vocab[a] + vocab[b]` — file order matters because a
 *    later merge may use an earlier merged token.
 * 3. The three special ids (sep / end / pad) sit after the merges and have no
 *    byte content: `vocabSize` must equal `256 + merges + 3`, otherwise the
 *    file is inconsistent and loading fails.
 *
 * Encoding is TAIT's rule: text -> UTF-8 bytes, then repeatedly merge the
 * adjacent pair with the **lowest** merge id until no known pair remains.
 * A literal SEP (`\x01`) or END (`\x02`) character in the input becomes the
 * single special id instead of being byte-encoded.
 *
 * Decoding concatenates the vocab bytes of each id and decodes the result as
 * UTF-8. Special ids are separators, not bytes: [decode] skips SEP and END
 * (and pad) so the displayed text never contains `\x01` / `\x02`;
 * [decodeRaw] keeps them, matching TAIT's `decode` which re-inserts them.
 */
class Tokenizer internal constructor(
    /** vocab[id] = the bytes that id stands for; empty for the 3 special ids. */
    val vocab: List<ByteArray>,
    /** (a, b) -> new id, the merge table in file order. */
    val merges: Map<Pair<Int, Int>, Int>,
    val sepId: Int,
    val endId: Int,
    val padId: Int,
) {
    val vocabSize: Int get() = vocab.size

    /** Encode [text] to token ids (no SEP appended — callers add it). */
    fun encode(text: String): IntArray {
        val result = ArrayList<Int>()
        val buf = StringBuilder()
        fun flush() {
            if (buf.isNotEmpty()) {
                result.addAll(encodeChunk(buf.toString().toByteArray(Charsets.UTF_8)))
                buf.clear()
            }
        }
        for (ch in text) {
            when (ch) {
                '\u0001' -> { flush(); result.add(sepId) }
                '\u0002' -> { flush(); result.add(endId) }
                else -> buf.append(ch)
            }
        }
        flush()
        return result.toIntArray()
    }

    /**
     * Decode [ids] to text, skipping SEP, END and pad ids.
     * Use this for anything shown to the user.
     */
    fun decode(ids: IntArray): String = decode(ids, keepSpecials = false)

    /**
     * Decode [ids] to text, keeping SEP (`\x01`) and END (`\x02`) characters
     * in place and skipping only pad ids — the same behaviour as TAIT's
     * `BPETokenizer.decode`.
     */
    fun decodeRaw(ids: IntArray): String = decode(ids, keepSpecials = true)

    private fun decode(ids: IntArray, keepSpecials: Boolean): String {
        val out = ArrayList<Byte>()
        val parts = StringBuilder()
        fun flushBytes() {
            if (out.isNotEmpty()) {
                parts.append(String(out.toByteArray(), Charsets.UTF_8))
                out.clear()
            }
        }
        for (id in ids) {
            when (id) {
                sepId -> {
                    flushBytes()
                    if (keepSpecials) parts.append('\u0001')
                }
                endId -> {
                    flushBytes()
                    if (keepSpecials) parts.append('\u0002')
                }
                padId -> { /* skipped in both modes, as in TAIT */ }
                else -> {
                    val bytes = vocab.getOrNull(id) ?: ByteArray(0)
                    for (b in bytes) out.add(b)
                }
            }
        }
        flushBytes()
        return parts.toString()
    }

    /** One BPE chunk: UTF-8 bytes merged by lowest merge id, as in TAIT. */
    private fun encodeChunk(bytes: ByteArray): List<Int> {
        val ids = ArrayList<Int>(bytes.size)
        for (b in bytes) ids.add(b.toInt() and 0xff)
        while (ids.size >= 2) {
            var bestPair: Pair<Int, Int>? = null
            var bestId = Int.MAX_VALUE
            for (i in 0 until ids.size - 1) {
                val pair = ids[i] to ids[i + 1]
                val mergeId = merges[pair]
                if (mergeId != null && mergeId < bestId) {
                    bestId = mergeId
                    bestPair = pair
                }
            }
            val pair = bestPair ?: break
            val merged = ArrayList<Int>(ids.size - 1)
            var i = 0
            while (i < ids.size) {
                if (i < ids.size - 1 && ids[i] == pair.first && ids[i + 1] == pair.second) {
                    merged.add(bestId)
                    i += 2
                } else {
                    merged.add(ids[i])
                    i += 1
                }
            }
            ids.clear()
            ids.addAll(merged)
        }
        return ids
    }

    companion object {
        /**
         * Rebuild a tokenizer from the merge arrays and special ids of an npz.
         * [vocabSize] is the first dimension of wte; the check
         * `256 + merges + 3 == vocabSize` is enforced here.
         */
        fun fromNpz(
            mergeA: IntArray,
            mergeB: IntArray,
            mergeId: IntArray,
            sepId: Int,
            endId: Int,
            padId: Int,
            vocabSize: Int,
        ): Tokenizer {
            require(mergeA.size == mergeB.size && mergeB.size == mergeId.size) {
                "merge arrays have different lengths: ${mergeA.size}, ${mergeB.size}, ${mergeId.size}"
            }
            if (256 + mergeA.size + 3 != vocabSize) {
                throw ModelLoadException(
                    "inconsistent model file: 256 + ${mergeA.size} merges + 3 special tokens = " +
                        "${256 + mergeA.size + 3}, but wte has $vocabSize rows"
                )
            }
            val vocab = ArrayList<ByteArray>(vocabSize)
            for (i in 0 until vocabSize) vocab.add(ByteArray(0))
            val defined = BooleanArray(vocabSize)
            for (i in 0 until 256) {
                vocab[i] = byteArrayOf(i.toByte())
                defined[i] = true
            }
            val merges = LinkedHashMap<Pair<Int, Int>, Int>(mergeA.size)
            for (k in mergeA.indices) {
                val a = mergeA[k]
                val b = mergeB[k]
                val nid = mergeId[k]
                if (a < 0 || a >= vocabSize || b < 0 || b >= vocabSize || !defined[a] || !defined[b]) {
                    throw ModelLoadException("merge #$k references unknown token ids ($a, $b -> $nid)")
                }
                if (nid < 256 || nid >= vocabSize || defined[nid]) {
                    throw ModelLoadException("merge #$k has invalid or duplicate new id $nid")
                }
                merges[a to b] = nid
                vocab[nid] = vocab[a] + vocab[b]
                defined[nid] = true
            }
            // The 3 special ids keep their empty byte content.
            return Tokenizer(vocab, merges, sepId, endId, padId)
        }
    }
}
