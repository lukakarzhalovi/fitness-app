# FitTrack — Jetpack Compose Fitness Tracker

A 100% Jetpack Compose (Material 3), pure-MVVM fitness tracker. No XML layouts, no `findViewById` — a single Compose Activity hosts a Bottom-Navigation-driven app with Dashboard, Workouts, and History tabs, backed by a local Room database.

---

## Requirements Compliance

### UI Technology
- **Pure Jetpack Compose** — zero XML layout files, zero `findViewById` calls.
- `MainActivity` is a single `ComponentActivity` that calls `setContent { FitTrackTheme { FitTrackNavHost(...) } }`.

### 1. Menu Implementation
Bottom Navigation Bar with three tabs, implemented with Material 3 `NavigationBar` / `NavigationBarItem`.

- **File:** `navigation/FitTrackNavHost.kt` — `Scaffold` with `bottomBar { NavigationBar { … } }`
- **Tabs:** Dashboard · Workouts · History (defined as a sealed class in `navigation/BottomNavItem.kt`)
- Tab state is preserved across switches via `saveState = true` / `restoreState = true`.

### 2. List Component
The History screen displays all logged workout sessions in a `LazyColumn`.

- **File:** `ui/screens/history/HistoryScreen.kt:39` — `LazyColumn { items(uiState.sessions, key = { it.id }) { … } }`
- Each row is a reusable `WorkoutListItem` composable with delete and tap-to-edit actions.

### 3. MVVM Architecture
Clean three-layer separation with no business logic in the UI layer:

| Layer | What lives here |
|---|---|
| **View** | Composable screens — observe `StateFlow` via `collectAsStateWithLifecycle()`, call ViewModel functions |
| **ViewModel** | `DashboardViewModel`, `WorkoutsViewModel`, `HistoryViewModel` — extend `ViewModel`, expose immutable `StateFlow<UiState>`, run coroutines in `viewModelScope` |
| **Repository / Data** | `WorkoutRepository` wraps `WorkoutDao`; Room `Flow` queries propagate changes reactively upward |

ViewModels are constructed with a manual `ViewModelFactory` (`di/ViewModelFactory.kt`) fed by an `AppContainer` (`di/AppContainer.kt`) — no Hilt, keeping the dependency graph explicit and simple.

### 4. Database — Room (Local Storage)
Full Room stack, no network dependency:

- `WorkoutSession` — `@Entity` with `id`, `exerciseName`, `sets`, `reps`, `weight`, `timestamp`
- `WorkoutDao` — `@Dao` with suspend CRUD functions and two `Flow`-backed queries (`getAll`, `getCountSince`)
- `FitTrackDatabase` — `@Database` singleton with thread-safe double-checked locking
- `WorkoutRepository` — thin wrapper; exposes `allSessions: Flow<List<WorkoutSession>>` and `countSince(epochMillis): Flow<Int>`

Every INSERT / UPDATE / DELETE automatically re-emits on the open `Flow`s, so both the History list and Dashboard stats update in real time without manual refresh calls.

### 5. Novel Feature — Animated Goal Ring
The Dashboard shows a custom, hand-drawn circular progress ring built with the low-level Compose `Canvas` API:

