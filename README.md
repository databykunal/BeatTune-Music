# BeatTune 🎧

BeatTune is a modern Android music player built with Kotlin and Jetpack Compose.

It is designed to provide a clean, smooth and simple music listening experience with features like search, playlists, favorites, queue and background playback.

## ✨ Features

- 🎵 Music search and playback
- 🔎 Fast and responsive search
- ❤️ Favorites
- 📂 Create and manage playlists
- 🕘 Recently played songs
- ▶️ Autoplay
- ⏭️ Next / Previous song
- 🔀 Shuffle
- 🔁 Repeat
- 📋 Playback queue
- 🎧 Mini player
- 🎶 Full-screen music player
- 🔔 Background playback and media notification controls
- 🏠 Music categories on the home screen
- 👤 Personalised home screen with first-launch name setup
- 🌙 Premium dark UI
- 🖼️ Album artwork
- ⚡ Playback caching and preloading for smoother transitions

## 🛠️ Built With

- Kotlin
- Jetpack Compose
- Media3 / ExoPlayer
- Room Database
- Hilt
- Ktor
- Coil
- NewPipe Extractor

## 🎵 Playback

BeatTune uses direct YouTube stream extraction as its primary playback method, with public Piped instances available as a fallback.

The app also uses stream caching, retry handling and preloading of upcoming songs to improve playback reliability.

Since BeatTune relies on external services for music search and stream resolution, their availability can affect playback and search results.

## 📚 Library

Your music library is stored locally on your device.

You can:

- Save songs to Favorites
- Create playlists
- Add songs to playlists
- Remove songs from playlists
- View recently played songs
- Play songs directly from your library

## 🔒 Privacy

BeatTune does not require an account or subscription.

Favorites, playlists and recently played data are stored locally on your device.

BeatTune does not upload your personal music library to a project-owned backend.

Music search and playback may communicate with third-party services required to provide these features.

## 🎨 Design

BeatTune uses a premium dark interface with:

- Deep navy backgrounds
- Violet and blue accents
- Subtle pink highlights
- Clean cards and controls
- Persistent mini player
- Modern bottom navigation
- Focused now-playing experience

The goal is to keep the interface modern and premium without excessive visual effects.

## 🚀 Build

The project uses:

- Kotlin
- Gradle
- Android Gradle Plugin
- Java 17
- Jetpack Compose

GitHub Actions is included to build the Android APK automatically.

To build locally:
⚠️ Disclaimer

BeatTune is an independent open-source project.

It is not affiliated with YouTube, Google, SimpMusic, Piped, or NewPipe.

BeatTune relies on third-party services and libraries for search and stream resolution. These services may change or become unavailable at any time.

See DIRECT_PLAYBACK_NOTICE.md for information regarding NewPipe Extractor licensing.

🤝 Contributing

Contributions, suggestions and bug reports are welcome.

If you find a bug or want to improve BeatTune, feel free to open an issue or submit a pull request.

📄 License

BeatTune is released under the MIT License.

See LICENSE for the full license.

Made with Kotlin & Jetpack Compose 🎧

```bash
./gradlew assembleDebug
