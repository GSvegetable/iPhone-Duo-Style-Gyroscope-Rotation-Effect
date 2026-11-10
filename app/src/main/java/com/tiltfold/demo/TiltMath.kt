package com.tiltfold.demo

import kotlin.math.abs
import kotlin.math.sqrt

/** Converts a gravity vector into a stable normalized roll signal. */
internal object TiltMath {
    private const val FULL_TILT_G = 0.72f
    private const val DEAD_ZONE = 0.012f

    fun normalize(gravityX: Float, gravityY: Float, gravityZ: Float, displayRotation: Int = 0): Float {
        val rollAxis = when (displayRotation) {
            1 -> gravityY
            2 -> -gravityX
            3 -> -gravityY
            else -> gravityX
        }
        val magnitude = sqrt(gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ)
        if (!magnitude.isFinite() || magnitude < 1f) return 0f
        return (rollAxis / magnitude / FULL_TILT_G).coerceIn(-1f, 1f)
    }

    fun applyDeadZone(value: Float): Float =
        if (abs(value) < DEAD_ZONE) 0f else value
}

/** First-order low-pass filter to reduce sensor jitter. */
internal class TiltSmoother(private val alpha: Float = 0.18f) {
    private var value = 0f

    fun update(target: Float): Float {
        val safeTarget = if (target.isFinite()) target.coerceIn(-1f, 1f) else 0f
        value += (safeTarget - value) * alpha.coerceIn(0.01f, 1f)
        return TiltMath.applyDeadZone(value)
    }

    fun reset() { value = 0f }
}
