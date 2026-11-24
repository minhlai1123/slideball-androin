package com.aerosphere3d.core.pal

import kotlin.math.sqrt

/**
 * Standard 3D Cartesian Vector in Right-Handed coordinates (+X right, +Y up, +Z forward)
 * Units: Meters (m) for position/displacement, m/s for velocity, m/s^2 for acceleration, N for force
 */
data class Vector3D(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    operator fun plus(other: Vector3D): Vector3D = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D): Vector3D = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double): Vector3D = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double): Vector3D = Vector3D(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus(): Vector3D = Vector3D(-x, -y, -z)

    fun dot(other: Vector3D): Double = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x
    )

    fun lengthSquared(): Double = x * x + y * y + z * z
    fun length(): Double = sqrt(lengthSquared())

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 1e-9) this / len else ZERO
    }

    fun distanceTo(other: Vector3D): Double = (this - other).length()

    companion object {
        val ZERO = Vector3D(0.0, 0.0, 0.0)
        val UP = Vector3D(0.0, 1.0, 0.0)
        val FORWARD = Vector3D(0.0, 0.0, 1.0)
        val RIGHT = Vector3D(1.0, 0.0, 0.0)
    }
}
