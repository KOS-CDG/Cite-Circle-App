# CLAUDE.md

Guidance for AI assistants working in this repository.

## What this is

**Cite Circle** — an Android app: a mock academic social network for researchers
(feed of papers, citation formatting/export, reading lists, funding & job
opportunities, a Gemini-backed chat assistant).

It was scaffolded by **Google AI Studio** (see `README.md`, `metadata.json`), which
explains several artifacts you'll notice: the `com.example` namespace, the
`.env`/secrets-plugin convention borrowed from web projects, and placeholder Firebase
config. The front-end is now feature-complete — every screen exists and every control
navigates somewhere — but most content below the paper feed is still **sample data**
rather than a real backend.

Single Gradle module: `:app`. Kotlin + Jetpack Compose (Material 3), no other modules.

## Build & run

**There is no Gradle wrapper checked in** (`gradlew` is absent; `gradle/` holds only
`libs.versions.toml`). Use the system Gradle, or open the project in Android Studio
and let it generate a wrapper.

```bash
gradle :app:assembleDebug            # build debug APK
gradle :app:testDebugUnitTest        # JVM + Robolectric + Roborazzi tests
gradle :app:connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
gradle :app:lint
```

Roborazzi screenshot tests record with:

```bash
gradle :app:testDebugUnitTest -Proborazzi.test.record=true
```

> **Dependency resolution requires `dl.google.com`.** AGP, AndroidX, Compose and
> Firebase all resolve from Google's Maven repo, which Gradle's `google()` shorthand
> points at `https://dl.google.com/dl/android/maven2/`. In sandboxes that block that
> host the build fails at configuration time with
> `Plugin [id: 'com.android.application', version: '9.1.1'] was not found`.
> That is a network policy problem, not a project misconfiguration —
> `maven.google.com` only redirects to the same blocked host, so there is no
> in-repo workaround. Build where Google Maven is reachable.

Notes:
- `gradle.properties` enables the configuration cache, parallel execution, and
  `kotlin.compiler.execution.strategy=in-process`. If you hit "Could not connect to
  Kotlin compile daemon", that setting is the existing workaround — don't remove it.

  **But it breaks on newer Gradle.** In-process compilation runs the Kotlin compiler
  inside the Gradle daemon, sharing its classpath. Gradle 9.7's embedded Kotlin then
  collides with the Compose compiler plugin built for Kotlin 2.2.10 and the build dies
  before parsing a single source file:

  ```
  e: Plugin androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar is
     incompatible with the current version of the compiler.
  Caused by: java.lang.AbstractMethodError: ComposePluginRegistrar does not define or
     inherit an implementation of 'abstract String getPluginId()'
  ```

  Override per-invocation rather than editing `gradle.properties`:
  `-Pkotlin.compiler.execution.strategy=daemon`. That is what
  `.github/workflows/ci.yml` does.
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
- Because of that bypass, `rememberResearcherIdentity()` treats "no Firebase user" as
  the normal path and falls back to a demo identity rather than an error.

## Layout

```
app/src/main/java/com/example/
  MainActivity.kt            Activity, Scaffold, registry header, bottom bar
  MyApplication.kt           Application; builds Room DB and PaperRepository
  HomeViewModel.kt           Paper state, loading flag, theme toggle, + ViewModelFactory
  data/
    AppDatabase.kt           SavedPaper entity, DAO, RoomDatabase, PaperRepository
    FirestoreRepository.kt   Batched upload of papers to Firestore
    SampleData.kt            All mock fixtures + id lookups
  network/
    GeminiApiService.kt      Retrofit interface, request/response models, RetrofitClient
  ui/
    navigation/              Routes (typed constants + builders), CiteCircleNavHost
    components/              Shared composables: PaperCard, CitationBlock, CitationChart,
                             EmptyState/ErrorState/LoadingList, ScreenHeader, SectionLabel,
                             QuoteBlock, InitialsAvatar, CiteCircleDefaults
    auth/ chat/ compose/ feed/ fields/ lists/ notifications/ onboarding/
    opportunities/ paper/ profile/ search/ settings/ theme/
```

