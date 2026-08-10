# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this is

**Cite Circle** — an Android app: a mock academic social network for researchers
(feed of papers, citation formatting/export, reading lists, funding & job
opportunities, a Gemini-backed chat assistant).

It was scaffolded by **Google AI Studio** (see `README.md`, `metadata.json`), which
explains several artifacts you'll notice: the `com.example` namespace, the
`.env`/secrets-plugin convention borrowed from web projects, placeholder Firebase
config, and leftover template tests. Most screens are UI-complete but backed by
hardcoded sample data — only the feed/profile paper list is persisted.

Single Gradle module: `:app`. Kotlin + Jetpack Compose (Material 3), no other modules.

## Build & run

**There is no Gradle wrapper checked in** (`gradlew` is absent; `gradle/` holds only
`libs.versions.toml`). Use the system Gradle, or open the project in Android Studio
and let it generate a wrapper.

```bash
gradle :app:assembleDebug            # build debug APK
gradle :app:testDebugUnitTest        # JVM + Robolectric tests (see caveat below)
gradle :app:connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
gradle :app:lint
```

Roborazzi screenshot tests record with:

```bash
gradle :app:testDebugUnitTest -Proborazzi.test.record=true
```

Notes:
- `gradle.properties` enables the configuration cache, parallel execution, and
  `kotlin.compiler.execution.strategy=in-process`. If you hit "Could not connect to
  Kotlin compile daemon", that setting is the existing workaround — don't remove it.
- The `release` build type is signed from `KEYSTORE_PATH` / `STORE_PASSWORD` /
  `KEY_PASSWORD` env vars, defaulting to `$rootDir/my-upload-key.jks` (not in the repo).
- `debug` is signed with `$rootDir/debug.keystore`, which is **gitignored and absent**.
  `README.md` instructs local developers to delete the line
  `signingConfig = signingConfigs.getByName("debugConfig")` from `app/build.gradle.kts`.
  Don't commit that deletion — it's a per-developer step.

### Secrets

The **secrets-gradle-plugin** is configured (`app/build.gradle.kts`) to read `.env`,
falling back to `.env.example`, and to emit `BuildConfig` fields:

- `.env` is gitignored; `.env.example` is the committed template.
- `GEMINI_API_KEY` becomes `BuildConfig.GEMINI_API_KEY`, consumed by `ChatViewModel`.
- `FIREBASE_APPCHECK_DEBUG_TOKEN` is on the plugin's `ignoreList`.

Never commit a real key, and never move a key into `.env.example`, source, or
`strings.xml`.

### Firebase — placeholder state

`app/google-services.json` is a **dummy file** (`dummy-project`, `dummy-api-key`) and
`googleServices.missing.passthrough=true` plus `MissingGoogleServicesStrategy.WARN`
keep the build green without real config. Consequences to keep in mind:

- `strings.xml` has `default_web_client_id = YOUR_WEB_CLIENT_ID`. `FirebaseAuthManager`
  explicitly checks for that sentinel and bails out.
- `AuthScreen` calls `onAuthSuccess()` even when sign-in fails — a deliberate demo
  bypass so the app is usable without Firebase. Preserve or remove it consciously;
  don't "fix" it by accident.
- `ProfileViewModel.signInWithGoogle` uses a hardcoded
  `"dummy-client-id.apps.googleusercontent.com"`.

## Layout

```
app/src/main/java/com/example/
  MainActivity.kt            Activity + NavHost + several screens & shared composables
  MyApplication.kt           Application; builds Room DB and PaperRepository
  HomeViewModel.kt           Feed/profile state, theme toggle, + ViewModelFactory
  data/
    AppDatabase.kt           SavedPaper entity, DAO, RoomDatabase, PaperRepository
    FirestoreRepository.kt   Batched upload of papers to Firestore
  network/
    GeminiApiService.kt      Retrofit interface, request/response models, RetrofitClient
  ui/
    auth/AuthScreen.kt, FirebaseAuthManager.kt
    chat/ChatScreen.kt, ChatViewModel.kt
    lists/ReadingListsScreen.kt
    opportunities/OpportunitiesScreen.kt
    profile/ProfileAuthScreen.kt, ProfileViewModel.kt
    theme/Color.kt, Theme.kt, Typography.kt
```

