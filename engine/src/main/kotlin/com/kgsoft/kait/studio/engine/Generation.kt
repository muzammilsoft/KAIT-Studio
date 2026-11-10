package com.kgsoft.kait.studio.engine

import kotlin.math.pow
import kotlin.random.Random

/**
 * The sampling pipeline of TAIT's `MiniGPT.generate` (`tait/core/model.py`),
 * factored out of [LoadedModel.generate] so each step can be read and tested
 * on its own. Order matters and matches TAIT exactly:
 *
 * 1. Zero the pad probability, renormalise (`probs[pad] = 0; probs /= sum`).
 * 2. Temperature: `p^(1/T)`, renormalise (T clamped to >= 1e-6, as in TAIT).
 * 3. top_k: keep the k largest probabilities, zero the rest.
 * 4. top_p: sort descending, keep the smallest prefix whose cumulative
 *    probability reaches top_p (TAIT: `cutoff = searchsorted(cum, top_p) + 1`).
 * 5. Renormalise.
 *
 * One deliberate difference from TAIT: the random draw uses Kotlin's
 * [Random] (seeded with the caller's seed) instead of NumPy's
 * `default_rng`. The distribution at every step is identical; the sampled
 * token for a given seed is not — NumPy and Kotlin simply have different
 * RNGs. Forward-probability parity (the pass bar) is unaffected.
 */
internal fun sampleDistribution(probs: FloatArray, padId: Int, config: GenerationConfig): DoubleArray? {
    val p = DoubleArray(probs.size) { probs[it].toDouble() }
    p[padId] = 0.0
    var sum = p.sum()
    if (sum <= 0.0) return null
    for (i in p.indices) p[i] /= sum

    val temp = maxOf(config.temperature.toDouble(), 1e-6)
    sum = 0.0
    for (i in p.indices) {
        p[i] = p[i].pow(1.0 / temp)
        sum += p[i]
    }
    if (sum <= 0.0) return null
    for (i in p.indices) p[i] /= sum

    if (config.topK > 0 && config.topK < p.size) {
        val keep = p.indices.sortedByDescending { p[it] }.take(config.topK).toSet()
        for (i in p.indices) if (i !in keep) p[i] = 0.0
    }

    if (config.topP > 0f && config.topP < 1f) {
        val order = p.indices.sortedByDescending { p[it] }
        var cum = 0.0
        var cutoff = order.size
        for (idx in order.indices) {
            cum += p[order[idx]]
            if (cum >= config.topP.toDouble()) {
                cutoff = idx + 1
                break
            }
        }
        val keep = order.take(cutoff).toSet()
        for (i in p.indices) if (i !in keep) p[i] = 0.0
    }

    sum = p.sum()
    if (sum <= 0.0) return null
    for (i in p.indices) p[i] /= sum
    return p
}

/** Draw one token id from a normalised distribution with a cumulative draw. */
internal fun sampleToken(p: DoubleArray, rng: Random): Int {
    val draw = rng.nextDouble()
    var acc = 0.0
    for (i in p.indices) {
        acc += p[i]
        if (draw < acc) return i
    }
    return p.size - 1
}
