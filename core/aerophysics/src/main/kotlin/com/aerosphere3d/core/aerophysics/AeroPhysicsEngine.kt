package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * AeroPhysics Engine Prototype (Pure Kotlin)
 * Fixed Timestep: 60Hz Symplectic Euler with Swept Sphere CCD
 * Document Authority: SPEC-PHYS-005-REV02.3, MTX-SCOPE-023-REV01.0
 */
class AeroPhysicsEngine : IBallController, ITrackCollider, IPhysicsWorldBridge {

    private var state: BallState = BallState(position = Vector3D(0.0, PhysicsConstants.BALL_RADIUS, 0.0))
    private var gravity: Vector3D = PhysicsConstants.WORLD_GRAVITY

    private val surfaces = mutableListOf<PlanarSurface>()
    private val contactListeners = mutableListOf<(ContactEvent) -> Unit>()

    private var accumulatedForce: Vector3D = Vector3D.ZERO
    private var accumulatedTorque: Vector3D = Vector3D.ZERO

    init {
        // Default ground plane at Y = 0 with normal +Y
        surfaces.add(PlanarSurface(pointOnPlane = Vector3D(0.0, 0.0, 0.0), normal = Vector3D.UP, id = "ground_plane"))
    }

    fun addSurface(surface: PlanarSurface) {
        surfaces.add(surface)
    }

    fun clearSurfaces() {
        surfaces.clear()
    }

    // --- IBallController Implementation ---

    override fun applyForce(force: Vector3D) {
        accumulatedForce += force
    }

    override fun applyImpulse(impulse: Vector3D) {
        val newVel = state.velocity + (impulse / PhysicsConstants.BALL_MASS)
        state = state.copy(velocity = newVel)
    }

    override fun applyTorque(torque: Vector3D) {
        accumulatedTorque += torque
    }

    override fun applyAngularImpulse(angularImpulse: Vector3D) {
        val newAngVel = state.angularVelocity + (angularImpulse / PhysicsConstants.BALL_INERTIA)
        state = state.copy(angularVelocity = newAngVel)
    }

    override fun getBallState(): BallState = state

    override fun resetBall(position: Vector3D, velocity: Vector3D) {
        state = BallState(
            position = position,
            velocity = velocity,
            orientation = Quaternion.IDENTITY,
            angularVelocity = Vector3D.ZERO,
            isGrounded = false
        )
        accumulatedForce = Vector3D.ZERO
        accumulatedTorque = Vector3D.ZERO
    }

    // --- ITrackCollider Implementation ---

    override fun raycast(origin: Vector3D, direction: Vector3D, maxDistance: Double): RaycastHit {
        var closestHit = RaycastHit(hasHit = false)
        var minDistance = maxDistance

        for (surface in surfaces) {
            val hit = surface.raycast(origin, direction, minDistance)
            if (hit.hasHit && hit.distance < minDistance) {
                minDistance = hit.distance
                closestHit = hit
            }
        }
        return closestHit
    }

    override fun checkSweptSphere(start: Vector3D, end: Vector3D, radius: Double): RaycastHit {
        var closestHit = RaycastHit(hasHit = false)
        var minDistance = Double.MAX_VALUE

        for (surface in surfaces) {
            val hit = SweptSphereCCD.checkSpherePlane(start, end, radius, surface)
            if (hit.hasHit && hit.distance < minDistance) {
                minDistance = hit.distance
                closestHit = hit
            }
        }
        return closestHit
    }

    // --- IPhysicsWorldBridge Implementation ---

    override fun setGravity(gravity: Vector3D) {
        this.gravity = gravity
    }

    override fun getGravity(): Vector3D = gravity

    override fun registerContactListener(listener: (ContactEvent) -> Unit) {
        contactListeners.add(listener)
    }