`MainActivity.kt` is the largest file (~730 lines) and is not just the Activity — it
also holds `FolioApp` (Scaffold + nav graph), `HomeScreen`, `ProfileScreen`,
`FieldsScreen`, `NotificationsScreen`, `NotificationDetailScreen`, plus the reusable
`PostCard`, `CitationBlock`, `CitationChart`, and `EmptyState` composables. New
feature screens should go in their own `ui/<feature>/` package instead of growing this
file further.

## Architecture

**Manual DI, no Hilt/Koin.** `MyApplication` owns `AppDatabase` and `PaperRepository`
as `lateinit` properties. `MainActivity` casts `LocalContext.applicationContext` to
`MyApplication` and passes `repository` into `HomeViewModelFactory`. Other view models
(`ChatViewModel`, `ProfileViewModel`) have no-arg constructors and are obtained with
plain `viewModel()`. If you add a view model needing dependencies, follow the
`HomeViewModelFactory` pattern.

**State** is `StateFlow` exposed from view models, collected in Compose with
`collectAsStateWithLifecycle()` (preferred) or `collectAsState()` (used in the chat and
profile screens). Room DAO returns `Flow<List<SavedPaper>>`, lifted with
`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`.

**Navigation** is a single `NavHost` in `FolioApp` with string routes:
`auth`, `feed`, `fields`, `lists`, `opps`, `profile`, `chat`, `notifications`,
`notification_detail`. Start destination depends on `authManager.getCurrentUser()`.
Note the quirk: `currentRoute` is a `mutableStateOf` mirror kept in sync by an
`addOnDestinationChangedListener` registered during composition, and it drives whether
the top bar and bottom bar render. Adding a route means also deciding whether it
belongs in the `items` list of the bottom `NavigationBar`.

**Persistence.** Room database `folio_db`, one entity `SavedPaper`, `version = 1`,
`exportSchema = false`. There are no migrations — changing the entity requires bumping
`version` and adding a migration (or `fallbackToDestructiveMigration()`), otherwise the
app crashes at startup for existing installs. `HomeViewModel.init` seeds one hardcoded
demo paper when the table is empty.

**Cloud sync** is one-directional and partial: `FirestoreRepository.syncPapersToCloud`
batch-writes to `users/{uid}/saved_papers/{paperId}` and no-ops when signed out. It is
only invoked from `HomeViewModel.savePaper`, which nothing currently calls.

**Gemini.** Two mechanisms coexist:
- The live path is hand-rolled Retrofit against
  `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`,
  with the key passed as a `key` query param from `BuildConfig.GEMINI_API_KEY`.
  `ChatViewModel` keeps its own `conversationHistory` list of `Content` and supports an
  optional `googleSearch` grounding tool and inline base64 JPEG images.
- `firebase-ai` is declared as a dependency but **not used anywhere**. Don't assume it
  is wired up.

Model IDs are string literals in `ChatViewModel` and the `FilterChip`s of `ChatScreen`;
they must be kept in sync if changed.

## Theming

`InkAndFieldNotesTheme` (`ui/theme/Theme.kt`) — an "ink and field notes" editorial
palette. **Dynamic color is deliberately disabled**; there's a comment saying so.
Colors come from the named constants in `Color.kt` (ForestGreen, Terracotta,
WarmOchre, ParchmentCream, CharcoalInk, …) — use those, not raw `Color(0x…)` literals.

Dark mode is **app state, not system state**: `HomeViewModel.isDarkMode` (a
`MutableStateFlow<Boolean>` defaulting to `false`, toggled from the profile screen) is
passed to the theme. It is not persisted across launches.

