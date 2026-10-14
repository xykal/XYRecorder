# 🎬 XYRecorder v1.0.5 - FINAL VERSION

## ✅ All Issues Fixed!

### 🔧 What Was Fixed:

1. **Screen Capture - NOW WORKING** ✅
   - Completely rewrote RecordingService to properly use MediaProjection API
   - Screen recording now captures the actual screen content
   - Creates MP4 output with H.264 video + AAC audio
   - 720p @ 30fps recording

2. **Floating Bubble - NOW WORKING** ✅
   - Floating overlay bubble appears when you exit the app during recording
   - Gradient purple-to-cyan circle with red recording dot
   - Real-time timer showing recording duration
   - Drag to move anywhere on screen
   - Tap to stop recording
   - Proper permission handling for SYSTEM_ALERT_WINDOW

3. **UI Completely Redesigned** ✅
   - Anti-default Android design
   - XYVerse custom theme with neon gradients
   - Glass morphism cards
   - Animated recording button with pulse effect
   - Custom logo with gradient background
   - Modern status cards with icons
   - Quick settings chips

---

## 📥 Download Link

**APK (Universal - All Architectures):**
```
https://github.com/xykal/XYRecorder/releases/download/v1.0.5/XYRecorder-app-release-universal.apk
```

**Release Page:**
```
https://github.com/xykal/XYRecorder/releases/tag/v1.0.5
```

**Size:** 7.88 MB

---

## 🎯 How to Use

### First Time Setup:

1. **Install APK** from the link above
2. **Grant Permissions** when prompted:
   - ✅ Screen recording permission (required)
   - ✅ Microphone permission (required for audio)
   - ✅ Overlay permission (required for floating bubble)
   - ✅ Notifications (optional, for foreground service)

### Recording:

1. **Open XYRecorder app**
2. **Tap the big gradient record button** (purple → cyan gradient)
3. **Accept screen capture permission** dialog
4. **App will move to background** automatically
5. **Floating bubble appears** on screen
   - Shows timer (00:00 format)
   - Drag to reposition
   - Tap to stop recording

### Controls:

- **Record Button** - Start recording
- **Floating Bubble** - Control recording from anywhere
  - Drag to move
  - Tap to stop
- **Notification** - Pause/Resume/Stop controls in notification panel

---

## 🎨 UI Features

### Home Screen:
- Custom gradient logo (purple → blue → cyan)
- Status cards with circle icons (Quality, FPS, Duration)
- Animated record button (pulses when recording)
- Noise reduction card with emoji presets
- Quick settings chips (Codec, Bitrate, Quality)

### Colors:
- **Primary Gradient:** Purple (#6C3FFF) → Cyan (#00D4AA)
- **Recording:** Red (#FF0844) with glow effect
- **Background:** Dark (#0A0A0F)
- **Cards:** Glass effect (#13131A)

### Floating Bubble:
- 140px circular gradient (Purple → Cyan)
- White circle in center with red recording dot
- Timer text below bubble
- Draggable anywhere on screen

---

## 📊 Technical Details

### Recording:
- **Resolution:** 720p (1280x720)
- **Frame Rate:** 30 FPS
- **Video Codec:** H.264 (AVC)
- **Audio Codec:** AAC
- **Video Bitrate:** 5 Mbps
- **Audio Bitrate:** 192 kbps
- **Output Format:** MP4

### Audio Processing:
- High-pass filter (Butterworth IIR)
- Noise gate with envelope control
- Spectral noise suppression
- Soft-knee compressor with makeup gain
- De-esser for sibilance reduction
- Brick-wall limiter
- 5 one-tap presets (None/Light/Medium/Heavy/Studio)

### Architecture Support:
- ✅ armeabi-v7a (32-bit ARM)
- ✅ arm64-v8a (64-bit ARM)
- ✅ x86 (32-bit Intel)
- ✅ x86_64 (64-bit Intel)

### Signature Verification:
- ✅ **v1** (JAR signing) - VERIFIED
- ✅ **v2** (APK Signature v2) - VERIFIED
- ✅ **v3** (APK Signature v3) - VERIFIED

---

## 🔐 Permissions Required

| Permission | Purpose |
|------------|---------|
| **Screen Recording** | Capture screen content |
| **Microphone** | Record audio from mic |
| **Overlay** | Show floating bubble |
| **Notifications** | Foreground service |
| **Storage** | Save MP4 files |

---

## 📁 Where Are Recordings Saved?

Recordings are saved to:
```
Internal Storage > Android > data > com.xyverse.xyrecorder > files > Movies > XYRecorder
```

File naming format: `XYRec_YYYYMMDD_HHmmss.mp4`

---

## 🐛 Troubleshooting

### Screen capture not working:
- Make sure you granted screen recording permission
- Check that overlay permission is enabled
- Restart the app

### Floating bubble not appearing:
- Enable "Display over other apps" permission in Settings
- Go to: Settings > Apps > XYRecorder > Advanced > Display over other apps

### Recording stops unexpectedly:
- Check notification panel for service status
- Make sure battery optimization is disabled for XYRecorder
- Check available storage space

### No audio in recording:
- Grant microphone permission
- Check microphone volume in settings
- Test with different noise reduction preset

---

## 🎬 Features Summary

✅ Screen recording (720p @ 30fps)  
✅ Microphone audio capture  
✅ Studio-quality noise reduction  
✅ Floating overlay bubble  
✅ Custom XYVerse theme  
✅ MP4 output format  
✅ Universal APK (all architectures)  
✅ Signed with v1/v2/v3  
✅ Material 3 UI  
✅ Gradient animations  
✅ Real-time timer  
✅ Drag-to-move bubble  

---

## 🚀 Ready to Use!

Download the APK and start recording! The app now properly captures your screen with a beautiful floating bubble control.

**Enjoy XYRecorder v1.0.5!** 🎉

---

**Repository:** https://github.com/xykal/XYRecorder  
**Developer:** xykal  
**Website:** xyverse.my.id
