@file:OptIn(ExperimentalUnsignedTypes::class)

package de.amklee.monomovie.util

import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.svg
import kotlinx.html.visit
import java.util.*
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.abs
import kotlin.math.max


// based in part on https://github.com/nayuki/QR-Code-generator (MIT License)

private inline fun Int.getBit(i: Int) = (this ushr i) and 1 != 0
private inline fun UInt.getBit(i: Int) = (this shr i) and 1u != 0u
private inline fun Boolean.toByte(): Byte = if (this) 1 else 0
private inline infix fun Byte.shl(bitCount: Int): Byte = (this.toInt() shl bitCount).toByte()
private inline infix fun UByte.shl(bitCount: Int): UByte = (this.toInt() shl bitCount).toUByte()
private inline infix fun UByte.shr(bitCount: Int): UByte = (this.toInt() ushr bitCount).toUByte()
private inline operator fun String.get(index: UByte) = this[index.toInt()]
private inline fun repeat(times: UByte, action: (Int) -> Unit) = repeat(times.toInt(), action)
private inline operator fun Int.minus(other: UByte) = this - other.toInt()

private const val MIN_VERSION = 1
private const val MAX_VERSION = 40

class BitBuffer {
    private var data: BitSet = BitSet()
    private var bitLength: Int = 0

    val length get() = bitLength

    operator fun get(index: Int): Boolean {
        if (index !in 0..<bitLength) throw IndexOutOfBoundsException("Index $index out of bounds for length $bitLength")
        return data[index]
    }

    /**
     * @param value the value to append
     * @param len the number of lower-order bits in the value to take
     */
    fun append(value: Int, len: Int): BitBuffer {
        if (len !in 0..<32 || value ushr len != 0) throw IllegalArgumentException("Value $value does not fit in $len bits")
        if (Int.MAX_VALUE - bitLength < len) throw IllegalStateException("Buffer overflow: cannot append $len bits to buffer of length $bitLength")
        for (i in (len - 1) downTo 0) {
            data[bitLength++] = value.getBit(i)
        }
        return this
    }

    fun append(other: BitBuffer): BitBuffer {
        if (Int.MAX_VALUE - bitLength < other.bitLength) throw IllegalStateException("Buffer overflow: cannot append ${other.bitLength} bits to buffer of length $bitLength")
        for (i in 0 until other.bitLength) {
            data[bitLength++] = other[i]
        }
        return this
    }

    fun copy(): BitBuffer {
        val copy = BitBuffer()
        copy.data = data.clone() as BitSet
        copy.bitLength = bitLength
        return copy
    }
}

class BitMatrix(val width: Int, val height: Int) {
    private val data: BitSet = BitSet(width * height)

    operator fun get(x: Int, y: Int): Boolean {
        if (x !in 0..<width || y !in 0..<height) throw IndexOutOfBoundsException("Index ($x, $y) out of bounds for size ($width, $height)")
        return data[y * width + x]
    }

    operator fun set(x: Int, y: Int, value: Boolean) {
        if (x !in 0..<width || y !in 0..<height) throw IndexOutOfBoundsException("Index ($x, $y) out of bounds for size ($width, $height)")
        data[y * width + x] = value
    }

    inline fun forEach(action: (x: Int, y: Int) -> Unit) {
        for (y in 0..<height) {
            for (x in 0..<width) {
                action(x, y)
            }
        }
    }
}

class DataTooLongException(msg: String?) : IllegalArgumentException(msg ?: "The supplied data does not fit any QR Code version")

class QrSegment(val mode: Mode, val numChars: Int, data: BitBuffer) {
    private val _data = data.copy() // defensive copy
    init {
        require(numChars >= 0) { "Invalid argument" }
    }

    val data get() = _data.copy() // defensive copy

    enum class Mode(val modeBits: Int, private val numBitsCharCount: IntArray) {
        NUMERIC     (0x1, intArrayOf(10, 12, 14)),
        ALPHANUMERIC(0x2, intArrayOf( 9, 11, 13)),
        BYTE        (0x4, intArrayOf( 8, 16, 16)),
        ECI         (0x7, intArrayOf( 0,  0,  0));

