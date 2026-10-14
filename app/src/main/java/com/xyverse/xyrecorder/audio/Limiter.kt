package com.xyverse.xyrecorder.audio

import kotlin.math.*

/**
 * Look-ahead brick-wall limiter.
 * Prevents any sample from exceeding the ceiling - guarantees no clipping.
 */
class Limiter(
    private var sampleRate: Int = 48000,
    private var ceilingDb: Float = -1f,
    private var releaseMs: Float = 50f
) {
    private var ceiling = 0f
    private var releaseCoeff = 0f
    private var currentGain = 1f

    init { recalculate() }

    fun setParameters(sampleRate: Int, ceilingDb: Float, releaseMs: Float) {
        this.sampleRate = sampleRate
        this.ceilingDb = ceilingDb
        this.releaseMs = releaseMs
        recalculate()
    }

    private fun recalculate() {
        ceiling = 10f.pow(ceilingDb / 20f)
        releaseCoeff = if (releaseMs > 0) exp(-1f / (releaseMs * sampleRate / 1000f)).toFloat() else 0f
    }

    fun reset() { currentGain = 1f }

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val x = buffer[i]
            val absX = abs(x)

            // Required gain to keep below ceiling
            val neededGain = if (absX > ceiling) ceiling / absX else 1f

            // Apply with release smoothing
            if (neededGain < currentGain) {
                currentGain = neededGain
            } else {
                currentGain = releaseCoeff * currentGain + (1f - releaseCoeff) * neededGain
            }

            buffer[i] = x * currentGain
        }
    }
}
