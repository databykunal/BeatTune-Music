# Sonify Project Structure

```text
SonifyMusic/
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sonify/music/
│       │   ├── MusicApplication.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── FavoriteSongEntity.kt
│       │   │   │   ├── MusicDatabase.kt
│       │   │   │   └── SongDao.kt
│       │   │   └── remote/
│       │   │       ├── PipedModels.kt
│       │   │       └── YouTubeMusicApi.kt
│       │   ├── di/AppModule.kt
│       │   ├── domain/model/Song.kt
│       │   ├── player/playback/
│       │   │   ├── MusicPlaybackController.kt
│       │   │   └── MusicPlaybackService.kt
│       │   └── presentation/
│       │       ├── MainActivity.kt
│       │       ├── MainViewModel.kt
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt
│       │       │   ├── LibraryScreen.kt
│       │       │   ├── MiniPlayer.kt
│       │       │   ├── PlayerScreen.kt
│       │       │   ├── SearchScreen.kt
│       │       │   └── SongRow.kt
│       │       └── theme/
│       │           ├── Color.kt
│       │           ├── Theme.kt
│       │           └── Type.kt
│       └── res/
│           ├── drawable/
│           └── values/
└── gradle.properties
```

## Responsibilities

### Remote data
`YouTubeMusicApi` handles instance discovery, timeouts, search, stream resolution and safe parsing.

### Local data
`Room` stores favorite songs only.

### Playback
`MusicPlaybackController` owns queue/transport state from the UI side. `MusicPlaybackService` owns the long-lived Media3 player and media session.

### Presentation
`MainViewModel` owns Home/Search/Library state. Compose screens render state and send user actions back to the ViewModel or playback controller.