        fun numCharCountBits(version: Int): Int {
            assert(version in MIN_VERSION..MAX_VERSION)
            return numBitsCharCount[(version + 7) / 17]
        }
    }

    companion object {
        private val NUMERIC_REGEX = Regex("[0-9]*")
        private val ALPHANUMERIC_REGEX = Regex("[A-Z0-9 $%*+./:-]*")
        private const val ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

        fun isNumeric(text: CharSequence) = NUMERIC_REGEX.matches(text)
        fun isAlphanumeric(text: CharSequence) = ALPHANUMERIC_REGEX.matches(text)

        fun makeBytes(data: ByteArray) = QrSegment(Mode.BYTE, data.size, BitBuffer().apply {
            for (b in data) {
                append(b.toUByte().toInt(), 8)
            }
        })

        fun makeNumeric(digits: CharSequence): QrSegment {
            if (!isNumeric(digits)) throw IllegalArgumentException("String contains non-numeric characters")
            return QrSegment(Mode.NUMERIC, digits.length, BitBuffer().apply {
                var i = 0
                while (i < digits.length) {
                    val n = minOf(digits.length - i, 3)
                    append(digits.subSequence(i, i + n).toString().toInt(), n * 3 + 1)
                    i += n
                }
            })
        }

        fun makeAlphanumeric(text: CharSequence): QrSegment {
            if (!isAlphanumeric(text)) throw IllegalArgumentException("String contains unencodable characters in alphanumeric mode")
            return QrSegment(Mode.ALPHANUMERIC, text.length, BitBuffer().apply {
                for (i in 0..(text.length - 2) step 2) {
                    val temp = ALPHANUMERIC_CHARSET.indexOf(text[i]) * 45 +
                               ALPHANUMERIC_CHARSET.indexOf(text[i + 1])
                    append(temp, 11)
                }
                if (text.length % 2 != 0) {
                    append(ALPHANUMERIC_CHARSET.indexOf(text.last()), 6)
                }
            })
        }

        fun makeSegments(text: CharSequence): List<QrSegment> {
            return when {
                text.isEmpty() -> emptyList()
                isNumeric(text) -> listOf(makeNumeric(text))
                isAlphanumeric(text) -> listOf(makeAlphanumeric(text))
                else -> listOf(makeBytes(text.toString().encodeToByteArray()))
            }
        }

        fun makeEci(assignVal: Int): QrSegment = QrSegment(Mode.ECI, 0, BitBuffer().apply {
            when {
                assignVal < 0 -> throw IllegalArgumentException("ECI assignment value out of range")
                assignVal < (1 shl 7) -> append(assignVal, 8)
                assignVal < (1 shl 14) -> append(2, 2).append(assignVal, 14)
                assignVal < (1 shl 21) -> append(6, 3).append(assignVal, 21)
                else -> throw IllegalArgumentException("ECI assignment value out of range")
            }
        })

        fun getTotalBits(segs: List<QrSegment>, version: Int): Int {
            var result: Long = 0
            for (seg in segs) {
                val ccbits = seg.mode.numCharCountBits(version)
                if (seg.numChars >= (1 shl ccbits)) return -1
                result += 4L + ccbits + seg.data.length
                if (result > Int.MAX_VALUE) return -1
            }
            return result.toInt()
        }
    }
}

/**
 * @param version the version number to use, which must be in the range 1 to 40 (inclusive)
 * @param errorCorrectionLevel the error correction level to use
 * @param dataCodewords the bytes representing segments to encode (without ECC)
 * @param mask the mask pattern to use, which is either &#x2212;1 for automatic choice or from 0 to 7 for fixed choice
 */
class QrCode(val version: Int, val errorCorrectionLevel: Ecc, dataCodewords: ByteArray, mask: Int) {
    /**
     * Constructs a QR Code with the specified version number,
     * error correction level, data codeword bytes, and mask number.
     *
     * This is a low-level API that most users should not use directly. A mid-level
     * API is the [.encodeSegments] function.
     * @throws NullPointerException if the byte array or error correction level is `null`
     * @throws IllegalArgumentException if the version or mask value is out of range,
     * or if the data is the wrong length for the specified version and error correction level
     */
    init {
        require(version in MIN_VERSION..MAX_VERSION) { "Version value out of range" }
        require(!(mask < -1 || mask > 7)) { "Mask value out of range" }
    }

