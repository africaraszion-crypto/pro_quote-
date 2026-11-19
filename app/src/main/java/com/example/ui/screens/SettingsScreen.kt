package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SupabaseConfig
import com.example.data.model.Workspace
import com.example.ui.components.BrandColorPicker
import com.example.ui.components.CurrencyDropdown
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.Cobalt50
import com.example.ui.theme.Cobalt600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600

@Composable
fun SettingsScreen(
    workspace: Workspace,
    defaultPaymentInstructions: String,
    supabaseConfig: SupabaseConfig,
    isSaving: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSaveSettings: (
        companyName: String,
        logoUrl: String,
        brandColor: String,
        currency: String,
        taxRate: Double,
        defaultPaymentInstructions: String
    ) -> Unit,
    onConfigureSupabase: () -> Unit
) {
    var companyName by remember(workspace) { mutableStateOf(workspace.companyName) }
    var logoUrl by remember(workspace) { mutableStateOf(workspace.logoUrl.orEmpty()) }
    var brandColor by remember(workspace) { mutableStateOf(workspace.brandColor) }
    var currency by remember(workspace) { mutableStateOf(workspace.currency) }
    var taxRateText by remember(workspace) { mutableStateOf(workspace.taxRate.toString()) }
    var paymentDefaults by remember(defaultPaymentInstructions) { mutableStateOf(defaultPaymentInstructions) }
    val activeLanguage = LocalAppLanguage.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = tr(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        }

        // Language Selection Card
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = Cobalt600,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(tr(R.string.language_section_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = tr(R.string.language_section_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppLanguage.English to tr(R.string.language_english_label),
                            AppLanguage.Swahili to tr(R.string.language_swahili_label)
                        ).forEach { (lang, label) ->
                            val selected = activeLanguage == lang
                            Surface(
                                onClick = { onSelectLanguage(lang) },
                                color = if (selected) Cobalt50 else Slate50,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Cobalt600.copy(alpha = 0.4f) else Slate200
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("language_option_${lang.code}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = null
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        )
                                    }
                                    Text(
                                        text = lang.shortCode,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (selected) Cobalt600 else Slate600
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(tr(R.string.settings_company_section), style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text(tr(R.string.settings_company_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_company_input")
                    )

                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text(tr(R.string.settings_logo_url)) },
                        placeholder = { Text("https://company.com/logo.png") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_logo_url_input")
                    )

                    BrandColorPicker(
                        selectedHex = brandColor,
                        onHexChanged = { brandColor = it }
                    )
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(tr(R.string.settings_financial_section), style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CurrencyDropdown(
                            selectedCurrency = currency,
                            onCurrencySelected = { currency = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text(tr(R.string.settings_tax_rate)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_tax_input")
                        )
                    }

                    OutlinedTextField(
                        value = paymentDefaults,
                        onValueChange = { paymentDefaults = it },
                        label = { Text(tr(R.string.settings_payment_defaults)) },
                        placeholder = { Text(tr(R.string.settings_payment_defaults_hint)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_payment_defaults_input")
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                companyName,
                                logoUrl,
                                brandColor,
                                currency,
                                taxRateText.toDoubleOrNull() ?: workspace.taxRate,
                                paymentDefaults
                            )
                        },
                        enabled = !isSaving && companyName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr(R.string.settings_saving))
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(R.string.settings_save_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(tr(R.string.settings_supabase_section), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = supabaseConfig.url.ifBlank { tr(R.string.settings_not_configured) },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = Slate600
                    )
                    OutlinedButton(
                        onClick = onConfigureSupabase,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr(R.string.settings_update_supabase))
                    }
                }
            }
        }
    }
}
