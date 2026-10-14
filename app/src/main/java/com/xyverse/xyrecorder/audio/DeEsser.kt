package com.xyverse.xyrecorder.audio

import kotlin.math.*

/**
 * De-Esser - frequency-selective compressor targeting sibilance (S, Sh, Ch sounds).
 * Uses a band-pass filter around the target frequency to detect sibilance,
 * then applies dynamic gain reduction only in that band.
 */
class DeEsser(
    private var sampleRate: Int = 48000,
    private var frequency: Int = 6000,
    private var thresholdDb: Float = -20f,
    private var ratio: Float = 4f,
    private var attackMs: Float = 5f,
    private var releaseMs: Float = 60f
) {
    // Simple peaking EQ band-pass (2nd order biquad)
    private var bpB0 = 0f; private var bpB1 = 0f; private var bpB2 = 0f
    private var bpA1 = 0f; private var bpA2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    // Envelope / gain reduction
    private var envDb = -96f
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var thresholdLinear = 0f
    private var slope = 0f

    init { recalculate() }

    fun setParameters(
        sampleRate: Int,
        frequency: Int,
        thresholdDb: Float,
        ratio: Float,
        attackMs: Float = 5f,
        releaseMs: Float = 60f
    ) {
        this.sampleRate = sampleRate
        this.frequency = frequency.coerceIn(2000, 10000)
        this.thresholdDb = thresholdDb
        this.ratio = ratio.coerceAtLeast(1f)
        this.attackMs = attackMs
        this.releaseMs = releaseMs
        recalculate()
    }

    private fun recalculate() {
        // Band-pass around target frequency, Q=2 (narrow)
        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * 2.0)
        val a0 = 1.0 + alpha
        bpB0 = (alpha / a0).toFloat()
        bpB1 = 0f
        bpB2 = (-alpha / a0).toFloat()
        bpA1 = (-2.0 * cos(w0) / a0).toFloat()
        bpA2 = ((1.0 - alpha) / a0).toFloat()

        attackCoeff = if (attackMs > 0) exp(-1f / (attackMs * sampleRate / 1000f)).toFloat() else 0f
        releaseCoeff = if (releaseMs > 0) exp(-1f / (releaseMs * sampleRate / 1000f)).toFloat() else 0f
        thresholdLinear = 10f.pow(thresholdDb / 20f)
        slope = 1f - (1f / ratio)
    }

    fun reset() { x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f; envDb = -96f }

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val x = buffer[i]
            // Band-pass filter to isolate sibilance
            val band = bpB0 * x + bpB1 * x1 + bpB2 * x2 - bpA1 * y1 - bpA2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = band

            // Detect level in sibilance band
            val level = abs(band)

            // Compute gain reduction if above threshold
            val gr = if (level > thresholdLinear) {
                val over = level / thresholdLinear
                val reduced = 1f + (over - 1f) * (1f - slope)
                1f / reduced
            } else 1f

            // Smooth with envelope
            val coeff = if (gr < envDb) attackCoeff else releaseCoeff
            envDb = coeff * envDb + (1f - coeff) * gr

            // Apply only to the original signal
            buffer[i] = x * envDb
        }
    }
}
