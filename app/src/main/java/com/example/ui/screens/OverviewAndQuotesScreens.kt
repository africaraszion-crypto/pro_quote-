package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Client
import com.example.data.model.Quote
import com.example.data.model.QuoteStatus
import com.example.data.model.Workspace
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.LocalAppResources
import com.example.ui.i18n.quoteStatusLabel
import com.example.ui.i18n.tr
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber600
import com.example.ui.theme.Cobalt50
import com.example.ui.theme.Cobalt600
import com.example.ui.theme.Coral600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.DashboardMetrics
import java.util.Locale

@Composable
fun OverviewScreen(
    workspace: Workspace,
    metrics: DashboardMetrics,
    quotes: List<Quote>,
    clients: List<Client>,
    isLoading: Boolean,
    onNewQuote: () -> Unit,
    onViewPublicQuote: (String) -> Unit,
    onEditQuote: (String) -> Unit,
    onCopiedLinkToast: (String) -> Unit,
    onViewAllQuotes: () -> Unit
) {
    val resources = LocalAppResources.current
    val clientMap = remember(clients) { clients.associateBy { it.id } }
    val recentQuotes = remember(quotes) { quotes.take(8) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tr(R.string.overview_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = tr(R.string.overview_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        }

        // 4 KPI Metric Cards (2x2 grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = tr(R.string.kpi_sent_title),
                        value = metrics.totalQuotesSent.toString(),
                        subtitle = tr(R.string.kpi_sent_sub, quotes.size),
                        icon = Icons.Outlined.Send,
                        accentBg = Cobalt50,
                        accentFg = Cobalt600,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = tr(R.string.kpi_accepted_title),
                        value = metrics.acceptedCount.toString(),
                        subtitle = tr(
                            R.string.kpi_accepted_sub,
                            String.format(Locale.US, "%.1f", metrics.conversionRatePercent)
                        ),
                        icon = Icons.Outlined.CheckCircle,
                        accentBg = Emerald50,
                        accentFg = Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = tr(R.string.kpi_revenue_title),
                        value = Formatters.formatCurrency(metrics.acceptedRevenue, workspace.currency),
                        subtitle = tr(R.string.kpi_revenue_sub),
                        icon = Icons.Outlined.AccountBalanceWallet,
                        accentBg = Emerald50,
                        accentFg = Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = tr(R.string.kpi_pipeline_title),
                        value = Formatters.formatCurrency(metrics.pendingPipelineValue, workspace.currency),
                        subtitle = tr(R.string.kpi_pipeline_sub),
                        icon = Icons.Outlined.HourglassTop,
                        accentBg = Amber100,
                        accentFg = Amber600,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(R.string.overview_recent_quotes),
                    style = MaterialTheme.typography.titleLarge
                )
                if (quotes.isNotEmpty()) {
                    TextButton(onClick = onViewAllQuotes) {
                        Text(tr(R.string.overview_view_all, quotes.size))
                    }
                }
            }
        }

        if (recentQuotes.isEmpty() && !isLoading) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    EmptyStateView(
                        title = tr(R.string.overview_empty_title),
                        subtitle = tr(R.string.overview_empty_subtitle),
                        actionLabel = tr(R.string.overview_empty_cta),
                        onAction = onNewQuote
                    )
                }
            }
        } else {
            items(recentQuotes, key = { it.id }) { quote ->
                val clientName = quote.clientId?.let { clientMap[it]?.clientName }
                    ?: tr(R.string.label_unassigned_client)
                QuoteRowCard(
                    quote = quote,
                    clientName = clientName,
                    currency = workspace.currency,
                    onView = { onViewPublicQuote(quote.id) },
                    onEdit = { onEditQuote(quote.id) },
                    onCopiedLink = {
                        onCopiedLinkToast(resources.getString(R.string.magic_link_copied_toast, quote.quoteNumber))
                    },
                    onDelete = null
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentBg: Color,
    accentFg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentFg,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotesScreen(
    workspace: Workspace,
    quotes: List<Quote>,
    clients: List<Client>,
    isLoading: Boolean,
    onNewQuote: () -> Unit,
    onViewPublicQuote: (String) -> Unit,
    onEditQuote: (String) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onCopiedLinkToast: (String) -> Unit
) {
    val resources = LocalAppResources.current
    val clientMap = remember(clients) { clients.associateBy { it.id } }
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<QuoteStatus?>(null) }
    var quotePendingDelete by remember { mutableStateOf<Quote?>(null) }

    val filtered = remember(quotes, search, selectedFilter, clientMap) {
        quotes.filter { quote ->
            val clientName = quote.clientId?.let { clientMap[it]?.clientName }.orEmpty()
            val matchesSearch = search.isBlank() ||
                quote.quoteNumber.contains(search, ignoreCase = true) ||
                clientName.contains(search, ignoreCase = true)
            val matchesStatus = selectedFilter == null || quote.quoteStatus == selectedFilter
            matchesSearch && matchesStatus
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr(R.string.quotes_title), style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(tr(R.string.quotes_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quotes_search_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text(tr(R.string.quotes_filter_all, quotes.size)) }
                    )
                    QuoteStatus.entries.forEach { st ->
                        val count = quotes.count { it.quoteStatus == st }
                        FilterChip(
                            selected = selectedFilter == st,
                            onClick = { selectedFilter = if (selectedFilter == st) null else st },
                            label = { Text("${quoteStatusLabel(st)} ($count)") }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    EmptyStateView(
                        title = if (quotes.isEmpty()) tr(R.string.quotes_empty_title) else tr(R.string.quotes_empty_filtered_title),
                        subtitle = if (quotes.isEmpty()) {
                            tr(R.string.quotes_empty_subtitle)
                        } else {
                            tr(R.string.quotes_empty_filtered_subtitle)
                        },
                        actionLabel = if (quotes.isEmpty()) tr(R.string.overview_empty_cta) else null,
                        onAction = if (quotes.isEmpty()) onNewQuote else null
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { quote ->
                val clientName = quote.clientId?.let { clientMap[it]?.clientName }
                    ?: tr(R.string.label_unassigned_client)
                QuoteRowCard(
                    quote = quote,
                    clientName = clientName,
                    currency = workspace.currency,
                    onView = { onViewPublicQuote(quote.id) },
                    onEdit = { onEditQuote(quote.id) },
                    onCopiedLink = {
                        onCopiedLinkToast(resources.getString(R.string.magic_link_copied_toast, quote.quoteNumber))
                    },
                    onDelete = { quotePendingDelete = quote }
                )
            }
        }
    }

    quotePendingDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { quotePendingDelete = null },
            title = { Text(tr(R.string.quotes_delete_title, q.quoteNumber)) },
            text = { Text(tr(R.string.quotes_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteQuote(q.id)
                        quotePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral600)
                ) {
                    Text(tr(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { quotePendingDelete = null }) { Text(tr(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun QuoteRowCard(
    quote: Quote,
    clientName: String,
    currency: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onCopiedLink: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val clipboard = LocalClipboardManager.current

    Surface(
        onClick = onView,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quote_card_${quote.quoteNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = quote.quoteNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Slate900
                    )
                    StatusBadge(status = quote.quoteStatus)
                }
                Text(
                    text = Formatters.formatCurrency(quote.totalAmount, currency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Slate900
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Formatters.formatDisplayDate(quote.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }

            HorizontalDivider(color = Slate100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onView) {
                    Icon(Icons.Outlined.Visibility, contentDescription = tr(R.string.action_view), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr(R.string.action_view))
                }
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(Formatters.buildMagicLink(quote.id)))
                        onCopiedLink()
                    }
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = tr(R.string.magic_link_copy), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr(R.string.label_magic_link))
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = tr(R.string.action_edit), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr(R.string.action_edit))
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = tr(R.string.action_delete),
                            tint = Coral600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
