package com.example.ui.viewmodel

import android.app.Application
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.SessionStore
import com.example.data.model.AuthSession
import com.example.data.model.Client
import com.example.data.model.EditableLineItem
import com.example.data.model.LibraryItem
import com.example.data.model.PublicQuoteBundle
import com.example.data.model.Quote
import com.example.data.model.QuoteStatus
import com.example.data.model.SupabaseConfig
import com.example.data.model.Workspace
import com.example.data.remote.SupabaseApiException
import com.example.data.remote.SupabaseClient
import com.example.data.repository.MissingWorkspaceException
import com.example.data.repository.NotAuthenticatedException
import com.example.data.repository.ProQuoteRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.localizedResourcesFor
import com.example.ui.util.Formatters
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthGateState {
    data object Loading : AuthGateState
    data object NeedsSupabaseConfig : AuthGateState
    data object Unauthenticated : AuthGateState
    data object NeedsOnboarding : AuthGateState
    data class Ready(val workspace: Workspace) : AuthGateState
}

sealed interface PublicQuoteUiState {
    data object Idle : PublicQuoteUiState
    data object Loading : PublicQuoteUiState
    data class Loaded(val bundle: PublicQuoteBundle) : PublicQuoteUiState
    data class Error(val message: String) : PublicQuoteUiState
}

