package com.aerosphere3d.core.aerophysics

import com.aerosphere3d.core.pal.Vector3D
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class LevelDifficulty(val label: String, val badgeColor: Long) {
    EASY("EASY", 0xFF22C55E),       // Vibrant Green
    NORMAL("NORMAL", 0xFF38BDF8),   // Sky Blue
    HARD("HARD", 0xFFF59E0B),       // Amber Gold
    EXPERT("EXPERT", 0xFFEF4444)    // Crimson Red
}

/**
 * Course Level Definition with metadata, track segments, obstacles, and collectibles
 */
class LevelCourse(
    val levelNumber: Int,
    val name: String,
    val difficulty: LevelDifficulty,
    val totalLength: Double,
    val segments: List<TrackSegment>,
    val obstacles: List<DestructibleCube>,
    val items: List<TrackItem>,
    val startPosition: Vector3D = Vector3D(0.0, 0.5, 2.0),
    val parTimeSeconds: Int = 30
) {
    fun getTrackHeightAt(z: Double): Double {
        for (segment in segments) {
            if (segment.containsZ(z)) {
                return segment.getHeightAt(z)
            }
        }
        return 0.0
    }

    fun getSegmentAt(z: Double): TrackSegment? {
        return segments.find { it.containsZ(z) }
    }
}

/**
 * 40-Level Progressive Course Builder
 * Procedurally generates 40 unique, deterministic, handcrafted-quality levels
 * covering Easy, Normal, Hard, and Expert difficulties.
 */
object LevelCourseBuilder {

    private val LEVEL_NAMES = listOf(
        // World 1: Timber Highlands (Easy, Levels 1-10)
        "Green Meadow", "Timber Slope", "Gentle Breeze", "Forest Pathway", "Oak Rapids",
        "River Crossing", "Sunlit Bridge", "Whispering Pines", "Maple Ridge", "Timber Summit",
        // World 2: Canyon Springs (Normal, Levels 11-20)
        "Canyon Descent", "Redwood Chasm", "S-Curve Valley", "Rocky Slopes", "Echo Gorge",
        "Boulder Run", "Narrow Ledge", "Speedway Canyon", "Twin Barricades", "Chasm Crossroad",
        // World 3: Alpine Ridge (Hard, Levels 21-30)
        "Alpine Crest", "Frostbite Dip", "Glacier Slalom", "Razor Edge", "Skyline Leap",
        "Spiral Ridge", "Avalanche Run", "Thunder Peak", "Mist Canyon", "Sky Sanctuary",
        // World 4: Apex Skyway (Expert, Levels 31-40)
        "Apex Citadel", "Titan's Rampart", "Steel & Timber", "Vortex Way", "Highwire Sprint",
        "Labyrinth Curve", "Meteor Drop", "Zenith Runway", "Shadow Pass", "The Titan's Gauntlet"
    )

    fun getLevelName(levelNumber: Int): String {
        val idx = (levelNumber - 1).coerceIn(0, LEVEL_NAMES.size - 1)
        return LEVEL_NAMES[idx]
    }

    fun getDifficulty(levelNumber: Int): LevelDifficulty {
        return when (levelNumber) {
            in 1..10 -> LevelDifficulty.EASY
            in 11..20 -> LevelDifficulty.NORMAL
            in 21..30 -> LevelDifficulty.HARD
            else -> LevelDifficulty.EXPERT
        }
    }

