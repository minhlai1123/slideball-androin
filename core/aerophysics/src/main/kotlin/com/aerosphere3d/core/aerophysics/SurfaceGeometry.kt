package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import com.aerosphere3d.core.pal.RaycastHit
import com.aerosphere3d.core.pal.Vector3D

/**
 * Geometric surface for track collision detection and physical response
 */
data class PlanarSurface(
    val pointOnPlane: Vector3D,
    val normal: Vector3D,
    val restitution: Double = PhysicsConstants.RESTITUTION_COEFFICIENT,
    val friction: Double = PhysicsConstants.KINETIC_FRICTION_COEFFICIENT,
    val id: String = "plane"
) {
    val normalizedNormal: Vector3D = normal.normalized()

    fun signedDistanceTo(point: Vector3D): Double {
        return (point - pointOnPlane).dot(normalizedNormal)
    }

    fun raycast(origin: Vector3D, direction: Vector3D, maxDistance: Double): RaycastHit {
        val denom = normalizedNormal.dot(direction)
        if (kotlin.math.abs(denom) < 1e-9) {
            return RaycastHit(hasHit = false)
        }
        val t = (pointOnPlane - origin).dot(normalizedNormal) / denom
        if (t in 0.0..maxDistance) {
            val hitPoint = origin + (direction * t)
            return RaycastHit(
                hasHit = true,
                hitPoint = hitPoint,
                normal = normalizedNormal,
                distance = t,
                surfaceMaterialId = id
            )
        }
        return RaycastHit(hasHit = false)
    }
}
