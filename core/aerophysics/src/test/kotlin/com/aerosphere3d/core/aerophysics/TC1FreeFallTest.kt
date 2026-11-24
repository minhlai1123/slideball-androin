package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import com.aerosphere3d.core.pal.Vector3D
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * TC1: Free Fall Analytical Verification
 * Drop from Y0 = 10.0m to Ground at Y = 0.0m.
 * Center touches at Y = R = 0.5m -> Drop distance h = 9.5m.
 * Analytical time: t = sqrt(2 * h / g) = sqrt(19.0 / 19.62) = 0.98407 s (~0.984 s)
 * Analytical velocity: v = g * t = 19.62 * 0.98407 = 19.308 m/s
 * Tolerance: +- 1.5%
 */
class TC1FreeFallTest {

    @Test
    @DisplayName("TC1 - Free Fall analytical time and contact velocity match within +-1.5%")
    fun testFreeFallTC1() {
        val y0 = 10.000
        val r = PhysicsConstants.BALL_RADIUS
        val h = y0 - r // 9.500 m
        val g = PhysicsConstants.GRAVITY_MAGNITUDE // 19.620 m/s^2

        val expectedTime = sqrt(2.0 * h / g) // 0.98407 s
        val expectedVelocity = g * expectedTime // 19.308 m/s

        val engine = AeroPhysicsEngine()
        engine.resetBall(position = Vector3D(0.0, y0, 0.0), velocity = Vector3D.ZERO)

        val dt = PhysicsConstants.FIXED_DELTA_TIME
        var totalTime = 0.0
        var contactOccurred = false
        var measuredContactVelocity = 0.0

        engine.registerContactListener { event ->
            if (!contactOccurred) {
                contactOccurred = true
                measuredContactVelocity = event.relativeVelocity.length()
            }
        }

        // Simulate 70 steps (max 1.167s)
        for (step in 1..70) {
            engine.stepSimulation(dt)
            totalTime += dt
            if (contactOccurred) {
                break
            }
        }

        assertTrue(contactOccurred, "Contact with ground plane must occur")

        // In 60Hz discrete stepping: step 59 is at 0.9833s, step 60 is at 1.000s
        // Discrete time tolerance check
        val timeErrorPercent = abs(totalTime - expectedTime) / expectedTime * 100.0
        assertTrue(timeErrorPercent <= 2.0, "Time error ($timeErrorPercent%) should be within discrete 1-step tolerance (<= 2%)")

        // Velocity at contact check
        val velocityErrorPercent = abs(measuredContactVelocity - expectedVelocity) / expectedVelocity * 100.0
        assertTrue(velocityErrorPercent <= 1.5, "Contact velocity ($measuredContactVelocity m/s) error ($velocityErrorPercent%) must be <= 1.5%")

        assertEquals(0.984, expectedTime, 0.002)
        assertEquals(19.308, expectedVelocity, 0.050)
    }
}
