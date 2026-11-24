package com.aerosphere3d.core.pal

/**
 * Rigid body dynamic state representation
 */
data class BallState(
    val position: Vector3D = Vector3D.ZERO,
    val velocity: Vector3D = Vector3D.ZERO,
    val orientation: Quaternion = Quaternion.IDENTITY,
    val angularVelocity: Vector3D = Vector3D.ZERO, // in rad/s
    val isGrounded: Boolean = false
)

/**
 * Geometric raycast hit result
 */
data class RaycastHit(
    val hasHit: Boolean = false,
    val hitPoint: Vector3D = Vector3D.ZERO,
    val normal: Vector3D = Vector3D.UP,
    val distance: Double = 0.0,
    val surfaceMaterialId: String = "default"
)

/**
 * Physical contact event for collision resolution
 */
data class ContactEvent(
    val contactPoint: Vector3D,
    val contactNormal: Vector3D,
    val penetrationDepth: Double,
    val relativeVelocity: Vector3D,
    val impulseMagnitude: Double
)
