# FitTrack — Android Fitness Tracker
### Final Exam Project Presentation
**Jetpack Compose · Room · MVVM · Material 3**

---

## Slide 1 — What is FitTrack?

FitTrack is a **fully offline** Android fitness tracker.

Three screens behind a **Bottom Navigation Bar**:

| Tab | What it does |
|---|---|
| **Dashboard** | Animated ring showing weekly workout goal progress |
| **Workouts** | Form to log a new session (or edit an existing one) |
| **History** | Scrollable list of all past sessions, with delete & edit |

**No backend. No login. No internet.** Everything lives in a local SQLite database managed by Room.

---

## Slide 2 — Tech Stack

```
Language       Kotlin
UI             Jetpack Compose (no XML layouts!)
Design system  Material 3
Database       Room (SQLite wrapper)
State          StateFlow + ViewModel
Navigation     Navigation Compose
Architecture   MVVM (Model–View–ViewModel)
DI             Manual AppContainer (no Hilt)
```

**Why Jetpack Compose?**
- Declarative UI — describe *what* to show, not *how* to draw it
- No `findViewById`, no `ViewHolder`, no XML
- One Activity, multiple Composable screens
- Animations are first-class citizens

---

## Slide 3 — App Architecture (Layers)

```
┌─────────────────────────────────────────────┐
│               UI Layer                       │
│   Composable Screens + ViewModels            │
│   Dashboard / Workouts / History             │
└──────────────────┬──────────────────────────┘
                   │ StateFlow (observed by UI)
┌──────────────────▼──────────────────────────┐
│            Repository Layer                  │
│   WorkoutRepository                         │
│   - single source of truth for data         │
└──────────────────┬──────────────────────────┘
                   │ suspend functions + Flow
┌──────────────────▼──────────────────────────┐
│             Data Layer (Room)                │
│   WorkoutDao → FitTrackDatabase              │
│   WorkoutSession (SQLite table)              │
└─────────────────────────────────────────────┘
```

Data flows **upward** through Flows.  
Events flow **downward** through suspend function calls.

---

## Slide 4 — Database Entity

The core data model. Room maps this Kotlin class directly to a SQLite table.

```kotlin
// WorkoutSession.kt
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val timestamp: Long = System.currentTimeMillis()
)
```

**Key points to explain:**
- `@Entity` → becomes a SQLite table
- `@PrimaryKey(autoGenerate = true)` → Room assigns a unique ID on insert
- `timestamp` defaults to *now* — no need to pass it manually
- `data class` → gives us `equals()`, `copy()`, `toString()` for free

---

## Slide 5 — Data Access Object (DAO)

The DAO is an interface. Room auto-generates the real implementation at compile time.

```kotlin
// WorkoutDao.kt
@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Delete
    suspend fun delete(session: WorkoutSession)

    // Flow — emits a new list every time the table changes
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<WorkoutSession>>

    // Used by the Dashboard ring — counts workouts since Monday
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE timestamp >= :sinceEpochMillis")
    fun getCountSince(sinceEpochMillis: Long): Flow<Int>
}
```

**Why `Flow` instead of `suspend fun`?**
- `suspend fun` returns once and is done
- `Flow` keeps emitting: whenever a session is added/deleted, the UI automatically updates — no manual refresh needed

---

## Slide 6 — Manual Dependency Injection

Instead of Hilt/Dagger, we use a simple manual container.

```kotlin
// AppContainer.kt
interface AppContainer {
    val workoutRepository: WorkoutRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    // 'by lazy' means the database is created only when first accessed
    private val database: FitTrackDatabase by lazy {
        FitTrackDatabase.getInstance(context)
    }

    override val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao())
    }
}
```

```kotlin
// FitTrackApplication.kt
class FitTrackApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)  // one instance for the whole app
    }
}
```

**One database. One repository. One source of truth.**

---

## Slide 7 — MVVM: ViewModel + StateFlow

The ViewModel holds UI state and survives screen rotations.

```kotlin
// DashboardViewModel.kt (simplified)
class DashboardViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // combine() merges two Flows: re-runs whenever either emits
            combine(
                repository.countSince(startOfWeekEpochMillis()),
                repository.allSessions
            ) { weekCount, allSessions ->
                DashboardUiState(
                    sessionsThisWeek = weekCount,
                    goalProgress = (weekCount.toFloat() / 5).coerceIn(0f, 1f),
                    totalSessions = allSessions.size,
                    currentStreak = computeStreak(allSessions)
                )
            }.collect { _uiState.value = it }
        }
    }
}
```

**The pattern:**
1. Repository emits new data via Flow
2. ViewModel reacts and rebuilds `UiState`
3. UI observes `uiState` and recomposes automatically

---

## Slide 8 — The "Wow" Feature: Animated Goal Ring

The Dashboard ring is drawn with Compose's `Canvas` API and animated with `animateFloatAsState`.

