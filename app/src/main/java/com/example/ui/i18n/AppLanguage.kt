package com.example.ui.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.R
import com.example.data.model.QuoteStatus
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val shortCode: String
) {
    English(code = "en", nativeName = "English", shortCode = "EN"),
    Swahili(code = "sw", nativeName = "Kiswahili", shortCode = "SW");

    fun toggled(): AppLanguage = when (this) {
        English -> Swahili
        Swahili -> English
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: English
        }
    }
}

fun localizedResourcesFor(context: Context, language: AppLanguage): Resources {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(language.code))
    return context.createConfigurationContext(config).resources
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }
val LocalAppResources = staticCompositionLocalOf<Resources> {
    error("LocalAppResources was not provided")
}

@Composable
@ReadOnlyComposable
fun tr(@StringRes resId: Int): String = LocalAppResources.current.getString(resId)

@Composable
@ReadOnlyComposable
fun tr(@StringRes resId: Int, vararg formatArgs: Any): String =
    LocalAppResources.current.getString(resId, *formatArgs)

@Composable
@ReadOnlyComposable
fun quoteStatusLabel(status: QuoteStatus): String {
    return when (status) {
        QuoteStatus.Draft -> tr(R.string.status_draft)
        QuoteStatus.Sent -> tr(R.string.status_sent)
        QuoteStatus.Accepted -> tr(R.string.status_accepted)
        QuoteStatus.Declined -> tr(R.string.status_declined)
    }
}

fun quoteStatusLabel(resources: Resources, status: QuoteStatus): String {
    return when (status) {
        QuoteStatus.Draft -> resources.getString(R.string.status_draft)
        QuoteStatus.Sent -> resources.getString(R.string.status_sent)
        QuoteStatus.Accepted -> resources.getString(R.string.status_accepted)
        QuoteStatus.Declined -> resources.getString(R.string.status_declined)
    }
}
