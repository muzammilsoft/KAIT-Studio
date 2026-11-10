package com.kgsoft.kait.studio.engine

/**
 * Split generated text into reasoning and answer, exactly like TAIT's
 * `extract_generation` (`tait/core/reasoning.py`):
 *
 * - If `<think>` is present, the text between it and `</think>` is the
 *   reasoning and everything after `</think>` is the answer (if there is no
 *   `</think>`, everything after `<think>` is the answer and the reasoning
 *   is empty).
 * - If `<answer>` is present in the answer, the text after it — up to
 *   `</answer>` if present — is the answer.
 * - With no tags at all, the reasoning is empty and the answer is the
 *   whole text.
 *
 * Both parts are trimmed, as in TAIT.
 */
fun extractReasoning(text: String): Pair<String, String> {
    var reasoning = ""
    var answer = text
    if ("<think>" in text) {
        val part = text.substringAfter("<think>", "")
        if ("</think>" in part) {
            reasoning = part.substringBefore("</think>").trim()
            answer = part.substringAfter("</think>", "")
        } else {
            answer = part
        }
    }
    if ("<answer>" in answer) {
        answer = answer.substringAfter("<answer>", "")
    }
    if ("</answer>" in answer) {
        answer = answer.substringBefore("</answer>")
    }
    return reasoning.trim() to answer.trim()
}
