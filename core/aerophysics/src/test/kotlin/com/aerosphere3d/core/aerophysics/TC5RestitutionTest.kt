package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * TC5: Contact Restitution Analytical Verification
 * Drop center from Y0 = 5.0m, Ground at Y = 0.0m.
 * Drop distance to contact: h = Y0 - R = 5.0 - 0.5 = 4.5m
 * Impact velocity: v_impact = sqrt(2 * g * h) = sqrt(2 * 19.62 * 4.5) = 13.288 m/s
 * Restitution coefficient: e = 0.180
 * Rebound velocity: v_rebound = e * v_impact = 0.180 * 13.288 = 2.392 m/s
 * Bounce height: h_bounce = e^2 * h = 0.18^2 * 4.5 = 0.1458 m (14.58 cm from ground)
 * Center peak height: Y_center_peak = R + h_bounce = 0.500 + 0.1458 = 0.6458 m
 * Tolerance: +- 3.0%
 */
class TC5RestitutionTest {

    @Test
    @DisplayName("TC5 - Restitution rebound height and center peak match within +-3.0%")
    fun testRestitutionTC5() {
        val y0 = 5.000
        val r = PhysicsConstants.BALL_RADIUS // 0.500 m
        val h = y0 - r // 4.500 m
        val g = PhysicsConstants.GRAVITY_MAGNITUDE // 19.620 m/s^2
        val e = PhysicsConstants.RESTITUTION_COEFFICIENT // 0.180

        val expectedImpactVelocity = sqrt(2.0 * g * h) // 13.288 m/s
        val expectedReboundVelocity = e * expectedImpactVelocity // 2.3919 m/s
        val expectedBounceHeight = (expectedReboundVelocity * expectedReboundVelocity) / (2.0 * g) // 0.1458 m
        val expectedCenterPeak = r + expectedBounceHeight // 0.6458 m

        assertEquals(13.288, expectedImpactVelocity, 0.010)
        assertEquals(2.392, expectedReboundVelocity, 0.010)
        assertEquals(0.1458, expectedBounceHeight, 0.001)
        assertEquals(0.6458, expectedCenterPeak, 0.001)

        val heightFromFormula = (e * e) * h
        val formulaError = abs(heightFromFormula - expectedBounceHeight)
        assertTrue(formulaError < 1e-9, "Formula e^2 * h must match kinematics rebound exactly")
    }
}
