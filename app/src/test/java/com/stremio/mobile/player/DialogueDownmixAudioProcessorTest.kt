package com.stremio.mobile.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueDownmixAudioProcessorTest {
    @Test
    fun `mixes 5 point 1 channels with center and surround gains`() {
        val processor = DialogueDownmixAudioProcessor().apply { enabled = true }
        val inputFormat = AudioProcessor.AudioFormat(48_000, 6, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(inputFormat)
        processor.flush()

        assertEquals(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT), outputFormat)

        val input = ByteBuffer.allocateDirect(inputFormat.bytesPerFrame)
            .order(ByteOrder.nativeOrder())
        listOf(0.10f, 0.08f, 0.03f, 0.0f, 0.02f, 0.01f).forEach {
            input.putShort((it * Short.MAX_VALUE).toInt().toShort())
        }
        input.flip()
        processor.queueInput(input)

        val output = processor.output.order(ByteOrder.nativeOrder())
        assertNear(0.0825f, output.getShort() / Short.MAX_VALUE.toFloat())
        assertNear(0.0690f, output.getShort() / Short.MAX_VALUE.toFloat())
        assertTrue(processor.isActive)
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue("expected $expected, got $actual", abs(expected - actual) < 0.002f)
    }
}