    val size: Int = version * 4 + 17

    private val modules = BitMatrix(size, size)
    private val isFunction = BitMatrix(size, size)
    val mask: UByte

    init {
        // Compute ECC, draw modules
        drawFunctionPatterns()
        val allCodewords = addEccAndInterleave(dataCodewords)
        drawCodewords(allCodewords)

        // Find best mask if mask == -1
        this.mask = if (mask != -1) mask.toUByte() else (0..7).minByOrNull { i ->
            val i = i.toUByte()
            applyMask(i)
            drawFormatBits(i)
            val penalty = this.penaltyScore
            applyMask(i) // Undoes the mask due to XOR
            penalty
        }!!.toUByte()

        assert(this.mask.toInt() in 0..7)

        // Do masking
        applyMask(this.mask) // Apply the final choice of mask
        drawFormatBits(this.mask) // Overwrite old format bits
    }

    fun getModule(x: Int, y: Int): Boolean {
        return x in 0..<size && 0 <= y && y < size && modules[x, y]
    }

    private fun drawFunctionPatterns() {
        for (i in 0..<size) {
            setFunctionModule(6, i, i % 2 == 0)
            setFunctionModule(i, 6, i % 2 == 0)
        }

        drawFinderPattern(3, 3)
        drawFinderPattern(size - 4, 3)
        drawFinderPattern(3, size - 4)

        val alignPatPos = this.alignmentPatternPositions
        val numAlign = alignPatPos.size
        for (i in 0..<numAlign) {
            for (j in 0..<numAlign) {
                if (!(i == 0 && j == 0 || i == 0 && j == numAlign - 1 || i == numAlign - 1 && j == 0)) drawAlignmentPattern(
                    alignPatPos[i],
                    alignPatPos[j]
                )
            }
        }

        drawFormatBits(0u)
        drawVersion()
    }

    private fun drawFormatBits(msk: UByte) {
        val data = errorCorrectionLevel.formatBits shl 3 or msk // errCorrLvl is uint2, mask is uint3
        var rem = data.toUInt()
        repeat(10) { rem = (rem shl 1) xor ((rem shr 9) * 0x537u) }
        val bits = (data.toUInt() shl 10 or rem) xor 0x5412u // uint15
        assert(bits shr 15 == 0u)

        for (i in 0..5) setFunctionModule(8, i, bits.getBit(i))
        setFunctionModule(8, 7, bits.getBit(6))
        setFunctionModule(8, 8, bits.getBit(7))
        setFunctionModule(7, 8, bits.getBit(8))
        for (i in 9..14) setFunctionModule(14 - i, 8, bits.getBit(i))

        for (i in 0..7) setFunctionModule(size - 1 - i, 8, bits.getBit(i))
        for (i in 8..14) setFunctionModule(8, size - 15 + i, bits.getBit(i))
        setFunctionModule(8, size - 8, true) // Always dark
    }

    private fun drawVersion() {
        if (version < 7) return

        var rem = version // version is uint6, in the range [7, 40]
        repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
        val bits = version shl 12 or rem // uint18
        assert(bits ushr 18 == 0)

        for (i in 0..17) {
            val bit = bits.getBit(i)
            val a = size - 11 + i % 3
            val b = i / 3
            setFunctionModule(a, b, bit)
            setFunctionModule(b, a, bit)
        }
    }

