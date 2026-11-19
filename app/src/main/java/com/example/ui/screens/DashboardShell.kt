package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Workspace
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.parseBrandColor
import com.example.ui.util.Formatters

enum class DashboardTab(
    @StringRes val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Overview(R.string.tab_overview, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Quotes(R.string.tab_quotes, Icons.Filled.Description, Icons.Outlined.Description),
    Clients(R.string.tab_clients, Icons.Filled.People, Icons.Outlined.People),
    Items(R.string.tab_items, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    Settings(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardShell(
    workspace: Workspace,
    userEmail: String,
    activeTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onNewQuoteClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenMagicLinkLookup: (String) -> Unit,
    onConfigureSupabaseClick: () -> Unit,
    onSignOutClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showMagicLookup by remember { mutableStateOf(false) }
    val brandColor = parseBrandColor(workspace.brandColor)
    val activeLanguage = LocalAppLanguage.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = Slate50,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(brandColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = workspace.companyName.take(2).uppercase().ifBlank { "PQ" },
                                        color = contrastOnColor(brandColor),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JetBrainsMonoFontFamily
                                        )
                                    )
                                }
                                Column {
                                    Text(
                                        text = workspace.companyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr(
                                            R.string.dashboard_tax_meta,
                                            workspace.currency,
                                            workspace.taxRate.toString()
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate600
                                    )
                                }
                            }
                        },
                        actions = {
                            FilledTonalButton(
                                onClick = onNewQuoteClick,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = brandColor,
                                    contentColor = contrastOnColor(brandColor)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("topbar_new_quote_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = tr(R.string.dashboard_new_quote),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(tr(R.string.dashboard_new_quote))
                            }

                            Box {
                                IconButton(
                                    onClick = { profileMenuExpanded = true },
                                    modifier = Modifier.testTag("profile_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = tr(R.string.dashboard_account_menu)
                                    )
                                }
                                DropdownMenu(
                                    expanded = profileMenuExpanded,
                                    onDismissRequest = { profileMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    tr(R.string.dashboard_signed_in_as),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Slate600
                                                )
                                                Text(
                                                    userEmail,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        onClick = {}
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                                        text = {
                                            Text(tr(R.string.language_menu_item, activeLanguage.nativeName))
                                        },
                                        onClick = {
                                            profileMenuExpanded = false
                                            onSelectLanguage(activeLanguage.toggled())
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                                        text = { Text(tr(R.string.dashboard_refresh_data)) },
                                        onClick = {
                                            profileMenuExpanded = false
                                            onRefreshClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Outlined.QrCode2, contentDescription = null) },
                                        text = { Text(tr(R.string.dashboard_open_magic_menu)) },
                                        onClick = {
                                            profileMenuExpanded = false
                                            showMagicLookup = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                                        text = { Text(tr(R.string.dashboard_supabase_menu)) },
                                        onClick = {
                                            profileMenuExpanded = false
                                            onConfigureSupabaseClick()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                                        text = { Text(tr(R.string.action_sign_out)) },
                                        onClick = {
                                            profileMenuExpanded = false
                                            onSignOutClick()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = Slate200)
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = Color.White,
                        windowInsets = NavigationBarDefaults.windowInsets
                    ) {
                        DashboardTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = tr(tab.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.size(12.dp))
                        DashboardTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = tr(tab.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
                content(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }

    if (showMagicLookup) {
        var lookup by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMagicLookup = false },
            title = { Text(tr(R.string.dashboard_lookup_title)) },
            text = {
                OutlinedTextField(
                    value = lookup,
                    onValueChange = { lookup = it },
                    label = { Text(tr(R.string.dashboard_lookup_label)) },
                    singleLine = true,
                    modifier = Modifier.padding(top = 4.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = Formatters.extractQuoteIdFromInput(lookup)
                        if (id.isNotBlank()) {
                            showMagicLookup = false
                            onOpenMagicLinkLookup(id)
                        }
                    },
                    enabled = lookup.isNotBlank()
                ) {
                    Text(tr(R.string.label_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMagicLookup = false }) {
                    Text(tr(R.string.action_cancel))
                }
            }
        )
    }
}
