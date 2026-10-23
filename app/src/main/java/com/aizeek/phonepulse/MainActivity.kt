package com.aizeek.phonepulse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import com.aizeek.phonepulse.update.UpdateRepository
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aizeek.phonepulse.intervention.ReturnGiftCenter
import com.aizeek.phonepulse.intervention.ReturnGiftStore
import com.aizeek.phonepulse.service.ScreenTrackerService
import com.aizeek.phonepulse.ui.MainScreen
import com.aizeek.phonepulse.ui.theme.BgDark
import com.aizeek.phonepulse.ui.theme.PhonePulseTheme

class MainActivity : ComponentActivity() {
    private var updateOpenRequest by mutableIntStateOf(0)
    private val giftStore by lazy { ReturnGiftStore(applicationContext) }

    override fun onPause() {
        super.onPause()
        // Return-gift bookkeeping: remember when the user left the app so the
        // next arrival can reward a real resting gap with stardust.
        try {
            giftStore.recordLeave()
            android.util.Log.i(TAG, "leave recorded")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val gift = giftStore.onArrive()
            android.util.Log.i(TAG, "arrive check: gift=${gift != null}")
            gift?.let { ReturnGiftCenter.post(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val TAG = "ReturnGift"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateOpenRequest = savedInstanceState?.getInt("update_open_request") ?: 0
        if (savedInstanceState == null && intent.action == UpdateRepository.OPEN_UPDATES) updateOpenRequest++
        enableEdgeToEdge()
        requestHighRefreshMode()

        // Ensure background service is running
        try {
            ScreenTrackerService.start(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            PhonePulseTheme {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        try {
                            ScreenTrackerService.start(this@MainActivity)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    MainScreen(updateOpenRequest = updateOpenRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == UpdateRepository.OPEN_UPDATES) updateOpenRequest++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("update_open_request", updateOpenRequest)
        super.onSaveInstanceState(outState)
    }

    /**
     * Perf: declare (softly) that the app supports the panel's highest refresh
     * rate. We deliberately prefer [android.view.WindowManager.LayoutParams.preferredRefreshRate]
     * over pinning preferredDisplayModeId: the request only lifts the default
     * third-party cap while the system frame-rate coordinator keeps full
     * authority — it idles the display down on static content and boosts it
     * while scrolling (auto-adapt). On HyperOS the user's global refresh-rate
     * setting stays the hard ceiling: system=60Hz -> app stays 60; system=120/
     * smart -> the app renders at the boosted rate. Pure pacing change —
     * identical pixels, layouts and animations.
     */
    private fun requestHighRefreshMode() {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return
            val current = display.mode
            var bestRate = current.refreshRate
            for (mode in display.supportedModes) {
                val sameResolution = mode.physicalWidth == current.physicalWidth &&
                    mode.physicalHeight == current.physicalHeight
                if (sameResolution && mode.refreshRate > bestRate + 0.1f) {
                    bestRate = mode.refreshRate
                }
            }
            if (bestRate > current.refreshRate + 0.1f) {
                val attrs = window.attributes
                attrs.preferredRefreshRate = bestRate
                window.attributes = attrs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
