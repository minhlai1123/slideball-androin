package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TC7: Steering Sensitivity Calibration Specification
 * Swipe gesture: 50 dp
 * Target lateral displacement: Delta X = 1.45 +- 0.08 m
 * Sensitivity coefficient: K_input = 1.45 / 50.0 = 0.029 m/dp
 * Assumed response window: 1.0s (Design assumption, not yet measured on device)
 * Status: PROVISIONAL CALIBRATION
 */
class TC7ProvisionalSpecTest {

    @Test
    @DisplayName("TC7 Specification - Verify Steering Mapping (Provisional)")
    fun testTC7SteeringSpecification() {
        val swipeDp = PhysicsConstants.TC7_INPUT_SWIPE_DP
        val targetDeltaX = PhysicsConstants.TC7_TARGET_DISPLACEMENT_X
        val expectedSensitivity = targetDeltaX / swipeDp

        assertEquals(0.029, expectedSensitivity, 1e-4)
        assertEquals(PhysicsConstants.TC7_SENSITIVITY_K_INPUT, expectedSensitivity, 1e-6)
        println("[TC7 STATUS: PROVISIONAL] Sensitivity K_input = $expectedSensitivity m/dp over assumed window ${PhysicsConstants.TC7_ASSUMED_RESPONSE_WINDOW}s")
    }
}