    private fun drawFinderPattern(x: Int, y: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val dist = max(abs(dx), abs(dy)) // Chebyshev/infinity norm
                val xx = x + dx
                val yy = y + dy
                if (xx in 0..<size && 0 <= yy && yy < size) setFunctionModule(xx, yy, dist != 2 && dist != 4)
            }
        }
    }

    private fun drawAlignmentPattern(x: Int, y: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) setFunctionModule(x + dx, y + dy, max(abs(dx), abs(dy)) != 1)
        }
    }

    private fun setFunctionModule(x: Int, y: Int, isDark: Boolean) {
        modules[x, y] = isDark
        isFunction[x, y] = true
    }

    private fun addEccAndInterleave(data: ByteArray): ByteArray {
        require(data.size == getNumDataCodewords(version, errorCorrectionLevel))

        val numBlocks = NUM_ERROR_CORRECTION_BLOCKS[errorCorrectionLevel.ordinal][version].toInt()
        val blockEccLen = ECC_CODEWORDS_PER_BLOCK[errorCorrectionLevel.ordinal][version].toUByte()
        val rawCodewords = getNumRawDataModules(version) / 8
        val numShortBlocks = numBlocks - rawCodewords % numBlocks
        val shortBlockLen = rawCodewords / numBlocks

        val blocks = arrayOfNulls<ByteArray>(numBlocks)
        val rsDiv = reedSolomonComputeDivisor(blockEccLen)
        run {
            var i = 0
            var k = 0
            while (i < numBlocks) {
                val dat =
                    data.copyOfRange(k, k + shortBlockLen - blockEccLen + (if (i < numShortBlocks) 0 else 1))
                k += dat.size
                val block = dat.copyOf(shortBlockLen + 1)
                val ecc = reedSolomonComputeRemainder(dat, rsDiv)
                System.arraycopy(ecc, 0, block, block.size - blockEccLen, ecc.size)
                blocks[i] = block
                i++
            }
        }

        val result = ByteArray(rawCodewords)
        var i = 0
        var k = 0
        while (i < blocks[0]!!.size) {
            for (j in blocks.indices) {
                if (i != shortBlockLen - blockEccLen || j >= numShortBlocks) {
                    result[k] = blocks[j]!![i]
                    k++
                }
            }
            i++
        }
        return result
    }

    private fun drawCodewords(data: ByteArray) {
        require(data.size == getNumRawDataModules(version) / 8)

        var i = 0
        var right = size - 1
        while (right >= 1) {
            // Index of right column in each column pair
            if (right == 6) right = 5
            for (vert in 0..<size) {  // Vertical counter
                for (j in 0..1) {
                    val x = right - j // Actual x coordinate
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert // Actual y coordinate
                    if (!isFunction[x, y] && i < data.size * 8) {
                        modules[x, y] = data[i ushr 3].toInt().getBit(7 - (i and 7))
                        i++
                    }
                }
            }
            right -= 2
        }
        assert(i == data.size * 8)
    }

    private fun applyMask(msk: UByte) {
        require(msk.toInt() in 0..7) { "Mask value out of range" }
        for (y in 0..<size) {
            for (x in 0..<size) {
                val invert: Boolean = when (msk.toInt()) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    7 -> ((x + y) % 2 + x * y % 3) % 2 == 0
                    else -> throw AssertionError()
                }
                modules[x, y] = modules[x, y] xor (invert and !isFunction[x, y])
            }
        }
    }


    private val penaltyScore: Int
        get() {
            var result = 0

            fun linePenalty(idx: (Int, Int) -> Pair<Int, Int>) {
                for (a in 0..<size) {
                    var runColor = false
                    var run = 0
                    val runHistory = IntArray(7)
                    for (b in 0..<size) {
                        val (x, y) = idx(a, b)
                        if (modules[x, y] == runColor) {
                            run++
                            if (run == 5) result += PENALTY_N1
                            else if (run > 5) result++
                        } else {
                            finderPenaltyAddHistory(run, runHistory)
                            if (!runColor) result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3
                            runColor = modules[x, y]
                            run = 1
                        }
                    }
                    result += finderPenaltyTerminateAndCount(runColor, run, runHistory) * PENALTY_N3
                }
            }

            // Adjacent modules in row having same color, and finder-like patterns
            linePenalty { a, b -> b to a }
            // Adjacent modules in column having same color, and finder-like patterns
            linePenalty { a, b -> a to b }

            // 2*2 blocks of modules having same color
            for (y in 0..<size - 1) {
                for (x in 0..<size - 1) {
                    val color = modules[x, y]
                    if (color == modules[x + 1, y] && color == modules[x, y + 1] && color == modules[x + 1, y + 1]) result += PENALTY_N2
                }
            }

            // Balance of dark and light modules
            var dark = 0
            modules.forEach { x, y ->
                 if (modules[x, y]) dark++
            }
            val total = size * size // Note that size is odd, so dark/total != 1/2
            // Compute the smallest integer k >= 0 such that (45-5k)% <= dark/total <= (55+5k)%
            val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
            assert(k in 0..9)
            result += k * PENALTY_N4
            assert(
                result in 0..2568888 // Non-tight upper bound based on default values of PENALTY_N1, ..., N4
            )
            return result
        }


    private val alignmentPatternPositions: IntArray
        /*---- Private helper functions ----*/
        get() {
            if (version == 1) return intArrayOf()
            else {
                val numAlign = version / 7 + 2
                val step = (version * 8 + numAlign * 3 + 5) / (numAlign * 4 - 4) * 2
                val result = IntArray(numAlign)
                result[0] = 6
                var i = result.size - 1
                var pos = size - 7
                while (i >= 1) {
                    result[i] = pos
                    i--
                    pos -= step
                }
                return result
            }
        }


    // Can only be called immediately after a light run is added, and
    // returns either 0, 1, or 2. A helper function for getPenaltyScore().
    private fun finderPenaltyCountPatterns(runHistory: IntArray): Int {
        val n = runHistory[1]
        assert(n <= size * 3)
        val core = n > 0 && runHistory[2] == n && runHistory[3] == n * 3 && runHistory[4] == n && runHistory[5] == n
        return ((if (core && runHistory[0] >= n * 4 && runHistory[6] >= n) 1 else 0)
                + (if (core && runHistory[6] >= n * 4 && runHistory[0] >= n) 1 else 0))
    }


    // Must be called at the end of a line (row or column) of modules. A helper function for getPenaltyScore().
    private fun finderPenaltyTerminateAndCount(
        currentRunColor: Boolean,
        currentRunLength: Int,
        runHistory: IntArray,
    ): Int {
        var currentRunLength = currentRunLength
        if (currentRunColor) {  // Terminate dark run
            finderPenaltyAddHistory(currentRunLength, runHistory)
            currentRunLength = 0
        }
        currentRunLength += size // Add light border to final run
        finderPenaltyAddHistory(currentRunLength, runHistory)
        return finderPenaltyCountPatterns(runHistory)
    }


    // Pushes the given value to the front and drops the last value. A helper function for getPenaltyScore().
    private fun finderPenaltyAddHistory(currentRunLength: Int, runHistory: IntArray) {
        var currentRunLength = currentRunLength
        if (runHistory[0] == 0) currentRunLength += size // Add light border to initial run

        System.arraycopy(runHistory, 0, runHistory, 1, runHistory.size - 1)
        runHistory[0] = currentRunLength
    }

    /**
     * The error correction level in a QR Code symbol.
     * @param formatBits In the range 0 to 3 (unsigned 2-bit integer).
     */
    enum class Ecc(val formatBits: UByte) {
        // Must be declared in ascending order of error protection
        // so that the implicit ordinal() and values() work properly
        /** The QR Code can tolerate about  7% erroneous codewords.  */
        LOW(1u),

        /** The QR Code can tolerate about 15% erroneous codewords.  */
        MEDIUM(0u),

        /** The QR Code can tolerate about 25% erroneous codewords.  */
        QUARTILE(3u),

        /** The QR Code can tolerate about 30% erroneous codewords.  */
        HIGH(2u)
    }

    companion object {
        /**
         * Returns a QR Code representing the specified Unicode text string at the specified error correction level.
         * As a conservative upper bound, this function is guaranteed to succeed for strings that have 738 or fewer
         * Unicode code points (not UTF-16 code units) if the low error correction level is used. The smallest possible
         * QR Code version is automatically chosen for the output. The ECC level of the result may be higher than the
         * ecl argument if it can be done without increasing the version.
         * @param text the text to be encoded (not `null`), which can be any Unicode string
         * @param ecl the error correction level to use (not `null`) (boostable)
         * @return a QR Code (not `null`) representing the text
         * @throws NullPointerException if the text or error correction level is `null`
         * @throws DataTooLongException if the text fails to fit in the
         * largest version QR Code at the ECL, which means it is too long
         */
        fun encodeText(text: CharSequence, ecl: Ecc): QrCode = encodeSegments(QrSegment.makeSegments(text), ecl)


        /**
         * Returns a QR Code representing the specified binary data at the specified error correction level.
         * This function always encodes using the binary segment mode, not any text mode. The maximum number of
         * bytes allowed is 2953. The smallest possible QR Code version is automatically chosen for the output.
         * The ECC level of the result may be higher than the ecl argument if it can be done without increasing the version.
         * @param data the binary data to encode (not `null`)
         * @param ecl the error correction level to use (not `null`) (boostable)
         * @return a QR Code (not `null`) representing the data
         * @throws NullPointerException if the data or error correction level is `null`
         * @throws DataTooLongException if the data fails to fit in the
         * largest version QR Code at the ECL, which means it is too long
         */
        fun encodeBinary(data: ByteArray, ecl: Ecc): QrCode = encodeSegments(listOf(QrSegment.makeBytes(data)), ecl)

        /**
         * Returns a QR Code representing the specified segments at the specified error correction
         * level. The smallest possible QR Code version is automatically chosen for the output. The ECC level
         * of the result may be higher than the ecl argument if it can be done without increasing the version.
         *
         * This function allows the user to create a custom sequence of segments that switches
         * between modes (such as alphanumeric and byte) to encode text in less space.
         * This is a mid-level API; the high-level API is [.encodeText]
         * and [.encodeBinary].
         * @param segs the segments to encode
         * @param ecl the error correction level to use (not `null`) (boostable)
         * @return a QR Code (not `null`) representing the segments
         * @throws NullPointerException if the list of segments, any segment, or the error correction level is `null`
         * @throws DataTooLongException if the segments fail to fit in the
         * largest version QR Code at the ECL, which means they are too long
         */
        @JvmOverloads
        fun encodeSegments(
            segs: List<QrSegment>,
            ecl: Ecc,
            minVersion: Int = MIN_VERSION,
            maxVersion: Int = MAX_VERSION,
            mask: Int = -1,
            boostEcl: Boolean = true,
        ): QrCode {
            var ecl = ecl
            require(!(!(minVersion in MIN_VERSION..maxVersion && maxVersion <= MAX_VERSION) || mask < -1 || mask > 7)) { "Invalid value" }
            var dataUsedBits: Int

            // Find the minimal version number to use
            var version: Int = minVersion
            while (true) {
                val dataCapacityBits =
                    getNumDataCodewords(version, ecl) * 8 // Number of data bits available
                dataUsedBits = QrSegment.getTotalBits(segs, version)
                if (dataUsedBits != -1 && dataUsedBits <= dataCapacityBits) break // This version number is found to be suitable

                if (version >= maxVersion) {  // All versions in the range could not fit the given data
                    var msg = "Segment too long"
                    if (dataUsedBits != -1) msg =
                        String.format("Data length = %d bits, Max capacity = %d bits", dataUsedBits, dataCapacityBits)
                    throw DataTooLongException(msg)
                }
                version++
            }
            assert(dataUsedBits != -1)


            // Increase the error correction level while the data still fits in the current version number
            for (newEcl in Ecc.entries) {  // From low to high
                if (boostEcl && dataUsedBits <= getNumDataCodewords(version, newEcl) * 8) ecl = newEcl
            }


            // Concatenate all segments to create the data bit string
            val bb = BitBuffer()
            for (seg in segs) {
                bb.append(seg.mode.modeBits, 4)
                bb.append(seg.numChars, seg.mode.numCharCountBits(version))
                bb.append(seg.data)
            }
            assert(bb.length == dataUsedBits)


            // Add terminator and pad up to a byte if applicable
            val dataCapacityBits = getNumDataCodewords(version, ecl) * 8
            assert(bb.length <= dataCapacityBits)
            bb.append(0, 4.coerceAtMost(dataCapacityBits - bb.length))
            bb.append(0, (8 - bb.length % 8) % 8)
            assert(bb.length % 8 == 0)


            // Pad with alternating bytes until data capacity is reached
            var padByte = 0xEC
            while (bb.length < dataCapacityBits) {
                bb.append(padByte, 8)
                padByte = padByte xor (0xEC xor 0x11)
            }


            // Pack bits into bytes in big endian
            val dataCodewords = ByteArray(bb.length / 8)
            for (i in 0..<bb.length) dataCodewords[i ushr 3] =
                dataCodewords[i ushr 3] or (bb[i].toByte() shl (7 - (i and 7)))


            // Create the QR Code object
            return QrCode(version, ecl, dataCodewords, mask)
        }


        // Returns the number of data bits that can be stored in a QR Code of the given version number, after
        // all function modules are excluded. This includes remainder bits, so it might not be a multiple of 8.
        // The result is in the range [208, 29648]. This could be implemented as a 40-entry lookup table.
        private fun getNumRawDataModules(ver: Int): Int {
            require(ver in MIN_VERSION..MAX_VERSION) { "Version number out of range" }

            val size = ver * 4 + 17
            var result = size * size // Number of modules in the whole QR Code square
            result -= 8 * 8 * 3 // Subtract the three finders with separators
            result -= 15 * 2 + 1 // Subtract the format information and dark module
            result -= (size - 16) * 2 // Subtract the timing patterns (excluding finders)
            // The five lines above are equivalent to: int result = (16 * ver + 128) * ver + 64;
            if (ver >= 2) {
                val numAlign = ver / 7 + 2
                result -= (numAlign - 1) * (numAlign - 1) * 25 // Subtract alignment patterns not overlapping with timing patterns
                result -= (numAlign - 2) * 2 * 20 // Subtract alignment patterns that overlap with timing patterns
                // The two lines above are equivalent to: result -= (25 * numAlign - 10) * numAlign - 55;
                if (ver >= 7) result -= 6 * 3 * 2 // Subtract version information
            }
            assert(result in 208..29648)
            return result
        }


        // Returns a Reed-Solomon ECC generator polynomial for the given degree. This could be
        // implemented as a lookup table over all possible parameter values, instead of as an algorithm.
        private fun reedSolomonComputeDivisor(degree: UByte): ByteArray {
            require(degree.toUInt() != 0u) { "Degree out of range" }
            // Polynomial coefficients are stored from highest to lowest power, excluding the leading term which is always 1.
            // For example the polynomial x^3 + 255x^2 + 8x + 93 is stored as the uint8 array {255, 8, 93}.
            val result = ByteArray(degree.toInt())
            result[degree.toInt() - 1] = 1 // Start off with the monomial x^0


            // Compute the product polynomial (x - r^0) * (x - r^1) * (x - r^2) * ... * (x - r^{degree-1}),
            // and drop the highest monomial term which is always 1x^degree.
            // Note that r = 0x02, which is a generator element of this field GF(2^8/0x11D).
            var root: UByte = 1u
            repeat(degree) {
                // Multiply the current product by (x - r^i)
                for (j in result.indices) {
                    result[j] = reedSolomonMultiply(result[j].toUByte(), root).toByte()
                    if (j + 1 < result.size) result[j] = result[j] xor result[j + 1]
                }
                root = reedSolomonMultiply(root, 0x02u)
            }
            return result
        }


        // Returns the Reed-Solomon error correction codeword for the given data and divisor polynomials.
        private fun reedSolomonComputeRemainder(data: ByteArray, divisor: ByteArray): ByteArray {
            val result = UByteArray(divisor.size)
            for (b in data) {  // Polynomial division
                val factor: UByte = (b.toUByte() xor result[0])
                System.arraycopy(result.asByteArray(), 1, result.asByteArray(), 0, result.size - 1)
                result[result.size - 1] = 0u
                for (i in result.indices) result[i] =
                    result[i] xor reedSolomonMultiply(divisor[i].toUByte(), factor)
            }
            return result.asByteArray()
        }


        // Returns the product of the two given field elements modulo GF(2^8/0x11D). The arguments and result
        // are unsigned 8-bit integers. This could be implemented as a lookup table of 256*256 entries of uint8.
        private fun reedSolomonMultiply(x: UByte, y: UByte): UByte {
            // Russian peasant multiplication
            var z = 0
            for (i in 7 downTo 0) {
                z = (z shl 1) xor ((z ushr 7) * 0x11D)
                z = z xor (((y shr i) and 1u) * x).toInt()
            }
            assert(z ushr 8 == 0)
            return z.toUByte()
        }


        // Returns the number of 8-bit data (i.e. not error correction) codewords contained in any
        // QR Code of the given version number and error correction level, with remainder bits discarded.
        // This stateless pure function could be implemented as a (40*4)-cell lookup table.
        fun getNumDataCodewords(ver: Int, ecl: Ecc): Int {
            return (getNumRawDataModules(ver) / 8
                    - ECC_CODEWORDS_PER_BLOCK[ecl.ordinal][ver]
                    * NUM_ERROR_CORRECTION_BLOCKS[ecl.ordinal][ver])
        }

        // For use in getPenaltyScore(), when evaluating which mask is best.
        private const val PENALTY_N1 = 3
        private const val PENALTY_N2 = 3
        private const val PENALTY_N3 = 40
        private const val PENALTY_N4 = 10


        private val ECC_CODEWORDS_PER_BLOCK = arrayOf(
            // Version: (note that index 0 is for padding, and is set to an illegal value)
            //           0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40    Error correction level
            byteArrayOf(-1,  7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),  // Low
            byteArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28),  // Medium
            byteArrayOf(-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),  // Quartile
            byteArrayOf(-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),  // High
        )

        private val NUM_ERROR_CORRECTION_BLOCKS = arrayOf(
            // Version: (note that index 0 is for padding, and is set to an illegal value)
            //           0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40    Error correction level
            byteArrayOf(-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4,  4,  4,  4,  4,  6,  6,  6,  6,  7,  8,  8,  9,  9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25),  // Low
            byteArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5,  5,  8,  9,  9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49),  // Medium
            byteArrayOf(-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8,  8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68),  // Quartile
            byteArrayOf(-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81),  // High
        )
    }
}

