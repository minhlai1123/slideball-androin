package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.PhysicsConstants
import com.aerosphere3d.core.pal.Vector3D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class GameState {
    READY,
    PLAYING,
    PAUSED,
    RESPAWNING,
    LEVEL_COMPLETED,
    PLINKO_BONUS
}

/**
 * Event emitted by the gameplay engine for UI audio/visual feedback
 */
sealed class GameEvent {
    data class CoinCollected(val amount: Int, val position: Vector3D) : GameEvent()
    data class KeyCollected(val keyIndex: Int, val position: Vector3D) : GameEvent()
    data class LifeGained(val totalLives: Int, val position: Vector3D) : GameEvent()
    data class CheckpointReached(val checkpointId: String, val position: Vector3D) : GameEvent()
    data class ObstacleShattered(val cubeId: String, val position: Vector3D) : GameEvent()
    data class BoosterActivated(val boostSpeed: Double) : GameEvent()
    data class BallFellOffTrack(val position: Vector3D) : GameEvent()
    data class LevelFinished(val level: Int, val coinsEarned: Int) : GameEvent()
}

/**
 * Full 3D Rolling Ball Gameplay Simulation Engine
 * Encapsulates the arcade rolling physics, touch inputs, track constraints, and scoring
 */
class GameplayEngine(var course: LevelCourse = LevelCourseBuilder.buildLevel1()) {

    var state: GameState = GameState.READY
        private set

    // Ball kinematics
    var ballPosition: Vector3D = course.startPosition
        private set
    var ballVelocity: Vector3D = Vector3D.ZERO
        private set
    var ballRotationAngle: Double = 0.0
        private set

    // Progression & Wallet (100% Zero-Ads Persistence)
    var currentLevel: Int = course.levelNumber
        private set
    var lives: Int = 1 // Starts with 1 life as observed in video (max 5)
        private set
    var coinsCollected: Int = 0
        private set
    var totalSessionCoins: Int = 0
        private set
    var keysCollected: Int = 0
        private set

    // Checkpoint
    var lastCheckpointPos: Vector3D = course.startPosition
        private set

    // Controls
    private var forwardInput: Double = 0.0 // -1 (brake) to +1 (drive)
    private var steerInput: Double = 0.0   // -1 (left) to +1 (right)

    // Event listeners
    private val eventListeners = mutableListOf<(GameEvent) -> Unit>()

    fun addEventListener(listener: (GameEvent) -> Unit) {
        eventListeners.add(listener)
    }

    private fun emitEvent(event: GameEvent) {
        for (l in eventListeners) l(event)
    }

    fun startRoll() {
        if (state == GameState.READY) {
            state = GameState.PLAYING
            // Initial gentle push forward
            ballVelocity = Vector3D(0.0, 0.0, 4.0)
        }
    }

    fun setInputs(forward: Double, steer: Double) {
        forwardInput = forward.coerceIn(-1.0, 1.0)
        steerInput = steer.coerceIn(-1.0, 1.0)
    }

    fun loadLevel(levelNum: Int) {
        val clamped = levelNum.coerceIn(1, 40)
        course = LevelCourseBuilder.buildLevel(clamped)
        currentLevel = clamped
        ballPosition = course.startPosition
        ballVelocity = Vector3D.ZERO
        ballRotationAngle = 0.0
        coinsCollected = 0
        lastCheckpointPos = course.startPosition
        state = GameState.READY
    }

