# FitTrack — Jetpack Compose Fitness Tracker

A 100% Jetpack Compose (Material 3), pure-MVVM fitness tracker built for a final exam project. No XML layouts, no `findViewById` — a single Compose Activity hosts a Bottom-Navigation-driven app with Dashboard, Workouts, and History tabs, backed by a local Room database.

## 1. App Concept

FitTrack is a fully offline, single-user fitness tracker with three tabs behind a Bottom Navigation Bar:

- **Dashboard** — the "wow" landing screen: a hand-drawn, animated circular **Goal Ring** (Canvas `drawArc`, animated with `animateFloatAsState`) showing progress toward a weekly workout goal, plus quick summary stats (total sessions, current streak).
- **Workouts** — a form screen to log a new workout session (exercise name, sets, reps, weight), which persists to Room and immediately feeds the Dashboard ring and History list.
- **History** — a `LazyColumn` of all past workout sessions (most recent first), each row showing exercise/sets/reps/weight/timestamp with a delete action and tap-to-edit (routes back into the Workouts form, pre-filled).

All three screens share one Room database, one `AppContainer` (manual DI — no Hilt), and StateFlow-based ViewModels.

## 2. Package Structure

```
com.example.final_exam_project/
├── FitTrackApplication.kt          # Application subclass; owns the single AppContainer instance
├── MainActivity.kt                 # ComponentActivity; setContent { FitTrackTheme { FitTrackNavHost(...) } }
│
├── data/
│   ├── local/
│   │   ├── WorkoutSession.kt       # @Entity: id, exerciseName, sets, reps, weight, timestamp
│   │   ├── WorkoutDao.kt           # @Dao: insert/update/delete/getAll(Flow)/getById/getCountSince
│   │   └── FitTrackDatabase.kt     # @Database, exposes workoutDao(), singleton getInstance()
│   └── repository/
│       └── WorkoutRepository.kt    # Wraps WorkoutDao; Flow + suspend CRUD
│
├── di/
│   ├── AppContainer.kt             # Interface + DefaultAppContainer(context): builds DB, Repository
│   └── ViewModelFactory.kt         # ViewModelProvider.Factory, built from an AppContainer
│
├── navigation/
│   ├── BottomNavItem.kt            # Sealed class: Dashboard, Workouts, History (route, label, icon)
│   ├── NavGraph.kt                 # NavHost wiring for each screen + edit-by-id Workouts route
│   └── FitTrackNavHost.kt          # Scaffold with bottomBar { NavigationBar } + the NavGraph
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # Material 3 color tokens
│   │   ├── Theme.kt                # FitTrackTheme composable (light/dark MaterialTheme wrapper)
│   │   └── Type.kt                 # Typography
│   │
│   ├── components/
│   │   ├── GoalRing.kt             # Canvas-based animated ring — the "wow" feature
│   │   ├── StatCard.kt             # Reusable summary-stat card (label + value)
│   │   ├── WorkoutListItem.kt      # Single row composable for the History LazyColumn
│   │   └── EmptyState.kt           # Reusable "no data yet" placeholder
│   │
│   └── screens/
│       ├── dashboard/
│       │   ├── DashboardScreen.kt      # Renders GoalRing + StatCards
│       │   ├── DashboardViewModel.kt   # Exposes StateFlow<DashboardUiState>
│       │   └── DashboardUiState.kt     # goalProgress, sessionsThisWeek, totalSessions, streak
│       ├── workouts/
│       │   ├── WorkoutsScreen.kt       # Form fields + Save button
│       │   ├── WorkoutsViewModel.kt    # Exposes StateFlow<WorkoutFormUiState>; save()/loadForEdit()
│       │   └── WorkoutFormUiState.kt   # Form fields as text + inline validation errors
│       └── history/
│           ├── HistoryScreen.kt        # LazyColumn of WorkoutListItem
│           ├── HistoryViewModel.kt     # Exposes StateFlow<HistoryUiState>
│           └── HistoryUiState.kt       # sessions: List<WorkoutSession>, isLoading
│
└── util/
    └── DateFormatter.kt             # timestamp -> display string helper
```

## 3. Database Schema

### `WorkoutSession` entity

| Column       | Type      | Notes                              |
|--------------|-----------|-------------------------------------|
| `id`         | `Long`    | `@PrimaryKey(autoGenerate = true)`  |
| `exerciseName` | `String` |                                    |
| `sets`       | `Int`     |                                      |
| `reps`       | `Int`     |                                      |
| `weight`     | `Double`  | kg                                   |
| `timestamp`  | `Long`    | epoch millis, defaults to `System.currentTimeMillis()` |

### `WorkoutDao`

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insert(session: WorkoutSession): Long

@Update
suspend fun update(session: WorkoutSession)

@Delete
suspend fun delete(session: WorkoutSession)

@Query("DELETE FROM workout_sessions WHERE id = :id")
suspend fun deleteById(id: Long)

@Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
fun getAll(): Flow<List<WorkoutSession>>

@Query("SELECT * FROM workout_sessions WHERE id = :id")
suspend fun getById(id: Long): WorkoutSession?

