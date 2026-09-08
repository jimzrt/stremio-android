package com.stremio.mobile.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/** PCM fold-down matching the MPV dialogue-focused downmix. */
class DialogueDownmixAudioProcessor : BaseAudioProcessor() {
    var enabled: Boolean = false

    private var channels = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (!enabled || inputAudioFormat.channelCount <= 2) return AudioProcessor.AudioFormat.NOT_SET
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = FloatArray(inputAudioFormat.channelCount)
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            2,
            inputAudioFormat.encoding,
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputFormat = inputAudioFormat
        val outputFormat = outputAudioFormat
        val frameCount = inputBuffer.remaining() / inputFormat.bytesPerFrame
        val output = replaceOutputBuffer(frameCount * outputFormat.bytesPerFrame)
        val input = inputBuffer.order(ByteOrder.nativeOrder())

        repeat(frameCount) {
            for (index in channels.indices) {
                channels[index] = if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) {
                    input.float
                } else {
                    input.short / Short.MAX_VALUE.toFloat()
                }
            }

            var left = channels[0] * FRONT_GAIN
            var right = channels[1] * FRONT_GAIN
            if (channels.size > 2) {
                left += channels[2] * CENTER_GAIN
                right += channels[2] * CENTER_GAIN
            }
            if (channels.size > 3) {
                left += channels[3] * LFE_GAIN
                right += channels[3] * LFE_GAIN
            }
            if (channels.size > 4) left += channels[4] * SURROUND_GAIN
            if (channels.size > 5) right += channels[5] * SURROUND_GAIN
            if (channels.size > 6) left += channels[6] * SURROUND_GAIN
            if (channels.size > 7) right += channels[7] * SURROUND_GAIN

            val peak = max(abs(left), abs(right))
            if (peak > COMPRESSOR_THRESHOLD) {
                val compressedPeak = COMPRESSOR_THRESHOLD +
                    (peak - COMPRESSOR_THRESHOLD) / COMPRESSOR_RATIO
                val gain = compressedPeak / peak
                left *= gain
                right *= gain
            }

            val limitedPeak = max(abs(left), abs(right))
            if (limitedPeak > LIMIT) {
                val gain = LIMIT / limitedPeak
                left *= gain
                right *= gain
            }

            if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) {
                output.putFloat(left)
                output.putFloat(right)
            } else {
                output.putShort(
                    (left * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                )
                output.putShort(
                    (right * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                )
            }
        }
        output.flip()
    }

    override fun onReset() {
        channels = FloatArray(0)
    }

    private companion object {
        const val FRONT_GAIN = 0.50f
        const val CENTER_GAIN = 0.85f
        const val LFE_GAIN = 0.20f
        const val SURROUND_GAIN = 0.35f
        const val COMPRESSOR_THRESHOLD = 0.125f
        const val COMPRESSOR_RATIO = 3.0f
        const val LIMIT = 0.95f
    }
}
