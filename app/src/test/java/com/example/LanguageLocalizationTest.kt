package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionStore
import com.example.data.model.QuoteStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.localizedResourcesFor
import com.example.ui.i18n.quoteStatusLabel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguageLocalizationTest {

    @Test
    fun `sessionStore defaults app language to English`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)
        assertEquals("en", store.getLanguageCode())
        assertEquals(AppLanguage.English, AppLanguage.fromCode(store.getLanguageCode()))
    }

    @Test
    fun `sessionStore persists switching to Swahili and back to English`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        store.saveLanguageCode(AppLanguage.Swahili.code)
        assertEquals("sw", store.getLanguageCode())
        assertEquals(AppLanguage.Swahili, AppLanguage.fromCode(store.getLanguageCode()))

        store.saveLanguageCode(AppLanguage.English.code)
        assertEquals("en", store.getLanguageCode())
    }

    @Test
    fun `localizedResourcesFor resolves both English and Swahili string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val enRes = localizedResourcesFor(context, AppLanguage.English)
        val swRes = localizedResourcesFor(context, AppLanguage.Swahili)

        assertEquals("Pro Quote", enRes.getString(R.string.app_name))
        assertEquals("Pro Quote", swRes.getString(R.string.app_name))

        assertEquals("Settings", enRes.getString(R.string.tab_settings))
        assertEquals("Mipangilio", swRes.getString(R.string.tab_settings))

        assertEquals("Accepted", quoteStatusLabel(enRes, QuoteStatus.Accepted))
        assertEquals("Imekubaliwa", quoteStatusLabel(swRes, QuoteStatus.Accepted))
        assertNotEquals(
            enRes.getString(R.string.landing_headline),
            swRes.getString(R.string.landing_headline)
        )
    }
}
