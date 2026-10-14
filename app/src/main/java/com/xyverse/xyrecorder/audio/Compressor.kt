package com.xyverse.xyrecorder.audio

import kotlin.math.*

/**
 * Dynamic range compressor with soft knee, attack/release envelope, and makeup gain.
 * Evens out audio levels for broadcast-quality consistency.
 */
class Compressor(
    private var sampleRate: Int = 48000,
    private var thresholdDb: Float = -24f,
    private var ratio: Float = 4f,
    private var attackMs: Float = 10f,
    private var releaseMs: Float = 100f,
    private var makeupGainDb: Float = 6f,
    private var kneeDb: Float = 6f
) {
    private var envDb: Float = -96f
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var makeupLinear = 1f
    private var thresholdLinear = 0f
    private var kneeLinear = 0f
    private var slope = 0f

    init { recalculate() }

    fun setParameters(
        sampleRate: Int,
        thresholdDb: Float,
        ratio: Float,
        attackMs: Float,
        releaseMs: Float,
        makeupGainDb: Float,
        kneeDb: Float
    ) {
        this.sampleRate = sampleRate
        this.thresholdDb = thresholdDb
        this.ratio = ratio.coerceAtLeast(1f)
        this.attackMs = attackMs
        this.releaseMs = releaseMs
        this.makeupGainDb = makeupGainDb
        this.kneeDb = kneeDb
        recalculate()
    }

    private fun recalculate() {
        attackCoeff = if (attackMs > 0) exp(-1f / (attackMs * sampleRate / 1000f)).toFloat() else 0f
        releaseCoeff = if (releaseMs > 0) exp(-1f / (releaseMs * sampleRate / 1000f)).toFloat() else 0f
        makeupLinear = dbToLinear(makeupGainDb)
        thresholdLinear = dbToLinear(thresholdDb)
        kneeLinear = dbToLinear(kneeDb.coerceAtLeast(0f))
        slope = 1f - (1f / ratio)
    }

    fun reset() { envDb = -96f }

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val x = buffer[i]
            val inputAbs = abs(x).coerceAtLeast(1e-10f)
            val inputDb = 20f * log10(inputAbs)

            // Soft knee gain computation
            val overDb = inputDb - thresholdDb
            val gainReductionDb = computeGainReduction(overDb)

            // Envelope follower (ballistics)
            val coeff = if (gainReductionDb < envDb) attackCoeff else releaseCoeff
            envDb = coeff * envDb + (1f - coeff) * gainReductionDb

            // Apply gain + makeup
            val totalGainDb = -envDb + makeupGainDb
            val gainLin = dbToLinear(totalGainDb).coerceAtMost(10f)
            buffer[i] = x * gainLin
        }
    }

    private fun computeGainReduction(overDb: Float): Float {
        val halfKnee = kneeDb / 2f
        return when {
            kneeDb <= 0f -> {
                // Hard knee
                if (overDb > 0f) overDb * slope else 0f
            }
            overDb < -halfKnee -> 0f
            overDb > halfKnee -> overDb * slope
            else -> {
                // Soft knee (quadratic transition)
                val x = overDb + halfKnee
                overDb * (slope * x * x / (2f * kneeDb) + (1f - slope) * x / kneeDb) /
                    (overDb + halfKnee)
            }
        }
    }

    private fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)
}