One screen per file, one package per feature. `MainActivity.kt` holds only the Activity
and the app chrome — new screens go in `ui/<feature>/`, never back into `MainActivity`.

## Architecture

**Manual DI, no Hilt/Koin.** `MyApplication` owns `AppDatabase` and `PaperRepository`
as `lateinit` properties. `MainActivity` casts `LocalContext.applicationContext` to
`MyApplication` and passes `repository` into `HomeViewModelFactory`. Other view models
(`ChatViewModel`, `ProfileViewModel`) have no-arg constructors and are obtained with
plain `viewModel()`. If you add a view model needing dependencies, follow the
`HomeViewModelFactory` pattern.

**A single `HomeViewModel` instance is hoisted** in `MainActivity` and passed down
through `CiteCircleNavHost` to every screen that needs papers. Detail screens do not
get their own view model — `PaperDetailScreen` and `UserProfileScreen` resolve their
navigation argument against `HomeViewModel.savedPapers`.

**State** is `StateFlow` exposed from view models, collected in Compose with
`collectAsStateWithLifecycle()`. Room DAO returns `Flow<List<SavedPaper>>`, lifted with
`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`. Because
that seeds with `emptyList()`, `HomeViewModel.isLoading` exists to distinguish "still
loading" from "genuinely empty" — use it before showing an empty state.

**Navigation.** All routes live in `ui/navigation/Routes.kt` as constants, with builder
functions for parameterised ones (`Routes.paperDetail(id)`). The graph is in
`CiteCircleNavHost.kt`. Two rules matter:

- Compare against the **pattern** (`Routes.PAPER_DETAIL` == `"paper_detail/{paperId}"`),
  never a resolved path — `NavDestination.route` reports the pattern.
- `isTopLevelRoute(route)` decides whether the registry header and bottom bar render.
  Top-level routes are the five in `topLevelDestinations`; everything else is
  full-screen and supplies its own `ScreenHeader` with a back affordance.

The current route comes from `navController.currentBackStackEntryAsState()`. Do not
reintroduce a manually mirrored `currentRoute` — the previous
`addOnDestinationChangedListener` version broke as soon as routes took arguments.

**Persistence.** Room database `folio_db`, one entity `SavedPaper`, `version = 1`,
`exportSchema = false`. There are no migrations — changing the entity requires bumping
`version` and adding a migration (or `fallbackToDestructiveMigration()`), otherwise the
app crashes at startup for existing installs. `HomeViewModel.init` seeds one demo paper
when the table is empty.

**Cloud sync** is one-directional and partial: `FirestoreRepository.syncPapersToCloud`
batch-writes to `users/{uid}/saved_papers/{paperId}` and no-ops when signed out. It runs
from `HomeViewModel.savePaper`, which reads the table back with `allPapers.first()`
*after* the write so the upload matches what was stored.

**Gemini.** Two mechanisms coexist:
- The live path is hand-rolled Retrofit against
  `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`,
  with the key passed as a `key` query param from `BuildConfig.GEMINI_API_KEY`.
  `ChatViewModel` keeps its own `conversationHistory` and supports an optional
  `googleSearch` grounding tool and inline base64 JPEG images.
- `firebase-ai` is declared as a dependency but **not used anywhere**. Don't assume it
  is wired up.

Model IDs are string literals in `ChatViewModel` and in `chatModels` in `ChatScreen.kt`;
keep them in sync.

## Theming

`InkAndFieldNotesTheme` (`ui/theme/Theme.kt`) — an "ink and field notes" editorial
palette. **Dynamic color is deliberately disabled**; there's a comment saying so.
Colors come from the named constants in `Color.kt` (ForestGreen, Terracotta,
WarmOchre, ParchmentCream, CharcoalInk, …) — use those, not raw `Color(0x…)` literals.

