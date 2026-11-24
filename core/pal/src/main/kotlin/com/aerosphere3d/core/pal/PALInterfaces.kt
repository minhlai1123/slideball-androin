package com.aerosphere3d.core.pal

/**
 * Controller interface to interact with the main rolling ball rigid body
 */
interface IBallController {
    fun applyForce(force: Vector3D)
    fun applyImpulse(impulse: Vector3D)
    fun applyTorque(torque: Vector3D)
    fun applyAngularImpulse(angularImpulse: Vector3D)
    fun getBallState(): BallState
    fun resetBall(position: Vector3D, velocity: Vector3D = Vector3D.ZERO)
}

/**
 * Interface to query track collision geometry
 */
interface ITrackCollider {
    fun raycast(origin: Vector3D, direction: Vector3D, maxDistance: Double): RaycastHit
    fun checkSweptSphere(start: Vector3D, end: Vector3D, radius: Double): RaycastHit
}

/**
 * Bridge interface synchronizing physics stepping and world parameters
 */
interface IPhysicsWorldBridge {
    fun stepSimulation(deltaTime: Double)
    fun setGravity(gravity: Vector3D)
    fun getGravity(): Vector3D
    fun registerContactListener(listener: (ContactEvent) -> Unit)
}
