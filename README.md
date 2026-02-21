# SelfieStickerAI

Turn any selfie into ready-to-use transparent stickers for WhatsApp and Telegram.

## Features
- 📸 Camera or Gallery selection
- 🤖 AI Background Removal (Google ML Kit - on-device, no internet)
- 🎨 Transparent PNG preview with checkerboard background
- 📐 Auto-resize to 512x512 WebP format
- 📤 One-tap share to WhatsApp/Telegram
- 🌙 Dark theme, fully offline

## Tech Stack
- **Platform**: Android (Kotlin)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **ML**: Google ML Kit Selfie Segmentation (on-device)
- **Camera**: CameraX
- **Format**: WebP with transparency

## Build Instructions
1. Clone repository
2. Open in Android Studio Hedgehog (2023.1.1) or newer
3. Sync Gradle
4. Run on device/emulator (min Android 7.0)

## Permissions
- Camera: For taking selfies
- Storage: For saving stickers

## License
MIT
