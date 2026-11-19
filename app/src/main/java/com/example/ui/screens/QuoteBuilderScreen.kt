package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import com.example.data.model.Client
import com.example.data.model.EditableLineItem
import com.example.data.model.LibraryItem
import com.example.data.model.Workspace
import com.example.ui.i18n.tr
import com.example.ui.theme.Cobalt600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.QuoteBuilderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteBuilderScreen(
    workspace: Workspace,
    builder: QuoteBuilderState,
    clients: List<Client>,
    libraryItems: List<LibraryItem>,
    isMutatingClient: Boolean,
    onUpdateBuilder: ((QuoteBuilderState) -> QuoteBuilderState) -> Unit,
    onAddBlankRow: () -> Unit,
    onAddFromLibrary: (LibraryItem) -> Unit,
    onMoveRow: (Int, Int) -> Unit,
    onRemoveRow: (String) -> Unit,
    onInlineCreateClient: (name: String, email: String, phone: String, address: String, onDone: (Client) -> Unit) -> Unit,
    onSaveDraft: () -> Unit,
    onSaveAndGetMagicLink: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showLibraryPicker by remember { mutableStateOf(false) }
    var showInlineClientModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var clientMenuExpanded by remember { mutableStateOf(false) }

    val selectedClient = clients.firstOrNull { it.id == builder.selectedClientId }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr(R.string.action_back))
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = if (builder.editingQuoteId == null) {
                                    tr(R.string.builder_new_title)
                                } else {
                                    tr(R.string.builder_edit_title, builder.quoteNumber)
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = tr(
                                    R.string.builder_issued_date,
                                    Formatters.formatDisplayDate(Formatters.todayIsoDate())
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600
                            )
                        }
                    }
                )
                HorizontalDivider(color = Slate200)
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    HorizontalDivider(color = Slate200)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSaveDraft,
                            enabled = !builder.isSaving,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_draft_button")
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(R.string.builder_save_draft))
                        }
                        Button(
                            onClick = onSaveAndGetMagicLink,
                            enabled = !builder.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Cobalt600),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("save_and_magic_link_button")
                        ) {
                            if (builder.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr(R.string.builder_save_magic))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (builder.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            // 1. Header Configuration Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(tr(R.string.builder_details_title), style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = builder.quoteNumber,
                                onValueChange = { v -> onUpdateBuilder { it.copy(quoteNumber = v) } },
                                label = { Text(tr(R.string.builder_quote_number)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quote_number_input")
                            )
                            OutlinedTextField(
                                value = builder.validUntil,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr(R.string.builder_valid_until)) },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Outlined.CalendarToday, contentDescription = tr(R.string.builder_select_date_cd))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clickable { showDatePicker = true }
                                    .testTag("valid_until_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = clientMenuExpanded,
                                onExpandedChange = { clientMenuExpanded = !clientMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedClient?.clientName ?: tr(R.string.builder_select_client_hint),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(tr(R.string.builder_client_label)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("quote_client_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = clientMenuExpanded,
                                    onDismissRequest = { clientMenuExpanded = false }
                                ) {
                                    if (clients.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(tr(R.string.builder_no_clients_dropdown)) },
                                            onClick = {
                                                clientMenuExpanded = false
                                                showInlineClientModal = true
                                            }
                                        )
                                    } else {
                                        clients.forEach { c ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(c.clientName, fontWeight = FontWeight.SemiBold)
                                                        if (!c.email.isNullOrBlank()) {
                                                            Text(c.email, style = MaterialTheme.typography.labelSmall, color = Slate600)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    onUpdateBuilder { it.copy(selectedClientId = c.id) }
                                                    clientMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { showInlineClientModal = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("inline_add_client_button")
                            ) {
                                Icon(Icons.Outlined.PersonAdd, contentDescription = tr(R.string.builder_quick_add_client_cd))
                            }
                        }
                    }
                }
            }

            // 2. Line Items Header & Picker Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr(R.string.builder_line_items_count, builder.lineItems.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showLibraryPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("pick_library_item_button")
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr(R.string.builder_catalog_btn))
                        }
                        Button(
                            onClick = onAddBlankRow,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_custom_row_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr(R.string.builder_row_btn))
                        }
                    }
                }
            }

            // 3. Dynamic Line Item Rows
            itemsIndexed(builder.lineItems, key = { _, item -> item.localId }) { index, line ->
                LineItemEditorCard(
                    index = index,
                    totalCount = builder.lineItems.size,
                    currency = workspace.currency,
                    line = line,
                    onChange = { updated ->
                        onUpdateBuilder { state ->
                            state.copy(
                                lineItems = state.lineItems.map { if (it.localId == updated.localId) updated else it }
                            )
                        }
                    },
                    onMoveUp = { onMoveRow(index, -1) },
                    onMoveDown = { onMoveRow(index, 1) },
                    onRemove = { onRemoveRow(line.localId) }
                )
            }

            // 4. Financial Summary Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(tr(R.string.builder_financial_summary), style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = builder.discountText,
                                onValueChange = { v -> onUpdateBuilder { it.copy(discountText = v) } },
                                label = { Text(tr(R.string.builder_discount_label, workspace.currency)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quote_discount_input")
                            )
                            OutlinedTextField(
                                value = builder.taxRateText,
                                onValueChange = { v -> onUpdateBuilder { it.copy(taxRateText = v) } },
                                label = { Text(tr(R.string.builder_tax_percent_label)) },
                                enabled = builder.taxEnabled,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(0.75f)
                                    .testTag("quote_tax_rate_input")
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr(R.string.builder_tax_switch_label), style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Switch(
                                    checked = builder.taxEnabled,
                                    onCheckedChange = { checked -> onUpdateBuilder { it.copy(taxEnabled = checked) } },
                                    modifier = Modifier.testTag("quote_tax_switch")
                                )
                            }
                        }

                        HorizontalDivider(color = Slate100)

                        SummaryRow(tr(R.string.builder_subtotal), Formatters.formatCurrency(builder.subtotal, workspace.currency))
                        if (builder.discountAmount > 0.0) {
                            SummaryRow(
                                tr(R.string.builder_discount),
                                "-${Formatters.formatCurrency(builder.discountAmount, workspace.currency)}"
                            )
                        }
                        SummaryRow(
                            label = if (builder.taxEnabled) {
                                tr(R.string.builder_tax_with_rate, builder.taxRate.toString())
                            } else {
                                tr(R.string.builder_tax_exempt)
                            },
                            value = Formatters.formatCurrency(builder.taxAmount, workspace.currency)
                        )
                        HorizontalDivider(color = Slate200)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(R.string.builder_grand_total),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = Formatters.formatCurrency(builder.grandTotal, workspace.currency),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Slate900
                            )
                        }
                    }
                }
            }

            // 5. Payment Instructions & Terms
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(tr(R.string.builder_payment_terms_title), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = builder.paymentInstructions,
                            onValueChange = { v -> onUpdateBuilder { it.copy(paymentInstructions = v) } },
                            label = { Text(tr(R.string.builder_payment_instructions_label)) },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quote_payment_instructions_input")
                        )
                        OutlinedTextField(
                            value = builder.notes,
                            onValueChange = { v -> onUpdateBuilder { it.copy(notes = v) } },
                            label = { Text(tr(R.string.builder_notes_label)) },
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quote_notes_input")
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onUpdateBuilder { it.copy(validUntil = Formatters.millisToIsoDate(millis)) }
                        }
                        showDatePicker = false
                    }
                ) { Text(tr(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(tr(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLibraryPicker) {
        AlertDialog(
            onDismissRequest = { showLibraryPicker = false },
            title = { Text(tr(R.string.builder_pick_library_title)) },
            text = {
                if (libraryItems.isEmpty()) {
                    Text(
                        tr(R.string.builder_pick_library_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(libraryItems) { _, item ->
                            Surface(
                                onClick = {
                                    onAddFromLibrary(item)
                                    showLibraryPicker = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate200),
                                color = Slate50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.itemName, style = MaterialTheme.typography.titleMedium)
                                        if (!item.description.isNullOrBlank()) {
                                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Slate600, maxLines = 1)
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatCurrency(item.unitPrice, workspace.currency),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryPicker = false }) { Text(tr(R.string.action_close)) }
            }
        )
    }

    if (showInlineClientModal) {
        ClientFormDialog(
            existingClient = null,
            isSaving = isMutatingClient,
            onSave = { name, email, phone, address ->
                onInlineCreateClient(name, email, phone, address) { created ->
                    onUpdateBuilder { it.copy(selectedClientId = created.id) }
                    showInlineClientModal = false
                }
            },
            onDismiss = { showInlineClientModal = false }
        )
    }
}

@Composable
private fun LineItemEditorCard(
    index: Int,
    totalCount: Int,
    currency: String,
    line: EditableLineItem,
    onChange: (EditableLineItem) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Slate100, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tr(R.string.builder_item_index, index + 1),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = Slate600
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = index > 0) {
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = tr(R.string.builder_move_up_cd), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = tr(R.string.builder_move_down_cd), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = tr(R.string.builder_remove_row_cd), tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            OutlinedTextField(
                value = line.itemName,
                onValueChange = { onChange(line.copy(itemName = it)) },
                label = { Text(tr(R.string.builder_item_name_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("line_item_name_$index")
            )

            OutlinedTextField(
                value = line.description,
                onValueChange = { onChange(line.copy(description = it)) },
                label = { Text(tr(R.string.builder_item_desc_label)) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = line.quantity,
                    onValueChange = { onChange(line.copy(quantity = it)) },
                    label = { Text(tr(R.string.builder_qty_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(0.75f)
                        .testTag("line_item_qty_$index")
                )
                OutlinedTextField(
                    value = line.unitPrice,
                    onValueChange = { onChange(line.copy(unitPrice = it)) },
                    label = { Text(tr(R.string.builder_unit_price_label, currency)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("line_item_price_$index")
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr(R.string.builder_line_total_label), style = MaterialTheme.typography.labelSmall, color = Slate600)
                    Text(
                        text = Formatters.formatCurrency(line.totalPriceValue, currency),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate600)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