Typography uses downloadable Google Fonts: **EB Garamond** for display/headline/title,
**Inter** for body/label; monospace is used ad hoc for citations and IDs. Screens lean
on a consistent visual vocabulary — 4.dp corner radii, 1.dp hairline borders at
`onBackground.copy(alpha = 0.1f)`, 24.dp content padding, and all-caps labels with
wide `letterSpacing` — match it when adding UI.

## Testing

- `app/src/test/` — JVM unit tests, Robolectric (`@Config(sdk = [36])`), and Roborazzi
  screenshot tests. `isIncludeAndroidResources = true` is set.
- `app/src/androidTest/` — Espresso/Compose instrumented tests.
- Reference screenshots live in `app/src/test/screenshots/`.

⚠️ **The template tests are stale and the unit-test source set does not compile as-is:**

- `GreetingScreenshotTest` references `MyApplicationTheme` and `Greeting`, neither of
  which exists (the theme is `InkAndFieldNotesTheme`; there is no `Greeting`
  composable).
- `ExampleRobolectricTest` asserts `app_name == "My Application"`, but `strings.xml`
  says `"Cite Circle"`.

Fix or delete these before relying on `testDebugUnitTest`, and don't report a green
test run you haven't actually seen pass.

## Conventions

- **Dependencies go through the version catalog** (`gradle/libs.versions.toml`) — always
  `libs.<alias>`, never a hardcoded coordinate string. Unused dependencies in
  `app/build.gradle.kts` are **commented out rather than deleted** (camera, coil,
  datastore, location, accompanist); there's an explicit comment saying this is
  intentional. Follow it.
- **KSP** (not kapt) for annotation processing: Room compiler and Moshi codegen.
- **Indentation is inconsistent by file** — 2 spaces in the Gradle files,
  `MainActivity.kt`, and `ui/theme/`; 4 spaces in `data/`, `network/`, and the `ui/`
  feature packages. Match the file you're editing rather than reformatting it.
- `MainActivity.kt` frequently uses **fully-qualified inline references**
  (`com.example.ui.lists.ReadingListsScreen()`,
  `androidx.compose.ui.platform.LocalContext.current`) instead of imports. Harmless;
  don't churn the file to normalize it unless that's the task.
- Sample/mock data is declared as top-level `val`s next to the screen that renders it
  (`sampleLists` in `ReadingListsScreen.kt`, `sampleOpportunities` in
  `OpportunitiesScreen.kt`). Keep new mock data local to its screen until it earns a
  repository.
- Deprecated Material icon usages are annotated with `@Suppress("DEPRECATION")` rather
  than migrated (e.g. `Icons.Filled.ArrowBack`).
- App id is `com.aistudio.folio.wzpx` while the code namespace is `com.example` — this
  mismatch is intentional AI Studio scaffolding. Renaming the package is a large,
  cross-cutting change (manifest, `google-services.json`, Firebase console); don't do it
  incidentally.

## Known rough edges

Useful context so these aren't mistaken for bugs you introduced:

- `HomeViewModel.savePaper` and `removePaper` are unreferenced; the UI has no
  create/delete path yet.
- `savePaper` collects `repository.allPapers.take(1)` then syncs `papers + paper`,
  duplicating the just-saved paper in the Firestore write.
- Citation "format" switching in `CitationBlock` is string surgery on a hardcoded
  `" (2026). "` substring, not real APA/MLA/Chicago formatting. BibTeX/RIS export
  buttons only show a Toast.
- `CitationChart`'s data points, the h-index, `FieldsScreen`'s field list and researcher
  counts, and the profile identity ("Dr. Jane Doe") are all hardcoded.
- `ProfileAuthScreen` / `ProfileViewModel` are a self-contained Firebase demo not
  reachable from the nav graph — the `profile` route renders `ProfileScreen` in
  `MainActivity.kt` instead.
- `ProfileViewModel.signInWithGoogle` checks `credential is GoogleIdTokenCredential`,
  whereas `FirebaseAuthManager` correctly checks for `CustomCredential` with the Google
  ID token type. The latter is the working implementation.
- Only `INTERNET` is declared in the manifest; there are no other permissions.

## Git

Development happens on feature branches; `main` is the default branch. Commit and push
only when asked.
