package com.tiltfold.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class TiltMathTest {
    @Test fun levelGravityProducesZeroTilt() {
        assertEquals(0f, TiltMath.normalize(0f, 0f, 9.81f), 0.0001f)
    }

    @Test fun tiltIsNormalizedAndClamped() {
        assertEquals(1f, TiltMath.normalize(9.81f, 0f, 0f), 0.0001f)
        assertEquals(-1f, TiltMath.normalize(-9.81f, 0f, 0f), 0.0001f)
    }

    @Test fun tiltAxisFollowsDisplayRotation() {
        assertEquals(1f, TiltMath.normalize(0f, 9.81f, 0f, 1), 0.0001f)
        assertEquals(1f, TiltMath.normalize(-9.81f, 0f, 0f, 2), 0.0001f)
        assertEquals(1f, TiltMath.normalize(0f, -9.81f, 0f, 3), 0.0001f)
    }

    @Test fun invalidOrTinyVectorsAreSafe() {
        assertEquals(0f, TiltMath.normalize(Float.NaN, 0f, 0f), 0.0001f)
        assertEquals(0f, TiltMath.normalize(0f, 0f, 0f), 0.0001f)
    }

    @Test fun smootherConvergesWithoutOvershoot() {
        val smoother = TiltSmoother(alpha = 0.25f)
        var result = 0f
        repeat(40) { result = smoother.update(0.8f) }
        assertEquals(0.8f, result, 0.001f)
        assert(result in 0f..0.8f)
    }

    @Test fun smootherRejectsNonFiniteValuesAndCanReset() {
        val smoother = TiltSmoother()
        repeat(10) { smoother.update(0.7f) }
        smoother.update(Float.NaN)
        smoother.reset()
        assertEquals(0f, smoother.update(0f), 0.0001f)
    }
}
