package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.RaycastHit
import com.aerosphere3d.core.pal.Vector3D

/**
 * Swept Sphere Continuous Collision Detection (CCD)
 * Prevents tunneling when moving at velocities up to 35 m/s across thin planar colliders
 */
object SweptSphereCCD {

    fun checkSpherePlane(
        start: Vector3D,
        end: Vector3D,
        radius: Double,
        plane: PlanarSurface
    ): RaycastHit {
        val d0 = plane.signedDistanceTo(start)
        val d1 = plane.signedDistanceTo(end)

        // Case 1: Sphere is completely in front of plane and doesn't reach contact boundary
        if (d0 >= radius && d1 >= radius) {
            return RaycastHit(hasHit = false)
        }

        // Case 2: Sphere already penetrating at start
        if (d0 < radius && d0 >= 0.0) {
            val contactPoint = start - (plane.normalizedNormal * d0)
            return RaycastHit(
                hasHit = true,
                hitPoint = contactPoint,
                normal = plane.normalizedNormal,
                distance = 0.0,
                surfaceMaterialId = plane.id
            )
        }

        // Case 3: Sphere crosses contact boundary during the step (Swept contact)
        val delta = d1 - d0
        if (kotlin.math.abs(delta) > 1e-9) {
            val t = (radius - d0) / delta
            if (t in 0.0..1.0) {
                val centerAtHit = start + ((end - start) * t)
                val contactPoint = centerAtHit - (plane.normalizedNormal * radius)
                val dist = (centerAtHit - start).length()
                return RaycastHit(
                    hasHit = true,
                    hitPoint = contactPoint,
                    normal = plane.normalizedNormal,
                    distance = dist,
                    surfaceMaterialId = plane.id
                )
            }
        }

        return RaycastHit(hasHit = false)
    }
}
