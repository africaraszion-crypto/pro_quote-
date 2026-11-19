package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Client
import com.example.data.model.LibraryItem
import com.example.data.model.Workspace
import com.example.ui.components.EmptyStateView
import com.example.ui.i18n.tr
import com.example.ui.theme.Coral600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.util.Formatters

@Composable
fun ClientsScreen(
    clients: List<Client>,
    isLoading: Boolean,
    isMutating: Boolean,
    onSaveClient: (existingId: String?, name: String, email: String, phone: String, address: String) -> Unit,
    onDeleteClient: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Client?>(null) }

    val filtered = remember(clients, search) {
        clients.filter {
            search.isBlank() ||
                it.clientName.contains(search, ignoreCase = true) ||
                it.email.orEmpty().contains(search, ignoreCase = true) ||
                it.phone.orEmpty().contains(search, ignoreCase = true)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tr(R.string.clients_title), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = tr(R.string.clients_count, clients.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_client_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr(R.string.clients_add_btn))
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(tr(R.string.clients_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clients_search_input")
                )
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
                        title = if (clients.isEmpty()) tr(R.string.clients_empty_title) else tr(R.string.clients_empty_filtered_title),
                        subtitle = tr(R.string.clients_empty_subtitle),
                        actionLabel = if (clients.isEmpty()) tr(R.string.clients_empty_cta) else null,
                        onAction = if (clients.isEmpty()) { { showCreateDialog = true } } else null
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { client ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = client.clientName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!client.email.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                                    Text(client.email, style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                }
                            }
                            if (!client.phone.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                                    Text(client.phone, style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                }
                            }
                            if (!client.address.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Slate600, modifier = Modifier.size(14.dp))
                                    Text(
                                        client.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate600,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { editingClient = client }) {
                                Icon(Icons.Outlined.Edit, contentDescription = tr(R.string.client_form_edit_title))
                            }
                            IconButton(onClick = { pendingDelete = client }) {
                                Icon(Icons.Outlined.Delete, contentDescription = tr(R.string.action_delete), tint = Coral600)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ClientFormDialog(
            existingClient = null,
            isSaving = isMutating,
            onSave = { name, email, phone, address ->
                onSaveClient(null, name, email, phone, address)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingClient?.let { c ->
        ClientFormDialog(
            existingClient = c,
            isSaving = isMutating,
            onSave = { name, email, phone, address ->
                onSaveClient(c.id, name, email, phone, address)
                editingClient = null
            },
            onDismiss = { editingClient = null }
        )
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr(R.string.clients_delete_title, c.clientName)) },
            text = { Text(tr(R.string.clients_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClient(c.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral600)
                ) { Text(tr(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(tr(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
fun ClientFormDialog(
    existingClient: Client?,
    isSaving: Boolean,
    onSave: (name: String, email: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(existingClient) { mutableStateOf(existingClient?.clientName.orEmpty()) }
    var email by remember(existingClient) { mutableStateOf(existingClient?.email.orEmpty()) }
    var phone by remember(existingClient) { mutableStateOf(existingClient?.phone.orEmpty()) }
    var address by remember(existingClient) { mutableStateOf(existingClient?.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingClient == null) tr(R.string.client_form_add_title)
                else tr(R.string.client_form_edit_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr(R.string.client_form_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(tr(R.string.client_form_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_email_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(tr(R.string.client_form_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(tr(R.string.client_form_address)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email, phone, address) },
                enabled = !isSaving && name.isNotBlank(),
                modifier = Modifier.testTag("client_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (existingClient == null) tr(R.string.client_form_save)
                        else tr(R.string.client_form_update)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr(R.string.action_cancel)) }
        }
    )
}

@Composable
fun ItemsLibraryScreen(
    workspace: Workspace,
    items: List<LibraryItem>,
    isLoading: Boolean,
    isMutating: Boolean,
    onSaveItem: (existingId: String?, name: String, description: String, unitPrice: Double) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LibraryItem?>(null) }
    var pendingDelete by remember { mutableStateOf<LibraryItem?>(null) }

    val filtered = remember(items, search) {
        items.filter {
            search.isBlank() ||
                it.itemName.contains(search, ignoreCase = true) ||
                it.description.orEmpty().contains(search, ignoreCase = true)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tr(R.string.items_title), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = tr(R.string.items_count, items.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_library_item_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr(R.string.items_add_btn))
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(tr(R.string.items_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("items_search_input")
                )
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
                        title = if (items.isEmpty()) tr(R.string.items_empty_title) else tr(R.string.items_empty_filtered_title),
                        subtitle = tr(R.string.items_empty_subtitle),
                        actionLabel = if (items.isEmpty()) tr(R.string.items_empty_cta) else null,
                        onAction = if (items.isEmpty()) { { showCreateDialog = true } } else null
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = Formatters.formatCurrency(item.unitPrice, workspace.currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Row {
                            IconButton(onClick = { editingItem = item }) {
                                Icon(Icons.Outlined.Edit, contentDescription = tr(R.string.item_form_edit_title))
                            }
                            IconButton(onClick = { pendingDelete = item }) {
                                Icon(Icons.Outlined.Delete, contentDescription = tr(R.string.action_delete), tint = Coral600)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        LibraryItemFormDialog(
            currency = workspace.currency,
            existingItem = null,
            isSaving = isMutating,
            onSave = { name, desc, price ->
                onSaveItem(null, name, desc, price)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingItem?.let { item ->
        LibraryItemFormDialog(
            currency = workspace.currency,
            existingItem = item,
            isSaving = isMutating,
            onSave = { name, desc, price ->
                onSaveItem(item.id, name, desc, price)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr(R.string.items_delete_title, item.itemName)) },
            text = { Text(tr(R.string.items_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(item.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral600)
                ) { Text(tr(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(tr(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun LibraryItemFormDialog(
    currency: String,
    existingItem: LibraryItem?,
    isSaving: Boolean,
    onSave: (name: String, description: String, unitPrice: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(existingItem) { mutableStateOf(existingItem?.itemName.orEmpty()) }
    var description by remember(existingItem) { mutableStateOf(existingItem?.description.orEmpty()) }
    var priceText by remember(existingItem) {
        mutableStateOf(
            existingItem?.unitPrice?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingItem == null) tr(R.string.item_form_add_title)
                else tr(R.string.item_form_edit_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr(R.string.item_form_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_item_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tr(R.string.item_form_desc)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_item_desc_input")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(tr(R.string.item_form_price, currency)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_item_price_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, description, priceText.toDoubleOrNull() ?: 0.0)
                },
                enabled = !isSaving && name.isNotBlank() && (priceText.toDoubleOrNull() != null),
                modifier = Modifier.testTag("library_item_save_button")
            ) {
                Text(
                    if (existingItem == null) tr(R.string.item_form_save)
                    else tr(R.string.item_form_update)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr(R.string.action_cancel)) }
        }
    )
}
