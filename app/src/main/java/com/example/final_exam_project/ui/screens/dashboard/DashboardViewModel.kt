package com.example.final_exam_project.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.final_exam_project.data.local.WorkoutSession
import com.example.final_exam_project.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // How many workouts per week the user is aiming for.
    private val weeklyGoalTarget = 5

    init {
        viewModelScope.launch {
            // combine() merges two Flows into one: whenever either emits (e.g. after
            // the user logs or deletes a workout), this block runs and rebuilds the
            // whole DashboardUiState from the latest values of both flows.
            combine(
                repository.countSince(startOfWeekEpochMillis()),
                repository.allSessions
            ) { weekCount, allSessions ->
                DashboardUiState(
                    sessionsThisWeek = weekCount,
                    weeklyGoalTarget = weeklyGoalTarget,
                    // coerceIn clamps to [0, 1] so the ring never overflows past 100%.
                    goalProgress = (weekCount.toFloat() / weeklyGoalTarget).coerceIn(0f, 1f),
                    totalSessions = allSessions.size,
                    currentStreak = computeStreak(allSessions)
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // Returns the epoch millis for midnight on Monday of the current week.
    // Using explicit arithmetic instead of Calendar.DAY_OF_WEEK assignment avoids
    // locale-specific first-day-of-week differences (some locales start on Sunday).
    private fun startOfWeekEpochMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Calendar.MONDAY = 2, Calendar.SUNDAY = 1
        // This formula always gives days since the most recent Monday (0–6).
        val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return cal.timeInMillis
    }

    // Counts consecutive days ending today (or yesterday, if the user hasn't
    // logged yet today) that each have at least one session.
    private fun computeStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0

        // Reduce each session's timestamp to a single integer "day key"
        // (year × 1000 + dayOfYear) so we can check membership in O(1).
        val sessionDays = sessions.map { dayKey(it.timestamp) }.toSet()

        // If the user hasn't logged today yet, start counting from yesterday so
        // the streak isn't immediately broken at midnight.
        val todayKey = dayKey(Calendar.getInstance().timeInMillis)
        val startCal = Calendar.getInstance().apply {
            if (todayKey !in sessionDays) add(Calendar.DAY_OF_YEAR, -1)
        }

        // If neither today nor yesterday has a session, streak is 0.
        if (dayKey(startCal.timeInMillis) !in sessionDays) return 0

        var streak = 0
        val cursor = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
        while (dayKey(cursor.timeInMillis) in sessionDays) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    // Converts an epoch-millis timestamp to a unique integer for its calendar day.
    private fun dayKey(epochMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
}
