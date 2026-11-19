package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PublicQuoteBundle
import com.example.data.model.QuoteStatus
import com.example.ui.components.Base64SignaturePreview
import com.example.ui.components.LanguagePillSwitch
import com.example.ui.components.SignaturePad
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberSignaturePadState
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppResources
import com.example.ui.i18n.tr
import com.example.ui.theme.Coral50
import com.example.ui.theme.Coral600
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.parseBrandColor
import com.example.ui.util.Formatters
import com.example.ui.util.QuotePdfPrinter
import com.example.ui.viewmodel.PublicQuoteUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicQuoteScreen(
    quoteId: String,
    state: PublicQuoteUiState,
    isSubmittingSignature: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onLoadQuote: (String) -> Unit,
    onAcceptAndSign: (quoteId: String, signatureDataUrl: String, signedByName: String) -> Unit,
    onDecline: (quoteId: String) -> Unit,
    onValidationToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activeLanguage = LocalAppLanguage.current

    LaunchedEffect(quoteId) {
        onLoadQuote(quoteId)
    }

    val loadedBundle = (state as? PublicQuoteUiState.Loaded)?.bundle
    val brandColor = parseBrandColor(loadedBundle?.workspace?.brandColor)

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
                                text = loadedBundle?.quote?.quoteNumber ?: tr(R.string.public_fallback_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                            Text(
                                text = "/q/$quoteId",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        LanguagePillSwitch(
                            onSelectLanguage = onSelectLanguage,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        if (loadedBundle != null) {
                            Button(
                                onClick = { QuotePdfPrinter.printQuote(context, loadedBundle, activeLanguage) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandColor,
                                    contentColor = contrastOnColor(brandColor)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("print_pdf_button")
                            ) {
                                Icon(Icons.Outlined.Print, contentDescription = tr(R.string.public_print_pdf_cd), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr(R.string.public_print_pdf))
                            }
                        }
                    }
                )
                HorizontalDivider(color = Slate200)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (state) {
                PublicQuoteUiState.Idle,
                PublicQuoteUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(tr(R.string.public_loading), style = MaterialTheme.typography.bodyMedium, color = Slate600)
                    }
                }

                is PublicQuoteUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tr(R.string.public_error_title), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = Slate600, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onLoadQuote(quoteId) }) { Text(tr(R.string.action_retry)) }
                    }
                }

                is PublicQuoteUiState.Loaded -> {
                    PublicQuoteDocumentContent(
                        bundle = state.bundle,
                        brandColor = brandColor,
                        isSubmittingSignature = isSubmittingSignature,
                        onAcceptAndSign = onAcceptAndSign,
                        onDecline = onDecline,
                        onValidationToast = onValidationToast
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicQuoteDocumentContent(
    bundle: PublicQuoteBundle,
    brandColor: Color,
    isSubmittingSignature: Boolean,
    onAcceptAndSign: (quoteId: String, signatureDataUrl: String, signedByName: String) -> Unit,
    onDecline: (quoteId: String) -> Unit,
    onValidationToast: (String) -> Unit
) {
    val resources = LocalAppResources.current
    val quote = bundle.quote
    val workspace = bundle.workspace
    val client = bundle.client
    val currency = workspace?.currency ?: "TZS"
    val signaturePadState = rememberSignaturePadState()
    var signerName by remember(quote.id) { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Document Card
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate200),
                shadowElevation = 2.dp
            ) {
                Column {
                    // Brand Accent Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(brandColor)
                    )

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Header Row: Workspace Branding + Quote Number & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!workspace?.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = workspace?.logoUrl,
                                        contentDescription = "${workspace?.companyName} Logo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                                Text(
                                    text = workspace?.companyName ?: "Pro Quote",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = brandColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = tr(R.string.public_doc_heading),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate500
                                )
                                Text(
                                    text = quote.quoteNumber,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                StatusBadge(status = quote.quoteStatus)
                            }
                        }

                        // Client & Date Metadata Card
                        Surface(
                            color = Slate50,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(tr(R.string.public_prepared_for), style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(
                                        text = client?.clientName ?: tr(R.string.label_client_fallback),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (!client?.email.isNullOrBlank()) {
                                        Text(client?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                    }
                                    if (!client?.phone.isNullOrBlank()) {
                                        Text(client?.phone.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                    }
                                    if (!client?.address.isNullOrBlank()) {
                                        Text(client?.address.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(tr(R.string.public_quote_dates), style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(
                                        text = tr(R.string.public_issued_on, Formatters.formatDisplayDate(quote.createdAt)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = tr(R.string.public_valid_until_on, Formatters.formatDisplayDate(quote.validUntil)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Line Items Table
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tr(R.string.public_col_item), style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.weight(1.8f))
                                Text(tr(R.string.public_col_qty), style = MaterialTheme.typography.labelSmall, color = Slate500, textAlign = TextAlign.End, modifier = Modifier.weight(0.5f))
                                Text(tr(R.string.public_col_total), style = MaterialTheme.typography.labelSmall, color = Slate500, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = Slate200)

                            bundle.items.forEach { item ->
                                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(item.itemName, style = MaterialTheme.typography.titleMedium)
                                        if (!item.description.isNullOrBlank()) {
                                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Slate600)
                                        }
                                        Text(
                                            text = tr(R.string.public_price_each, Formatters.formatCurrency(item.unitPrice, currency)),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = Slate500
                                        )
                                    }
                                    Text(
                                        text = qtyStr,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.5f)
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(item.totalPrice, currency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = Slate100)
                            }
                        }

                        // Totals Breakdown
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DocTotalRow(tr(R.string.builder_subtotal), Formatters.formatCurrency(quote.subtotal, currency))
                            if (quote.discountAmount > 0.0) {
                                DocTotalRow(tr(R.string.builder_discount), "-${Formatters.formatCurrency(quote.discountAmount, currency)}")
                            }
                            DocTotalRow(tr(R.string.builder_tax_switch_label), Formatters.formatCurrency(quote.taxAmount, currency))
                            HorizontalDivider(color = brandColor, thickness = 2.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr(R.string.public_total_amount), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = Formatters.formatCurrency(quote.totalAmount, currency),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = brandColor
                                )
                            }
                        }

                        if (!quote.paymentInstructions.isNullOrBlank()) {
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(tr(R.string.public_payment_instructions), style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(quote.paymentInstructions, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (!quote.notes.isNullOrBlank()) {
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(tr(R.string.public_terms_conditions), style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(quote.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // E-Signature / Acceptance Certificate Section
        item {
            when (quote.quoteStatus) {
                QuoteStatus.Accepted -> {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Emerald600.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accepted_certificate_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Verified,
                                    contentDescription = tr(R.string.public_accepted_title),
                                    tint = Emerald600
                                )
                                Text(
                                    text = tr(R.string.public_accepted_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Emerald600
                                )
                            }
                            if (!quote.signatureDataUrl.isNullOrBlank()) {
                                Base64SignaturePreview(dataUrl = quote.signatureDataUrl)
                            }
                            Text(
                                text = tr(
                                    R.string.public_signed_by_on,
                                    quote.signedByName ?: tr(R.string.label_client_fallback),
                                    Formatters.formatDisplayDateTime(quote.signedAt)
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Slate900
                            )
                        }
                    }
                }

                QuoteStatus.Declined -> {
                    Surface(
                        color = Coral50,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Coral600.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null, tint = Coral600)
                            Column {
                                Text(tr(R.string.public_declined_title), style = MaterialTheme.typography.titleMedium, color = Coral600)
                                Text(tr(R.string.public_declined_desc), style = MaterialTheme.typography.bodyMedium, color = Slate600)
                            }
                        }
                    }
                }

                QuoteStatus.Draft,
                QuoteStatus.Sent -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = tr(R.string.public_esign_title),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = tr(R.string.public_esign_desc, quote.quoteNumber),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600
                            )

                            SignaturePad(state = signaturePadState)

                            OutlinedTextField(
                                value = signerName,
                                onValueChange = { signerName = it },
                                label = { Text(tr(R.string.public_signer_name_label)) },
                                placeholder = { Text(tr(R.string.public_signer_name_hint)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signer_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDecline(quote.id) },
                                    enabled = !isSubmittingSignature,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral600),
                                    border = BorderStroke(1.dp, Coral600.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("decline_quote_button")
                                ) {
                                    Text(tr(R.string.public_decline_btn))
                                }
                                Button(
                                    onClick = {
                                        val dataUrl = signaturePadState.exportToBase64Png()
                                        when {
                                            dataUrl == null -> onValidationToast(resources.getString(R.string.public_draw_signature_prompt))
                                            signerName.isBlank() -> onValidationToast(resources.getString(R.string.public_enter_signer_prompt))
                                            else -> onAcceptAndSign(quote.id, dataUrl, signerName)
                                        }
                                    },
                                    enabled = !isSubmittingSignature,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(48.dp)
                                        .testTag("accept_sign_quote_button")
                                ) {
                                    if (isSubmittingSignature) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr(R.string.public_accept_sign_btn))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocTotalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Slate600)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
