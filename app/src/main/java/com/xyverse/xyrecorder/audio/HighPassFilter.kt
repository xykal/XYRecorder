package com.xyverse.xyrecorder.audio

import kotlin.math.*

/**
 * High-pass filter - 2nd order Butterworth IIR
 * Removes low-frequency rumble, HVAC noise, desk bumps.
 */
class HighPassFilter(
    private var sampleRate: Int = 48000,
    private var cutoffFrequency: Float = 80f
) {
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    init { recalculate() }

    fun setParameters(sampleRate: Int, cutoffHz: Float) {
        if (this.sampleRate == sampleRate && this.cutoffFrequency == cutoffHz) return
        this.sampleRate = sampleRate
        this.cutoffFrequency = cutoffHz.coerceIn(20f, (sampleRate / 2 - 10).toFloat())
        recalculate()
    }

    private fun recalculate() {
        // 2nd-order Butterworth HPF coefficients
        val omega = 2.0 * PI * cutoffFrequency / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * sqrt(2.0)) // Q = sqrt(2)/2 for Butterworth

        val b0d = (1.0 + cs) / 2.0
        val b1d = -(1.0 + cs)
        val b2d = (1.0 + cs) / 2.0
        val a0d = 1.0 + alpha
        val a1d = -2.0 * cs
        val a2d = 1.0 - alpha

        b0 = (b0d / a0d).toFloat()
        b1 = (b1d / a0d).toFloat()
        b2 = (b2d / a0d).toFloat()
        a1 = (a1d / a0d).toFloat()
        a2 = (a2d / a0d).toFloat()
    }

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    /** Process a buffer of Float samples (in-place). */
    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val x0 = buffer[i]
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            buffer[i] = y0
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
        }
    }
}
