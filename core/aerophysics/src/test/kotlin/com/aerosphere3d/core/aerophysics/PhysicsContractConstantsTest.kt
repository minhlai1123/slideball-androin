package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhysicsContractConstantsTest {

    @Test
    @DisplayName("Verify World Gravity is locked to 19.62 m/s^2 (2.0g Arcade)")
    fun testWorldGravityInvariant() {
        assertEquals(19.620, PhysicsConstants.GRAVITY_MAGNITUDE, 1e-6)
        assertEquals(0.0, PhysicsConstants.WORLD_GRAVITY.x, 1e-6)
        assertEquals(-19.620, PhysicsConstants.WORLD_GRAVITY.y, 1e-6)
        assertEquals(0.0, PhysicsConstants.WORLD_GRAVITY.z, 1e-6)
    }

    @Test
    @DisplayName("Verify Fixed Timestep is locked to 60.0 Hz")
    fun testFixedTimestep() {
        assertEquals(60.0, PhysicsConstants.FIXED_TIMESTEP_HZ, 1e-6)
        assertEquals(1.0 / 60.0, PhysicsConstants.FIXED_DELTA_TIME, 1e-6)
    }

    @Test
    @DisplayName("Verify Solid Sphere Moment of Inertia I = (2/5) * M * R^2")
    fun testMomentOfInertia() {
        val expectedInertia = (2.0 / 5.0) * PhysicsConstants.BALL_MASS * PhysicsConstants.BALL_RADIUS * PhysicsConstants.BALL_RADIUS
        assertEquals(0.500, expectedInertia, 1e-6)
        assertEquals(expectedInertia, PhysicsConstants.BALL_INERTIA, 1e-6)
    }
}