Dark mode is **app state, not system state**: `HomeViewModel.isDarkMode` (a
`MutableStateFlow<Boolean>` defaulting to `false`, toggled from Settings) is passed to
the theme. It is not persisted across launches.

Typography uses downloadable Google Fonts: **EB Garamond** for display/headline/title,
**Inter** for body/label; monospace is used ad hoc for citations and IDs.

**Reuse `CiteCircleDefaults`** (`ui/components/Common.kt`) rather than re-deriving the
design tokens: `CardShape` (4.dp), `ButtonShape` (2.dp), `ScreenPadding` (24.dp),
`hairlineColor()` / `cardBorder()` (1.dp at 10% `onBackground`). All-caps labels with
wide `letterSpacing` go through `SectionLabel`.

## Testing

- `app/src/test/` — JVM unit tests, Robolectric (`@Config(sdk = [36])`), and Roborazzi
  screenshot tests. `isIncludeAndroidResources = true` is set.
- `app/src/androidTest/` — Espresso/Compose instrumented tests.
- Reference screenshots live in `app/src/test/screenshots/`.

`ComponentScreenshotTest` captures the shared components in **both themes** — the
palette is hand-rolled and dynamic color is off, so dark mode will not fix itself.
Screens are not screenshotted directly because they need a Room-backed `HomeViewModel`;
component coverage is the practical substitute.

## Conventions

- **Dependencies go through the version catalog** (`gradle/libs.versions.toml`) — always
  `libs.<alias>`, never a hardcoded coordinate string. Unused dependencies in
  `app/build.gradle.kts` are **commented out rather than deleted** (camera, coil,
  datastore, location, accompanist); there's an explicit comment saying this is
  intentional. Follow it.
- **KSP** (not kapt) for annotation processing: Room compiler and Moshi codegen.
- **Mock data lives in `data/SampleData.kt`**, not next to the screen that renders it.
  Detail screens resolve a navigation argument through `SampleData.fieldById(…)` and
  friends, which per-screen top-level vals could not support. Add new fixtures there.
- **Composables take callbacks, not view models**, wherever they can — `PaperCard` takes
  `onEndorse`/`onViewContext` so it is reusable and previewable. Screens wire the
  callbacks to navigation in `CiteCircleNavHost`.
- **4-space indentation** in all of `ui/` and `data/`; the Gradle files and
  `ui/theme/` remain at 2 spaces. Match the file you're editing.
- Deprecated Material icon usages are annotated with `@Suppress("DEPRECATION")` rather
  than migrated (e.g. `Icons.Filled.ArrowBack`, `Icons.Outlined.Chat`).
- App id is `com.aistudio.folio.wzpx` while the code namespace is `com.example` — this
  mismatch is intentional AI Studio scaffolding. Renaming the package is a large,
  cross-cutting change (manifest, `google-services.json`, Firebase console); don't do it
  incidentally.

## Known rough edges

Useful context so these aren't mistaken for bugs you introduced:

- **UI strings are hardcoded inline**, not in `strings.xml`, and there are no `@Preview`
  composables. Both are known gaps.
- Citation "format" switching in `CitationBlock` is string surgery on a hardcoded
  `" (2026). "` substring, not real APA/MLA/Chicago formatting. BibTeX/RIS export
  buttons only show a Toast.
- `CitationChart`'s data points and h-index are hardcoded sample values, shown on both
  your own profile and other researchers'.
- Reading lists, opportunities, fields and notifications are **read-only sample data**.
  The list editor's save path shows a confirmation and pops back without persisting;
  follow/save toggles are local `remember` state.
- `UserProfileScreen` takes a **paper id**, not a user id — papers are the only real
  records, so the author is derived from the paper. There is no researcher directory.
- Notification "VIEW FULL PAPER" lands on the feed: notifications reference papers that
  do not exist in the local Room table.
- Only `INTERNET` is declared in the manifest; there are no other permissions.

## Git

Development happens on feature branches; `main` is the default branch. Commit and push
only when asked.
