package com.kgsoft.kait.studio.engine

import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Read a TAIT .npz file (a zip of .npy entries) from an [InputStream].
 *
 * TAIT writes uncompressed (stored) entries with `np.savez`, but a future
 * export may use `np.savez_compressed` (deflate). [ZipInputStream] inflates
 * both, so both are accepted here.
 *
 * @return map from entry name without the `.npy` suffix to its parsed array.
 * @throws ModelLoadException if the stream is not a zip, or any entry is not
 *   a valid .npy file.
 */
fun readNpz(input: InputStream): Map<String, Npy> {
    val out = LinkedHashMap<String, Npy>()
    ZipInputStream(input).use { zip ->
        var entry = zip.nextEntry
        var sawAny = false
        while (entry != null) {
            if (!entry.isDirectory) {
                sawAny = true
                val name = entry.name.removeSuffix(".npy")
                // NOTE: do NOT use InputStream.readAllBytes() here. It is a
                // Java 9 API that Android only provides from API 33, and it
                // crashes with NoSuchMethodError on older phones (seen on
                // Android 10 / API 29). Kotlin's readBytes() loops with a
                // buffer and works on every API level we support.
                val bytes = zip.readBytes()
                out[name] = try {
                    parseNpy(bytes)
                } catch (e: ModelLoadException) {
                    throw ModelLoadException("entry '${entry.name}': ${e.message}")
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        if (!sawAny) throw ModelLoadException("npz file contains no entries")
    }
    return out
}
