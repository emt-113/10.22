package com.aizeek.phonepulse.service

import android.content.Context
import com.aizeek.phonepulse.data.AppDatabase
import com.aizeek.phonepulse.data.ScreenSession
import com.aizeek.phonepulse.util.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object ScreenStateHolder {
    /** Screen-off gaps shorter than this keep the "亮屏" session alive (continuous usage). */
    const val SHORT_OFF_GRACE_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private val _stateStartTime = MutableStateFlow(System.currentTimeMillis())
    val stateStartTime: StateFlow<Long> = _stateStartTime.asStateFlow()

    /** Non-null while a screen-off gap is inside the grace window (session suspended). */
    @Volatile private var suspendedOffAtMs: Long? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentDurationMs: StateFlow<Long> = stateStartTime.flatMapLatest { startTime ->
        flow {
            while (true) {
                emit((System.currentTimeMillis() - startTime).coerceAtLeast(0L))
                delay(1000)
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), 0L)

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _lastScreenOffDuration = MutableStateFlow<Long?>(null)
    val lastScreenOffDuration: StateFlow<Long?> = _lastScreenOffDuration.asStateFlow()

    private val _lastScreenOnDuration = MutableStateFlow<Long?>(null)
    val lastScreenOnDuration: StateFlow<Long?> = _lastScreenOnDuration.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    private val _currentDateKey = MutableStateFlow(TimeFormatter.todayKey())
    val currentDateKey: StateFlow<String> = _currentDateKey.asStateFlow()

    fun checkDateRollover() {
        val today = TimeFormatter.todayKey()
        if (_currentDateKey.value != today) {
            _currentDateKey.value = today
        }
    }

    fun refreshDateKey() {
        _currentDateKey.value = TimeFormatter.todayKey()
    }

    fun splitSessionByDay(session: ScreenSession): List<ScreenSession> {
        val startDateKey = TimeFormatter.dateKey(session.startTime)
        val endDateKey = TimeFormatter.dateKey(session.endTime)
        if (startDateKey == endDateKey) {
            return listOf(session)
        }

        val result = mutableListOf<ScreenSession>()
        var curStart = session.startTime
        while (curStart < session.endTime) {
            val curStartOfDay = TimeFormatter.getStartOfDay(curStart)
            val nextDayStart = curStartOfDay + 24 * 3600_000L
            val curEnd = minOf(session.endTime, nextDayStart)
            val dur = (curEnd - curStart).coerceAtLeast(0L)
            result.add(
                ScreenSession(
                    type = session.type,
                    startTime = curStart,
                    endTime = curEnd,
                    durationMs = dur,
                    dateKey = TimeFormatter.dateKey(curStart)
                )
            )
            curStart = nextDayStart
        }
        return result
    }

    /**
     * Called when screen state transitions (e.g. from SCREEN_OFF to SCREEN_ON or vice versa)
     *
     * Short screen-off gaps (< [SHORT_OFF_GRACE_MS]) suspend rather than close the
     * current "亮屏" session: the on-screen timer continues and the persisted session
     * excludes the gap, so both "continuous usage" and "real screen-on time" stay correct.
     */
    fun onScreenStateChanged(context: Context, newIsScreenOn: Boolean) {
        if (_isScreenOn.value == newIsScreenOn) return

        val now = System.currentTimeMillis()
        val previousStartTime = _stateStartTime.value

        if (!newIsScreenOn) {
            // SCREEN OFF: suspend the session (do not persist yet — grace may resume it).
            _isScreenOn.value = false
            suspendedOffAtMs = now
            _lastScreenOnDuration.value = (now - previousStartTime).coerceAtLeast(0L)
            return
        }

        // SCREEN ON
        val offAt = suspendedOffAtMs
        if (decideOnScreenOn(offAt, now, SHORT_OFF_GRACE_MS) == OnScreenOnDecision.RESUME_ABSORB) {
            // Absorb the short gap into the logical session start: timer continues.
            _stateStartTime.value = previousStartTime + (now - offAt!!)
            suspendedOffAtMs = null
            _isScreenOn.value = true
            refreshDateKey()
            return
        }

        // Long screen-off (or cold start): close the suspended session, then restart.
        if (offAt != null) {
            persistSession(context, "SCREEN_ON", previousStartTime, offAt)
            persistSession(context, "SCREEN_OFF", offAt, now)
            _lastScreenOffDuration.value = (now - offAt).coerceAtLeast(0L)
        }
        suspendedOffAtMs = null
        _isScreenOn.value = true
        _stateStartTime.value = now
        refreshDateKey()
    }

    internal enum class OnScreenOnDecision { RESUME_ABSORB, CLOSE_AND_RESTART }

    /** Pure decision for a screen-on event; unit-tested directly. */
    internal fun decideOnScreenOn(suspendedOffAtMs: Long?, nowMs: Long, graceMs: Long): OnScreenOnDecision =
        if (suspendedOffAtMs != null && nowMs - suspendedOffAtMs < graceMs) OnScreenOnDecision.RESUME_ABSORB
        else OnScreenOnDecision.CLOSE_AND_RESTART

    private fun persistSession(context: Context, type: String, start: Long, end: Long) {
        val durationMs = (end - start).coerceAtLeast(0L)
        val rawSession = ScreenSession(
            type = type,
            startTime = start,
            endTime = end,
            durationMs = durationMs,
            dateKey = TimeFormatter.dateKey(start)
        )
        val sessionsToInsert = splitSessionByDay(rawSession)
        scope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(context).screenSessionDao()
                dao.insertAll(sessionsToInsert)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Persists a suspended session whose grace window has expired (called from the
     * minute ticker so a session is never lost when the screen stays off).
     */
    fun flushSuspendedSessionIfExpired(context: Context) {
        val offAt = suspendedOffAtMs ?: return
        if (decideOnScreenOn(offAt, System.currentTimeMillis(), SHORT_OFF_GRACE_MS) == OnScreenOnDecision.RESUME_ABSORB) return
        val start = _stateStartTime.value
        persistSession(context, "SCREEN_ON", start, offAt)
        _lastScreenOnDuration.value = (offAt - start).coerceAtLeast(0L)
        suspendedOffAtMs = null
    }

    /**
     * Initializes state from DB and preferences
     */
    fun initialize(context: Context) {
        refreshDateKey()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val isScreenCurrentlyOn = powerManager?.isInteractive ?: true
        _isScreenOn.value = isScreenCurrentlyOn
        _stateStartTime.value = System.currentTimeMillis()

        scope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(context).screenSessionDao()
                val lastOff = dao.getLastSessionByTypeSync("SCREEN_OFF")
                val lastOn = dao.getLastSessionByTypeSync("SCREEN_ON")
                if (lastOff != null) {
                    _lastScreenOffDuration.value = lastOff.durationMs
                }
                if (lastOn != null) {
                    _lastScreenOnDuration.value = lastOn.durationMs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Synchronizes current screen state when service starts or receives system callbacks
     */
    fun syncScreenState(isCurrentlyOn: Boolean) {
        checkDateRollover()
        if (_isScreenOn.value != isCurrentlyOn) {
            _isScreenOn.value = isCurrentlyOn
            _stateStartTime.value = System.currentTimeMillis()
        }
    }

    /**
     * Allows manual simulation of state change for preview/testing
     */
    fun simulateStateToggle(context: Context) {
        onScreenStateChanged(context, !_isScreenOn.value)
    }
}
