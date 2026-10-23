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
  fun `app name follows the Pausia brand across locales`() {
    // Rebrand 2026-10-23: zh locales resolve 息律, others fall back to the
    // default (English) name Pausia.
    assertEquals("Pausia", appNameFor(Locale.US))
    assertEquals("Pausia", appNameFor(Locale.JAPAN))
    assertEquals("息律", appNameFor(Locale.SIMPLIFIED_CHINESE))
    assertEquals("息律", appNameFor(Locale.TRADITIONAL_CHINESE))
    assertEquals("息律", appNameFor(Locale.forLanguageTag("zh-HK")))
    assertEquals("息律", appNameFor(Locale.forLanguageTag("zh-Hans-SG")))
  }
}
