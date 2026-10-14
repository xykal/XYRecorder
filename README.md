# 🎬 XYRecorder for Android

**Professional Screen Recorder with Studio-Quality Audio Processing**

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android%207.0%2B-blue" />
  <img src="https://img.shields.io/badge/version-1.0.0-purple" />
  <img src="https://img.shields.io/badge/license-MIT-green" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF" />
</p>

---

## ✨ Features

### 📹 Video Recording
- **Resolution**: 720p, 1080p, 1440p, 4K
- **Frame Rate**: 30 FPS / 60 FPS
- **Codecs**: H.264, H.265/HEVC, VP8, VP9
- **Hardware Acceleration**: Native MediaCodec
- **Bitrate**: 3 - 20 Mbps
- **Output**: MP4 with faststart

### 🎤 Audio Capture
- **Microphone**: Real-time capture with noise processing
- **System/Internal Audio**: AudioPlaybackCapture API (Android 10+)
- **Independent Volume Control**: Per-source mixing

### 🎚️ Studio-Quality Noise Reduction

Professional audio processing chain — **makes your mic sound like a $500 studio mic**:

| Stage | Effect |
|-------|--------|
| **Android NoiseSuppressor** | Hardware-accelerated noise reduction |
| **Acoustic Echo Canceler** | Removes echo from speakers |
| **High-Pass Filter** | Butterworth IIR — removes rumble, HVAC, desk bumps |
| **Noise Gate** | Silences background noise during pauses |
| **Noise Suppression** | Adaptive spectral subtraction |
| **Compressor** | Soft-knee dynamics with makeup gain |
| **De-Esser** | Frequency-selective sibilance reduction |
| **Limiter** | Brick-wall ceiling — zero clipping guaranteed |

### 🎛️ One-Tap Presets

| Preset | Description |
|--------|-------------|
| 🔇 **None** | Raw audio, no processing |
| 🌤️ **Light** | Basic cleanup for quiet rooms |
| ☀️ **Medium** | Balanced (default) |
| 🔥 **Heavy** | Aggressive noise removal |
| 🎙️ **Studio** | Full broadcast-quality chain |

Every parameter is fully adjustable — attack, release, threshold, ratio, knee, ceiling, frequency, etc.

---

## 📱 Screenshots

_Dark-themed Material 3 UI with:_
- 🏠 Home screen with recording controls
- 🎚️ Dedicated noise reduction panel
- ⚙️ Full video/audio/general settings
- 📊 Real-time audio level meters (mic + processed output)

---

## 🚀 Quick Start

### Download APK
Get the latest APK from the [Releases page](https://github.com/xykal/XYRecorder/releases).

### Build from Source
```bash
git clone https://github.com/xykal/XYRecorder.git
cd XYRecorder
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release (Signed)
```bash
# Set environment variables
export KEYSTORE_FILE=./release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=xyrecorder
export KEY_PASSWORD=your_password

./gradlew assembleRelease
```

---

## 🏗️ Architecture

```
app/src/main/java/com/xyverse/xyrecorder/
├── MainActivity.kt              # Entry point, Compose UI host
├── XYRecorderApp.kt             # Application class, DI
├── audio/
│   ├── AudioProcessor.kt        # Full DSP processing chain
│   ├── HighPassFilter.kt        # 2nd-order Butterworth IIR
│   ├── NoiseGate.kt             # Envelope-based noise gate
│   ├── Compressor.kt            # Soft-knee dynamic compressor
│   ├── DeEsser.kt               # Frequency-selective de-esser
│   └── Limiter.kt               # Brick-wall look-ahead limiter
├── recorder/
│   └── ScreenRecorder.kt        # MediaProjection + MediaCodec + MediaMuxer
├── service/
│   └── RecordingService.kt      # Foreground service
├── data/
│   ├── Settings.kt              # Data models & presets
│   └── SettingsRepository.kt    # DataStore persistence
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   └── Theme.kt
    ├── components/
    │   ├── RecordingControls.kt
    │   └── NoiseReductionPanel.kt
    └── screens/
        ├── HomeScreen.kt
        ├── SettingsScreen.kt
        └── MainViewModel.kt
```

### Recording Pipeline

```
Screen → MediaProjection → Surface → MediaCodec (H.264) ─┐
                                                          ├→ MediaMuxer → MP4
Mic → AudioRecord → NoiseSuppressor → HPF → Gate →       │
     → Suppression → Compressor → DeEsser → Limiter       │
     → MediaCodec (AAC) ──────────────────────────────────┘
                                                          ↑
System Audio → AudioRecord (REMOTE_SUBMIX) ───────────────┘
```

---

## 🔧 Tech Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Language |
| Jetpack Compose | UI framework |
| Material 3 | Design system |
| MediaProjection API | Screen capture |
| MediaCodec | Hardware video encoding |
| MediaMuxer | MP4 muxing |
| AudioRecord | Audio capture |
| AudioEffect (NS/AEC/AGC) | Native audio processing |
| DataStore | Settings persistence |
| Kotlin Coroutines | Async operations |
| GitHub Actions | CI/CD + APK building |

---

## 📋 Requirements

- **Android 7.0+** (API 24)
- **Android 10+** (API 29) for system/internal audio capture
- **Microphone permission** for voice recording
- **Screen capture permission** (granted at runtime)

---

## 🔐 Code Signing (GitHub Actions)

To build signed release APKs in CI, add these secrets to your repo:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Your keystore file encoded as base64 |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

Generate a keystore:
```bash
keytool -genkey -v -keystore release.keystore \
  -alias xyrecorder -keyalg RSA -keysize 2048 -validity 10000
```

Convert to base64:
```bash
base64 release.keystore | tr -d '\n'
```

---

## 📝 License

MIT License — See [LICENSE](LICENSE) for details.

---

<p align="center">
  Made with ❤️ by <a href="https://xyverse.my.id">XYVerse</a>
</p>
