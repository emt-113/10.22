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
  fun `app name is PhonePulse across all supported locales`() {
    // Brand unification: every locale resolves the same PhonePulse identity.
    assertEquals("PhonePulse", appNameFor(Locale.US))
    assertEquals("PhonePulse", appNameFor(Locale.JAPAN))
    assertEquals("PhonePulse", appNameFor(Locale.SIMPLIFIED_CHINESE))
    assertEquals("PhonePulse", appNameFor(Locale.TRADITIONAL_CHINESE))
    assertEquals("PhonePulse", appNameFor(Locale.forLanguageTag("zh-HK")))
    assertEquals("PhonePulse", appNameFor(Locale.forLanguageTag("zh-Hans-SG")))
  }
}