    /**
     * Fixed-rate gameplay simulation tick (60 Hz)
     */
    fun update(dt: Double = 1.0 / 60.0) {
        if (state != GameState.PLAYING) return

        val z = ballPosition.z
        val segment = course.getSegmentAt(z)

        // 1. Compute forces & accelerations
        var forwardForce = 0.0
        if (forwardInput > 0.0) {
            // Forward swipe acceleration: 12 m/s^2
            forwardForce += forwardInput * 12.0 * PhysicsConstants.BALL_MASS
        } else if (forwardInput < 0.0) {
            // Backward swipe brake deceleration: -18.5 m/s^2
            forwardForce += forwardInput * 18.5 * PhysicsConstants.BALL_MASS
        } else {
            // Arcade glide rolling resistance
            val currentSpeed = ballVelocity.z
            if (currentSpeed > 0.5) {
                forwardForce -= PhysicsConstants.ROLLING_RESISTANCE_COEFFICIENT * PhysicsConstants.GRAVITY_MAGNITUDE * PhysicsConstants.BALL_MASS
            }
        }

        // Lateral steering: direct responsive displacement mapped from TC7 (0.029 m/dp)
        var lateralForce = steerInput * 35.0 * PhysicsConstants.BALL_MASS

        // Gravity contribution based on slope
        val slopeAngle = segment?.getSlopeAt(z) ?: 0.0
        val gravitySlopeForce = -PhysicsConstants.GRAVITY_MAGNITUDE * sin(slopeAngle) * PhysicsConstants.BALL_MASS
        forwardForce += gravitySlopeForce

        // 2. Integrate linear velocities
        val accelZ = forwardForce / PhysicsConstants.BALL_MASS
        val accelX = lateralForce / PhysicsConstants.BALL_MASS

        var vz = (ballVelocity.z + accelZ * dt).coerceIn(0.0, 26.0) // Clamp max speed to 26 m/s
        var vx = (ballVelocity.x + accelX * dt).coerceIn(-7.5, 7.5)

        // Speed booster zone effect
        if (segment?.isBooster == true && abs(ballPosition.x) > 1.0) {
            vz = min(28.0, vz + 15.0 * dt)
            emitEvent(GameEvent.BoosterActivated(vz))
        }

        // 3. Update candidate position
        var newX = ballPosition.x + vx * dt
        val newZ = ballPosition.z + vz * dt

        // 4. Track boundaries and floor collision
        val targetFloorY = course.getTrackHeightAt(newZ)
        var newY = targetFloorY + PhysicsConstants.BALL_RADIUS

        // Side rails collision check
        val trackHalfWidth = (segment?.width ?: 4.0) / 2.0
        val railLimit = trackHalfWidth - PhysicsConstants.BALL_RADIUS

        if (segment?.hasLeftRail == true && newX < -railLimit) {
            newX = -railLimit
            vx = -vx * PhysicsConstants.RESTITUTION_COEFFICIENT // Elastic rail bounce
        } else if (segment?.hasRightRail == true && newX > railLimit) {
            newX = railLimit
            vx = -vx * PhysicsConstants.RESTITUTION_COEFFICIENT
        } else if (abs(newX) > trackHalfWidth + 0.8) {
            // Fallen off track! Apply full free fall
            newY = ballPosition.y + PhysicsConstants.WORLD_GRAVITY.y * dt
        }

        // 5. Update ball position & angular rotation
        ballPosition = Vector3D(newX, newY, newZ)
        ballVelocity = Vector3D(vx, 0.0, vz)
        ballRotationAngle += (vz / PhysicsConstants.BALL_RADIUS) * dt

        // 6. Check fall into void (Fall off edge)
        if (ballPosition.y < -8.0) {
            handleVoidFall()
            return
        }

        // 7. Check Destructible Obstacles (TC6 Option A Star Cubes)
        for (cube in course.obstacles) {
            if (!cube.isDestroyed && cube.checkCollision(ballPosition, PhysicsConstants.BALL_RADIUS)) {
                cube.shatter(ballVelocity, ballPosition)
                // Retain 71.19% kinetic energy (Option A target: v_exit = 0.8437 * v_in)
                val retainSpeedFactor = 0.8437
                ballVelocity = Vector3D(ballVelocity.x * 0.7, ballVelocity.y, ballVelocity.z * retainSpeedFactor)
                emitEvent(GameEvent.ObstacleShattered(cube.id, cube.position))
            }
            cube.update(dt)
        }

        // 8. Check Item Pickups
        for (item in course.items) {
            if (!item.isCollected && item.checkCollection(ballPosition, PhysicsConstants.BALL_RADIUS)) {
                when (item.type) {
                    CollectibleType.COIN -> {
                        coinsCollected += 10
                        totalSessionCoins += 10
                        emitEvent(GameEvent.CoinCollected(10, item.position))
                    }
                    CollectibleType.GOLDEN_KEY -> {
                        keysCollected += 1
                        emitEvent(GameEvent.KeyCollected(keysCollected, item.position))
                    }
                    CollectibleType.SPARE_BALL -> {
                        lives = min(5, lives + 1)
                        emitEvent(GameEvent.LifeGained(lives, item.position))
                    }
                    CollectibleType.CHECKPOINT -> {
                        lastCheckpointPos = Vector3D(0.0, course.getTrackHeightAt(item.position.z) + PhysicsConstants.BALL_RADIUS, item.position.z)
                        emitEvent(GameEvent.CheckpointReached(item.id, item.position))
                    }
                    CollectibleType.FINISH_LINE -> {
                        state = GameState.LEVEL_COMPLETED
                        emitEvent(GameEvent.LevelFinished(currentLevel, coinsCollected))
                    }
                }
            }
        }
    }

    private fun handleVoidFall() {
        emitEvent(GameEvent.BallFellOffTrack(ballPosition))
        if (lives > 1) {
            lives -= 1
        }
        // Respawn at last checkpoint safely
        ballPosition = lastCheckpointPos
        ballVelocity = Vector3D(0.0, 0.0, 3.0)
    }

    fun completePlinko() {
        state = GameState.READY
        val nextLevel = if (currentLevel < 40) currentLevel + 1 else 1
        loadLevel(nextLevel)
    }
}