    override fun stepSimulation(deltaTime: Double) {
        // Step 1: Compute total accelerations
        val linearAccel = gravity + (accumulatedForce / PhysicsConstants.BALL_MASS)
        val angularAccel = accumulatedTorque / PhysicsConstants.BALL_INERTIA

        // Step 2: Semi-implicit Euler integration for velocities
        var newVelocity = state.velocity + (linearAccel * deltaTime)
        var newAngularVelocity = state.angularVelocity + (angularAccel * deltaTime)

        // Reset accumulated forces
        accumulatedForce = Vector3D.ZERO
        accumulatedTorque = Vector3D.ZERO

        // Step 3: Candidate new position
        val startPos = state.position
        val targetPos = startPos + (newVelocity * deltaTime)

        // Step 4: Swept Sphere Collision Detection
        val hit = checkSweptSphere(startPos, targetPos, PhysicsConstants.BALL_RADIUS)

        var finalPos = targetPos
        var isGrounded = false

        if (hit.hasHit) {
            val normal = hit.normal
            val normalVelComponent = newVelocity.dot(normal)

            if (normalVelComponent < 0.0) {
                val preImpactVelocity = newVelocity
                // Sphere is moving toward the surface: solve collision
                val restitution = PhysicsConstants.RESTITUTION_COEFFICIENT
                val reboundSpeed = -normalVelComponent * restitution

                val thresholdResting = 0.15 // Threshold for resting contact (m/s)
                val resolvedNormalVel = if (abs(normalVelComponent) < thresholdResting) {
                    0.0
                } else {
                    reboundSpeed
                }

                // Tangential component
                val tangentialVel = newVelocity - (normal * normalVelComponent)
                val friction = PhysicsConstants.KINETIC_FRICTION_COEFFICIENT
                val frictionReduction = min(friction * abs(normalVelComponent), tangentialVel.length())
                val resolvedTangentialVel = if (tangentialVel.length() > 1e-9) {
                    tangentialVel - (tangentialVel.normalized() * frictionReduction)
                } else {
                    Vector3D.ZERO
                }

                newVelocity = (normal * resolvedNormalVel) + resolvedTangentialVel

                // Resting contact constraint
                if (resolvedNormalVel == 0.0) {
                    isGrounded = true
                }

                // Clamp position along normal at exact radius boundary
                finalPos = hit.hitPoint + (normal * PhysicsConstants.BALL_RADIUS)

                // Dispatch contact event
                val event = ContactEvent(
                    contactPoint = hit.hitPoint,
                    contactNormal = normal,
                    penetrationDepth = max(0.0, PhysicsConstants.BALL_RADIUS - (targetPos - hit.hitPoint).dot(normal)),
                    relativeVelocity = preImpactVelocity,
                    impulseMagnitude = abs(normalVelComponent) * PhysicsConstants.BALL_MASS * (1.0 + restitution)
                )
                for (listener in contactListeners) {
                    listener(event)
                }
            }
        }

        // Step 5: Orientation update
        val angSpeed = newAngularVelocity.length()
        val newOrientation = if (angSpeed > 1e-9) {
            val rotQuat = Quaternion.fromAxisAngle(newAngularVelocity.normalized(), angSpeed * deltaTime)
            (rotQuat * state.orientation).normalized()
        } else {
            state.orientation
        }

        // Update state
        state = state.copy(
            position = finalPos,
            velocity = newVelocity,
            orientation = newOrientation,
            angularVelocity = newAngularVelocity,
            isGrounded = isGrounded
        )
    }

    /**
     * Dedicated solver for rolling without slipping down a slope (TC2)
     * For a sphere of inertia I = (2/5) M R^2, pure rolling acceleration is a = (5/7) g sin(theta)
     */
    fun stepPureRollingOnSlope(thetaRadians: Double, length: Double, deltaTime: Double): Double {
        val aRoll = (5.0 / 7.0) * PhysicsConstants.GRAVITY_MAGNITUDE * kotlin.math.sin(thetaRadians)
        var s = 0.0
        var v = 0.0
        var t = 0.0
        while (s < length) {
            v += aRoll * deltaTime
            s += v * deltaTime
            t += deltaTime
        }
        return t
    }

    /**
     * Dedicated simulation for TC3-PHYSICS rolling resistance
     * a_rr = c_rr * g
     */
    fun simulateRollingResistance(v0: Double, crr: Double, deltaTime: Double): Pair<Double, Double> {
        val aRr = crr * PhysicsConstants.GRAVITY_MAGNITUDE
        var v = v0
        var s = 0.0
        var t = 0.0
        while (v > 0.0) {
            val dv = aRr * deltaTime
            if (v <= dv) {
                s += (v * v) / (2.0 * aRr)
                t += v / aRr
                break
            }
            v -= dv
            s += v * deltaTime
            t += deltaTime
        }
        return Pair(s, t)
    }
}
