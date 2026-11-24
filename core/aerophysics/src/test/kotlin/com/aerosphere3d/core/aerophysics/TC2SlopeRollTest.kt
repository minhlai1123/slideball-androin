package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * TC2: Slope Roll Analytical Verification
 * Incline angle: 15 degrees, Slope length L = 20.0m
 * Solid sphere rolling without slipping: I = (2/5) M R^2
 * a_roll = (5/7) * g * sin(15 deg) = (5/7) * 19.62 * 0.258819 = 3.62716 m/s^2
 * v_exit = sqrt(2 * a_roll * L) = sqrt(2 * 3.62716 * 20) = 12.045 m/s
 * t_roll = sqrt(2 * L / a_roll) = sqrt(40 / 3.62716) = 3.321 s
 * Tolerance: +- 1.5%
 */
class TC2SlopeRollTest {

    @Test
    @DisplayName("TC2 - Slope Roll 15 deg pure rolling acceleration, velocity and time")
    fun testSlopeRollTC2() {
        val thetaDegrees = 15.0
        val thetaRadians = Math.toRadians(thetaDegrees)
        val length = 20.000 // 20m

        val g = PhysicsConstants.GRAVITY_MAGNITUDE // 19.620 m/s^2
        val expectedAcceleration = (5.0 / 7.0) * g * sin(thetaRadians) // 3.62716 m/s^2
        val expectedExitVelocity = sqrt(2.0 * expectedAcceleration * length) // 12.045 m/s
        val expectedTime = sqrt(2.0 * length / expectedAcceleration) // 3.321 s

        val engine = AeroPhysicsEngine()
        val dt = PhysicsConstants.FIXED_DELTA_TIME
        val simulatedTime = engine.stepPureRollingOnSlope(thetaRadians, length, dt)

        val timeErrorPercent = abs(simulatedTime - expectedTime) / expectedTime * 100.0
        assertTrue(timeErrorPercent <= 1.5, "Simulated time ($simulatedTime s) error ($timeErrorPercent%) must be <= 1.5%")

        assertEquals(3.627, expectedAcceleration, 0.005)
        assertEquals(12.045, expectedExitVelocity, 0.010)
        assertEquals(3.321, expectedTime, 0.010)
    }
}
