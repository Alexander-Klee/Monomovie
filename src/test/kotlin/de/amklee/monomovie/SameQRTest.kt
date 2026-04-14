package de.amklee.monomovie

import de.amklee.monomovie.util.DataTooLongException as DataTooLongExceptionK
import de.amklee.monomovie.util.QrCode as QrCodeK
import de.amklee.monomovie.util.QrSegment as QrSegmentK
import de.amklee.monomovie.util.BitBuffer as BitBufferK
import io.nayuki.qrcodegen.DataTooLongException as DataTooLongExceptionJ
import io.nayuki.qrcodegen.QrCode as QrCodeJ
import io.nayuki.qrcodegen.QrSegment as QrSegmentJ
import io.nayuki.qrcodegen.BitBuffer as BitBufferJ
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SameQRTest {
    fun randomString(length: Int, mode: QrSegmentK.Mode): String {
        return List(length) { when (mode) {
            QrSegmentK.Mode.NUMERIC -> ('0'..'9').random()
            QrSegmentK.Mode.ALPHANUMERIC -> QrSegmentJ.ALPHANUMERIC_CHARSET.random()
            QrSegmentK.Mode.BYTE -> (0..255).random().toChar()
            QrSegmentK.Mode.ECI -> throw IllegalArgumentException("ECI mode not supported for random string generation")
        } }.joinToString("")
    }

    fun assertEquals(expected: List<QrSegmentJ>, actual: List<QrSegmentK>) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) assertEquals(expected[i], actual[i])
    }

    fun assertEquals(expected: QrSegmentJ, actual: QrSegmentK) {
        assertEquals(expected.mode.name, actual.mode.name)
        assertEquals(expected.numChars, actual.numChars)
        assertEquals(expected.data, actual.data)
    }

    fun assertEquals(expected: QrCodeJ, actual: QrCodeK) {
        assertEquals(expected.version, actual.version, "Version values differ")
        assertEquals(expected.errorCorrectionLevel.name, actual.errorCorrectionLevel.name, "Error correction levels differ")
        assertEquals(expected.size, actual.size, "Size values differ")
        assertEquals(expected.mask, actual.mask.toInt(), "Mask values differ")
        for (y in 0 until expected.size) {
            for (x in 0 until expected.size) {
                assertEquals(expected.getModule(x, y), actual.getModule(x, y), "Module values differ at ($x, $y)")
            }
        }
    }

    fun assertEquals(expected: BitBufferJ, actual: BitBufferK) {
        assertEquals(expected.bitLength(), actual.length)
        for (i in 0 until expected.bitLength()) {
            assertEquals(expected.getBit(i), if (actual[i]) 1 else 0)
        }
    }

    private val supportedModes = listOf(QrSegmentK.Mode.NUMERIC, QrSegmentK.Mode.ALPHANUMERIC, QrSegmentK.Mode.BYTE)

    @Test
    fun testSameQR() {
        repeat(1000) {
            val data = randomString((10..1000).random(), supportedModes.random())
            val ecc = QrCodeK.Ecc.entries.toTypedArray().random()

            val qr1 = QrSegmentK.makeSegments(data)
            val qr2 = QrSegmentJ.makeSegments(data)
            assertEquals(qr2, qr1)

            val aqr2 = try {
                QrCodeJ.encodeSegments(qr2, QrCodeJ.Ecc.valueOf(ecc.name))
            } catch (t: DataTooLongExceptionJ) {
                null
            }
            if (aqr2 != null) {
                val aqr1 = QrCodeK.encodeSegments(qr1, ecc)
                assertEquals(aqr2, aqr1)
            } else {
                assertThrows<DataTooLongExceptionK> { QrCodeK.encodeSegments(qr1, ecc) }
            }
        }
    }
}