@Query("SELECT COUNT(*) FROM workout_sessions WHERE timestamp >= :sinceEpochMillis")
fun getCountSince(sinceEpochMillis: Long): Flow<Int>
```

`getCountSince` backs the Dashboard's weekly-goal-ring computation.

### `FitTrackDatabase`

Single `@Database(entities = [WorkoutSession::class], version = 1)`, exposing `workoutDao()`, built via a thread-safe singleton `getInstance(context)`.

## 4. Step-by-Step Implementation Plan / Milestones

- [x] **Milestone 0 — Verify toolchain (flagged risk).** Resolved: this project relies on AGP 9's built-in/embedded Kotlin compilation, not the classic `org.jetbrains.kotlin.android` plugin. The root `build.gradle.kts` applies only `com.android.application`, `org.jetbrains.kotlin.plugin.compose` (Compose compiler plugin) and `com.google.devtools.ksp` — no separate Kotlin Android plugin is needed, and KSP (for Room) coexists with it cleanly.
- [x] **Milestone 1 — Compose setup.** Compose BOM, `activity-compose`, and `material3` are in `gradle/libs.versions.toml` and `app/build.gradle.kts`; `buildFeatures.compose = true` is set. `res/layout/activity_main.xml` was removed — `MainActivity` is a single Compose entry point (`setContent { FitTrackTheme { FitTrackNavHost(...) } }`). Verified with a clean build (`./gradlew clean compileDebugKotlin`) — builds successfully. *Not yet verified on a physical device/emulator (no `adb`/emulator available in this environment) — do that manually in Android Studio before considering this fully closed.*
- [x] **Milestone 2 — Room data layer.** `room-runtime`, `room-ktx`, `room-compiler` (via KSP) are wired in. `WorkoutSession`, `WorkoutDao`, `FitTrackDatabase`, and `WorkoutRepository` are fully implemented (not stubs). Verified: `kspDebugKotlin` and `compileDebugKotlin` both succeed in a clean build.
- [x] **Milestone 3 — Manual DI + ViewModel wiring.** `AppContainer`/`DefaultAppContainer`, `FitTrackApplication` (registered in the manifest), and `ViewModelFactory` are fully implemented and compile end-to-end. *Runtime instantiation on-device not yet manually verified in this session.*
- [x] **Milestone 4 — Navigation + Bottom Bar shell.** `navigation-compose` is wired in; `FitTrackNavHost` + `FitTrackNavGraph` are fully implemented with the three tabs and an edit-by-id Workouts route, including `popUpTo` + `launchSingleTop` + `restoreState`. *Tap-through behavior not yet manually verified on a device/emulator.*
- [x] **Milestone 5 — Workouts + History screens.** `WorkoutsViewModel` now has `onExerciseNameChange/onSetsChange/onRepsChange/onWeightChange`, `save()` (validates → insert or update), `loadForEdit(id)`, and `onSavedHandled()`. `WorkoutsScreen` wires all callbacks, shows inline validation errors via `supportingText`, and uses two `LaunchedEffect` blocks: one to pre-fill the form on edit, one to navigate away after save. `HistoryViewModel.init` collects `repository.allSessions` into state, and `delete()` calls `repository.deleteSession()` (Room's Flow re-emits the updated list automatically). `HistoryScreen` now passes `{ viewModel.delete(it) }` to `onDelete`. NavGraph passes proper `onSaved` lambdas: new-workout route navigates to History, edit route pops back. Build verified: `./gradlew compileDebugKotlin` succeeds. *Runtime smoke test (log → History → delete → edit) not yet done on device/emulator.*
- [ ] **Milestone 6 — Dashboard + animated Goal Ring.** Still stubbed: `GoalRing.kt`'s `Canvas` block is empty (just a `TODO`), and `DashboardViewModel` never updates `_uiState` from the repository. Implement the real `drawArc` background track + animated progress arc using `animateFloatAsState`, and compute `goalProgress` from `repository.countSince(startOfWeekEpochMillis)` against a weekly goal target.
- [ ] **Milestone 7 — Polish & theming.** Not started. Finalize `Color.kt`/`Theme.kt`/`Type.kt` (real light/dark M3 scheme, optional dynamic color on Android 12+). Add empty-state visuals, edge-to-edge insets handling, app icon/name. Final smoke test across all three tabs in light and dark mode, including persistence across process death.

### Current state

Milestones 0–4 are code-complete and compile cleanly (verified via `./gradlew clean compileDebugKotlin`, which runs `kspDebugKotlin` for Room's annotation processing too). That covers: the Gradle/Compose toolchain, the full Room data layer, manual DI, and the navigation shell with bottom bar. None of this has been exercised on a real device/emulator yet — only build-time compilation is confirmed.

Milestones 5–7 are the remaining work: `WorkoutsScreen`/`WorkoutsViewModel`, `HistoryScreen`'s delete action, the animated `GoalRing`, and `DashboardViewModel`'s stat computation are all still `TODO`/placeholder bodies. The app currently builds and launches, but logging a workout, deleting one, and the Dashboard ring's progress display won't do anything yet.
