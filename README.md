# AeroSphere 3D (Going Balls) — Android Native Edition

A premium, bright, kid-friendly 3D rolling ball platformer developed natively for Android with **100% Zero-Ads** compliance, custom 60Hz arcade physics, Filament PBR 3D viewport, comprehensive 40-level progression, and dynamic sound effects.

---

## 🌟 Key Features

### 1. 40 Deterministic Hand-Crafted Levels
- **Tier 1 — Easy (Levels 1–10):** Gentle straightaways, wide turns, introductory star cubes, and gentle speed boosts.
- **Tier 2 — Normal (Levels 11–20):** S-curves, banked ramps, moving hazards, and split paths.
- **Tier 3 — Hard (Levels 21–30):** Narrow planks, rail-less cliff edges, alternating ramp jumps, and dense obstacle clusters.
- **Tier 4 — Expert (Levels 31–40):** Precision gaps, moving platforms, high-speed slaloms, and tight finish runs.
- **Level Select Grid Modal:** Full 40-level selector with tier filter chips (`ALL`, `EASY`, `NORM`, `HARD`, `EXP`), best score tracking, and instant level jump.

### 2. Bright, Kid-Friendly Visual Aesthetics
- **Sky & Atmosphere:** Sunny blue gradient sky with animated fluffy white clouds and lush green rolling hills.
- **Track & Rails:** Honey birch wooden planks with candy-cane red & white safety guide rails and glowing finish archways.
- **Player Ball:** High-gloss cyan & coral swirl rolling sphere with realistic lighting, specular highlights, and rolling animation.
- **Destructible Obstacles:** Golden star collectible crates and smashable barrier cubes.

### 3. Dual-Viewport Rendering Architecture
- **Filament PBR Viewport (`FilamentSphereViewport`):** Powered by Google Filament 1.51.0 PBR engine rendering realistic physical materials, glTF 2.0 models (`sphere.glb`), directional sunlight, and IBL spherical harmonics.
- **Canvas PBR Fallback (`PBRSphereViewport`):** Lightweight, 60 FPS software fallback viewport ensuring universal device compatibility across any Android hardware.

### 4. 60Hz Arcade Physics Engine
- **Fixed Sub-stepping:** Deterministic 60Hz update loop ($\Delta t = 1/60\text{ s}$).
- **World Gravity:** Authentic arcade feel tuned to $19.620\text{ m/s}^2$ ($2.0g$).
- **Destructible Star Cubes (TC6):** Precision smashing threshold ($13.5\text{ m/s}$) with $71.19\%$ kinetic energy retention.
- **Intuitive Touch Controls:** Fluid swipe & drag ball steering with responsive velocity damping and momentum.

### 5. Rich Audio & Sound Effects Engine (`GameAudioManager`)
- **Cheerful BGM:** Looping melodic background soundtrack (`bgm_gameplay.wav`).
- **Real-Time SFX:** Dynamic rolling pitch/volume, button clicks (`sfx_button.wav`), checkpoints (`sfx_checkpoint.wav`), coin pickups (`sfx_coin.wav`), key unlocks (`sfx_key.wav`), extra lives (`sfx_life.wav`), obstacle smashes (`sfx_smash.wav`), and plinko pegs (`sfx_plinko_peg.wav`).
- **Victory Fanfare:** Celebration jingle upon crossing the checkered finish line (`sfx_victory_fanfare.wav`).
- **Audio Toggle:** One-tap mute/unmute control in the HUD.

### 6. Post-Level Plinko Carnival Minigame (`PlinkoMinigameView`)
- Interactive pegboard minigame where players drop their ball through bouncy pins to win coin multipliers ($2\times$, $3\times$, $5\times$, $10\times$).

### 7. 100% Zero-Ads & Privacy-First
- **No Ad SDKs:** Zero Google AdMob, Unity Ads, AppLovin, or third-party ad networks.
- **No Tracking or Spyware:** No telemetry, tracking permissions, or user profiling.
- **Kid-Safe & Offline:** Fully playable without an internet connection.

---

## 🏗️ Project Architecture

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/aerosphere3d/game/
│   │   │   │   ├── MainActivity.kt               # Lifecycle & Compose root
│   │   │   │   ├── audio/
│   │   │   │   │   └── GameAudioManager.kt       # SoundPool & MediaPlayer audio engine
│   │   │   │   └── ui/
│   │   │   │       ├── FilamentSphereViewport.kt # Filament 1.51.0 3D PBR renderer
│   │   │   │       ├── PBRSphereViewport.kt      # 60 FPS fallback canvas renderer
│   │   │   │       ├── GamePlayScreen.kt         # Compose Game UI, HUD & 40-Level Grid
│   │   │   │       ├── PlinkoMinigameView.kt     # Plinko carnival minigame
│   │   │   │       └── theme/Theme.kt            # Kid-friendly vibrant design theme
│   │   │   ├── assets/models/
│   │   │   │   └── sphere.glb                    # glTF 2.0 PBR Ball Model
│   │   │   └── res/raw/                          # 9 Audio files (BGM & SFX WAV)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── core/
│   ├── aerophysics/
│   │   ├── src/main/kotlin/com/aerosphere3d/core/aerophysics/
│   │   │   ├── AeroPhysicsEngine.kt              # Core physics stepping & integration
│   │   │   ├── GameplayEngine.kt                 # 60Hz 2.0g Arcade Physics
│   │   │   ├── LevelCourseBuilder.kt             # 40 deterministic track layouts
│   │   │   ├── SurfaceGeometry.kt                # Splines, ramps & collision hulls
│   │   │   ├── SweptSphereCCD.kt                 # Continuous collision detection
│   │   │   └── TrackCourse.kt                    # Course segment specifications
│   │   └── src/test/kotlin/                      # TC1-TC8 Physics spec unit tests
│   └── pal/                                      # Physics Abstraction Layer (Vector3D, Quaternion)
├── gradle/wrapper/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34 (compileSdk 34, minSdk 26)
- Physical device or Android Emulator with OpenGL ES 3.0+ support

### Build via Gradle
```bash
# Debug Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

---

## 🔒 Verification & Compliance
- **Physical Device Runtime Verification:** Verified on Android hardware (OpenGL ES 3.2, 60 FPS sustained).
- **Ad Check:** Zero ad dependencies, zero network ad calls, zero ad view components.
- **Dual Parity:** Fully matched with iOS counterpart repository (`goingballs_ios`).
