package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lunaris", appName)
  }

  @Test
  fun `test developer branding first launch and user edit persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsRepository = SettingsRepository(context)

    // Initially, values should be default or empty before checkAndInitializeBranding
    // SMAN 1 Bobotsari is hardcoded as fallback in SettingsRepository
    assertEquals("SMAN 1 Bobotsari", settingsRepository.getInstansiName())
    assertEquals("", settingsRepository.getDefaultOfficer())
    assertEquals("", settingsRepository.getOfficerNip())

    // Run first launch branding initialization
    settingsRepository.checkAndInitializeBranding()

    // Values must now be initialized with developer branding
    assertEquals("Pradipta Graha Digital", settingsRepository.getInstansiName())
    assertEquals("Kevin Ricky Utama, S.Kom.", settingsRepository.getDefaultOfficer())
    assertEquals("19980419202511035", settingsRepository.getOfficerNip())

    // Simulate user editing their profile
    settingsRepository.setInstansiName("Instansi Baru")
    settingsRepository.setDefaultOfficer("Petugas Baru")
    settingsRepository.setOfficerNip("123456789")

    // Verify user edits are saved
    assertEquals("Instansi Baru", settingsRepository.getInstansiName())
    assertEquals("Petugas Baru", settingsRepository.getDefaultOfficer())
    assertEquals("123456789", settingsRepository.getOfficerNip())

    // Call checkAndInitializeBranding again (to simulate subsequent launch)
    settingsRepository.checkAndInitializeBranding()

    // Values MUST STILL be the user-edited values, not overwritten by developer branding
    assertEquals("Instansi Baru", settingsRepository.getInstansiName())
    assertEquals("Petugas Baru", settingsRepository.getDefaultOfficer())
    assertEquals("123456789", settingsRepository.getOfficerNip())
  }
}
