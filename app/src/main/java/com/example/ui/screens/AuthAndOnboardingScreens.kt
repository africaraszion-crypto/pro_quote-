package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BrandColorPicker
import com.example.ui.components.CurrencyDropdown
import com.example.ui.components.LanguagePillSwitch
import com.example.ui.components.ProQuoteLogoMark
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.Coral50
import com.example.ui.theme.Coral600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600

@Composable
fun AuthScreen(
    isSignUp: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    noticeMessage: String?,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onBackToLanding: () -> Unit,
    onConfigureSupabase: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLanding) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(R.string.auth_back_to_landing)
                    )
                }
                ProQuoteLogoMark()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguagePillSwitch(onSelectLanguage = onSelectLanguage)
                    IconButton(onClick = onConfigureSupabase) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = tr(R.string.auth_configure_supabase)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isSignUp) tr(R.string.auth_signup_title) else tr(R.string.auth_login_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = if (isSignUp) {
                                tr(R.string.auth_signup_subtitle)
                            } else {
                                tr(R.string.auth_login_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(tr(R.string.auth_email_label)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr(R.string.auth_password_label)) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) tr(R.string.auth_hide_password) else tr(R.string.auth_show_password)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = Coral50,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Coral600.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = errorMessage,
                                color = Coral600,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (!noticeMessage.isNullOrBlank()) {
                        Surface(
                            color = Emerald50,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = noticeMessage,
                                color = Emerald600,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onSubmit(email, password) },
                        enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSignUp) tr(R.string.auth_creating_account) else tr(R.string.auth_signing_in))
                        } else {
                            Text(if (isSignUp) tr(R.string.auth_create_account) else tr(R.string.action_sign_in))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUp) tr(R.string.auth_has_account) else tr(R.string.auth_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                        TextButton(
                            onClick = onToggleMode,
                            modifier = Modifier.testTag("auth_toggle_mode_button")
                        ) {
                            Text(if (isSignUp) tr(R.string.action_sign_in) else tr(R.string.landing_get_started))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    userEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectLanguage: (AppLanguage) -> Unit,
    onCompleteOnboarding: (companyName: String, currency: String, taxRate: Double, brandColor: String) -> Unit,
    onSignOut: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("TZS") }
    var taxRateText by remember { mutableStateOf("18.0") }
    var brandColor by remember { mutableStateOf("#0f172a") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProQuoteLogoMark()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguagePillSwitch(onSelectLanguage = onSelectLanguage)
                    TextButton(onClick = onSignOut) {
                        Text(tr(R.string.action_sign_out))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = tr(R.string.onboarding_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = tr(R.string.onboarding_subtitle, userEmail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text(tr(R.string.onboarding_company_label)) },
                        placeholder = { Text("Acme Studio Ltd") },
                        leadingIcon = { Icon(Icons.Outlined.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_company_input")
                    )

                    CurrencyDropdown(
                        selectedCurrency = currency,
                        onCurrencySelected = { currency = it }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text(tr(R.string.onboarding_tax_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_tax_input")
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("0", "16", "18", "20").forEach { preset ->
                                FilterChip(
                                    selected = taxRateText.toDoubleOrNull() == preset.toDouble(),
                                    onClick = { taxRateText = preset },
                                    label = { Text("$preset%") }
                                )
                            }
                        }
                    }

                    BrandColorPicker(
                        selectedHex = brandColor,
                        onHexChanged = { brandColor = it }
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = Coral50,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Coral600.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = errorMessage,
                                color = Coral600,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCompleteOnboarding(
                                companyName,
                                currency,
                                taxRateText.toDoubleOrNull() ?: 18.0,
                                brandColor
                            )
                        },
                        enabled = !isLoading && companyName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr(R.string.onboarding_creating))
                        } else {
                            Text(
                                text = tr(R.string.onboarding_launch),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
