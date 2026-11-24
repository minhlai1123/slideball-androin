package com.aerosphere3d.core.pal

/**
 * AeroSphere 3D - Shared Physics Contract Constants
 * Document Authority: SPEC-PHYS-005-REV02.3, MTX-SCOPE-023-REV01.0
 * Standard SI Units: Meter (m), Second (s), Kilogram (kg), Newton (N), Radian (rad)
 */
object PhysicsConstants {
    // World Gravity - 2.0g Arcade Model (FROZEN BY PROJECT OWNER)
    const val GRAVITY_MAGNITUDE: Double = 19.620
    val WORLD_GRAVITY: Vector3D = Vector3D(0.0, -19.620, 0.0)

    // Simulation Clock
    const val FIXED_TIMESTEP_HZ: Double = 60.0
    const val FIXED_DELTA_TIME: Double = 1.0 / FIXED_TIMESTEP_HZ // 0.016666666666666666 s

    // Ball Rigid Body Dynamics
    const val BALL_RADIUS: Double = 0.500 // 50 cm
    const val BALL_MASS: Double = 5.000 // 5 kg
    const val BALL_INERTIA: Double = (2.0 / 5.0) * BALL_MASS * BALL_RADIUS * BALL_RADIUS // 0.500 kg*m^2

    // Material & Contact Properties
    const val RESTITUTION_COEFFICIENT: Double = 0.180
    const val KINETIC_FRICTION_COEFFICIENT: Double = 0.450
    const val STATIC_FRICTION_COEFFICIENT: Double = 0.650
    const val ROLLING_RESISTANCE_COEFFICIENT: Double = 0.020 // TC3-PHYSICS

    // Gameplay Regulation Forces
    const val ARCADE_GLIDE_DAMPING: Double = 6.164 // TC3-ARCADE deceleration m/s^2
    const val EMERGENCY_BRAKE_DECELERATION: Double = 18.500 // TC4 deceleration m/s^2

    // TC6 Option A Approved Target Specifications (Pending Prototype Validation)
    const val TC6_INITIAL_VELOCITY: Double = 16.000
    const val TC6_TARGET_EXIT_VELOCITY: Double = 13.500
    const val TC6_EXIT_VELOCITY_TOLERANCE: Double = 0.500
    const val TC6_KINETIC_ENERGY_RETAINED_RATIO: Double = 0.711914 // 71.19%
    const val TC6_KINETIC_ENERGY_LOSS_RATIO: Double = 0.288086 // 28.81%

    // TC7 Input Calibration Target (Provisional)
    const val TC7_INPUT_SWIPE_DP: Double = 50.0
    const val TC7_TARGET_DISPLACEMENT_X: Double = 1.450
    const val TC7_SENSITIVITY_K_INPUT: Double = 0.029 // m/dp
    const val TC7_ASSUMED_RESPONSE_WINDOW: Double = 1.000 // seconds (design assumption)

    // TC8 Touch Latency Budget (Target - Not Measured)
    const val TC8_MAX_TOUCH_TO_PHOTON_LATENCY_MS: Double = 35.0
}
