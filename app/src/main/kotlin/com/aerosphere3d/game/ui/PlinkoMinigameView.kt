package com.aerosphere3d.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aerosphere3d.game.audio.GameAudioManager
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 3D Plinko Bonus Minigame (Modeled directly after reference video 00:20 - 00:27)
 * Upgraded with Bright, Colorful Carnival Styling & Real Audio SFX for Kids
 * 100% Zero-Ads: Free drops earned by skill, NO "Watch Video for x3" advertisements!
 */
@Composable
fun PlinkoMinigameView(
    onContinueNextLevel: (Int) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { GameAudioManager.getInstance(context) }

    var hammerMeter by remember { mutableFloatStateOf(10f) }
    var isDropping by remember { mutableStateOf(false) }
    var droppedCoinsEarned by remember { mutableIntStateOf(0) }
    var targetSlot by remember { mutableStateOf<Int?>(null) }
    var ballPos by remember { mutableStateOf(Offset(180f, 60f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }

    // Pegs layout (Triangle Lattice)
    val pegs = remember {
        val list = mutableListOf<Offset>()
        val rows = 7
        val startY = 120f
        val spacingY = 45f
        for (r in 0 until rows) {
            val count = r + 4
            val startX = 200f - (count - 1) * 22f
            for (c in 0 until count) {
                list.add(Offset(startX + c * 44f, startY + r * spacingY))
            }
        }
        list
    }

    val slotValues = remember { listOf(10, 50, 100, 50, 10) }

    // Drop Simulation Coroutine
    LaunchedEffect(isDropping) {
        if (isDropping) {
            targetSlot = null
            var x = 200f + (Random.nextFloat() - 0.5f) * 60f
            var y = 60f
            var vx = (Random.nextFloat() - 0.5f) * 3f
            var vy = 2f

            while (y < 460f) {
                vy += 0.85f // gravity pull
                x += vx
                y += vy

                // Bounce off pegs
                for (peg in pegs) {
                    val dx = x - peg.x
                    val dy = y - peg.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist < 18f) {
                        val nx = dx / dist
                        val ny = dy / dist
                        val dot = vx * nx + vy * ny
                        vx = (vx - 1.6f * dot * nx) + (Random.nextFloat() - 0.5f) * 2.5f
                        vy = (vy - 1.6f * dot * ny).coerceAtLeast(1.5f)
                        x = peg.x + nx * 18.5f
                        y = peg.y + ny * 18.5f

                        // Audio feedback for peg tap
                        audioManager.playPlinkoPeg()
                    }
                }

                // Wall bounds
                if (x < 35f) { x = 35f; vx = abs(vx) }
                if (x > 365f) { x = 365f; vx = -abs(vx) }

                ballPos = Offset(x, y)
                delay(16) // ~60fps
            }

            // Determine which slot ball landed in
            val slotWidth = 340f / 5f
            val slotIndex = ((x - 30f) / slotWidth).toInt().coerceIn(0, 4)
            targetSlot = slotIndex
            val reward = slotValues[slotIndex]
            droppedCoinsEarned = reward
            hammerMeter = (hammerMeter + 10f).coerceAtMost(100f)
            audioManager.playCoin()
            isDropping = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1E3A8A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Text(
                text = "🎪 BONUS PLINKO 🎪",
                color = Color(0xFFFEF08A),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Drop the golden ball for free bonus coins!",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Piggy Bank Header Box with Hammer Meter
            Box(
                modifier = Modifier
                    .width(330.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF0FFFFFF))
                    .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🐷 PIGGY BANK",
                        color = Color(0xFFEC4899),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "HAMMER: ${hammerMeter.toInt()}%",
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Plinko Board Canvas (Vibrant Carnival Turquoise)
            Box(
                modifier = Modifier
                    .size(width = 340.dp, height = 440.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0D9488))
                    .border(3.dp, Color(0xFFFDE047), RoundedCornerShape(18.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val scaleX = canvasWidth / 400f
                    val scaleY = size.height / 520f

                    // 1. Draw Pegs (Golden pins with white shine)
                    for (peg in pegs) {
                        drawCircle(
                            color = Color(0xFFFDE047),
                            radius = 6.5f,
                            center = Offset(peg.x * scaleX, peg.y * scaleY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = Offset(peg.x * scaleX - 1.5f, peg.y * scaleY - 1.5f)
                        )
                    }

                    // 2. Draw 2 Special Skin Pockets in Middle
                    drawCircle(
                        color = Color(0xFF042F2E),
                        radius = 24f,
                        center = Offset(130f * scaleX, 260f * scaleY)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = 24f,
                        center = Offset(130f * scaleX, 260f * scaleY),
                        style = Stroke(width = 3.5f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 16f,
                        center = Offset(130f * scaleX, 260f * scaleY)
                    )

                    drawCircle(
                        color = Color(0xFF042F2E),
                        radius = 24f,
                        center = Offset(270f * scaleX, 260f * scaleY)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = 24f,
                        center = Offset(270f * scaleX, 260f * scaleY),
                        style = Stroke(width = 3.5f)
                    )

                    // 3. Draw Bottom Trays & Slot Dividers
                    val slotW = (canvasWidth - 20f) / 5f
                    for (i in 0..5) {
                        val divX = 10f + i * slotW
                        drawLine(
                            color = Color(0xFFFDE047),
                            start = Offset(divX, size.height - 70f),
                            end = Offset(divX, size.height - 10f),
                            strokeWidth = 3.5f
                        )
                    }

                    // 4. Draw Ball (Shiny Golden Bouncing Ball)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFDE047), Color(0xFFEA580C)),
                            center = Offset(ballPos.x * scaleX - 4f, ballPos.y * scaleY - 4f),
                            radius = 18f
                        ),
                        radius = 14f,
                        center = Offset(ballPos.x * scaleX, ballPos.y * scaleY)
                    )
                }

                // Slot labels at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    slotValues.forEachIndexed { idx, valAmount ->
                        val isSelected = targetSlot == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFF59E0B) else Color(0xCC042F2E))
                                .border(1.dp, if (isSelected) Color.White else Color(0xFF2DD4BF), RoundedCornerShape(8.dp))
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$valAmount",
                                color = if (isSelected) Color.White else Color(0xFFFDE047),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (!isDropping && targetSlot == null) {
                Button(
                    onClick = {
                        audioManager.playButton()
                        isDropping = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .width(240.dp)
                        .height(52.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(26.dp))
                ) {
                    Text(
                        text = "⭐ DROP BALL ⭐",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else if (targetSlot != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎉 YOU WON +$droppedCoinsEarned COINS! 🎉",
                        color = Color(0xFFFEF08A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            audioManager.playButton()
                            onContinueNextLevel(droppedCoinsEarned)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .width(240.dp)
                            .height(50.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(26.dp))
                    ) {
                        Text(
                            text = "CONTINUE ▶",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
