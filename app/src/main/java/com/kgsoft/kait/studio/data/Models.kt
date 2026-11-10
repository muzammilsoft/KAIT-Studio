package com.kgsoft.kait.studio.data

/** One entry of the curated models gallery (assets/gallery.json). */
data class GalleryModel(
    val id: String,
    val name: String,
    val repo: String,
    val description: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val vocabSize: Int,
    val dModel: Int,
    val dFf: Int,
    val blockSize: Int,
    val examples: Int,
    val epochs: Int,
    val finalLoss: Double,
    val valLoss: Double,
)

/** One entry of the curated datasets gallery (assets/datasets.json). */
data class GalleryDataset(
    val id: String,
    val name: String,
    val description: String,
    val sourceUrl: String,
    val downloadUrl: String,
    val fields: List<String>,
    val lineCount: Int,
    val sample: List<Map<String, String>>,
)

/** One public model found on the Hugging Face Hub (live search). */
data class HubModel(
    val id: String,
    val downloads: Int,
    val likes: Int,
)

/** One chat message. The model's thinking is kept separate from its answer. */
data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val reasoning: String = "",
    val millis: Long = 0,
    val newTokens: Int = 0,
    val isError: Boolean = false,
)
