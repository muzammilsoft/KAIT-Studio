package com.kgsoft.kait.studio.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * JVM parity + behaviour tests for the KAIT engine.
 *
 * Golden files (copied from ~/workspace/kait-npz-test, never modified there):
 * - tiny.npz: untrained MiniGPT saved by TAIT (vocab 303, d_model 16,
 *   d_ff 32, block_size 53, 12 merges; prompt "Hello" -> ids [72, 274, 282]).
 * - ref_last.npy: the last-position probabilities of TAIT's Python forward
 *   on those ids, saved by ref.py.
 */
class EngineTest {

    private fun loadTiny(): LoadedModel {
        val stream = javaClass.classLoader.getResourceAsStream("tiny.npz")
            ?: error("test resource tiny.npz not found on classpath")
        return stream.use { KaitEngine.load(it) }
    }

    private fun loadRefLast(): FloatArray {
        val stream = javaClass.classLoader.getResourceAsStream("ref_last.npy")
            ?: error("test resource ref_last.npy not found on classpath")
        val bytes = stream.use { it.readAllBytes() }
        return parseNpy(bytes).floats!!
    }

    @Test
    fun forwardMatchesPython() {
        val model = loadTiny()
        assertEquals(303, model.vocabSize)
        assertEquals(16, model.dModel)
        assertEquals(32, model.dFf)
        assertEquals(53, model.blockSize)

        // Prompt "Hello" from ref.py.
        val ids = intArrayOf(72, 274, 282)
        val probs = model.forwardProbs(ids)
        val ref = loadRefLast()
        assertEquals(ref.size, probs.size, "vocab size mismatch vs reference")

        var maxDiff = 0f
        var kotlinArgmax = 0
        var refArgmax = 0
        for (i in probs.indices) {
            val d = abs(probs[i] - ref[i])
            if (d > maxDiff) maxDiff = d
            if (probs[i] > probs[kotlinArgmax]) kotlinArgmax = i
            if (ref[i] > ref[refArgmax]) refArgmax = i
        }
        val top5 = probs.indices.sortedByDescending { probs[it] }.take(5)
        println("Kotlin top5: " + top5.joinToString { "${it}=${probs[it]}" })
        println("Python top5: " + ref.indices.sortedByDescending { ref[it] }.take(5)
            .joinToString { "${it}=${ref[it]}" })
        println("maxAbsDiff=$maxDiff kotlinArgmax=$kotlinArgmax refArgmax=$refArgmax")
        println(if (maxDiff < 1e-3f && kotlinArgmax == refArgmax) "PASS" else "FAIL")
        assertTrue(maxDiff < 1e-3f, "forward max abs diff $maxDiff exceeds 1e-3")
        assertEquals(refArgmax, kotlinArgmax, "argmax mismatch: Kotlin=$kotlinArgmax Python=$refArgmax")
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val model = loadTiny()
        val texts = listOf(
            "Hello world",
            "The quick brown fox jumps over the lazy dog.",
            "مرحبا بالعالم",
            "الذكاء الاصطناعي على الهاتف",
            "Hello مرحبا 123!",
        )
        for (text in texts) {
            val ids = model.encode(text)
            val back = model.decode(ids)
            println("roundtrip: '$text' -> ${ids.toList()} -> '$back'")
            assertEquals(text, back, "round-trip failed for '$text'")
        }
        // SEP / END are separators, not display text: encode handles them as
        // single special ids and decode skips them.
        val withSep = model.encode("a\u0001b\u0002c")
        assertTrue(model.sepId in withSep.toList(), "SEP char must encode to sepId")
        assertTrue(model.endId in withSep.toList(), "END char must encode to endId")
        assertEquals("abc", model.decode(withSep))
    }

    @Test
    fun reasoningExtraction() {
        val (r1, a1) = extractReasoning("<think>خطوات</think><answer>الجواب</answer>")
        assertEquals("خطوات", r1)
        assertEquals("الجواب", a1)

        val (r2, a2) = extractReasoning("just an answer, no tags")
        assertEquals("", r2)
        assertEquals("just an answer, no tags", a2)

        val (r3, a3) = extractReasoning("<think>only thinking, no end tag")
        assertEquals("", r3)
        assertEquals("only thinking, no end tag", a3)

        val (r4, a4) = extractReasoning("<answer>wrapped answer</answer>")
        assertEquals("", r4)
        assertEquals("wrapped answer", a4)
        println("PASS reasoning extraction")
    }

    @Test
    fun missingMergeDataIsRejected() {
        val tinyBytes = javaClass.classLoader.getResourceAsStream("tiny.npz")!!.use { it.readAllBytes() }
        // Rebuild the zip without merge_a (everything else untouched).
        val stripped = ByteArrayOutputStream()
        ZipOutputStream(stripped).use { out ->
            ZipInputStream(ByteArrayInputStream(tinyBytes)).use { inp ->
                var entry = inp.nextEntry
                while (entry != null) {
                    if (entry.name != "merge_a.npy") {
                        out.putNextEntry(ZipEntry(entry.name))
                        out.write(inp.readAllBytes())
                        out.closeEntry()
                    }
                    inp.closeEntry()
                    entry = inp.nextEntry
                }
            }
        }
        val ex = assertFailsWith<ModelLoadException> {
            KaitEngine.load(ByteArrayInputStream(stripped.toByteArray()))
        }
        println("PASS missing merge_a rejected: ${ex.message}")
    }

    @Test
    fun generateIsDeterministicForSameSeed() {
        val model = loadTiny()
        val config = GenerationConfig(maxNewTokens = 10, temperature = 0.7f, topK = 8, topP = 0.9f)
        val first = model.generate("Hello", config, seed = 42L)
        val second = model.generate("Hello", config, seed = 42L)
        println("generate(seed=42): newTokens=${first.newTokens} millis=${first.millis} raw='${first.raw}'")
        assertEquals(first.raw, second.raw, "same seed must give the same text")
        assertEquals(first.newTokens, second.newTokens)
        assertTrue(first.newTokens in 1..10)
        // Reasoning/answer split must be consistent with raw.
        val (r, a) = extractReasoning(first.raw)
        assertEquals(r, first.reasoning)
        assertEquals(a, first.answer)
        println("PASS generate determinism")
    }
}