    /**
     * Build any level from 1 to 40 with balanced physics, track geometry, obstacles, and collectibles
     */
    fun buildLevel(levelNumber: Int): LevelCourse {
        val clampedLevel = levelNumber.coerceIn(1, 40)
        val name = getLevelName(clampedLevel)
        val difficulty = getDifficulty(clampedLevel)

        // Deterministic PRNG seeded by level number for reproducible procedural generation
        val rng = Random(clampedLevel * 1337 + 42)

        // Progressive track length: 100m (Level 1) to 256m (Level 40)
        val totalLength = 100.0 + (clampedLevel - 1) * 4.0
        val parTime = (totalLength / 5.5).toInt().coerceAtLeast(18)

        // Progressive track width: 4.5m (Level 1) down to 2.2m (Level 40)
        val baseWidth = (4.5 - (clampedLevel - 1) * 0.058).coerceIn(2.2, 4.5)

        val segments = mutableListOf<TrackSegment>()
        val obstacles = mutableListOf<DestructibleCube>()
        val items = mutableListOf<TrackItem>()

        // 1. Generate Track Segments
        var currentZ = 0.0
        var currentY = 0.0
        var segmentIdx = 0

        while (currentZ < totalLength) {
            val segLength = rng.nextDouble(18.0, 32.0).coerceAtMost(totalLength - currentZ)
            val nextZ = currentZ + segLength

            // Determine segment features based on difficulty
            val isStartOrEnd = currentZ == 0.0 || nextZ >= totalLength - 15.0
            val isDip = !isStartOrEnd && (segmentIdx % 2 == 1)
            val isBooster = !isStartOrEnd && (clampedLevel >= 5 && rng.nextDouble() < 0.35)

            // Rails are abundant in Easy, scarcer in Hard/Expert
            val railProb = when (difficulty) {
                LevelDifficulty.EASY -> 1.0
                LevelDifficulty.NORMAL -> 0.75
                LevelDifficulty.HARD -> 0.45
                LevelDifficulty.EXPERT -> 0.30
            }

            val hasLeftRail = isStartOrEnd || rng.nextDouble() < railProb
            val hasRightRail = isStartOrEnd || rng.nextDouble() < railProb

            // Slopes & Dips
            val deltaY = if (isStartOrEnd) 0.0 else rng.nextDouble(-1.8, 0.4)
            val nextY = currentY + deltaY
            val dipDepth = if (isDip) rng.nextDouble(2.0, 3.0 + clampedLevel * 0.05) else 0.0

            segments.add(
                TrackSegment(
                    startZ = currentZ,
                    endZ = nextZ,
                    width = baseWidth,
                    startY = currentY,
                    endY = nextY,
                    hasLeftRail = hasLeftRail,
                    hasRightRail = hasRightRail,
                    isBooster = isBooster,
                    isCurvedDip = isDip,
                    dipDepth = dipDepth
                )
            )

            currentZ = nextZ
            currentY = nextY
            segmentIdx++
        }

        // Helper to get floor Y
        fun getFloorY(z: Double): Double {
            for (s in segments) {
                if (s.containsZ(z)) return s.getHeightAt(z)
            }
            return 0.0
        }

        // 2. Generate Destructible Cube Obstacles
        // Number of cube clusters scales with difficulty
        val clusterCount = when (difficulty) {
            LevelDifficulty.EASY -> if (clampedLevel == 1) 1 else 2
            LevelDifficulty.NORMAL -> 3
            LevelDifficulty.HARD -> 4
            LevelDifficulty.EXPERT -> 5
        }

        val stepDist = totalLength / (clusterCount + 1)
        for (i in 1..clusterCount) {
            val clusterZ = i * stepDist + rng.nextDouble(-4.0, 4.0)
            val floorY = getFloorY(clusterZ)

            if (difficulty == LevelDifficulty.EASY && clampedLevel <= 3) {
                // Easy: 2 staggered cubes allowing easy bypass
                obstacles.add(DestructibleCube(id = "l${clampedLevel}_c${i}_1", position = Vector3D(-0.7, floorY + 0.3, clusterZ)))
                obstacles.add(DestructibleCube(id = "l${clampedLevel}_c${i}_2", position = Vector3D(0.7, floorY + 0.3, clusterZ + 2.5)))
            } else {
                // Normal / Hard / Expert: Full 3x2 or 2x2 walls blocking track (TC6 Option A)
                val cols = if (baseWidth < 3.0) -1..0 else -1..1
                val rows = 0..1
                val cubeW = 0.65
                for (c in cols) {
                    for (r in rows) {
                        obstacles.add(
                            DestructibleCube(
                                id = "l${clampedLevel}_c${i}_${c}_$r",
                                position = Vector3D(c * cubeW, floorY + 0.3 + r * cubeW, clusterZ)
                            )
                        )
                    }
                }
            }
        }

        // 3. Generate Checkpoints (1 in Easy, 2 in Normal/Hard, 3 in Expert)
        val checkpointCount = when (difficulty) {
            LevelDifficulty.EASY -> 1
            LevelDifficulty.NORMAL -> 2
            LevelDifficulty.HARD -> 2
            LevelDifficulty.EXPERT -> 3
        }

        for (cp in 1..checkpointCount) {
            val cpZ = (cp.toDouble() / (checkpointCount + 1)) * totalLength
            val cpY = getFloorY(cpZ)
            items.add(TrackItem(id = "l${clampedLevel}_chk_$cp", type = CollectibleType.CHECKPOINT, position = Vector3D(0.0, cpY + 0.1, cpZ), radius = 1.8))
            // Place Spare Ball (+1 Life) right after each checkpoint as seen in video!
            items.add(TrackItem(id = "l${clampedLevel}_ball_$cp", type = CollectibleType.SPARE_BALL, position = Vector3D(0.0, cpY + 0.8, cpZ + 5.0)))
        }

        // 4. Generate Golden Keys (1 Key every 1-2 levels, cycle of 3)
        if (clampedLevel % 2 != 0 || clampedLevel == 2) {
            val keyZ = totalLength * 0.45 + rng.nextDouble(-6.0, 6.0)
            val keyY = getFloorY(keyZ)
            items.add(TrackItem(id = "l${clampedLevel}_key", type = CollectibleType.GOLDEN_KEY, position = Vector3D(0.0, keyY + 0.7, keyZ)))
        }

        // 5. Generate Coins along the track
        val coinCount = 6 + (clampedLevel % 4) * 2 // 6 to 12 coin clusters
        val coinStep = (totalLength - 20.0) / coinCount
        for (c in 1..coinCount) {
            val coinZ = 10.0 + c * coinStep + rng.nextDouble(-2.0, 2.0)
            val coinY = getFloorY(coinZ)
            val coinX = if (rng.nextDouble() < 0.4) (rng.nextDouble(-1.0, 1.0) * (baseWidth * 0.3)) else 0.0
            items.add(TrackItem(id = "l${clampedLevel}_coin_$c", type = CollectibleType.COIN, position = Vector3D(coinX, coinY + 0.6, coinZ)))
        }

        // 6. Finish Line at totalLength
        val finishY = getFloorY(totalLength)
        items.add(TrackItem(id = "l${clampedLevel}_finish", type = CollectibleType.FINISH_LINE, position = Vector3D(0.0, finishY + 0.1, totalLength), radius = 2.0))

        return LevelCourse(
            levelNumber = clampedLevel,
            name = name,
            difficulty = difficulty,
            totalLength = totalLength,
            segments = segments,
            obstacles = obstacles,
            items = items,
            startPosition = Vector3D(0.0, getFloorY(2.0) + 0.5, 2.0),
            parTimeSeconds = parTime
        )
    }

    // Retain backward compatibility methods
    fun buildLevel1(): LevelCourse = buildLevel(1)
    fun buildLevel2(): LevelCourse = buildLevel(2)
}
