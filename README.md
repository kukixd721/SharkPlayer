# 🦈 Shark Player

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**Shark Player** is a cutting-edge music player for Android, built with **Jetpack Compose** and **Material 3 Expressive** design. It offers a seamless, high-performance audio experience with a focus on visual aesthetics and advanced features for power users.

---

## ✨ Key Features

### 🎧 Audio & Playback
- **Reactive Visualizer**: High-precision FFT processing system with quadratic grouping for dynamic frequency response.
- **Equalizer**: 10-band equalizer with dedicated bass boost and virtualization controls.
- **Seamless Crossfade**: Smooth transitions between tracks to keep the music flowing.
- **Dynamic Queue**: Access and manage your play queue instantly by **swiping up** from the player screen.
- **Sleep Timer**: Fall asleep to your music with a programmable auto-off timer.

### 🎤 Advanced Lyrics Engine
- **Karaoke Mode**: Smooth, synchronized lyrics support with `.lrc` file compatibility.
- **Integrated Lyrics Editor**: Edit lyrics directly within the app and embed them into file metadata using **JAudioTagger**.
- **Auto-Search**: Automatically fetch missing lyrics from online databases.
- **Social Sharing**: Create beautiful, shareable images with lyrics snippets for your social media stories.
- **Enhanced Scrolling**: Fixed and optimized lyrics scrolling for a frustration-free experience.

### 📂 Library & Downloads
- **Multi-Source Downloader**: 
  - 🎥 **YouTube**: Audio extraction powered by **yt-dlp**.
  - 🎵 **Spotify**: (has some bugs) Experimental support (beta) to sync and download your favorite tracks.
- **Download Management**: New dedicated **Download History** and a comprehensive **Download Guide** for new users.
- **Smart Organization**: Automatically categorizes your local library by Artists, Albums, Genres, and Folders.
- **Library Scanner**: Dynamic background scanning to keep your library up to date.
- **Tag & Cover Art Editor**: Full control over your file metadata and album art.

### 🎨 Personalization (Material You)
- **Dynamic Color Support**: The entire UI adapts to your device's wallpaper and theme.
- **Expressive Design**: Modern UI with fluid animations, glassmorphism effects (dynamic blur), and customizable rounded corners.
- **Visualizer Customization**: Toggle the visualizer and adjust its sensitivity to your liking.
- **Always-On Display (AOD)**: Keep the player visible while your phone is on its desk stand.

---

## 🛠️ Technical Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a modern, declarative interface.
- **Audio Engine**: [Jetpack Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3) for robust media playback.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for fast and efficient image rendering.
- **Metadata Management**: [JAudioTagger](http://www.jthink.net/jaudiotagger/) for deep file tagging capabilities.
- **Asynchronous Flow**: Kotlin Coroutines & StateFlow for a responsive, lag-free UI.
- **System Tools**: Integrated **Logcat Monitor** for debugging and performance tracking.

---

## 🚀 Getting Started

1. **Clone the repo**:
   ```bash
   git clone https://github.com/kukoso721/shark-player.git
   ```
2. **Open in Android Studio**: Use **Jellyfish** or later for full Compose support.
3. **Build & Run**: Deploy to any device running **Android 8.0 (API 26)** or higher.

---

## 📝 Roadmap
- [ ] Stabilize Spotify download support.
- [ ] Add Cloud Storage integration (Drive, SoundCloud).
- [ ] Implement local network streaming (DLNA/UPnP).
- [ ] Additional Visualizer themes.
- (If you have any ideas, let me know for later).

---

## ⚠️ Disclaimer
This app is intended for personal use and management of local music libraries. The integrated downloader tools (**yt-dlp**) should be used in compliance with the terms of service of the content providers and local copyright laws.

---

## 🤝 Support & Contribution
Found a bug or have a suggestion? Open an [issue](https://github.com/kukoso721/shark-player/issues) or submit a PR.

Developed by **kukoso721** 🚬