```kotlin
// GoalRing.kt
@Composable
fun GoalRing(progress: Float, modifier: Modifier = Modifier) {

    // Smoothly animates from old progress to new over 900ms
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "goalRingProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 24.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            // Grey background ring (always visible)
            drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

            // Colored progress arc (starts at 12 o'clock = 270°)
            if (animatedProgress > 0f) {
                drawArc(color = progressColor, startAngle = 270f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
        }
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
    }
}
```

**Why `startAngle = 270°`?** Android places 0° at the 3-o'clock position. 270° is 12 o'clock — where a progress ring naturally starts.

---

## Slide 9 — Workouts Screen (Form + Validation)

```kotlin
// WorkoutsScreen.kt (key parts)
@Composable
fun WorkoutsScreen(viewModelFactory: ViewModelFactory, editingId: Long? = null, onSaved: () -> Unit = {}) {
    val viewModel: WorkoutsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pre-fill form if editing an existing session
    LaunchedEffect(editingId) { viewModel.loadForEdit(editingId) }

    // Navigate away automatically when save completes
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) { viewModel.onSavedHandled(); onSaved() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.exerciseName,
            onValueChange = { viewModel.onExerciseNameChange(it) },
            label = { Text("Exercise name") },
            isError = uiState.exerciseNameError != null,
            supportingText = uiState.exerciseNameError?.let { error -> { Text(error) } },
            modifier = Modifier.fillMaxWidth()
        )
        // ... Sets, Reps, Weight fields follow the same pattern

        Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.editingId != null) "Update workout" else "Save workout")
        }
    }
}
```

**`LaunchedEffect`** — a side-effect block that runs when its key changes. Used here to:
- Load an existing session for editing (key = `editingId`)
- Navigate away after save (key = `isSaved`)

---

## Slide 10 — Navigation

```kotlin
// FitTrackNavHost.kt
@Composable
fun FitTrackNavHost(viewModelFactory: ViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true   // don't stack duplicate tabs
                                restoreState = true      // restore scroll/form state
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { ... }
}
```

**Navigation routes:**
- `"dashboard"` — Dashboard tab
- `"workouts"` — New workout form
- `"workouts/{id}"` — Edit existing session (passes ID via route argument)
- `"history"` — History list

---

## Slide 11 — Material 3 Theming (Light + Dark)

```kotlin
// Theme.kt
@Composable
fun FitTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        // Android 12+ can extract colors from the user's wallpaper
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else           dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColors   // our hand-picked dark palette
        else      -> LightColors  // our hand-picked light palette
    }

    MaterialTheme(colorScheme = colorScheme, typography = FitTrackTypography, content = content)
}
```

Custom brand palette in `Color.kt`:
```kotlin
val FitTrackPrimary   = Color(0xFF3D5AFE)   // Indigo blue
val FitTrackSecondary = Color(0xFF00BFA5)   // Teal
val FitTrackTertiary  = Color(0xFFFF6D00)   // Orange accent
```

**Three color sources, in priority order:**
1. Dynamic color (Android 12+, wallpaper-based)
2. Our dark palette
3. Our light palette

---

## Slide 12 — Key Design Decisions

| Decision | Why |
|---|---|
| **Jetpack Compose over XML** | Less boilerplate, animations are built-in, fully declarative |
| **Room over raw SQLite** | Compile-time SQL validation, auto-generated boilerplate, Flow integration |
| **StateFlow over LiveData** | Kotlin-native, works with coroutines, supports `combine()` |
| **Manual DI over Hilt** | Simpler to understand, no annotation processing overhead for a small app |
| **`Flow` for all queries** | UI automatically reacts to data changes — no manual refresh |
| **`LaunchedEffect` for navigation** | Navigation after save is a side-effect, not UI state |

---

## Slide 13 — Streak Algorithm

One of the more interesting pieces of logic:

```kotlin
// DashboardViewModel.kt
private fun computeStreak(sessions: List<WorkoutSession>): Int {
    if (sessions.isEmpty()) return 0

    // Convert each session to a unique "day key" (year*1000 + dayOfYear)
    val sessionDays = sessions.map { dayKey(it.timestamp) }.toSet()

    // Start from today; if nothing logged today yet, start from yesterday
    val todayKey = dayKey(Calendar.getInstance().timeInMillis)
    val startCal = Calendar.getInstance().apply {
        if (todayKey !in sessionDays) add(Calendar.DAY_OF_YEAR, -1)
    }

    // Count consecutive days backwards
    var streak = 0
    val cursor = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
    while (dayKey(cursor.timeInMillis) in sessionDays) {
        streak++
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun dayKey(epochMillis: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
}
```

**The trick:** convert timestamps to integer "day keys" → fast `Set` lookup → walk backwards until a gap is found.

---

## Slide 14 — History Screen (Reactive List)

