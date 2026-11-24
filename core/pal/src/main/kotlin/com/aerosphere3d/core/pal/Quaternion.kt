package com.aerosphere3d.core.pal

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard Hamilton Quaternion representation (x, y, z, w)
 */
data class Quaternion(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val w: Double = 1.0
) {
    fun normalized(): Quaternion {
        val len = sqrt(x * x + y * y + z * z + w * w)
        return if (len > 1e-9) {
            Quaternion(x / len, y / len, z / len, w / len)
        } else {
            IDENTITY
        }
    }

    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(
            w = w * other.w - x * other.x - y * other.y - z * other.z,
            x = w * other.x + x * other.w + y * other.z - z * other.y,
            y = w * other.y - x * other.z + y * other.w + z * other.x,
            z = w * other.z + x * other.y - y * other.x + z * other.w
        ).normalized()
    }

    fun rotate(v: Vector3D): Vector3D {
        val qv = Vector3D(x, y, z)
        val uv = qv.cross(v)
        val uuv = qv.cross(uv)
        return v + (uv * (2.0 * w)) + (uuv * 2.0)
    }

    companion object {
        val IDENTITY = Quaternion(0.0, 0.0, 0.0, 1.0)

        fun fromAxisAngle(axis: Vector3D, angleRadians: Double): Quaternion {
            val halfAngle = angleRadians * 0.5
            val sinHalf = sin(halfAngle)
            val normAxis = axis.normalized()
            return Quaternion(
                x = normAxis.x * sinHalf,
                y = normAxis.y * sinHalf,
                z = normAxis.z * sinHalf,
                w = cos(halfAngle)
            ).normalized()
        }
    }
}