object QrCodeRenderer {
    fun renderSVG(qr: QrCode) = renderSVG(qr.size, qr.size) { x, y -> qr.getModule(x, y) }
    fun renderSVG(matrix: BitMatrix) = renderSVG(matrix.width, matrix.height) { x, y -> matrix[x, y] }
    private inline fun renderSVG(width: Int, height: Int, isDark: (x: Int, y: Int) -> Boolean) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\" stroke=\"none\">\n")
        append("<style type=\"text/css\">\n")
        append(".black {fill:#000000;}\n")
        append("</style>\n")
        append("<path class=\"black\"  d=\"")

        renderSvgPath(width, height, isDark)

        append("\"/>\n")
        append("</svg>\n")
    }
}

private inline fun StringBuilder.renderSvgPath(width: Int, height: Int, isDark: (x: Int, y: Int) -> Boolean) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (isDark(x, y)) append(" M$x,${y}h1v1h-1z")
        }
    }
}

fun FlowContent.QrCode(qr: QrCode, border: Int = 2) {
    require(border >= 0) { "Border size cannot be negative" }
    svg {
        attributes["viewBox"] = "${-border} ${-border} ${qr.size + border * 2} ${qr.size + border * 2}"
        attributes["stroke"] = "none"
        HTMLTag(
            "path",
            consumer,
            mapOf("class" to "qrCodePath", "d" to buildString { renderSvgPath(qr.size, qr.size) { x, y -> qr.getModule(x, y) } }),
            "http://www.w3.org/2000/svg",
            inlineTag = true,
            emptyTag = true,
        ).apply { visit {} }
    }
}
