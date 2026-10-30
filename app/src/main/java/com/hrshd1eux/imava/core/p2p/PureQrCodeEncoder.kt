package com.hrshd1eux.imava.core.p2p

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Pure Kotlin, standalone QR Code Generator (ISO/IEC 18004).
 * Completely zero external dependencies, zero Google libraries, zero proprietary code.
 * Generates standards-compliant QR Code bitmaps for local LAN URLs.
 */
object PureQrCodeEncoder {

    // Galois Field GF(256) tables with primitive polynomial 0x11D (x^8 + x^4 + x^3 + x^2 + 1)
    private val expTable = IntArray(512)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            expTable[i + 255] = x
            logTable[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        logTable[0] = 0
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[(logTable[x] + logTable[y]) % 255]
    }

    // EC Level M parameters for Versions 1 to 6
    private data class VersionInfo(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignmentCoords: IntArray
    )

    private val versionTable = arrayOf(
        VersionInfo(1, 16, 10, 1, intArrayOf()),
        VersionInfo(2, 28, 16, 1, intArrayOf(6, 18)),
        VersionInfo(3, 44, 26, 1, intArrayOf(6, 22)),
        VersionInfo(4, 64, 18, 2, intArrayOf(6, 26)),
        VersionInfo(5, 86, 24, 2, intArrayOf(6, 30)),
        VersionInfo(6, 108, 28, 2, intArrayOf(6, 34))
    )

    fun encodeToBitmap(
        content: String,
        targetSizePx: Int = 420,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        val modules = encodeToMatrix(content) ?: return null
        val moduleCount = modules.size
        val scale = maxOf(1, targetSizePx / moduleCount)
        val quietZone = 2
        val totalModules = moduleCount + quietZone * 2
        val bitmapSize = totalModules * scale

        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmapSize * bitmapSize) { backgroundColor }

        for (r in 0 until moduleCount) {
            val startY = (r + quietZone) * scale
            for (c in 0 until moduleCount) {
                if (modules[r][c]) {
                    val startX = (c + quietZone) * scale
                    for (y in startY until startY + scale) {
                        val rowOffset = y * bitmapSize
                        for (x in startX until startX + scale) {
                            pixels[rowOffset + x] = foregroundColor
                        }
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, bitmapSize, 0, 0, bitmapSize, bitmapSize)
        return bitmap
    }

    fun encodeToMatrix(content: String): Array<BooleanArray>? {
        if (content.isBlank()) return null
        val utf8Bytes = content.toByteArray(Charsets.UTF_8)
        val payloadLen = utf8Bytes.size

        // Byte mode requires: 4 bits mode + 8 bits length + payload bytes + up to 4 bits terminator
        val requiredDataBits = 4 + 8 + (payloadLen * 8)
        val requiredDataBytes = (requiredDataBits + 7) / 8

        val versionInfo = versionTable.firstOrNull { it.totalDataCodewords >= requiredDataBytes }
            ?: return null // Content exceeds Version 6 capacity (~100 bytes)

        val v = versionInfo.version
        val size = 17 + 4 * v

        // 1. Build data bit stream
        val bitBuffer = mutableListOf<Boolean>()
        fun putBits(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                bitBuffer.add(((value shr i) and 1) == 1)
            }
        }

        // Mode: 0100 (Byte mode)
        putBits(0b0100, 4)
        // Character count: 8 bits for versions 1-9
        putBits(payloadLen, 8)
        // Data bytes
        for (b in utf8Bytes) {
            putBits(b.toInt() and 0xFF, 8)
        }
        // Terminator: up to 4 zeros
        val maxDataBits = versionInfo.totalDataCodewords * 8
        val terminatorLen = minOf(4, maxDataBits - bitBuffer.size)
        for (i in 0 until terminatorLen) {
            bitBuffer.add(false)
        }
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(false)
        }
        // Pad with alternating 0xEC and 0x11
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < maxDataBits) {
            putBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(versionInfo.totalDataCodewords)
        for (i in dataCodewords.indices) {
            var byteVal = 0
            for (bit in 0 until 8) {
                if (bitBuffer[i * 8 + bit]) {
                    byteVal = byteVal or (1 shl (7 - bit))
                }
            }
            dataCodewords[i] = byteVal
        }

        // 2. Compute Reed-Solomon Error Correction
        val numBlocks = versionInfo.numBlocks
        val dataPerBlock = versionInfo.totalDataCodewords / numBlocks
        val ecPerBlock = versionInfo.ecCodewordsPerBlock

        val genPoly = computeGeneratorPoly(ecPerBlock)
        val dataBlocks = Array(numBlocks) { b ->
            dataCodewords.sliceArray(b * dataPerBlock until (b + 1) * dataPerBlock)
        }
        val ecBlocks = Array(numBlocks) { b ->
            computeReedSolomon(dataBlocks[b], genPoly, ecPerBlock)
        }

        // 3. Interleave data and EC codewords
        val finalCodewords = mutableListOf<Int>()
        for (i in 0 until dataPerBlock) {
            for (b in 0 until numBlocks) {
                finalCodewords.add(dataBlocks[b][i])
            }
        }
        for (i in 0 until ecPerBlock) {
            for (b in 0 until numBlocks) {
                finalCodewords.add(ecBlocks[b][i])
            }
        }

        // 4. Construct Matrix & Function Patterns
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // Finder patterns
        fun placeFinder(top: Int, left: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBlack = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    matrix[top + r][left + c] = isBlack
                    reserved[top + r][left + c] = true
                }
            }
            // Separator white borders
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = top + r
                    val cc = left + c
                    if (rr in 0 until size && cc in 0 until size && !reserved[rr][cc]) {
                        matrix[rr][cc] = false
                        reserved[rr][cc] = true
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            val isBlack = (i % 2 == 0)
            if (!reserved[6][i]) {
                matrix[6][i] = isBlack
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = isBlack
                reserved[i][6] = true
            }
        }

