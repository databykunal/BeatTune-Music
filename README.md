# BeatTune Music

BeatTune is a clean, dark Android music player built with Kotlin + Jetpack Compose + Media3.

## Current app flow

- Home with music categories and fresh picks
- Debounced music search with cancellation/race protection
- Favorites stored locally with Room
- Mini-player and full now-playing screen
- Play / pause / seek / previous / next
- Shuffle + repeat controls
- Background playback through MediaSessionService
- Robust loading, empty and error states
- Piped instance discovery with fallback instances
- Artwork loaded through Coil

## Architecture

```text
Compose UI
   ↓
MainViewModel ──→ YouTubeMusicApi ──→ Piped instances
   │
   └────────────→ Room / Favorites

Compose Player UI
   ↓
MusicPlaybackController
   ↓
MediaController
   ↓
MusicPlaybackService
   ↓
Media3 / ExoPlayer
```

The app does not upload music files or require a backend owned by Sonify. Search and stream URLs are resolved through publicly available Piped API instances. Because these instances are community-hosted, the app discovers current instances and falls back when one is unavailable.

## Build

The GitHub Actions workflow uses Java 17 and Gradle 8.2 to build the debug APK.

## Notes

Sonify is inspired by modern music-player UX patterns such as a persistent mini-player, a focused now-playing screen and library-first navigation. It is an independent project and is not affiliated with SimpMusic, YouTube, or Google.


## Playback architecture

Sonify now uses a direct YouTube extraction provider as the primary playback path. Public Piped instances are retained only as a fallback, so playback is no longer dependent on one community-hosted Piped server. Stream URLs are fetched immediately before playback, Media3 uses a dedicated HTTP data source, and playback errors trigger one fresh-source retry.

See `DIRECT_PLAYBACK_NOTICE.md` for the NewPipe Extractor licensing notice.


## V7 Premium UI update
- Refined BeatTune palette: deep navy surfaces with restrained violet/blue accents.
- Reduced saturated gradients and glow-heavy elements for a calmer premium look.
- Launcher icon uses the same restrained BeatTune palette.
- Network timeouts/fallback limits were tightened to reduce unnecessary waiting on unhealthy providers.
- First-launch display-name onboarding remains local-only and user-editable.
