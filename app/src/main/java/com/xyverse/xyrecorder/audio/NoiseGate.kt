package com.xyverse.xyrecorder.audio

import kotlin.math.*

/**
 * Noise gate - silences signal below a threshold.
 * Smoothly opens/closes with attack/release/hold envelope to avoid clicks.
 */
class NoiseGate(
    private var sampleRate: Int = 48000,
    private var thresholdDb: Float = -45f,
    private var attackMs: Float = 5f,
    private var releaseMs: Float = 100f,
    private var holdMs: Float = 50f
) {
    private var gain: Float = 0f       // 0..1 current gain
    private var holdCounter = 0
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var holdSamples = 0
    private var thresholdLinear = 0f

    init { recalculate() }

    fun setParameters(
        sampleRate: Int,
        thresholdDb: Float,
        attackMs: Float,
        releaseMs: Float,
        holdMs: Float
    ) {
        this.sampleRate = sampleRate
        this.thresholdDb = thresholdDb
        this.attackMs = attackMs
        this.releaseMs = releaseMs
        this.holdMs = holdMs
        recalculate()
    }

    private fun recalculate() {
        thresholdLinear = dbToLinear(thresholdDb)
        attackCoeff = if (attackMs > 0) (1f - exp(-1f / (attackMs * sampleRate / 1000f))).toFloat() else 1f
        releaseCoeff = if (releaseMs > 0) (1f - exp(-1f / (releaseMs * sampleRate / 1000f))).toFloat() else 1f
        holdSamples = (holdMs * sampleRate / 1000f).toInt().coerceAtLeast(0)
    }

    fun reset() { gain = 0f; holdCounter = 0 }

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val inputLevel = abs(buffer[i])
            val target = if (inputLevel >= thresholdLinear) 1f else 0f

            if (target > gain) {
                // Attack
                gain += attackCoeff * (target - gain)
                holdCounter = holdSamples
            } else if (holdCounter > 0) {
                holdCounter--
            } else {
                // Release
                gain += releaseCoeff * (target - gain)
                if (gain < 0.0001f) gain = 0f
            }

            buffer[i] *= gain
        }
    }

    private fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)
}