- **File:** `ui/components/GoalRing.kt`
- Drawn with `Canvas { drawArc(…) }` — two arcs: a grey background track (360°) and a coloured progress arc that starts at 270° (12-o'clock) and sweeps proportionally.
- Animated with `animateFloatAsState(targetValue = progress, animationSpec = tween(900ms, FastOutSlowInEasing))` — the arc sweeps smoothly whenever the workout count changes.
- `DashboardViewModel` feeds it by using `combine()` on two concurrent Room `Flow`s (`countSince` + `allSessions`) to compute weekly goal progress and a consecutive-day workout streak in real time.

---

## App Concept

FitTrack is a fully offline, single-user fitness tracker with three screens:

- **Dashboard** — animated Goal Ring showing progress toward a 5-workouts-per-week goal, plus total sessions and current streak stat cards.
- **Workouts** — form to log a new session (exercise name, sets, reps, weight in kg) with inline validation; also doubles as the edit form when tapping a History row.
- **History** — `LazyColumn` of all past sessions (most recent first), each with delete and tap-to-edit actions.

---

## Tech Stack

| Category | Library / Tool |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (`navigation-compose`) |
| State | `StateFlow` / `MutableStateFlow` + `collectAsStateWithLifecycle` |
| Database | Room (`room-runtime`, `room-ktx`, KSP annotation processor) |
| DI | Manual (`AppContainer` + `ViewModelFactory`) |
| Build | AGP 9, KSP, Compose compiler plugin |

---

## Package Structure

```
com.example.final_exam_project/
├── FitTrackApplication.kt          # Application subclass; owns the single AppContainer instance
├── MainActivity.kt                 # ComponentActivity; setContent { FitTrackTheme { FitTrackNavHost(...) } }
│
├── data/
│   ├── local/
│   │   ├── WorkoutSession.kt       # @Entity
│   │   ├── WorkoutDao.kt           # @Dao: insert/update/delete/getAll(Flow)/getById/getCountSince(Flow)
│   │   └── FitTrackDatabase.kt     # @Database singleton
│   └── repository/
│       └── WorkoutRepository.kt    # Wraps WorkoutDao; exposes Flow + suspend CRUD
│
├── di/
│   ├── AppContainer.kt             # Interface + DefaultAppContainer: builds DB → Repository
│   └── ViewModelFactory.kt         # ViewModelProvider.Factory; injects repository into each ViewModel
│
├── navigation/
│   ├── BottomNavItem.kt            # Sealed class: Dashboard, Workouts, History
│   ├── NavGraph.kt                 # NavHost with all routes including edit-by-id
│   └── FitTrackNavHost.kt          # Scaffold + NavigationBar + NavGraph
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # Material 3 color tokens (light + dark)
│   │   ├── Theme.kt                # FitTrackTheme composable
│   │   └── Type.kt                 # Typography scale
│   ├── components/
│   │   ├── GoalRing.kt             # Canvas-based animated ring (novel feature)
│   │   ├── StatCard.kt             # Reusable summary stat card
│   │   ├── WorkoutListItem.kt      # Single row for the History LazyColumn
│   │   └── EmptyState.kt           # "No data yet" placeholder
│   └── screens/
│       ├── dashboard/
│       │   ├── DashboardScreen.kt      # Renders GoalRing + StatCards
│       │   ├── DashboardViewModel.kt   # combine(countSince, allSessions) → StateFlow<DashboardUiState>
│       │   └── DashboardUiState.kt     # goalProgress, sessionsThisWeek, weeklyGoalTarget, totalSessions, streak
│       ├── workouts/
│       │   ├── WorkoutsScreen.kt       # Form fields + Save/Update button
│       │   ├── WorkoutsViewModel.kt    # save() + loadForEdit(id) → StateFlow<WorkoutFormUiState>
│       │   └── WorkoutFormUiState.kt   # Form field strings + inline validation error messages
│       └── history/
│           ├── HistoryScreen.kt        # LazyColumn of WorkoutListItem
│           ├── HistoryViewModel.kt     # Collects allSessions Flow → StateFlow<HistoryUiState>
│           └── HistoryUiState.kt       # sessions: List<WorkoutSession>, isLoading: Boolean
│
└── util/
    └── DateFormatter.kt             # epoch millis → human-readable string
```

---

## Database Schema

### `WorkoutSession` entity

| Column | Type | Notes |
|---|---|---|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` |
| `exerciseName` | `String` | |
| `sets` | `Int` | |
| `reps` | `Int` | |
| `weight` | `Double` | kg |
| `timestamp` | `Long` | epoch millis, defaults to `System.currentTimeMillis()` |

### Key DAO queries

```kotlin
@Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
fun getAll(): Flow<List<WorkoutSession>>

@Query("SELECT COUNT(*) FROM workout_sessions WHERE timestamp >= :sinceEpochMillis")
fun getCountSince(sinceEpochMillis: Long): Flow<Int>
```

Both are `Flow`-backed — Room re-emits automatically on any data change.

---

## Milestones

- [x] **Milestone 0** — Toolchain verified. AGP 9 with embedded Kotlin compilation; `org.jetbrains.kotlin.plugin.compose` Compose compiler plugin; KSP for Room annotation processing.
- [x] **Milestone 1** — Compose setup. BOM, `activity-compose`, `material3` wired in; `buildFeatures.compose = true`; `activity_main.xml` removed; single-Activity Compose entry point confirmed.
- [x] **Milestone 2** — Room data layer. `WorkoutSession`, `WorkoutDao`, `FitTrackDatabase`, `WorkoutRepository` fully implemented; `kspDebugKotlin` + `compileDebugKotlin` pass.
- [x] **Milestone 3** — Manual DI + ViewModel wiring. `AppContainer`, `DefaultAppContainer`, `FitTrackApplication`, and `ViewModelFactory` complete.
- [x] **Milestone 4** — Navigation + Bottom Bar. `FitTrackNavHost` + `FitTrackNavGraph` with three tabs and edit-by-id route; `popUpTo` + `launchSingleTop` + `restoreState` configured.
- [x] **Milestone 5** — Workouts + History screens. Full form validation, inline errors, edit pre-fill (`loadForEdit`), post-save navigation (`LaunchedEffect`), delete with automatic Room Flow re-emission.
- [x] **Milestone 6** — Dashboard + animated Goal Ring. `GoalRing.kt` draws two `drawArc` layers with `StrokeCap.Round`; animated via `animateFloatAsState` (900ms, `FastOutSlowInEasing`). `DashboardViewModel` uses `combine()` on two Room Flows to compute `goalProgress`, `totalSessions`, and `currentStreak`.
- [x] **Milestone 7** — Polish & theming. Material 3 light/dark color scheme in `Color.kt`/`Theme.kt`; edge-to-edge insets via `WindowInsets.safeDrawing`; empty-state composable; `EmptyState` shown in History before first workout.
