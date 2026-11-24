package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import com.aerosphere3d.core.pal.Vector3D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Track Segment definition for 3D Roller Course
 * Replicates the oak wood plank track, curved dips, hazard railings, and speed boosters
 */
data class TrackSegment(
    val startZ: Double,
    val endZ: Double,
    val width: Double = 4.0,
    val startY: Double = 0.0,
    val endY: Double = 0.0,
    val hasLeftRail: Boolean = true,
    val hasRightRail: Boolean = true,
    val isBooster: Boolean = false,
    val isCurvedDip: Boolean = false,
    val dipDepth: Double = 0.0
) {
    fun length(): Double = endZ - startZ

    fun containsZ(z: Double): Boolean = z in startZ..endZ

    /**
     * Compute ground height Y at a given Z position along the segment
     */
    fun getHeightAt(z: Double): Double {
        if (!containsZ(z)) return startY
        val t = (z - startZ) / (endZ - startZ)
        val linearY = startY + t * (endY - startY)
        return if (isCurvedDip) {
            // Sinusoidal dip simulating the smooth roller drop in Level 1 & Level 2
            linearY - dipDepth * sin(t * Math.PI)
        } else {
            linearY
        }
    }

    /**
     * Compute track surface slope angle in radians
     */
    fun getSlopeAt(z: Double): Double {
        val dz = 0.1
        val y1 = getHeightAt(max(startZ, z - dz))
        val y2 = getHeightAt(min(endZ, z + dz))
        return kotlin.math.atan2(y2 - y1, 2 * dz)
    }
}

/**
 * Destructible Purple Star Cube Obstacle (TC6 Option A Specification)
 * 6-DOF dynamic rigid body scattering when struck by the ball at high speed
 */
data class DestructibleCube(
    val id: String,
    var position: Vector3D,
    var velocity: Vector3D = Vector3D.ZERO,
    var angularVelocity: Vector3D = Vector3D.ZERO,
    var isDestroyed: Boolean = false,
    val size: Double = 0.60,
    val mass: Double = 0.80
) {
    fun update(deltaTime: Double) {
        if (isDestroyed) {
            // Apply gravity and update position after shattering
            velocity += PhysicsConstants.WORLD_GRAVITY * deltaTime
            position += velocity * deltaTime
        }
    }

    /**
     * Sphere vs Cube bounding box collision check
     */
    fun checkCollision(ballPos: Vector3D, ballRadius: Double): Boolean {
        if (isDestroyed) return false
        val half = size / 2.0
        val clampedX = max(position.x - half, min(ballPos.x, position.x + half))
        val clampedY = max(position.y - half, min(ballPos.y, position.y + half))
        val clampedZ = max(position.z - half, min(ballPos.z, position.z + half))
        val closestPoint = Vector3D(clampedX, clampedY, clampedZ)
        return (ballPos - closestPoint).length() < ballRadius
    }

    /**
     * Shatter and scatter cube with impulse when hit by ball
     */
    fun shatter(impactVelocity: Vector3D, impactPoint: Vector3D) {
        isDestroyed = true
        // Scatter outward and forward
        val scatterDir = (position - impactPoint).normalized()
        val forwardFactor = 0.75
        val scatterFactor = 0.65
        val verticalFactor = 4.5
        velocity = Vector3D(
            scatterDir.x * impactVelocity.length() * scatterFactor,
            verticalFactor + abs(scatterDir.y) * 2.0,
            impactVelocity.z * forwardFactor
        )
        angularVelocity = Vector3D(
            (Math.random() - 0.5) * 20.0,
            (Math.random() - 0.5) * 20.0,
            (Math.random() - 0.5) * 20.0
        )
    }
}

/**
 * Collectible Item: Coins, Keys, Spare Balls (Lives), Checkpoints
 */
enum class CollectibleType {
    COIN,
    GOLDEN_KEY,
    SPARE_BALL,
    CHECKPOINT,
    FINISH_LINE
}

data class TrackItem(
    val id: String,
    val type: CollectibleType,
    val position: Vector3D,
    var isCollected: Boolean = false,
    val radius: Double = 0.60
) {
    fun checkCollection(ballPos: Vector3D, ballRadius: Double): Boolean {
        if (isCollected) return false
        val dist = (ballPos - position).length()
        if (dist <= ballRadius + radius) {
            isCollected = true
            return true
        }
        return false
    }
}
