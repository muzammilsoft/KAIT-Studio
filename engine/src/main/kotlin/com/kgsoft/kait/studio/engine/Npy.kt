package com.kgsoft.kait.studio.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One parsed .npy array (NumPy format version 1).
 *
 * Exactly one of [floats], [ints], [longs] is non-null, decided by [descr]:
 * - `<f4` / `|f4` -> [floats] (float32)
 * - `<f8` -> [floats] (float64 narrowed to Float, TAIT only writes float32)
 * - `<i4` / `|i4` -> [ints] (int32, BPE merges)
 * - `<i8` -> [longs] (int64, scalars such as sep_id / block_size)
 *
 * Data is row-major (C order), little-endian, as written by TAIT's np.savez.
 */
data class Npy(
    val shape: List<Int>,
    val descr: String,
    val floats: FloatArray?,
    val ints: IntArray?,
    val longs: LongArray?,
) {
    /** Number of elements: product of [shape], or 1 for a scalar (empty shape). */
    val count: Int get() = if (shape.isEmpty()) 1 else shape.fold(1) { a, b -> a * b }

    /** Value of a scalar array (empty shape, exactly one element). */
    fun scalarLong(): Long = when {
        longs != null -> longs[0]
        ints != null -> ints[0].toLong()
        floats != null -> floats[0].toLong()
        else -> throw ModelLoadException("npy array has no data")
    }

    fun requireFloats(name: String): FloatArray =
        floats ?: throw ModelLoadException("entry '$name' is not a float array (descr=$descr)")

    fun requireInts(name: String): IntArray =
        ints ?: throw ModelLoadException("entry '$name' is not an int32 array (descr=$descr)")
}

/**
 * Parse one .npy file (version 1 only) from its raw bytes.
 *
 * Layout: magic `\x93NUMPY`, version (1, 0), 2-byte little-endian header length,
 * ASCII header dict, then raw data. The header dict carries `descr`,
 * `fortran_order` and `shape`.
 *
 * @throws ModelLoadException on a bad magic, an unsupported version or descr,
 *   or `fortran_order: True` (TAIT always writes C order; Fortran order is
 *   refused with a clear error instead of being silently misread).
 */
fun parseNpy(bytes: ByteArray): Npy {
    if (bytes.size < 10 || bytes[0] != 0x93.toByte() || bytes[1] != 'N'.code.toByte() ||
        bytes[2] != 'U'.code.toByte() || bytes[3] != 'M'.code.toByte() ||
        bytes[4] != 'P'.code.toByte() || bytes[5] != 'Y'.code.toByte()
    ) {
        throw ModelLoadException("not an .npy file (bad magic)")
    }
    if (bytes[6] != 1.toByte()) {
        throw ModelLoadException("unsupported .npy version ${bytes[6]}.${bytes[7]} (only version 1 is supported)")
    }
    val headerLen = ByteBuffer.wrap(bytes, 8, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xffff
    if (10 + headerLen > bytes.size) throw ModelLoadException(".npy header length exceeds file size")
    val header = String(bytes, 10, headerLen, Charsets.US_ASCII)
    val descr = Regex("'descr':\\s*'([^']+)'").find(header)?.groupValues?.get(1)
        ?: throw ModelLoadException("cannot find 'descr' in .npy header: $header")
    if (header.contains("'fortran_order': True")) {
        throw ModelLoadException("fortran_order True is not supported (TAIT writes C order); header: $header")
    }
    val shapeStr = Regex("'shape':\\s*\\(([^)]*)\\)").find(header)?.groupValues?.get(1)
        ?: throw ModelLoadException("cannot find 'shape' in .npy header: $header")
    val shape = if (shapeStr.trim().isEmpty()) {
        emptyList()
    } else {
        shapeStr.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    val count = if (shape.isEmpty()) 1 else shape.fold(1) { a, b -> a * b }
    val off = 10 + headerLen
    if (bytes.size - off < count * elementSize(descr)) {
        throw ModelLoadException(".npy data truncated: need ${count * elementSize(descr)} bytes, have ${bytes.size - off}")
    }
    val buf = ByteBuffer.wrap(bytes, off, bytes.size - off).order(ByteOrder.LITTLE_ENDIAN)
    return when (descr) {
        "<f4", "|f4" -> Npy(shape, descr, FloatArray(count) { buf.float }, null, null)
        "<f8" -> Npy(shape, descr, FloatArray(count) { buf.double.toFloat() }, null, null)
        "<i4", "|i4" -> Npy(shape, descr, null, IntArray(count) { buf.int }, null)
        "<i8" -> Npy(shape, descr, null, null, LongArray(count) { buf.long })
        else -> throw ModelLoadException("unsupported descr '$descr' in .npy header: $header")
    }
}

private fun elementSize(descr: String): Int = when (descr) {
    "<f4", "|f4", "<i4", "|i4" -> 4
    "<f8", "<i8" -> 8
    else -> throw ModelLoadException("unsupported descr '$descr'")
}
