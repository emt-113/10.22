package com.aizeek.phonepulse.intervention

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aizeek.phonepulse.MainActivity
import com.aizeek.phonepulse.R
import com.aizeek.phonepulse.data.UsageStatsRepository
import com.aizeek.phonepulse.util.TimeFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User-adjustable coach parameters, persisted in SharedPreferences so the feature
 * needs no schema migration and no new dependencies. Every knob is runtime-tunable
 * for demo purposes (small budget/limit values trigger alerts within minutes).
 */
class UsageCoachSettings(context: Context) {
    data class CoachPrefs(
        val enabled: Boolean = false,
        /** Daily screen-on budget in minutes; null = budget alerts off. */
        val budgetMinutes: Int? = null,
        /** Continuous usage limit in minutes; null = continuous alerts off. */
        val continuousLimitMinutes: Int? = null,
        /** Screen-off grace window in seconds (< grace merges into the session). */
        val graceSeconds: Int = 30
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_coach", Context.MODE_PRIVATE)

    @Volatile var current: CoachPrefs = read()
        private set

    val graceMs: Long get() = current.graceSeconds.coerceIn(5, 300) * 1000L

    /**
     * Reload from SharedPreferences. Required because the UI card and the service
     * controller hold separate instances of this class; without it the controller
     * never sees newly saved settings.
     */
    fun refresh() {
        current = read()
    }

    private fun read(): CoachPrefs = CoachPrefs(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        budgetMinutes = prefs.getInt(KEY_BUDGET, -1).takeIf { it > 0 },
        continuousLimitMinutes = prefs.getInt(KEY_CONTINUOUS, -1).takeIf { it > 0 },
        graceSeconds = prefs.getInt(KEY_GRACE, 30).coerceIn(5, 300)
    )

    fun save(enabled: Boolean, budgetMinutes: Int?, continuousLimitMinutes: Int?, graceSeconds: Int) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_BUDGET, budgetMinutes ?: -1)
            .putInt(KEY_CONTINUOUS, continuousLimitMinutes ?: -1)
            .putInt(KEY_GRACE, graceSeconds.coerceIn(5, 300))
            .apply()
        current = read()
        changeCounter.value += 1
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BUDGET = "budget_minutes"
        private const val KEY_CONTINUOUS = "continuous_limit_minutes"
        private const val KEY_GRACE = "grace_seconds"

        // Cross-instance change signal: the UI card and the service controller hold
        // separate instances of this class, so a save reaches the controller through
        // this shared flow — it reacts within the reaction delay instead of waiting
        // for the next heartbeat.
        private val changeCounter = MutableStateFlow(0)
        val changes: StateFlow<Int> = changeCounter
    }
}

/** Posts coach alerts on a dedicated channel; silently no-ops without permission. */
class UsageCoachNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            // Clean up superseded channel ids from earlier iterations (v1/v2/v3).
            manager?.deleteNotificationChannel("usage_coach_channel")
            manager?.deleteNotificationChannel("usage_coach_channel_v2")
            manager?.deleteNotificationChannel("usage_coach_channel_v3")
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.coach_channel_name),
                // HIGH: reminders must pop as heads-up banners with sound, like messages.
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.coach_channel_desc)
                setShowBadge(false)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                enableVibration(true)
                enableLights(true)
                // Coach reminders are user-requested: bypass DND so they still land.
                setBypassDnd(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun canNotify(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun show(alert: UsageCoachEngine.CoachAlert) {
        if (!canNotify()) return
        android.util.Log.i("UsageCoach", "show alert type=${alert.type} key=${alert.cooldownKey}")
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, alert.type.ordinal, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            // Belt and braces: some OEM builds (MIUI) gate heads-up + sound at the
            // app level, so also set sound/priority on the notification itself.
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_ID + alert.type.ordinal, notification)
    }

    companion object {
        // v3: v2 used IMPORTANCE_DEFAULT which some OEM builds (MIUI) deliver
        // silently without heads-up. v3 is IMPORTANCE_HIGH so reminders pop with sound.
        // v4: localized channel name (Chinese) — channel names are baked at creation,
        // so a new id is required for the rename to reach existing installs.
        const val CHANNEL_ID = "usage_coach_channel_v4"
        const val NOTIFICATION_BASE_ID = 2001
    }
}

