package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * TC3-PHYSICS: Natural Rolling Resistance Analytical Verification
 * Model: Planar deceleration under constant rolling resistance coefficient c_rr = 0.020
 * Initial velocity: v0 = 15.0 m/s
 * Deceleration: a_rr = c_rr * g = 0.020 * 19.620 = 0.3924 m/s^2
 * Stopping distance: S = v0^2 / (2 * a_rr) = 225.0 / 0.7848 = 286.697 m (~286.70 m)
 * Stopping time: t = v0 / a_rr = 15.0 / 0.3924 = 38.226 s (~38.23 s)
 * Tolerance: +- 2.0%
 */
class TC3PhysicsRollingResistanceTest {

    @Test
    @DisplayName("TC3-PHYSICS - Planar rolling resistance stopping distance and time")
    fun testRollingResistanceTC3() {
        val v0 = 15.000
        val crr = PhysicsConstants.ROLLING_RESISTANCE_COEFFICIENT // 0.020
        val g = PhysicsConstants.GRAVITY_MAGNITUDE // 19.620

        val expectedDecel = crr * g // 0.3924 m/s^2
        val expectedDistance = (v0 * v0) / (2.0 * expectedDecel) // 286.697 m
        val expectedTime = v0 / expectedDecel // 38.226 s

        val engine = AeroPhysicsEngine()
        val dt = PhysicsConstants.FIXED_DELTA_TIME
        val (simDistance, simTime) = engine.simulateRollingResistance(v0, crr, dt)

        val distErrorPercent = abs(simDistance - expectedDistance) / expectedDistance * 100.0
        val timeErrorPercent = abs(simTime - expectedTime) / expectedTime * 100.0

        assertTrue(distErrorPercent <= 2.0, "Stopping distance ($simDistance m) error ($distErrorPercent%) must be <= 2.0%")
        assertTrue(timeErrorPercent <= 2.0, "Stopping time ($simTime s) error ($timeErrorPercent%) must be <= 2.0%")

        assertEquals(0.3924, expectedDecel, 1e-4)
        assertEquals(286.70, expectedDistance, 0.10)
        assertEquals(38.23, expectedTime, 0.10)
    }
}
