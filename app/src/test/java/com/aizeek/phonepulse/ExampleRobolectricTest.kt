package com.aizeek.phonepulse

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExampleRobolectricTest {

  private fun appNameFor(locale: Locale): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return context.createConfigurationContext(configuration).getString(R.string.app_name)
  }

  @Test
  fun `app name follows the ScreenRhythm Planet brand across locales`() {
    // Rebrand 2026-10-23: zh locales resolve 屏律星球, others fall back to the
    // default (English) name.
    assertEquals("ScreenRhythm Planet", appNameFor(Locale.US))
    assertEquals("ScreenRhythm Planet", appNameFor(Locale.JAPAN))
    assertEquals("屏律星球", appNameFor(Locale.SIMPLIFIED_CHINESE))
    assertEquals("屏律星球", appNameFor(Locale.TRADITIONAL_CHINESE))
    assertEquals("屏律星球", appNameFor(Locale.forLanguageTag("zh-HK")))
    assertEquals("屏律星球", appNameFor(Locale.forLanguageTag("zh-Hans-SG")))
  }
}