/**
 * Glue: owns the day state (tracker, accumulated minutes, fired keys), feeds from
 * screen broadcasts, evaluates rules on a heartbeat, and posts notifications.
 * The service only calls [feedScreen], [start], and [stop] — everything else lives
 * here so the engine stays testable and the service hook stays minimal.
 */
class UsageCoachController(
    private val context: Context,
    private val settings: UsageCoachSettings,
    private val notifier: UsageCoachNotifier,
    // Injectable so tests keep the backfill hop inside virtual time.
    private val backfillDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val dayStore = PlanetDayStore(context)
    // The service heartbeats on Dispatchers.Default while screen broadcasts arrive
    // on the main thread; every mutation of coach state is serialized on [lock].
    private val lock = Any()
    private var tracker = newTracker()
    private var dayKey: String = todayKey()
    private var accumulatedMs: Long = readAccumulated() * 60_000L
    private var accumulatedMinutes: Int = readAccumulated()
    private var lastBeatElapsed: Long? = null
    private var firedKeys: MutableSet<String> = readFired()
    private var sessionAlertedKeys: MutableSet<String> = mutableSetOf()
    private var lastAlertedSessionStart: Long? = null
    private var heartbeatJob: Job? = null
    private var reactionJob: Job? = null
    private var lastScreenOn: Boolean = isScreenOnNow()

    private fun newTracker() = ContinuousSessionTracker(settings.graceMs)

    fun start(scope: CoroutineScope) {
        notifier.ensureChannel()
        // Seed the tracker with the current screen state: a service that starts
        // while the screen is already on must begin the session immediately,
        // otherwise continuous-usage reminders never fire until a broadcast cycle.
        val screenOn = isScreenOnNow()
        if (screenOn) tracker.onScreenOn(SystemClockElapsed())
        lastScreenOn = screenOn
        heartbeatJob = scope.launch {
            settings.refresh()
            synchronized(lock) { rollDayIfNeeded() }
            // First open / process restart: adopt the system's authoritative
            // screen-on total for today and evaluate immediately, so the home
            // card never sits on a stale "0 分钟 / 未加载" for the first minute.
            backfillFromSystemStats()
            synchronized(lock) {
                feedInternal(isScreenOnNow(), SystemClockElapsed())
                evaluate()
            }
            while (isActive) {
                delay(HEARTBEAT_MS)
                // Refresh BEFORE evaluating: the UI card saves into
                // SharedPreferences from a different instance of this class.
                settings.refresh()
                synchronized(lock) {
                    rollDayIfNeeded()
                    feedInternal(isScreenOnNow(), SystemClockElapsed())
                    evaluate()
                }
            }
        }
        // Instant reaction path (no heartbeat wait): a UI-card save is debounced to
        // coalesce per-keystroke writes into one evaluation, and UI resume ticks go
        // straight through. Evaluating even when disabled keeps the home hero
        // mirroring the current settings instead of freezing on the last snapshot.
        reactionJob = scope.launch {
            merge(UsageCoachSettings.changes.drop(1), refreshTick.drop(1)).collectLatest {
                delay(REACTION_DELAY_MS)
                settings.refresh()
                synchronized(lock) {
                    rollDayIfNeeded()
                    evaluate()
                }
            }
        }
    }

    /**
     * Adopt the system's interactive-time total for today when it is ahead of our
     * own accumulation (fresh install, process death, OEM-killed service). The
     * system value is ground truth and already spans up to "now", so adopting it
     * and resuming delta accumulation from here never double-counts.
     */
    private suspend fun backfillFromSystemStats() {
        val sysMinutes = withContext(backfillDispatcher) { systemScreenOnMinutesToday() }
            ?: return
        synchronized(lock) {
            if (sysMinutes > accumulatedMinutes) {
                accumulatedMs = sysMinutes * 60_000L
                accumulatedMinutes = sysMinutes
                saveAccumulated()
            }
        }
    }

    private fun systemScreenOnMinutesToday(): Int? {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            if (!UsageStatsRepository(context).hasUsageStatsPermission()) return null
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as?
                android.app.usage.UsageStatsManager ?: return null
            val now = System.currentTimeMillis()
            val start = TimeFormatter.getStartOfDay(now)
            val events = usm.queryEvents(start, now) ?: return null
            val event = android.app.usage.UsageEvents.Event()
            var onMs = 0L
            var lastOnMs = -1L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> lastOnMs = event.timeStamp
                    android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        if (lastOnMs > 0) onMs += (event.timeStamp - lastOnMs).coerceAtLeast(0)
                        lastOnMs = -1L
                    }
                }
            }
            if (lastOnMs > 0) onMs += (now - lastOnMs).coerceAtLeast(0)
            ((onMs / 60_000L).toInt()).takeIf { it > 0 }
        } catch (t: Throwable) {
            null
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        reactionJob?.cancel()
        reactionJob = null
    }

    /** Feed a screen transition; timestamps use the monotonic clock. */
    fun feedScreen(screenOn: Boolean, elapsedMs: Long) {
        synchronized(lock) {
            settings.refresh()
            rollDayIfNeeded()
            feedInternal(screenOn, elapsedMs)
            if (screenOn) {
                // The dark gap before this wake broadcast must not count as usage:
                // re-anchor the accumulation beat so evaluate() adds nothing for it.
                lastBeatElapsed = elapsedMs
            }
            // Evaluate on every transition (not only screen-off) so the published
            // live state follows reality immediately.
            evaluate()
        }
    }

    private fun feedInternal(screenOn: Boolean, elapsedMs: Long) {
        if (screenOn && !lastScreenOn) tracker.onScreenOn(elapsedMs)
        if (!screenOn && lastScreenOn) tracker.onScreenOff(elapsedMs)
        lastScreenOn = screenOn
    }

    private fun evaluate() {
        settings.refresh() // UI card and controller are separate instances; sync first.
        val prefs = settings.current
        val now = SystemClockElapsed()
        // Accumulate real screen-on time for the day from heartbeat deltas.
        val last = lastBeatElapsed
        if (last != null && lastScreenOn && now > last) accumulatedMs += now - last
        lastBeatElapsed = now
        accumulatedMinutes = (accumulatedMs / 60_000L).toInt()
        val sessionMinutes = tracker.currentSessionMinutes(now)
        val engineConfig = if (prefs.enabled) UsageCoachEngine.CoachConfig(
            dailyBudgetMinutes = prefs.budgetMinutes,
            continuousLimitMinutes = prefs.continuousLimitMinutes
        ) else UsageCoachEngine.CoachConfig(dailyBudgetMinutes = null, continuousLimitMinutes = null)

        val budget = UsageCoachEngine.budgetAlert(accumulatedMinutes, engineConfig, firedKeys)
        if (budget != null) {
            firedKeys += budget.cooldownKey
            persistFired()
            notifier.show(budget)
        }
        // Continuous alerts cool down PER SESSION (not per day): a fresh continuous
        // usage period after a real break deserves a fresh reminder, but the same
        // session never spams.
        val sessionStart = tracker.sessionStartMs()
        if (sessionStart != lastAlertedSessionStart) {
            sessionAlertedKeys.clear()
            lastAlertedSessionStart = sessionStart
        }
        val continuous = UsageCoachEngine.continuousAlert(
            sessionMinutes, engineConfig, sessionAlertedKeys
        )
        if (continuous != null) {
            sessionAlertedKeys += continuous.cooldownKey
            notifier.show(continuous)
        }
        saveAccumulated()
        publishLiveState()
    }

    /** Snapshot the planet card renders from; refreshed every heartbeat (5s) and on every event. */
    private fun publishLiveState() {
        val prefs = settings.current
        // Coach off = plan off: publish nulls so the home hero shows 未设置 instead
        // of a budget frozen from the last on-period, and so the toggle has a
        // visible effect immediately.
        val effectiveBudget = prefs.budgetMinutes.takeIf { prefs.enabled }
        val effectiveLimit = prefs.continuousLimitMinutes.takeIf { prefs.enabled }
        val records = (0 until 7).map { offset ->
            val key = PlanetEngine.shiftKey(dayKey, -offset)
            if (key == dayKey) {
                // Today is still in progress: never counted as achieved/failed until
                // the day-rollover archive decides it.
                PlanetEngine.DayRecord(
                    dateKey = key,
                    screenOnMinutes = accumulatedMinutes,
                    budgetMinutes = effectiveBudget,
                    achieved = null
                )
            } else dayStore.get(key)
        }
        liveState.value = LiveState(
            enabled = prefs.enabled,
            todayMinutes = accumulatedMinutes,
            sessionMinutes = tracker.currentSessionMinutes(SystemClockElapsed()),
            budgetMinutes = effectiveBudget,
            continuousLimitMinutes = effectiveLimit,
            stage = PlanetEngine.stage(records, dayKey),
            streakDays = PlanetEngine.consecutiveAchievedDays(records),
            last7Days = records
        )
    }

    private fun rollDayIfNeeded() {
        val today = todayKey()
        if (today != dayKey) {
            // Archive yesterday for the planet (real screen-on minutes + budget + achieved).
            // Per the engine contract a day without an active budget is not a valid
            // day (null = feature off), so disabled days never grow the planet.
            val prefs = settings.current
            val budget = prefs.budgetMinutes.takeIf { prefs.enabled }
            dayStore.put(PlanetEngine.DayRecord(
                dateKey = dayKey,
                screenOnMinutes = accumulatedMinutes,
                budgetMinutes = budget,
                achieved = budget != null && accumulatedMinutes in 0..budget
            ))
            dayKey = today
            accumulatedMinutes = 0
            firedKeys = mutableSetOf()
            sessionAlertedKeys.clear()
            lastAlertedSessionStart = null
            tracker = newTracker()
            persistFired()
            saveAccumulated()
        }
    }

    private fun readAccumulated(): Int =
        context.getSharedPreferences(PREFS_DAY, Context.MODE_PRIVATE)
            .getInt("${KEY_ACCUM}_$dayKey", 0)

    private fun saveAccumulated() {
        context.getSharedPreferences(PREFS_DAY, Context.MODE_PRIVATE)
            .edit().putInt("${KEY_ACCUM}_$dayKey", accumulatedMinutes).apply()
    }

    private fun readFired(): MutableSet<String> =
        context.getSharedPreferences(PREFS_DAY, Context.MODE_PRIVATE)
            .getStringSet("${KEY_FIRED}_$dayKey", emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun persistFired() {
        context.getSharedPreferences(PREFS_DAY, Context.MODE_PRIVATE)
            .edit().putStringSet("${KEY_FIRED}_$dayKey", firedKeys).apply()
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isScreenOnNow(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return pm?.isInteractive ?: true
    }

    companion object {
        data class LiveState(
            val enabled: Boolean = false,
            val todayMinutes: Int = 0,
            val sessionMinutes: Int = 0,
            val budgetMinutes: Int? = null,
            val continuousLimitMinutes: Int? = null,
            val stage: PlanetEngine.Stage = PlanetEngine.Stage.SPROUT,
            val streakDays: Int = 0,
            val last7Days: List<PlanetEngine.DayRecord?> = emptyList()
        )

        private val liveState = MutableStateFlow(LiveState())
        fun liveState(): StateFlow<LiveState> = liveState.asStateFlow()

        // UI-side instant-refresh request (app resumed, tab switch, ...). Coalesced
        // with the settings-change signal by the reaction collector in start().
        private val refreshTick = MutableStateFlow(0)
        fun requestRefresh() {
            refreshTick.value += 1
        }

        private const val PREFS_DAY = "usage_coach_day"
        private const val KEY_ACCUM = "accum_minutes"
        private const val KEY_FIRED = "fired_keys"
        // 5s: the service already samples battery at 5s while the screen is on, so
        // the coach beat is never the bottleneck; it only bounds how long a fresh
        // screen-on minute can stay invisible (worst case one beat).
        private const val HEARTBEAT_MS = 5_000L
        // Coalescing window for UI-card saves: per-keystroke writes must not spam
        // evaluations, and intermediate typed values ("2" of "240") must not alert.
        private const val REACTION_DELAY_MS = 300L
        fun SystemClockElapsed(): Long = android.os.SystemClock.elapsedRealtime()
    }
}