```kotlin
// HistoryScreen.kt
@Composable
fun HistoryScreen(viewModelFactory: ViewModelFactory, onEditSession: (Long) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading  -> Unit          // avoid "empty" flash before first DB read
        uiState.sessions.isEmpty() ->
            EmptyState(message = "No workouts logged yet",
                       subtitle = "Head to Workouts to log your first session")
        else ->
            LazyColumn {
                items(uiState.sessions, key = { it.id }) { session ->
                    WorkoutListItem(
                        session = session,
                        onClick = { onEditSession(it.id) },  // navigates to edit form
                        onDelete = { viewModel.delete(it) }
                    )
                }
            }
    }
}
```

**`key = { it.id }`** — tells Compose which items are the same between recompositions, enabling smooth insert/delete animations without re-drawing the whole list.

---

## Slide 15 — App Screen Mockups

```
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│    DASHBOARD         │   │    WORKOUTS           │   │    HISTORY           │
│                      │   │                      │   │                      │
│   Weekly Goal        │   │  ┌────────────────┐  │   │ ┌──────────────────┐ │
│                      │   │  │ Exercise name  │  │   │ │ Bench Press      │ │
│      ╭───────╮       │   │  └────────────────┘  │   │ │ 3×10 · 80kg     │ │
│    ╭─╯  60%  ╰─╮     │   │  ┌────────┐ ┌─────┐  │   │ │ 2 days ago   🗑 │ │
│    │            │     │   │  │  Sets  │ │Reps │  │   │ └──────────────────┘ │
│    ╰────────────╯     │   │  └────────┘ └─────┘  │   │ ┌──────────────────┐ │
│                      │   │  ┌────────────────┐  │   │ │ Squat            │ │
│  3 / 5 workouts      │   │  │  Weight (kg)   │  │   │ │ 4×8 · 100kg     │ │
│  this week           │   │  └────────────────┘  │   │ │ 3 days ago   🗑 │ │
│                      │   │  ┌────────────────┐  │   │ └──────────────────┘ │
│ [Total: 18] [5d 🔥]  │   │  │  Save workout  │  │   │                      │
│                      │   │  └────────────────┘  │   │   (scrollable...)    │
├──────────────────────┤   ├──────────────────────┤   ├──────────────────────┤
│  🏠 Dashboard        │   │  🏠 Dashboard        │   │  🏠 Dashboard        │
│  ➕ Workouts         │   │  ➕ Workouts         │   │  ➕ Workouts         │
│  📋 History          │   │  📋 History          │   │  📋 History          │
└──────────────────────┘   └──────────────────────┘   └──────────────────────┘
```

> **Tip for presenter:** Replace these mockups with actual screenshots from the running app on your device.

---

## Slide 16 — Project Stats

```
Total Kotlin files:    ~20 files
Lines of code:         ~800 lines
Database tables:       1  (workout_sessions)
Composable screens:    3  (Dashboard, Workouts, History)
Reusable components:   4  (GoalRing, StatCard, WorkoutListItem, EmptyState)
ViewModels:            3  (one per screen)
```

**Milestones completed:**
- [x] M0 — Gradle / toolchain setup (AGP 9 + KSP)
- [x] M1 — Jetpack Compose entry point (no XML)
- [x] M2 — Room data layer (Entity, DAO, Database)
- [x] M3 — Manual DI + ViewModel wiring
- [x] M4 — Navigation + Bottom Bar
- [x] M5 — Workouts form + History list with delete/edit
- [x] M6 — Animated Dashboard Goal Ring
- [x] M7 — Material 3 theme, light/dark, dynamic color

---

## Slide 17 — What I Learned

1. **Declarative UI is different** — instead of imperatively setting text/colors, you describe what the screen *should look like* given the current state

2. **`Flow` makes reactivity automatic** — Room emits changes, ViewModels transform them, screens just render — no polling, no manual refresh

3. **`animateFloatAsState` makes animation trivial** — one line to animate any numeric value smoothly

4. **`Canvas` gives you full drawing control** — the Goal Ring is pure geometry: `drawArc()` with correct angle offsets

5. **MVVM separates concerns cleanly** — the ViewModel knows nothing about Compose; the screen knows nothing about Room

6. **Manual DI is enough for small apps** — `by lazy` in `AppContainer` gives you a singleton without any framework

---

## Slide 18 — Live Demo Checklist

1. Open app → **Dashboard** shows ring at 0% initially
2. Tap **Workouts** → fill in exercise name, sets, reps, weight → tap **Save**
3. App navigates to **History** → new entry appears instantly (reactive Flow!)
4. Go back to **Dashboard** → ring animates to new progress (e.g. 20% if goal = 5)
5. In **History** → tap a row → **Workouts** form opens pre-filled → change weight → **Update**
6. Swipe or tap delete → entry disappears, Dashboard updates immediately
7. Enable **dark mode** in system settings → app theme switches automatically

---

*FitTrack · Final Exam Project · Built with Kotlin + Jetpack Compose*
