# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Building

Open in Android Studio (Hedgehog or newer). Gradle sync runs automatically.

```bash
# CLI build (requires Android SDK on PATH)
./gradlew assembleDebug
./gradlew installDebug   # install on connected device/emulator
```

No test suite is configured.

## API base URL

- **Debug (emulator):** `http://10.0.2.2:3001/api/` — `10.0.2.2` is the emulator's alias for your machine's localhost
- **Release:** set `buildConfigField("String", "API_BASE_URL", "\"https://your-backend.railway.app/api/\"")` in `app/build.gradle.kts`

Change the release URL in `app/build.gradle.kts` before shipping. `network_security_config.xml` allows cleartext only for `10.0.2.2`.

## Architecture

Single-Activity (`MainActivity.kt`) + Jetpack Compose + Navigation Compose. No dependency injection framework — `ApiClient.create(context)` is called directly in each `AndroidViewModel`.

```
data/
  api/
    ApiClient.kt       — OkHttp + Retrofit factory; adds Bearer token from TokenManager;
                         clears token on 401; logs bodies in debug builds
    ApiService.kt      — Retrofit interface (all endpoints)
    models/            — Gson-deserialized DTOs (AuthModels, DashboardModels, ProfileModels)
  TokenManager.kt      — SharedPreferences wrapper: saveAuth(token, user), getToken(), getUser(), clear()

ui/
  theme/               — Color.kt (palette + scoreColor/difficultyColor helpers), Theme.kt, Type.kt
  components/
    SharedComponents.kt — ScoreRing (Canvas), YouTubePlayer (WebView), BreakdownGrid,
                          SentenceBlock, ScCard, ScTextField, ScButton, DifficultyBadge,
                          LoadingScreen, ErrorText, scorePraise()
  auth/                — AuthScreen + AuthViewModel (Register steps 1-3, Sign In)
  home/                — HomeScreen + HomeViewModel (today's scene, submit, post-submission feedback)
  feedback/            — FeedbackScreen + FeedbackViewModel (past submission detail)
  profile/             — ProfileScreen + ProfileViewModel (stats + paginated history)

navigation/NavGraph.kt — NavHost with bottom bar (Home, Profile); routes: auth, home, profile, feedback/{id}
```

## Key patterns

**ViewModel state:** each VM has `_uiState: MutableStateFlow<ScreenUiState>`. Screens collect with `collectAsState()`. State is a data class, never mutated directly.

**API error handling:** all `viewModelScope.launch` blocks have a `catch (e: Exception)` that sets an error message on the state. The `ApiClient` auth interceptor does not throw on 401 — it clears the token silently; the next API call requiring auth will fail with a different error and the screen shows it.

**YouTube embeds:** `extractYouTubeId(url)` in `SharedComponents.kt` pulls the 11-char video ID. `YouTubePlayer` renders a `WebView` at 16:9 aspect ratio with JS enabled.

**Speech input:** `HomeScreen` wires `SpeechRecognizer` with `PARTIAL_RESULTS=true`. Partial results append to `baseText`; final results replace it. Requires `RECORD_AUDIO` permission (runtime-requested via `ActivityResultContracts.RequestPermission`).

**`scorePraise()`** is a plain (non-composable) function in `SharedComponents.kt` — call it inside `Text(...)`.

## Color reference

```kotlin
Background   = 0xFF0E0E1A   // page background
CardBackground = 0xFF1A1A2E  // card fill
Accent       = 0xFFE8FF47   // yellow-green — CTAs, score ring
TextPrimary  = 0xFFF0F0F0
TextSecondary = 0xFF74748A
Success      = 0xFF4ADE80   // score ≥ 8
Warning      = 0xFFF59E0B   // score 5–7
Danger       = 0xFFF87171   // score < 5
Purple       = 0xFF7C6FEF   // improved sentence accent border
```
