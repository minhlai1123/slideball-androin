package com.aerosphere3d.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.aerosphere3d.game.audio.GameAudioManager
import com.aerosphere3d.game.ui.GamePlayScreen
import com.aerosphere3d.game.ui.PlinkoMinigameView
import com.aerosphere3d.game.ui.theme.AeroSphereTheme

/**
 * AeroSphere 3D Native Android Main Entry Activity
 * Full 3D Rolling Adventure Gameplay with 40 Progressive Levels & Native Audio
 * 100% Zero-Ads Invariant
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AeroSphereTheme {
                var currentLevel by remember { mutableIntStateOf(1) }
                var showPlinko by remember { mutableStateOf(false) }

                if (showPlinko) {
                    PlinkoMinigameView(
                        onContinueNextLevel = { earnedCoins ->
                            // Advance to next level (1..40 loop)
                            currentLevel = if (currentLevel < 40) currentLevel + 1 else 1
                            showPlinko = false
                        }
                    )
                } else {
                    key(currentLevel) {
                        GamePlayScreen(
                            initialLevel = currentLevel,
                            onSelectLevel = { selectedLevel ->
                                currentLevel = selectedLevel.coerceIn(1, 40)
                            },
                            onOpenPlinko = {
                                showPlinko = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GameAudioManager.getInstance(this).pauseBgm()
    }

    override fun onResume() {
        super.onResume()
        GameAudioManager.getInstance(this).resumeBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        GameAudioManager.getInstance(this).release()
    }
}