        // Alignment patterns for version >= 2
        val alignCoords = versionInfo.alignmentCoords
        if (alignCoords.isNotEmpty()) {
            for (r in alignCoords) {
                for (c in alignCoords) {
                    // Check if overlapping with finder patterns
                    if ((r <= 8 && c <= 8) || (r <= 8 && c >= size - 8) || (r >= size - 8 && c <= 8)) {
                        continue
                    }
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val rr = r + dr
                            val cc = c + dc
                            val isBlack = (abs(dr) == 2 || abs(dc) == 2 || (dr == 0 && dc == 0))
                            matrix[rr][cc] = isBlack
                            reserved[rr][cc] = true
                        }
                    }
                }
            }
        }

        // Dark module
        val darkRow = 4 * v + 9
        matrix[darkRow][8] = true
        reserved[darkRow][8] = true

        // Reserve format info areas
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // 5. Place Data Codewords into Matrix (Zigzag upwards and downwards)
        val allBits = mutableListOf<Boolean>()
        for (cw in finalCodewords) {
            for (b in 7 downTo 0) {
                allBits.add(((cw shr b) and 1) == 1)
            }
        }
        // Remainder bits for versions 2-6 (7 bits for version 2-6)
        val remainderBitsCount = when (v) {
            2, 3, 4, 5, 6 -> 7
            else -> 0
        }
        for (i in 0 until remainderBitsCount) {
            allBits.add(false)
        }

        var bitIdx = 0
        var up = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // Skip timing column 6
            val rowRange = if (up) (size - 1 downTo 0) else (0 until size)
            for (row in rowRange) {
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!reserved[row][c]) {
                        val bit = if (bitIdx < allBits.size) allBits[bitIdx++] else false
                        matrix[row][c] = bit
                    }
                }
            }
            up = !up
            col -= 2
        }

        // 6. Apply Mask Pattern (Mask 0: (row + col) % 2 == 0)
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!reserved[r][c]) {
                    if ((r + c) % 2 == 0) {
                        matrix[r][c] = !matrix[r][c]
                    }
                }
            }
        }

        // 7. Place Format Information (EC Level M: 00, Mask 0: 000 -> 0b00000)
        // Precomputed BCH(15,5) format bits for Level M + Mask 0 XORed with 0x5412 = 0b101010110000011 (0x5583)
        val formatBits = 0b101010110000011
        for (i in 0..14) {
            val bit = ((formatBits shr (14 - i)) and 1) == 1
            // Top-left placement
            val (r1, c1) = when {
                i < 6 -> Pair(8, i)
                i == 6 -> Pair(8, 7)
                i == 7 -> Pair(8, 8)
                i == 8 -> Pair(7, 8)
                else -> Pair(14 - i, 8)
            }
            matrix[r1][c1] = bit

            // Split placement (top-right and bottom-left)
            val (r2, c2) = if (i < 8) {
                Pair(size - 1 - i, 8)
            } else {
                Pair(8, size - 15 + i)
            }
            matrix[r2][c2] = bit
        }

        return matrix
    }

    private fun abs(a: Int): Int = if (a < 0) -a else a

    private fun computeGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = intArrayOf(1, expTable[i])
            poly = polyMul(poly, next)
        }
        return poly
    }

    private fun polyMul(p1: IntArray, p2: IntArray): IntArray {
        val res = IntArray(p1.size + p2.size - 1)
        for (i in p1.indices) {
            for (j in p2.indices) {
                res[i + j] = res[i + j] xor gfMul(p1[i], p2[j])
            }
        }
        return res
    }

    private fun computeReedSolomon(data: IntArray, genPoly: IntArray, ecCount: Int): IntArray {
        val msg = IntArray(data.size + ecCount)
        System.arraycopy(data, 0, msg, 0, data.size)
        for (i in data.indices) {
            val lead = msg[i]
            if (lead != 0) {
                for (j in genPoly.indices) {
                    msg[i + j] = msg[i + j] xor gfMul(genPoly[j], lead)
                }
            }
        }
        val ec = IntArray(ecCount)
        System.arraycopy(msg, data.size, ec, 0, ecCount)
        return ec
    }
}
