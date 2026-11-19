package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.LanguagePillSwitch
import com.example.ui.components.ProQuoteLogoMark
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.Cobalt50
import com.example.ui.theme.Cobalt600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters

@Composable
fun LandingScreen(
    isSupabaseConnected: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    onConfigureSupabaseClick: () -> Unit,
    onOpenPublicQuote: (String) -> Unit
) {
    var showOpenQuoteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Slate50)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProQuoteLogoMark()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguagePillSwitch(onSelectLanguage = onSelectLanguage)

                        Surface(
                            onClick = onConfigureSupabaseClick,
                            color = if (isSupabaseConnected) Emerald50 else Cobalt50,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSupabaseConnected) Emerald600.copy(alpha = 0.25f) else Cobalt600.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("supabase_status_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = tr(R.string.dashboard_supabase_menu),
                                    tint = if (isSupabaseConnected) Emerald600 else Cobalt600,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSupabaseConnected) tr(R.string.landing_supabase_live) else tr(R.string.landing_connect_db),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSupabaseConnected) Emerald600 else Cobalt600
                                )
                            }
                        }

                        TextButton(
                            onClick = onSignInClick,
                            modifier = Modifier.testTag("landing_signin_button")
                        ) {
                            Text(tr(R.string.action_sign_in))
                        }
                    }
                }

                // Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = R.drawable.img_landing_hero,
                            contentDescription = tr(R.string.landing_hero_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = Emerald500.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = tr(R.string.landing_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Emerald500,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Text(
                                text = tr(R.string.landing_headline),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = Color.White
                                )
                            )
                            Text(
                                text = tr(R.string.landing_subheadline),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Button(
                                    onClick = onGetStartedClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald500,
                                        contentColor = Slate900
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("landing_get_started_button")
                                ) {
                                    Text(
                                        text = tr(R.string.landing_get_started),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSignInClick,
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text(tr(R.string.action_sign_in))
                                    }
                                    OutlinedButton(
                                        onClick = { showOpenQuoteDialog = true },
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("open_magic_link_entry_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.QrCode2,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr(R.string.landing_open_magic_btn))
                                    }
                                }
                            }
                        }
                    }
                }

                // Value Proposition Grid
                Text(
                    text = tr(R.string.landing_features_heading),
                    style = MaterialTheme.typography.headlineMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            icon = Icons.Outlined.Description,
                            title = tr(R.string.landing_feat_quotes_title),
                            description = tr(R.string.landing_feat_quotes_desc),
                            modifier = Modifier.weight(1f)
                        )
                        FeatureCard(
                            icon = Icons.Outlined.Draw,
                            title = tr(R.string.landing_feat_sign_title),
                            description = tr(R.string.landing_feat_sign_desc),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            icon = Icons.Outlined.CurrencyExchange,
                            title = tr(R.string.landing_feat_currency_title),
                            description = tr(R.string.landing_feat_currency_desc),
                            modifier = Modifier.weight(1f)
                        )
                        FeatureCard(
                            icon = Icons.Outlined.Link,
                            title = tr(R.string.landing_feat_links_title),
                            description = tr(R.string.landing_feat_links_desc),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showOpenQuoteDialog) {
        var inputId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOpenQuoteDialog = false },
            title = { Text(tr(R.string.landing_lookup_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(R.string.landing_lookup_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    OutlinedTextField(
                        value = inputId,
                        onValueChange = { inputId = it },
                        label = { Text(tr(R.string.landing_lookup_label)) },
                        placeholder = { Text("e.g. /q/550e8400-e29b-...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("magic_link_lookup_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val extracted = Formatters.extractQuoteIdFromInput(inputId)
                        if (extracted.isNotBlank()) {
                            showOpenQuoteDialog = false
                            onOpenPublicQuote(extracted)
                        }
                    },
                    enabled = inputId.isNotBlank(),
                    modifier = Modifier.testTag("magic_link_lookup_submit")
                ) {
                    Text(tr(R.string.landing_lookup_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenQuoteDialog = false }) {
                    Text(tr(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Cobalt600,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
        }
    }
}
