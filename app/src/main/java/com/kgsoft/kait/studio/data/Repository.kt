package com.kgsoft.kait.studio.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Local galleries (bundled JSON) plus the live Hugging Face Hub API.
 * All network calls run on Dispatchers.IO with short timeouts; nothing
 * here ever needs an account or a token.
 */
object Repository {

    fun loadGalleryModels(context: Context): List<GalleryModel> {
        val text = context.assets.open("gallery.json").bufferedReader().readText()
        val root = JSONObject(text)
        val out = mutableListOf<GalleryModel>()
        val arr = root.optJSONArray("models") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                GalleryModel(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    repo = o.optString("repo"),
                    description = o.optString("description"),
                    downloadUrl = o.optString("download_url"),
                    fileName = o.optString("file"),
                    sizeBytes = o.optLong("size_bytes"),
                    sha256 = o.optString("sha256"),
                    vocabSize = o.optInt("vocab_size"),
                    dModel = o.optInt("d_model"),
                    dFf = o.optInt("d_ff"),
                    blockSize = o.optInt("block_size"),
                    examples = o.optInt("examples"),
                    epochs = o.optInt("epochs"),
                    finalLoss = o.optDouble("final_loss"),
                    valLoss = o.optDouble("val_loss"),
                )
            )
        }
        return out
    }

    fun loadGalleryDatasets(context: Context): List<GalleryDataset> {
        val text = context.assets.open("datasets.json").bufferedReader().readText()
        val root = JSONObject(text)
        val out = mutableListOf<GalleryDataset>()
        val arr = root.optJSONArray("datasets") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val fields = mutableListOf<String>()
            val fArr = o.optJSONArray("fields") ?: JSONArray()
            for (j in 0 until fArr.length()) fields.add(fArr.optString(j))
            val sample = mutableListOf<Map<String, String>>()
            val sArr = o.optJSONArray("sample") ?: JSONArray()
            for (j in 0 until sArr.length()) {
                val s = sArr.getJSONObject(j)
                val map = mutableMapOf<String, String>()
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = s.optString(k)
                }
                sample.add(map)
            }
            out.add(
                GalleryDataset(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    description = o.optString("description"),
                    sourceUrl = o.optString("source_url"),
                    downloadUrl = o.optString("download_url"),
                    fields = fields,
                    lineCount = o.optInt("line_count"),
                    sample = sample,
                )
            )
        }
        return out
    }

    /** Live public search on the Hugging Face Hub. No login, no token. */
    suspend fun searchHubModels(query: String): List<HubModel> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://huggingface.co/api/models?search=$encoded&limit=20")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader().readText()
            val arr = JSONArray(text)
            val out = mutableListOf<HubModel>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    HubModel(
                        id = o.optString("id"),
                        downloads = o.optInt("downloads"),
                        likes = o.optInt("likes"),
                    )
                )
            }
            out
        } finally {
            connection.disconnect()
        }
    }

    /** Download a file with progress callbacks (downloaded bytes, total or -1). */
    suspend fun downloadFile(
        url: String,
        dest: File,
        onProgress: (Long, Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun listLocalModels(modelsDir: File): List<File> {
        if (!modelsDir.exists()) return emptyList()
        return modelsDir.listFiles { f -> f.name.endsWith(".npz") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