data class ToastEvent(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

data class QuoteBuilderState(
    val editingQuoteId: String? = null,
    val quoteNumber: String = "QT-1001",
    val selectedClientId: String? = null,
    val validUntil: String = Formatters.daysFromNowIsoDate(14),
    val lineItems: List<EditableLineItem> = listOf(EditableLineItem(localId = UUID.randomUUID().toString())),
    val discountText: String = "0",
    val taxEnabled: Boolean = true,
    val taxRateText: String = "18.0",
    val notes: String = "",
    val paymentInstructions: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
) {
    val subtotal: Double
        get() = lineItems.sumOf { it.totalPriceValue }
    val discountAmount: Double
        get() = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val taxRate: Double
        get() = if (taxEnabled) (taxRateText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) else 0.0
    val taxableBase: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount: Double
        get() = taxableBase * (taxRate / 100.0)
    val grandTotal: Double
        get() = taxableBase + taxAmount
}

data class DashboardMetrics(
    val totalQuotesSent: Int = 0,
    val acceptedCount: Int = 0,
    val conversionRatePercent: Double = 0.0,
    val acceptedRevenue: Double = 0.0,
    val pendingPipelineValue: Double = 0.0
)

data class ProQuoteUiState(
    val language: AppLanguage = AppLanguage.English,
    val config: SupabaseConfig = SupabaseConfig(
        url = SessionStore.FALLBACK_SUPABASE_URL,
        key = SessionStore.FALLBACK_SUPABASE_KEY
    ),
    val session: AuthSession? = null,
    val authGate: AuthGateState = AuthGateState.Loading,
    val showConnectModal: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionError: String? = null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val authNotice: String? = null,
    val isOnboardingLoading: Boolean = false,
    val onboardingError: String? = null,
    val isDashboardLoading: Boolean = false,
    val clients: List<Client> = emptyList(),
    val libraryItems: List<LibraryItem> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val defaultPaymentInstructions: String = "",
    val isMutatingEntity: Boolean = false,
    val quoteBuilder: QuoteBuilderState = QuoteBuilderState(),
    val magicLinkShareQuote: Quote? = null,
    val publicQuoteState: PublicQuoteUiState = PublicQuoteUiState.Idle,
    val isSubmittingSignature: Boolean = false,
    val toast: ToastEvent? = null
) {
    val workspace: Workspace?
        get() = (authGate as? AuthGateState.Ready)?.workspace

    val metrics: DashboardMetrics
        get() {
            val sentQuotes = quotes.filter {
                it.quoteStatus == QuoteStatus.Sent ||
                    it.quoteStatus == QuoteStatus.Accepted ||
                    it.quoteStatus == QuoteStatus.Declined
            }
            val accepted = quotes.filter { it.quoteStatus == QuoteStatus.Accepted }
            val pending = quotes.filter { it.quoteStatus == QuoteStatus.Sent }
            val rate = if (sentQuotes.isNotEmpty()) {
                (accepted.size.toDouble() / sentQuotes.size.toDouble()) * 100.0
            } else 0.0
            return DashboardMetrics(
                totalQuotesSent = sentQuotes.size,
                acceptedCount = accepted.size,
                conversionRatePercent = rate,
                acceptedRevenue = accepted.sumOf { it.totalAmount },
                pendingPipelineValue = pending.sumOf { it.totalAmount }
            )
        }
}

class ProQuoteViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionStore = SessionStore(application.applicationContext)
    private val supabaseClient = SupabaseClient(sessionStore)
    private val repository = ProQuoteRepository(sessionStore, supabaseClient)

    private val _uiState = MutableStateFlow(ProQuoteUiState())
    val uiState: StateFlow<ProQuoteUiState> = _uiState.asStateFlow()

    private var cachedLocaleLanguage: AppLanguage? = null
    private var cachedLocaleResources: Resources? = null

    init {
        bootstrap()
    }

    private fun resourcesForCurrentLanguage(
        language: AppLanguage = _uiState.value.language
    ): Resources {
        val existing = cachedLocaleResources
        if (existing != null && cachedLocaleLanguage == language) {
            return existing
        }
        val created = localizedResourcesFor(getApplication(), language)
        cachedLocaleLanguage = language
        cachedLocaleResources = created
        return created
    }

    private fun localizedString(@StringRes resId: Int, vararg args: Any): String {
        return resourcesForCurrentLanguage().getString(resId, *args)
    }

    fun setLanguage(language: AppLanguage, showFeedbackToast: Boolean = true) {
        val previous = _uiState.value.language
        if (previous == language) return
        val newRes = resourcesForCurrentLanguage(language)
        _uiState.update {
            it.copy(
                language = language,
                toast = if (showFeedbackToast) {
                    ToastEvent(
                        message = newRes.getString(R.string.language_changed_toast, language.nativeName)
                    )
                } else it.toast
            )
        }
        viewModelScope.launch {
            sessionStore.saveLanguageCode(language.code)
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val language = AppLanguage.fromCode(sessionStore.getLanguageCode())
            val config = repository.getConfig()
            val session = repository.getSession()
            _uiState.update {
                it.copy(
                    language = language,
                    config = config,
                    session = session,
                    showConnectModal = !config.isConfigured
                )
            }
            evaluateAuthGate()
        }
    }

    suspend fun evaluateAuthGate() {
        val config = repository.getConfig()
        val session = repository.getSession()
        if (!config.isConfigured) {
            _uiState.update {
                it.copy(
                    config = config,
                    session = session,
                    authGate = AuthGateState.NeedsSupabaseConfig
                )
            }
            return
        }
        if (session == null) {
            _uiState.update {
                it.copy(
                    config = config,
                    session = null,
                    authGate = AuthGateState.Unauthenticated
                )
            }
            return
        }
        _uiState.update { it.copy(config = config, session = session, authGate = AuthGateState.Loading) }
        val wsResult = repository.fetchWorkspaceForCurrentUser()
        wsResult.fold(
            onSuccess = { ws ->
                if (ws == null) {
                    _uiState.update {
                        it.copy(
                            authGate = AuthGateState.NeedsOnboarding,
                            clients = emptyList(),
                            libraryItems = emptyList(),
                            quotes = emptyList(),
                            defaultPaymentInstructions = "",
                            magicLinkShareQuote = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(authGate = AuthGateState.Ready(ws)) }
                    loadDashboardData(ws.id)
                }
            },
            onFailure = { err ->
                if (isAuthExpiredError(err)) {
                    repository.signOut()
                    _uiState.update {
                        it.copy(
                            session = null,
                            authGate = AuthGateState.Unauthenticated,
                            authError = localizedString(R.string.msg_session_expired)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            authGate = AuthGateState.Unauthenticated,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_unable_verify_session),
                                isError = true
                            )
                        )
                    }
                }
            }
        )
    }

    fun openConnectModal() {
        _uiState.update { it.copy(showConnectModal = true, connectionError = null) }
    }

    fun dismissConnectModal() {
        if (_uiState.value.config.isConfigured) {
            _uiState.update { it.copy(showConnectModal = false, connectionError = null) }
        }
    }

    fun testAndSaveSupabaseConfig(url: String, key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionError = null) }
            val res = repository.testAndSaveSupabaseConfig(url, key)
            res.fold(
                onSuccess = {
                    val newConfig = repository.getConfig()
                    _uiState.update {
                        it.copy(
                            config = newConfig,
                            isTestingConnection = false,
                            showConnectModal = false,
                            connectionError = null,
                            toast = ToastEvent(message = localizedString(R.string.msg_connected_supabase))
                        )
                    }
                    evaluateAuthGate()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionError = err.message ?: localizedString(R.string.msg_connection_failed)
                        )
                    }
                }
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            val res = repository.signIn(email, password)
            res.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            session = session,
                            toast = ToastEvent(message = localizedString(R.string.msg_signed_in_as, session.email))
                        )
                    }
                    evaluateAuthGate()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = err.message ?: localizedString(R.string.msg_sign_in_failed)
                        )
                    }
                }
            )
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authNotice = null) }
            val res = repository.signUp(email, password)
            res.fold(
                onSuccess = { authResp ->
                    if (!authResp.accessToken.isNullOrBlank()) {
                        val session = repository.getSession()
                        _uiState.update {
                            it.copy(
                                isAuthLoading = false,
                                session = session,
                                toast = ToastEvent(message = localizedString(R.string.msg_account_created))
                            )
                        }
                        evaluateAuthGate()
                    } else {
                        _uiState.update {
                            it.copy(
                                isAuthLoading = false,
                                authNotice = localizedString(R.string.msg_confirm_email_sent, email)
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = err.message ?: localizedString(R.string.msg_sign_up_failed)
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(
                    session = null,
                    authGate = if (it.config.isConfigured) AuthGateState.Unauthenticated else AuthGateState.NeedsSupabaseConfig,
                    clients = emptyList(),
                    libraryItems = emptyList(),
                    quotes = emptyList(),
                    toast = ToastEvent(message = localizedString(R.string.msg_signed_out))
                )
            }
        }
    }

    fun clearAuthMessages() {
        _uiState.update { it.copy(authError = null, authNotice = null) }
    }

    // --- Onboarding ---
    fun completeOnboarding(
        companyName: String,
        currency: String,
        taxRate: Double,
        brandColor: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOnboardingLoading = true, onboardingError = null) }
            val res = repository.createWorkspace(
                companyName = companyName,
                currency = currency,
                taxRate = taxRate,
                brandColor = brandColor
            )
            res.fold(
                onSuccess = { ws ->
                    _uiState.update {
                        it.copy(
                            isOnboardingLoading = false,
                            authGate = AuthGateState.Ready(ws),
                            toast = ToastEvent(message = localizedString(R.string.msg_workspace_created, ws.companyName))
                        )
                    }
                    loadDashboardData(ws.id)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOnboardingLoading = false,
                            onboardingError = formatErrorMessage(err, localizedString(R.string.msg_workspace_create_failed))
                        )
                    }
                }
            )
        }
    }

    // --- Dashboard Data ---
    fun refreshDashboard() {
        viewModelScope.launch {
            if (_uiState.value.workspace == null) {
                redirectMissingWorkspaceOrLogin()
                return@launch
            }
            _uiState.update { it.copy(isDashboardLoading = true) }
            repository.verifyWorkspaceForCurrentUser().fold(
                onSuccess = { verifiedWs ->
                    _uiState.update { it.copy(authGate = AuthGateState.Ready(verifiedWs)) }
                    loadDashboardData(verifiedWs.id)
                },
                onFailure = { err ->
                    if (isWorkspaceMissingError(err)) {
                        handleMissingWorkspace()
                    } else if (isAuthExpiredError(err)) {
                        handleExpiredSession()
                    } else {
                        _uiState.update {
                            it.copy(
                                isDashboardLoading = false,
                                toast = ToastEvent(
                                    message = err.message ?: localizedString(R.string.msg_workspace_refresh_failed),
                                    isError = true
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    private fun loadDashboardData(workspaceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDashboardLoading = true) }
            val clientsDeferred = async { repository.fetchClients(workspaceId) }
            val itemsDeferred = async { repository.fetchLibraryItems(workspaceId) }
            val quotesDeferred = async { repository.fetchQuotes(workspaceId) }
            val defaultPayDeferred = async { repository.getDefaultPaymentInstructions(workspaceId) }

            val clientsRes = clientsDeferred.await()
            val itemsRes = itemsDeferred.await()
            val quotesRes = quotesDeferred.await()
            val defaultPay = defaultPayDeferred.await()

            val firstError = clientsRes.exceptionOrNull()
                ?: itemsRes.exceptionOrNull()
                ?: quotesRes.exceptionOrNull()

            if (firstError != null && isWorkspaceMissingError(firstError)) {
                handleMissingWorkspace()
                return@launch
            }

            _uiState.update {
                it.copy(
                    isDashboardLoading = false,
                    clients = clientsRes.getOrDefault(it.clients),
                    libraryItems = itemsRes.getOrDefault(it.libraryItems),
                    quotes = quotesRes.getOrDefault(it.quotes),
                    defaultPaymentInstructions = defaultPay,
                    toast = firstError?.let { err ->
                        ToastEvent(
                            message = err.message ?: localizedString(R.string.msg_workspace_load_error),
                            isError = true
                        )
                    } ?: it.toast
                )
            }
        }
    }

    // --- Clients CRUD ---
    fun saveClient(
        existingId: String?,
        clientName: String,
        email: String,
        phone: String,
        address: String,
        onCreated: ((Client) -> Unit)? = null
    ) {
        val ws = _uiState.value.workspace
        if (ws == null) {
            viewModelScope.launch { redirectMissingWorkspaceOrLogin() }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            val res = if (existingId.isNullOrBlank()) {
                repository.createClient(ws.id, clientName, email, phone, address)
            } else {
                repository.updateClient(existingId, ws.id, clientName, email, phone, address)
            }
            res.fold(
                onSuccess = { saved ->
                    _uiState.update { state ->
                        val updated = if (existingId.isNullOrBlank()) {
                            listOf(saved) + state.clients
                        } else {
                            state.clients.map { if (it.id == saved.id) saved else it }
                        }
                        state.copy(
                            isMutatingEntity = false,
                            clients = updated,
                            toast = ToastEvent(
                                message = if (existingId.isNullOrBlank()) {
                                    localizedString(R.string.msg_client_added)
                                } else {
                                    localizedString(R.string.msg_client_updated)
                                }
                            )
                        )
                    }
                    onCreated?.invoke(saved)
                },
                onFailure = { err ->
                    when {
                        isWorkspaceMissingError(err) -> handleMissingWorkspace()
                        isAuthExpiredError(err) -> handleExpiredSession()
                        else -> _uiState.update {
                            it.copy(
                                isMutatingEntity = false,
                                toast = ToastEvent(
                                    message = formatErrorMessage(err, localizedString(R.string.msg_client_save_failed)),
                                    isError = true
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            repository.deleteClient(clientId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutatingEntity = false,
                            clients = state.clients.filterNot { it.id == clientId },
                            toast = ToastEvent(message = localizedString(R.string.msg_client_deleted))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isMutatingEntity = false,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_client_delete_failed),
                                isError = true
                            )
                        )
                    }
                }
            )
        }
    }

    // --- Items Library CRUD ---
    fun saveLibraryItem(
        existingId: String?,
        itemName: String,
        description: String,
        unitPrice: Double
    ) {
        val ws = _uiState.value.workspace
        if (ws == null) {
            viewModelScope.launch { redirectMissingWorkspaceOrLogin() }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            val res = if (existingId.isNullOrBlank()) {
                repository.createLibraryItem(ws.id, itemName, description, unitPrice)
            } else {
                repository.updateLibraryItem(existingId, ws.id, itemName, description, unitPrice)
            }
            res.fold(
                onSuccess = { saved ->
                    _uiState.update { state ->
                        val updated = if (existingId.isNullOrBlank()) {
                            listOf(saved) + state.libraryItems
                        } else {
                            state.libraryItems.map { if (it.id == saved.id) saved else it }
                        }
                        state.copy(
                            isMutatingEntity = false,
                            libraryItems = updated,
                            toast = ToastEvent(
                                message = if (existingId.isNullOrBlank()) {
                                    localizedString(R.string.msg_item_added)
                                } else {
                                    localizedString(R.string.msg_item_updated)
                                }
                            )
                        )
                    }
                },
                onFailure = { err ->
                    when {
                        isWorkspaceMissingError(err) -> handleMissingWorkspace()
                        isAuthExpiredError(err) -> handleExpiredSession()
                        else -> _uiState.update {
                            it.copy(
                                isMutatingEntity = false,
                                toast = ToastEvent(
                                    message = formatErrorMessage(err, localizedString(R.string.msg_item_save_failed)),
                                    isError = true
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    fun deleteLibraryItem(itemId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            repository.deleteLibraryItem(itemId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutatingEntity = false,
                            libraryItems = state.libraryItems.filterNot { it.id == itemId },
                            toast = ToastEvent(message = localizedString(R.string.msg_item_deleted))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isMutatingEntity = false,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_item_delete_failed),
                                isError = true
                            )
                        )
                    }
                }
            )
        }
    }

    // --- Quote Builder ---
    fun initNewQuoteBuilder() {
        val ws = _uiState.value.workspace
        if (ws == null) {
            viewModelScope.launch { redirectMissingWorkspaceOrLogin() }
            return
        }
        val nextNum = Formatters.nextQuoteNumber(_uiState.value.quotes)
        val taxRate = ws.taxRate
        val defaultNotes = localizedString(R.string.msg_default_quote_notes)
        _uiState.update {
            it.copy(
                quoteBuilder = QuoteBuilderState(
                    editingQuoteId = null,
                    quoteNumber = nextNum,
                    selectedClientId = it.clients.firstOrNull()?.id,
                    validUntil = Formatters.daysFromNowIsoDate(14),
                    lineItems = listOf(EditableLineItem(localId = UUID.randomUUID().toString())),
                    discountText = "0",
                    taxEnabled = taxRate > 0.0,
                    taxRateText = taxRate.toString(),
                    notes = defaultNotes,
                    paymentInstructions = it.defaultPaymentInstructions
                )
            )
        }
    }

    fun loadQuoteForEdit(quoteId: String) {
        val quote = _uiState.value.quotes.firstOrNull { it.id == quoteId } ?: return
        val ws = _uiState.value.workspace
        viewModelScope.launch {
            _uiState.update {
                it.copy(quoteBuilder = it.quoteBuilder.copy(isLoading = true, editingQuoteId = quoteId))
            }
            val itemsRes = repository.fetchQuoteItems(quoteId)
            val loadedItems = itemsRes.getOrDefault(emptyList()).map { item ->
                EditableLineItem(
                    localId = item.id,
                    itemName = item.itemName,
                    description = item.description.orEmpty(),
                    quantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                    unitPrice = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toLong().toString() else item.unitPrice.toString()
                )
            }.ifEmpty {
                listOf(EditableLineItem(localId = UUID.randomUUID().toString()))
            }

            val impliedRate = if (quote.taxAmount > 0.0 && (quote.subtotal - quote.discountAmount) > 0.0) {
                val pct = (quote.taxAmount / (quote.subtotal - quote.discountAmount)) * 100.0
                String.format(java.util.Locale.US, "%.1f", pct)
            } else {
                (ws?.taxRate ?: 18.0).toString()
            }

            _uiState.update {
                it.copy(
                    quoteBuilder = QuoteBuilderState(
                        editingQuoteId = quote.id,
                        quoteNumber = quote.quoteNumber,
                        selectedClientId = quote.clientId,
                        validUntil = quote.validUntil ?: Formatters.daysFromNowIsoDate(14),
                        lineItems = loadedItems,
                        discountText = if (quote.discountAmount == 0.0) "0" else quote.discountAmount.toString(),
                        taxEnabled = quote.taxAmount > 0.0,
                        taxRateText = impliedRate,
                        notes = quote.notes.orEmpty(),
                        paymentInstructions = quote.paymentInstructions.orEmpty(),
                        isLoading = false
                    )
                )
            }
        }
    }

    fun updateBuilder(transform: (QuoteBuilderState) -> QuoteBuilderState) {
        _uiState.update { it.copy(quoteBuilder = transform(it.quoteBuilder)) }
    }

    fun addLineItemFromLibrary(item: LibraryItem) {
        val newLine = EditableLineItem(
            localId = UUID.randomUUID().toString(),
            itemName = item.itemName,
            description = item.description.orEmpty(),
            quantity = "1",
            unitPrice = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toLong().toString() else item.unitPrice.toString()
        )
        _uiState.update { state ->
            val current = state.quoteBuilder.lineItems
            val cleaned = if (current.size == 1 && current.first().itemName.isBlank()) {
                listOf(newLine)
            } else {
                current + newLine
            }
            state.copy(quoteBuilder = state.quoteBuilder.copy(lineItems = cleaned))
        }
    }

    fun addBlankLineItem() {
        _uiState.update { state ->
            state.copy(
                quoteBuilder = state.quoteBuilder.copy(
                    lineItems = state.quoteBuilder.lineItems + EditableLineItem(localId = UUID.randomUUID().toString())
                )
            )
        }
    }

    fun moveLineItem(index: Int, delta: Int) {
        _uiState.update { state ->
            val list = state.quoteBuilder.lineItems.toMutableList()
            val target = index + delta
            if (index in list.indices && target in list.indices) {
                val moved = list.removeAt(index)
                list.add(target, moved)
            }
            state.copy(quoteBuilder = state.quoteBuilder.copy(lineItems = list))
        }
    }

    fun removeLineItem(localId: String) {
        _uiState.update { state ->
            val remaining = state.quoteBuilder.lineItems.filterNot { it.localId == localId }
                .ifEmpty { listOf(EditableLineItem(localId = UUID.randomUUID().toString())) }
            state.copy(quoteBuilder = state.quoteBuilder.copy(lineItems = remaining))
        }
    }

    fun saveQuote(sendMagicLink: Boolean, onSaved: (Quote) -> Unit) {
        val ws = _uiState.value.workspace
        if (ws == null) {
            viewModelScope.launch { redirectMissingWorkspaceOrLogin() }
            return
        }
        val builder = _uiState.value.quoteBuilder
        val validLines = builder.lineItems.filter { it.itemName.isNotBlank() }
        if (validLines.isEmpty()) {
            showToast(localizedString(R.string.msg_add_line_item_required), isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(quoteBuilder = it.quoteBuilder.copy(isSaving = true)) }
            val targetStatus = if (sendMagicLink) "Sent" else "Draft"
            val res = repository.saveQuoteWithItems(
                existingQuoteId = builder.editingQuoteId,
                workspaceId = ws.id,
                clientId = builder.selectedClientId,
                quoteNumber = builder.quoteNumber.ifBlank { Formatters.nextQuoteNumber(_uiState.value.quotes) },
                status = targetStatus,
                validUntil = builder.validUntil,
                notes = builder.notes,
                paymentInstructions = builder.paymentInstructions,
                subtotal = builder.subtotal,
                taxAmount = builder.taxAmount,
                discountAmount = builder.discountAmount,
                totalAmount = builder.grandTotal,
                lineItems = validLines
            )
            res.fold(
                onSuccess = { saved ->
                    _uiState.update { state ->
                        val updatedQuotes = if (builder.editingQuoteId.isNullOrBlank()) {
                            listOf(saved) + state.quotes
                        } else {
                            state.quotes.map { if (it.id == saved.id) saved else it }
                        }
                        state.copy(
                            quotes = updatedQuotes,
                            quoteBuilder = state.quoteBuilder.copy(isSaving = false, editingQuoteId = saved.id),
                            magicLinkShareQuote = if (sendMagicLink) saved else null,
                            toast = ToastEvent(
                                message = if (sendMagicLink) {
                                    localizedString(R.string.msg_quote_sent_toast)
                                } else {
                                    localizedString(R.string.msg_quote_draft_toast)
                                }
                            )
                        )
                    }
                    onSaved(saved)
                },
                onFailure = { err ->
                    when {
                        isWorkspaceMissingError(err) -> handleMissingWorkspace()
                        isAuthExpiredError(err) -> handleExpiredSession()
                        else -> _uiState.update {
                            it.copy(
                                quoteBuilder = it.quoteBuilder.copy(isSaving = false),
                                toast = ToastEvent(
                                    message = formatErrorMessage(err, localizedString(R.string.msg_quote_save_failed)),
                                    isError = true
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            repository.deleteQuote(quoteId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isMutatingEntity = false,
                            quotes = state.quotes.filterNot { it.id == quoteId },
                            toast = ToastEvent(message = localizedString(R.string.msg_quote_deleted))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isMutatingEntity = false,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_quote_delete_failed),
                                isError = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun dismissMagicLinkModal() {
        _uiState.update { it.copy(magicLinkShareQuote = null) }
    }

    fun openMagicLinkModal(quote: Quote) {
        _uiState.update { it.copy(magicLinkShareQuote = quote) }
    }

    // --- Public Magic Link Viewer (/q/:id) ---
    fun loadPublicQuote(quoteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(publicQuoteState = PublicQuoteUiState.Loading) }
            repository.fetchPublicQuoteBundle(quoteId).fold(
                onSuccess = { bundle ->
                    _uiState.update { it.copy(publicQuoteState = PublicQuoteUiState.Loaded(bundle)) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            publicQuoteState = PublicQuoteUiState.Error(
                                err.message ?: localizedString(R.string.msg_public_load_failed)
                            )
                        )
                    }
                }
            )
        }
    }

    fun acceptAndSignPublicQuote(quoteId: String, signatureDataUrl: String, signedByName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingSignature = true) }
            repository.acceptAndSignPublicQuote(quoteId, signatureDataUrl, signedByName).fold(
                onSuccess = { updatedQuote ->
                    _uiState.update { state ->
                        val loaded = (state.publicQuoteState as? PublicQuoteUiState.Loaded)?.bundle
                        val updatedBundle = loaded?.copy(quote = updatedQuote)
                        state.copy(
                            isSubmittingSignature = false,
                            publicQuoteState = updatedBundle?.let { PublicQuoteUiState.Loaded(it) }
                                ?: state.publicQuoteState,
                            quotes = state.quotes.map { if (it.id == updatedQuote.id) updatedQuote else it },
                            toast = ToastEvent(message = localizedString(R.string.msg_quote_signed_toast))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmittingSignature = false,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_signature_failed),
                                isError = true
                            )
                        )
                    }
                }
            )
        }
    }

    fun declinePublicQuote(quoteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingSignature = true) }
            repository.declinePublicQuote(quoteId).fold(
                onSuccess = { updatedQuote ->
                    _uiState.update { state ->
                        val loaded = (state.publicQuoteState as? PublicQuoteUiState.Loaded)?.bundle
                        val updatedBundle = loaded?.copy(quote = updatedQuote)
                        state.copy(
                            isSubmittingSignature = false,
                            publicQuoteState = updatedBundle?.let { PublicQuoteUiState.Loaded(it) }
                                ?: state.publicQuoteState,
                            quotes = state.quotes.map { if (it.id == updatedQuote.id) updatedQuote else it },
                            toast = ToastEvent(message = localizedString(R.string.msg_quote_declined_toast))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmittingSignature = false,
                            toast = ToastEvent(
                                message = err.message ?: localizedString(R.string.msg_quote_decline_failed),
                                isError = true
                            )
                        )
                    }
                }
            )
        }
    }

    // --- Workspace Settings ---
    fun updateWorkspaceSettings(
        companyName: String,
        logoUrl: String,
        brandColor: String,
        currency: String,
        taxRate: Double,
        defaultPaymentInstructions: String
    ) {
        val ws = _uiState.value.workspace
        if (ws == null) {
            viewModelScope.launch { redirectMissingWorkspaceOrLogin() }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingEntity = true) }
            repository.updateWorkspace(
                workspaceId = ws.id,
                companyName = companyName,
                logoUrl = logoUrl,
                brandColor = brandColor,
                currency = currency,
                taxRate = taxRate,
                defaultPaymentInstructions = defaultPaymentInstructions
            ).fold(
                onSuccess = { updatedWs ->
                    _uiState.update {
                        it.copy(
                            isMutatingEntity = false,
                            authGate = AuthGateState.Ready(updatedWs),
                            defaultPaymentInstructions = defaultPaymentInstructions,
                            toast = ToastEvent(message = localizedString(R.string.msg_settings_saved))
                        )
                    }
                },
                onFailure = { err ->
                    when {
                        isWorkspaceMissingError(err) -> handleMissingWorkspace()
                        isAuthExpiredError(err) -> handleExpiredSession()
                        else -> _uiState.update {
                            it.copy(
                                isMutatingEntity = false,
                                toast = ToastEvent(
                                    message = formatErrorMessage(err, localizedString(R.string.msg_settings_save_failed)),
                                    isError = true
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    private suspend fun redirectMissingWorkspaceOrLogin() {
        val session = repository.getSession()
        if (session == null || session.accessToken.isBlank() || session.userId.isBlank()) {
            handleExpiredSession()
        } else {
            handleMissingWorkspace()
        }
    }

    private fun handleMissingWorkspace(
        message: String = localizedString(R.string.msg_missing_workspace)
    ) {
        _uiState.update {
            it.copy(
                authGate = AuthGateState.NeedsOnboarding,
                isDashboardLoading = false,
                isMutatingEntity = false,
                quoteBuilder = it.quoteBuilder.copy(isSaving = false, isLoading = false),
                clients = emptyList(),
                libraryItems = emptyList(),
                quotes = emptyList(),
                defaultPaymentInstructions = "",
                magicLinkShareQuote = null,
                toast = ToastEvent(message = message, isError = false)
            )
        }
    }

    private fun handleExpiredSession() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(
                    session = null,
                    authGate = if (it.config.isConfigured) AuthGateState.Unauthenticated else AuthGateState.NeedsSupabaseConfig,
                    isDashboardLoading = false,
                    isMutatingEntity = false,
                    quoteBuilder = it.quoteBuilder.copy(isSaving = false, isLoading = false),
                    clients = emptyList(),
                    libraryItems = emptyList(),
                    quotes = emptyList(),
                    defaultPaymentInstructions = "",
                    magicLinkShareQuote = null,
                    authError = localizedString(R.string.msg_session_expired)
                )
            }
        }
    }

    private fun isWorkspaceMissingError(err: Throwable): Boolean {
        return err is MissingWorkspaceException
    }

    private fun formatErrorMessage(err: Throwable, fallback: String): String {
        if (err is SupabaseApiException && !err.details.isNullOrBlank()) {
            return "${err.message}: ${err.details}"
        }
        return err.message ?: fallback
    }

    private fun isAuthExpiredError(err: Throwable): Boolean {
        if (err is NotAuthenticatedException) return true
        if (err is SupabaseApiException && err.statusCode == 401) return true
        val msg = err.message.orEmpty()
        return msg.contains("JWT", ignoreCase = true) || msg.contains("401")
    }

    fun showToast(message: String, isError: Boolean = false) {
        _uiState.update { it.copy(toast = ToastEvent(message = message, isError = isError)) }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
