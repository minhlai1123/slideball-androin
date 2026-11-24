package com.aerosphere3d.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerosphere3d.core.aerophysics.*
import com.aerosphere3d.core.pal.PhysicsConstants
import com.aerosphere3d.core.pal.Vector3D
import com.aerosphere3d.game.audio.GameAudioManager
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

data class ConfettiParticle(
    var pos: Offset,
    var vel: Offset,
    var color: Color,
    var size: Float,
    var rotation: Float
)

data class FloatingText(
    val id: Long,
    val text: String,
    val color: Color,
    val worldPos: Vector3D,
    var alpha: Float = 1.0f,
    var offsetY: Float = 0f
)

/**
 * 3D Rolling Adventure Gameplay Screen
 * Featuring Vibrant, Bright, Cheerful Kid-Friendly Graphics & Real Native Sound Effects + BGM + Victory Fanfare
 * 40 Progressive Levels & 100% Zero-Ads Invariant
 */
@Composable
fun GamePlayScreen(
    initialLevel: Int = 1,
    onSelectLevel: (Int) -> Unit = {},
    onOpenPlinko: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }
    var isMuted by remember { mutableStateOf(false) }

    val engine = remember(initialLevel) { GameplayEngine(LevelCourseBuilder.buildLevel(initialLevel)) }

    var ballPos by remember { mutableStateOf(engine.ballPosition) }
    var ballSpeed by remember { mutableDoubleStateOf(0.0) }
    var ballRotAngle by remember { mutableDoubleStateOf(0.0) }
    var gameState by remember { mutableStateOf(engine.state) }
    var currentLevelNum by remember { mutableIntStateOf(engine.currentLevel) }
    var lives by remember { mutableIntStateOf(engine.lives) }
    var coins by remember { mutableIntStateOf(engine.coinsCollected) }
    var keys by remember { mutableIntStateOf(engine.keysCollected) }

    var showLevelSelector by remember { mutableStateOf(false) }
    var difficultyFilter by remember { mutableStateOf<LevelDifficulty?>(null) }

    val floatingTexts = remember { mutableStateListOf<FloatingText>() }
    val confettiParticles = remember { mutableStateListOf<ConfettiParticle>() }

    // Register gameplay audio/visual event listeners
    DisposableEffect(engine) {
        engine.addEventListener { event ->
            when (event) {
                is GameEvent.CoinCollected -> {
                    audioManager.playCoin()
                    floatingTexts.add(
                        FloatingText(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            text = "+10 🪙",
                            color = Color(0xFFFDE047),
                            worldPos = event.position
                        )
                    )
                }
                is GameEvent.KeyCollected -> {
                    audioManager.playKey()
                    floatingTexts.add(
                        FloatingText(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            text = "KEY! 🔑",
                            color = Color(0xFF38BDF8),
                            worldPos = event.position
                        )
                    )
                }
                is GameEvent.LifeGained -> {
                    audioManager.playLife()
                    floatingTexts.add(
                        FloatingText(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            text = "+1 BALL! ⚽",
                            color = Color(0xFF34D399),
                            worldPos = event.position
                        )
                    )
                }
                is GameEvent.ObstacleShattered -> {
                    audioManager.playSmash()
                    floatingTexts.add(
                        FloatingText(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            text = "SMASH! ★",
                            color = Color(0xFFF43F5E),
                            worldPos = event.position
                        )
                    )
                }
                is GameEvent.CheckpointReached -> {
                    audioManager.playCheckpoint()
                }
                is GameEvent.LevelFinished -> {
                    // Play level victory celebration fanfare!
                    audioManager.playVictoryFanfare()

                    // Spawn celebratory rainbow party confetti
                    confettiParticles.clear()
                    val rainbowColors = listOf(
                        Color(0xFFF43F5E), // Coral Pink
                        Color(0xFF38BDF8), // Cyan
                        Color(0xFF10B981), // Emerald
                        Color(0xFFFBBF24), // Gold
                        Color(0xFFA855F7), // Purple
                        Color(0xFFFF7A00), // Tangerine
                        Color(0xFF06B6D4)  // Turquoise
                    )
                    for (i in 0 until 100) {
                        confettiParticles.add(
                            ConfettiParticle(
                                pos = Offset(Random.nextFloat() * 1100f, Random.nextFloat() * 400f - 200f),
                                vel = Offset((Random.nextFloat() - 0.5f) * 7f, Random.nextFloat() * 5f + 3f),
                                color = rainbowColors[Random.nextInt(rainbowColors.size)],
                                size = Random.nextFloat() * 14f + 8f,
                                rotation = Random.nextFloat() * 360f
                            )
                        )
                    }
                }
                else -> {}
            }
        }
        onDispose {
            audioManager.pauseBgm()
        }
    }

    // 60 Hz Game Loop
    LaunchedEffect(engine) {
        var lastTime = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000.0).coerceIn(0.005, 0.033)
            lastTime = now

            engine.update(dt)

            ballPos = engine.ballPosition
            ballSpeed = engine.ballVelocity.length()
            ballRotAngle = engine.ballRotationAngle
            gameState = engine.state
            currentLevelNum = engine.currentLevel
            lives = engine.lives
            coins = engine.coinsCollected
            keys = engine.keysCollected

            // Update floating action texts
            val textIter = floatingTexts.iterator()
            while (textIter.hasNext()) {
                val ft = textIter.next()
                ft.offsetY += 1.6f
                ft.alpha -= 0.02f
                if (ft.alpha <= 0f) {
                    textIter.remove()
                }
            }

            // Update party confetti if level completed
            if (gameState == GameState.LEVEL_COMPLETED) {
                for (p in confettiParticles) {
                    p.pos += p.vel
                    p.rotation += 5f
                }
            }

            delay(16) // ~60fps
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF38BDF8))
            .pointerInput(gameState) {
                detectDragGestures(
                    onDragStart = {
                        if (gameState == GameState.READY) {
                            audioManager.startBgm()
                            engine.startRoll()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val forwardVal = (-dragAmount.y * 0.08).coerceIn(-1.0, 1.0)
                        val steerVal = (dragAmount.x * 0.05).coerceIn(-1.0, 1.0)
                        engine.setInputs(forwardVal, steerVal)
                    },
                    onDragEnd = {
                        engine.setInputs(0.0, 0.0)
                    },
                    onDragCancel = {
                        engine.setInputs(0.0, 0.0)
                    }
                )
            }
    ) {
        // 1. 3D Perspective Viewport Canvas with Kid-Friendly Bright Graphics
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenW = size.width
            val screenH = size.height

            // Spring-Arm Chase Camera positioning
            val camDistance = 4.5
            val camHeight = 2.2
            val camX = ballPos.x * 0.4
            val camY = ballPos.y + camHeight
            val camZ = ballPos.z - camDistance

            // Dynamic FOV based on speed
            val speedFactor = (ballSpeed / 25.0).coerceIn(0.0, 1.0)
            val fovDeg = 60.0 + speedFactor * 14.0
            val fovRad = Math.toRadians(fovDeg)
            val focalLength = (screenH / 2.0) / tan(fovRad / 2.0)

            // Perspective Projection helper
            fun project(worldX: Double, worldY: Double, worldZ: Double): Offset? {
                val relZ = worldZ - camZ
                if (relZ <= 0.2) return null
                val relX = worldX - camX
                val relY = worldY - camY

                val u = (screenW / 2.0) + (relX / relZ) * focalLength
                val v = (screenH * 0.58) - (relY / relZ) * focalLength
                return Offset(u.toFloat(), v.toFloat())
            }

            val horizonY = screenH * 0.58f

            // A. DRAW BRIGHT SUNNY DAYTIME SKY
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8), // Vivid Cartoon Sky Blue
                        Color(0xFF7DD3FC), // Light Sky Cyan
                        Color(0xFFBAE6FD), // Soft Pastel Blue
                        Color(0xFFFEF3C7)  // Sunny Golden Horizon Glow
                    ),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset.Zero,
                size = Size(screenW, horizonY)
            )

            // B. CHEERFUL GLOWING CARTOON SUN
            val sunCenter = Offset(screenW * 0.82f, screenH * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x77FEF08A), Color(0x33FBBF24), Color.Transparent),
                    center = sunCenter,
                    radius = 120f
                ),
                radius = 120f,
                center = sunCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFFEF08A), Color(0xFFFBBF24)),
                    center = sunCenter,
                    radius = 46f
                ),
                radius = 46f,
                center = sunCenter
            )

            // C. FLUFFY WHITE CARTOON CLOUDS
            fun drawCartoonCloud(cx: Float, cy: Float, scale: Float) {
                val cloudColor = Color.White.copy(alpha = 0.95f)
                val shadowColor = Color(0xFFE0F2FE)
                drawOval(
                    color = shadowColor,
                    topLeft = Offset(cx - 45f * scale, cy - 8f * scale),
                    size = Size(90f * scale, 30f * scale)
                )
                drawCircle(cloudColor, radius = 20f * scale, center = Offset(cx - 24f * scale, cy + 2f * scale))
                drawCircle(cloudColor, radius = 28f * scale, center = Offset(cx, cy - 7f * scale))
                drawCircle(cloudColor, radius = 22f * scale, center = Offset(cx + 24f * scale, cy + 3f * scale))
            }

            val timeSec = System.currentTimeMillis() * 0.001
            val cloudOffset1 = (((timeSec * 7.0) % (screenW + 240)) - 120).toFloat()
            val cloudOffset2 = ((((timeSec * 4.5) + 320.0) % (screenW + 240)) - 120).toFloat()
            val cloudOffset3 = ((((timeSec * 9.0) + 160.0) % (screenW + 240)) - 120).toFloat()

            drawCartoonCloud(cloudOffset1, screenH * 0.12f, 1.25f)
            drawCartoonCloud(cloudOffset2, screenH * 0.25f, 0.9f)
            drawCartoonCloud(cloudOffset3, screenH * 0.36f, 1.1f)

            // D. DISTANT ROLLING GREEN HILLS
            val hillBack = Path().apply {
                moveTo(0f, horizonY)
                quadraticTo(screenW * 0.25f, horizonY - 45f, screenW * 0.5f, horizonY - 18f)
                quadraticTo(screenW * 0.75f, horizonY - 50f, screenW, horizonY - 22f)
                lineTo(screenW, screenH)
                lineTo(0f, screenH)
                close()
            }
            drawPath(hillBack, color = Color(0xFF86EFAC))

            val hillFront = Path().apply {
                moveTo(0f, horizonY)
                quadraticTo(screenW * 0.35f, horizonY - 30f, screenW * 0.65f, horizonY - 38f)
                quadraticTo(screenW * 0.85f, horizonY - 14f, screenW, horizonY)
                lineTo(screenW, screenH)
                lineTo(0f, screenH)
                close()
            }
            drawPath(hillFront, color = Color(0xFF4ADE80))

            // Ground base below horizon
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF15803D)),
                    startY = horizonY,
                    endY = screenH
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(screenW, screenH - horizonY)
            )

            // 1. DRAW 3D TRACK SEGMENTS (Warm Honey Birch Planks & Candy Cane Rails)
            for (seg in engine.course.segments) {
                if (seg.endZ < camZ - 2.0 || seg.startZ > camZ + 85.0) continue

                val stepZ = 2.5
                var currZ = max(seg.startZ, camZ - 1.0)
                while (currZ < seg.endZ) {
                    val nextZ = min(seg.endZ, currZ + stepZ)
                    val y1 = seg.getHeightAt(currZ)
                    val y2 = seg.getHeightAt(nextZ)
                    val hw = seg.width / 2.0

                    val pTL = project(-hw, y1, currZ)
                    val pTR = project(hw, y1, currZ)
                    val pBR = project(hw, y2, nextZ)
                    val pBL = project(-hw, y2, nextZ)

                    if (pTL != null && pTR != null && pBR != null && pBL != null) {
                        val plankPath = Path().apply {
                            moveTo(pTL.x, pTL.y)
                            lineTo(pTR.x, pTR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBL.x, pBL.y)
                            close()
                        }

                        val isBooster = seg.isBooster
                        val plankColor = if (isBooster) {
                            Color(0xFF10B981)
                        } else if ((currZ.toInt() / 2) % 2 == 0) {
                            Color(0xFFFDE68A)
                        } else {
                            Color(0xFFF59E0B)
                        }

                        drawPath(plankPath, color = plankColor)

                        drawLine(Color(0xFFB45309), pTL, pBL, strokeWidth = 3f)
                        drawLine(Color(0xFFB45309), pTR, pBR, strokeWidth = 3f)
                        drawLine(Color(0x3378350F), pTL, pTR, strokeWidth = 2f)

                        // Booster glowing golden arrows
                        if (isBooster) {
                            val arrowCenter = project(0.0, (y1 + y2) / 2.0 + 0.05, (currZ + nextZ) / 2.0)
                            if (arrowCenter != null) {
                                drawCircle(Color.White, radius = 8f, center = arrowCenter)
                                drawCircle(Color(0xFFFDE047), radius = 5f, center = arrowCenter)
                            }
                        }

                        // Left Candy Cane Hazard Rail (Alternating Red & White)
                        if (seg.hasLeftRail) {
                            val rL1 = project(-hw - 0.25, y1 + 0.45, currZ)
                            val rL2 = project(-hw - 0.25, y2 + 0.45, nextZ)
                            if (rL1 != null && rL2 != null) {
                                val railColor = if ((currZ.toInt()) % 2 == 0) Color(0xFFEF4444) else Color(0xFFFFFFFF)
                                drawLine(railColor, rL1, rL2, strokeWidth = 7f)
                                val postBase = project(-hw - 0.25, y1, currZ)
                                if (postBase != null) {
                                    drawLine(Color(0xFFE2E8F0), postBase, rL1, strokeWidth = 3.5f)
                                }
                            }
                        }

                        // Right Candy Cane Hazard Rail (Alternating Red & White)
                        if (seg.hasRightRail) {
                            val rR1 = project(hw + 0.25, y1 + 0.45, currZ)
                            val rR2 = project(hw + 0.25, y2 + 0.45, nextZ)
                            if (rR1 != null && rR2 != null) {
                                val railColor = if ((currZ.toInt()) % 2 == 0) Color(0xFFEF4444) else Color(0xFFFFFFFF)
                                drawLine(railColor, rR1, rR2, strokeWidth = 7f)
                                val postBase = project(hw + 0.25, y1, currZ)
                                if (postBase != null) {
                                    drawLine(Color(0xFFE2E8F0), postBase, rR1, strokeWidth = 3.5f)
                                }
                            }
                        }
                    }
                    currZ = nextZ
                }
            }

            // 2. DRAW CHECKPOINTS, COLLECTIBLES & CARNIVAL FINISH LINE
            for (item in engine.course.items) {
                if (item.position.z < camZ - 2.0 || item.position.z > camZ + 75.0) continue
                when (item.type) {
                    CollectibleType.CHECKPOINT -> {
                        val pCenter = project(item.position.x, item.position.y, item.position.z)
                        if (pCenter != null) {
                            drawCircle(Color(0x4406B6D4), radius = 48f, center = pCenter)
                            drawCircle(Color(0xFF06B6D4), radius = 24f, center = pCenter, style = Stroke(width = 5f))
                            drawCircle(Color(0xFFFDE047), radius = 10f, center = pCenter)
                            drawCircle(Color.White, radius = 5f, center = pCenter)
                        }
                    }
                    CollectibleType.FINISH_LINE -> {
                        val hw = 2.0
                        val pL = project(-hw, item.position.y, item.position.z)
                        val pR = project(hw, item.position.y, item.position.z)
                        if (pL != null && pR != null) {
                            drawLine(Color.White, pL, pR, strokeWidth = 16f)
                            drawLine(Color(0xFFEF4444), pL, pR, strokeWidth = 8f)
                            val pBal1 = project(-1.2, item.position.y + 2.0, item.position.z)
                            val pBal2 = project(1.2, item.position.y + 2.0, item.position.z)
                            if (pBal1 != null) drawCircle(Color(0xFFF43F5E), radius = 16f, center = pBal1)
                            if (pBal2 != null) drawCircle(Color(0xFF38BDF8), radius = 16f, center = pBal2)
                        }
                    }
                    CollectibleType.COIN -> {
                        if (!item.isCollected) {
                            val bounceY = sin(System.currentTimeMillis() * 0.006) * 0.15
                            val p = project(item.position.x, item.position.y + bounceY, item.position.z)
                            if (p != null) {
                                drawCircle(Color(0x55FBBF24), radius = 20f, center = p)
                                drawCircle(Color(0xFFFBBF24), radius = 15f, center = p)
                                drawCircle(Color(0xFFFEF08A), radius = 11f, center = p)
                                drawCircle(Color(0xFFB45309), radius = 15f, center = p, style = Stroke(width = 2.5f))
                                drawCircle(Color.White, radius = 4f, center = p)
                            }
                        }
                    }
                    CollectibleType.GOLDEN_KEY -> {
                        if (!item.isCollected) {
                            val p = project(item.position.x, item.position.y + 0.35, item.position.z)
                            if (p != null) {
                                drawCircle(Color(0x6638BDF8), radius = 26f, center = p)
                                drawCircle(Color(0xFFFBBF24), radius = 20f, center = p)
                                drawCircle(Color.White, radius = 7f, center = p)
                            }
                        }
                    }
                    CollectibleType.SPARE_BALL -> {
                        if (!item.isCollected) {
                            val p = project(item.position.x, item.position.y + 0.45, item.position.z)
                            if (p != null) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0284C7)),
                                        center = Offset(p.x - 5f, p.y - 5f),
                                        radius = 20f
                                    ),
                                    radius = 18f,
                                    center = p
                                )
                            }
                        }
                    }
                }
            }

            // 3. DRAW TOY STAR CUBES (Destructible Obstacles)
            for (cube in engine.course.obstacles) {
                if (cube.position.z < camZ - 2.0 || cube.position.z > camZ + 80.0) continue
                val p = project(cube.position.x, cube.position.y, cube.position.z)
                if (p != null) {
                    val relZ = cube.position.z - camZ
                    val cubeScreenSize = ((cube.size / relZ) * focalLength).toFloat().coerceIn(10f, 130f)
                    val halfS = cubeScreenSize / 2f

                    drawRoundRect(
                        color = Color(0xFFEC4899),
                        topLeft = Offset(p.x - halfS, p.y - halfS),
                        size = Size(cubeScreenSize, cubeScreenSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(p.x - halfS, p.y - halfS),
                        size = Size(cubeScreenSize, cubeScreenSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                        style = Stroke(width = max(2f, cubeScreenSize * 0.1f))
                    )
                    drawCircle(Color(0xFFFDE047), radius = cubeScreenSize * 0.22f, center = p)
                    drawCircle(Color.White, radius = cubeScreenSize * 0.1f, center = Offset(p.x - cubeScreenSize * 0.05f, p.y - cubeScreenSize * 0.05f))
                }
            }

            // 4. DRAW ROLLING BALL (Glossy Superhero Cyan & Coral Swirl Ball)
            val pBall = project(ballPos.x, ballPos.y, ballPos.z)
            if (pBall != null) {
                val relZ = ballPos.z - camZ
                val ballScreenR = ((PhysicsConstants.BALL_RADIUS / relZ) * focalLength).toFloat().coerceIn(18f, 160f)

                // Drop Shadow
                val pShadow = project(ballPos.x, ballPos.y - PhysicsConstants.BALL_RADIUS, ballPos.z)
                if (pShadow != null) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x66000000), Color.Transparent),
                            center = pShadow,
                            radius = ballScreenR * 0.95f
                        ),
                        topLeft = Offset(pShadow.x - ballScreenR * 0.9f, pShadow.y - ballScreenR * 0.25f),
                        size = Size(ballScreenR * 1.8f, ballScreenR * 0.5f)
                    )
                }

                // Base Sphere
                val lightOff = Offset(pBall.x - ballScreenR * 0.35f, pBall.y - ballScreenR * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA5F3FC),
                            Color(0xFF06B6D4),
                            Color(0xFF0284C7),
                            Color(0xFF1E3A8A)
                        ),
                        center = lightOff,
                        radius = ballScreenR * 1.25f
                    ),
                    radius = ballScreenR,
                    center = pBall
                )

                // Rotating racing stripes
                val rotX = cos(ballRotAngle).toFloat() * ballScreenR * 0.65f
                drawLine(
                    color = Color(0xFFFDE047),
                    start = Offset(pBall.x - rotX, pBall.y - ballScreenR * 0.7f),
                    end = Offset(pBall.x + rotX, pBall.y + ballScreenR * 0.7f),
                    strokeWidth = max(3f, ballScreenR * 0.14f)
                )

                val rotX2 = cos(ballRotAngle + 0.4).toFloat() * ballScreenR * 0.65f
                drawLine(
                    color = Color(0xFFF43F5E),
                    start = Offset(pBall.x - rotX2, pBall.y - ballScreenR * 0.7f),
                    end = Offset(pBall.x + rotX2, pBall.y + ballScreenR * 0.7f),
                    strokeWidth = max(2f, ballScreenR * 0.08f)
                )

                // Specular highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xEEFFFFFF), Color(0x00FFFFFF)),
                        center = lightOff,
                        radius = ballScreenR * 0.38f
                    ),
                    radius = ballScreenR * 0.38f,
                    center = lightOff
                )

                // Glowing rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x2238BDF8), Color(0x9967E8F9)),
                        center = pBall,
                        radius = ballScreenR
                    ),
                    radius = ballScreenR,
                    center = pBall
                )
            }

            // 5. DRAW FLOATING ACTION TEXTS
            for (ft in floatingTexts) {
                val p = project(ft.worldPos.x, ft.worldPos.y + ft.offsetY * 0.05, ft.worldPos.z)
                if (p != null) {
                    drawCircle(ft.color.copy(alpha = ft.alpha * 0.4f), radius = 26f, center = p)
                }
            }

            // 6. DRAW CONFETTI PARTICLES
            if (gameState == GameState.LEVEL_COMPLETED) {
                for (cp in confettiParticles) {
                    drawRect(
                        color = cp.color,
                        topLeft = cp.pos,
                        size = Size(cp.size, cp.size * 0.6f)
                    )
                }
            }
        }

        // 2. HUD OVERLAY (Kid-Friendly Cheerful Bubbly Theme)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 14.dp, end = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD ROW: Levels Button + Mute Button | Level Title & Progress Bar | Lives Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Map/Levels Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                audioManager.playButton()
                                showLevelSelector = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🗺️", fontSize = 14.sp)
                            Text(text = "MAP", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Sound Toggle Mute Button (🔊 / 🔇)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color(0xFFEF4444) else Color(0xFF10B981))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                isMuted = audioManager.toggleMute()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isMuted) "🔇" else "🔊", fontSize = 16.sp)
                    }
                }

                // Center: Clickable Level Badge & Progress Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            audioManager.playButton()
                            showLevelSelector = true
                        }
                ) {
                    // Level Badge + Difficulty Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF8B5CF6))
                                .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LEVEL $currentLevelNum",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }

                        // Difficulty Pill
                        val diffColor = Color(engine.course.difficulty.badgeColor)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(diffColor)
                                .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = engine.course.difficulty.label,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Level Name Subtitle
                    Text(
                        text = engine.course.name,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bubbly Progress Bar
                    val progressRatio = (ballPos.z / engine.course.totalLength).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x55000000))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressRatio)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B))))
                        )
                    }
                }

                // Right: 5-Ball Lives Column (Cute Smiling Mini Spheres)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (i in 0 until 5) {
                        val hasLife = i < lives
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (hasLife) Color(0xFF38BDF8) else Color(0xCCEF4444))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hasLife) {
                                Text(text = "✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text(text = "●", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // HUD Stats Banner (Coins & Keys collected)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keys Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xDD0284C7))
                        .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🔑 KEYS: $keys / 3",
                        color = Color(0xFFFEF08A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Coins Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xDDF59E0B))
                        .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🪙 COINS: $coins",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 3. START GAME PROMPT (Playful & Welcoming for Kids)
        if (gameState == GameState.READY && !showLevelSelector) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            audioManager.startBgm()
                            engine.startRoll()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF0FFFFFF))
                        .border(3.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "🌟 AEROSPHERE 3D 🌟",
                        color = Color(0xFF0284C7),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Level info & Difficulty Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LVL $currentLevelNum: ${engine.course.name}",
                            color = Color(0xFFB45309),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val diffColor = Color(engine.course.difficulty.badgeColor)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(diffColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = engine.course.difficulty.label,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Swipe forward/backward to roll & brake\nDrag left/right to steer!",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Continue Button (Juicy Green Pill)
                    Button(
                        onClick = {
                            audioManager.playButton()
                            audioManager.startBgm()
                            engine.startRoll()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "▶ PLAY NOW",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Select Level Button
                    Button(
                        onClick = {
                            audioManager.playButton()
                            showLevelSelector = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "🗺️ SELECT LEVEL (1-40)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. LEVEL COMPLETED CELEBRATION SCREEN (Victory Fanfare plays!)
        if (gameState == GameState.LEVEL_COMPLETED && !showLevelSelector) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF0FFFFFF))
                        .border(3.dp, Color(0xFFFBBF24), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF8B5CF6))
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LEVEL $currentLevelNum",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = engine.course.name,
                        color = Color(0xFF334155),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10B981))
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎉 COMPLETED! 🎉",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "COLLECTED COINS",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+$coins COINS 🪙",
                        color = Color(0xFFD97706),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Keys Collected: $keys / 3 🔑",
                        color = Color(0xFF0284C7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Claim Reward (Plinko Minigame)
                    Button(
                        onClick = {
                            audioManager.playButton()
                            audioManager.stopVictoryFanfare()
                            onOpenPlinko()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "🎁 CLAIM REWARD (PLINKO)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Next Level Button
                    val nextLvl = if (currentLevelNum < 40) currentLevelNum + 1 else 1
                    Button(
                        onClick = {
                            audioManager.playButton()
                            audioManager.stopVictoryFanfare()
                            audioManager.startBgm()
                            onSelectLevel(nextLvl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "NEXT LEVEL (LVL $nextLvl) ▶",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Choose Level Button
                    Button(
                        onClick = {
                            audioManager.playButton()
                            showLevelSelector = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "🗺️ SELECT LEVEL (1-40)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. 40-LEVEL SELECTION MODAL
        if (showLevelSelector) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xF0FFFFFF))
                        .border(3.dp, Color(0xFF38BDF8), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎮 SELECT LEVEL (1 - 40)",
                                color = Color(0xFF0284C7),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Pick your favorite track to play!",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }

                        // Close "✕" Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable {
                                    audioManager.playButton()
                                    showLevelSelector = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✕", color = Color(0xFF334155), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Difficulty Filter Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filterOptions = listOf(
                            Triple("ALL", null, Color(0xFF0284C7)),
                            Triple("EASY", LevelDifficulty.EASY, Color(LevelDifficulty.EASY.badgeColor)),
                            Triple("NORM", LevelDifficulty.NORMAL, Color(LevelDifficulty.NORMAL.badgeColor)),
                            Triple("HARD", LevelDifficulty.HARD, Color(LevelDifficulty.HARD.badgeColor)),
                            Triple("EXP", LevelDifficulty.EXPERT, Color(LevelDifficulty.EXPERT.badgeColor))
                        )

                        for ((label, diff, col) in filterOptions) {
                            val isSelected = difficultyFilter == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) col else Color(0xFFF1F5F9))
                                    .border(1.5.dp, col, RoundedCornerShape(10.dp))
                                    .clickable {
                                        audioManager.playButton()
                                        difficultyFilter = diff
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else col,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 40 Levels Grid
                    val allLevels = (1..40).toList()
                    val filteredLevels = allLevels.filter { lvl ->
                        val d = LevelCourseBuilder.getDifficulty(lvl)
                        difficultyFilter == null || difficultyFilter == d
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLevels) { lvl ->
                            val diff = LevelCourseBuilder.getDifficulty(lvl)
                            val diffColor = Color(diff.badgeColor)
                            val isCurrent = lvl == currentLevelNum
                            val name = LevelCourseBuilder.getLevelName(lvl)

                            Box(
                                modifier = Modifier
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                                    .border(
                                        width = if (isCurrent) 2.5.dp else 1.5.dp,
                                        color = if (isCurrent) Color(0xFFF59E0B) else diffColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        audioManager.playButton()
                                        audioManager.stopVictoryFanfare()
                                        audioManager.startBgm()
                                        showLevelSelector = false
                                        onSelectLevel(lvl)
                                    }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "$lvl",
                                            color = if (isCurrent) Color(0xFFB45309) else Color(0xFF1E293B),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(diffColor)
                                        )
                                    }

                                    Text(
                                        text = name,
                                        color = Color(0xFF475569),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = diff.label,
                                        color = diffColor,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
