package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.QuoteStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppResources
import com.example.ui.i18n.quoteStatusLabel
import com.example.ui.i18n.tr
import com.example.ui.theme.Cobalt50
import com.example.ui.theme.Cobalt600
import com.example.ui.theme.Coral50
import com.example.ui.theme.Coral600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.parseBrandColor
import com.example.ui.util.Formatters

@Composable
fun LanguagePillSwitch(
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = LocalAppLanguage.current
    Surface(
        onClick = { onSelectLanguage(active.toggled()) },
        shape = RoundedCornerShape(999.dp),
        color = Slate100,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("language_toggle_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = tr(R.string.language_section_title),
                tint = Slate600,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = active.shortCode,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                ),
                color = Slate900
            )
        }
    }
}

@Composable
fun ProQuoteLogoMark(
    modifier: Modifier = Modifier,
    brandColor: Color = Slate900
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(brandColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PQ",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            )
        }
        Text(
            text = "Pro Quote",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun StatusBadge(
    status: QuoteStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        QuoteStatus.Draft -> Slate100 to Slate600
        QuoteStatus.Sent -> Cobalt50 to Cobalt600
        QuoteStatus.Accepted -> Emerald50 to Emerald600
        QuoteStatus.Declined -> Coral50 to Coral600
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = quoteStatusLabel(status),
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text(tr(R.string.label_currency)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("currency_dropdown")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Formatters.supportedCurrencies.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = code,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    onClick = {
                        onCurrencySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandColorPicker(
    selectedHex: String,
    onHexChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        "#0f172a", // Midnight Slate
        "#2563eb", // Cobalt
        "#059669", // Emerald
        "#7c3aed", // Violet
        "#dc2626", // Crimson
        "#ea580c", // Terracotta
        "#0d9488"  // Teal
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = tr(R.string.label_primary_brand_color),
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { hex ->
                val color = parseBrandColor(hex)
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Slate200,
                            shape = CircleShape
                        )
                        .clickable { onHexChanged(hex) }
                        .testTag("color_swatch_${hex.removePrefix("#")}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = tr(R.string.label_selected_color, hex),
                            tint = contrastOnColor(color),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = selectedHex,
            onValueChange = { onHexChanged(it) },
            label = { Text(tr(R.string.label_brand_hex_code)) },
            singleLine = true,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(parseBrandColor(selectedHex))
                        .border(1.dp, Slate200, CircleShape)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("brand_color_hex_input")
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = R.drawable.img_empty_state,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ConnectSupabaseDialog(
    initialUrl: String,
    initialKey: String,
    isLoading: Boolean,
    errorMessage: String?,
    onTestAndSave: (String, String) -> Unit,
    onDismiss: (() -> Unit)?
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var key by remember(initialKey) { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = { if (onDismiss != null && !isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = tr(R.string.supabase_connect_title),
                tint = Emerald600
            )
        },
        title = {
            Text(
                text = tr(R.string.supabase_connect_title),
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr(R.string.supabase_connect_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(tr(R.string.supabase_url_label)) },
                    placeholder = { Text("https://your-project.supabase.co") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url_input")
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(tr(R.string.supabase_key_label)) },
                    placeholder = { Text("sb_publishable_... or eyJ...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_key_input")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = Coral50,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Coral600.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = errorMessage,
                            color = Coral600,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTestAndSave(url, key) },
                enabled = !isLoading && url.isNotBlank() && key.isNotBlank(),
                modifier = Modifier.testTag("connect_supabase_submit")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr(R.string.supabase_connecting))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(tr(R.string.supabase_connect_save))
                }
            }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text(tr(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
fun MagicLinkShareDialog(
    quoteId: String,
    quoteNumber: String,
    onPreviewPublicQuote: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val resources = LocalAppResources.current
    var copied by remember { mutableStateOf(false) }
    val magicLink = Formatters.buildMagicLink(quoteId)
    val webPath = Formatters.buildWebPathPreview(quoteId)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = tr(R.string.label_magic_link),
                tint = Cobalt600
            )
        },
        title = {
            Text(
                text = tr(R.string.magic_link_ready_title),
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr(R.string.magic_link_ready_desc, quoteNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = tr(R.string.magic_link_route_label, webPath),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Text(
                            text = magicLink,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Slate900
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(magicLink))
                            copied = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_magic_link_button")
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                            contentDescription = tr(R.string.magic_link_copy),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) tr(R.string.magic_link_copied) else tr(R.string.magic_link_copy))
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    resources.getString(R.string.magic_link_share_subject, quoteNumber)
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    resources.getString(R.string.magic_link_share_body, quoteNumber, magicLink)
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    resources.getString(R.string.magic_link_share_chooser)
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = tr(R.string.magic_link_share),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(tr(R.string.magic_link_share))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPreviewPublicQuote,
                colors = ButtonDefaults.buttonColors(containerColor = Cobalt600),
                modifier = Modifier.testTag("preview_public_quote_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(tr(R.string.magic_link_open_viewer))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_done))
            }
        }
    )
}
