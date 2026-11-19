package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ConnectSupabaseDialog
import com.example.ui.components.MagicLinkShareDialog
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppResources
import com.example.ui.i18n.localizedResourcesFor
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardShell
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.ItemsLibraryScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PublicQuoteScreen
import com.example.ui.screens.QuoteBuilderScreen
import com.example.ui.screens.QuotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.Coral600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.ProQuoteTheme
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AuthGateState
import com.example.ui.viewmodel.ProQuoteViewModel
import kotlinx.coroutines.delay

object Routes {
    const val LANDING = "landing"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val QUOTE_NEW = "dashboard/quotes/new"
    const val QUOTE_EDIT = "dashboard/quotes/edit/{quoteId}"
    const val PUBLIC_QUOTE = "q/{quoteId}"

    fun quoteEdit(quoteId: String) = "dashboard/quotes/edit/$quoteId"
    fun publicQuote(quoteId: String) = "q/$quoteId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkQuoteId = extractMagicLinkQuoteId(intent)

        setContent {
            val viewModel: ProQuoteViewModel = viewModel()
            ProQuoteApp(
                viewModel = viewModel,
                initialPublicQuoteId = deepLinkQuoteId
            )
        }
    }

    private fun extractMagicLinkQuoteId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "proquote" && data.host == "q") {
            return data.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
        }
        val path = data.path ?: return null
        if (path.contains("/q/")) {
            return path.substringAfterLast("/q/").takeIf { it.isNotBlank() }
        }
        return null
    }
}

@Composable
private fun ProQuoteApp(
    viewModel: ProQuoteViewModel,
    initialPublicQuoteId: String?
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var activeTab by rememberSaveable { mutableStateOf(DashboardTab.Overview) }
    var handledDeepLink by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Deep link handling (/q/:id) without requiring auth
    LaunchedEffect(initialPublicQuoteId) {
        if (!handledDeepLink && !initialPublicQuoteId.isNullOrBlank()) {
            handledDeepLink = true
            navController.navigate(Routes.publicQuote(initialPublicQuoteId))
        }
    }

    // Route Guard Logic
    LaunchedEffect(state.authGate, currentRoute) {
        if (currentRoute == null || currentRoute.startsWith("q/")) {
            return@LaunchedEffect
        }
        when (state.authGate) {
            AuthGateState.Loading -> Unit
            AuthGateState.NeedsSupabaseConfig,
            AuthGateState.Unauthenticated -> {
                if (currentRoute.startsWith("dashboard") || currentRoute == Routes.ONBOARDING) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            AuthGateState.NeedsOnboarding -> {
                if (currentRoute != Routes.ONBOARDING) {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is AuthGateState.Ready -> {
                if (currentRoute == Routes.LANDING ||
                    currentRoute == Routes.LOGIN ||
                    currentRoute == Routes.SIGNUP ||
                    currentRoute == Routes.ONBOARDING
                ) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // Auto-dismiss Toast notifications
    LaunchedEffect(state.toast?.id) {
        if (state.toast != null) {
            delay(3200)
            viewModel.dismissToast()
        }
    }

    val context = LocalContext.current
    val appResources = remember(context, state.language) {
        localizedResourcesFor(context, state.language)
    }

    CompositionLocalProvider(
        LocalAppLanguage provides state.language,
        LocalAppResources provides appResources
    ) {
        ProQuoteTheme(brandColorHex = state.workspace?.brandColor) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.LANDING
                ) {
                    composable(Routes.LANDING) {
                        if (state.authGate == AuthGateState.Loading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LandingScreen(
                                isSupabaseConnected = state.config.isConfigured,
                                onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                                onGetStartedClick = {
                                    viewModel.clearAuthMessages()
                                    navController.navigate(Routes.SIGNUP)
                                },
                                onSignInClick = {
                                    viewModel.clearAuthMessages()
                                    navController.navigate(Routes.LOGIN)
                                },
                                onConfigureSupabaseClick = { viewModel.openConnectModal() },
                                onOpenPublicQuote = { id ->
                                    navController.navigate(Routes.publicQuote(id))
                                }
                            )
                        }
                    }

                    composable(Routes.LOGIN) {
                        AuthScreen(
                            isSignUp = false,
                            isLoading = state.isAuthLoading,
                            errorMessage = state.authError,
                            noticeMessage = state.authNotice,
                            onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                            onToggleMode = {
                                viewModel.clearAuthMessages()
                                navController.navigate(Routes.SIGNUP) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            },
                            onSubmit = { email, password -> viewModel.signIn(email, password) },
                            onBackToLanding = {
                                navController.navigate(Routes.LANDING) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onConfigureSupabase = { viewModel.openConnectModal() }
                        )
                    }

                    composable(Routes.SIGNUP) {
                        AuthScreen(
                            isSignUp = true,
                            isLoading = state.isAuthLoading,
                            errorMessage = state.authError,
                            noticeMessage = state.authNotice,
                            onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                            onToggleMode = {
                                viewModel.clearAuthMessages()
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.SIGNUP) { inclusive = true }
                                }
                            },
                            onSubmit = { email, password -> viewModel.signUp(email, password) },
                            onBackToLanding = {
                                navController.navigate(Routes.LANDING) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onConfigureSupabase = { viewModel.openConnectModal() }
                        )
                    }

                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            userEmail = state.session?.email.orEmpty(),
                            isLoading = state.isOnboardingLoading,
                            errorMessage = state.onboardingError,
                            onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                            onCompleteOnboarding = { companyName, currency, taxRate, brandColor ->
                                viewModel.completeOnboarding(companyName, currency, taxRate, brandColor)
                            },
                            onSignOut = { viewModel.signOut() }
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        val workspace = state.workspace
                        if (workspace == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            DashboardShell(
                                workspace = workspace,
                                userEmail = state.session?.email.orEmpty(),
                                activeTab = activeTab,
                                onSelectTab = { activeTab = it },
                                onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                                onNewQuoteClick = {
                                    viewModel.initNewQuoteBuilder()
                                    navController.navigate(Routes.QUOTE_NEW)
                                },
                                onRefreshClick = { viewModel.refreshDashboard() },
                                onOpenMagicLinkLookup = { id ->
                                    navController.navigate(Routes.publicQuote(id))
                                },
                                onConfigureSupabaseClick = { viewModel.openConnectModal() },
                                onSignOutClick = { viewModel.signOut() }
                            ) { contentModifier ->
                        Box(modifier = contentModifier) {
                            when (activeTab) {
                                DashboardTab.Overview -> OverviewScreen(
                                    workspace = workspace,
                                    metrics = state.metrics,
                                    quotes = state.quotes,
                                    clients = state.clients,
                                    isLoading = state.isDashboardLoading,
                                    onNewQuote = {
                                        viewModel.initNewQuoteBuilder()
                                        navController.navigate(Routes.QUOTE_NEW)
                                    },
                                    onViewPublicQuote = { id ->
                                        navController.navigate(Routes.publicQuote(id))
                                    },
                                    onEditQuote = { id ->
                                        viewModel.loadQuoteForEdit(id)
                                        navController.navigate(Routes.quoteEdit(id))
                                    },
                                    onCopiedLinkToast = { msg -> viewModel.showToast(msg) },
                                    onViewAllQuotes = { activeTab = DashboardTab.Quotes }
                                )

                                DashboardTab.Quotes -> QuotesScreen(
                                    workspace = workspace,
                                    quotes = state.quotes,
                                    clients = state.clients,
                                    isLoading = state.isDashboardLoading,
                                    onNewQuote = {
                                        viewModel.initNewQuoteBuilder()
                                        navController.navigate(Routes.QUOTE_NEW)
                                    },
                                    onViewPublicQuote = { id ->
                                        navController.navigate(Routes.publicQuote(id))
                                    },
                                    onEditQuote = { id ->
                                        viewModel.loadQuoteForEdit(id)
                                        navController.navigate(Routes.quoteEdit(id))
                                    },
                                    onDeleteQuote = { id -> viewModel.deleteQuote(id) },
                                    onCopiedLinkToast = { msg -> viewModel.showToast(msg) }
                                )

                                DashboardTab.Clients -> ClientsScreen(
                                    clients = state.clients,
                                    isLoading = state.isDashboardLoading,
                                    isMutating = state.isMutatingEntity,
                                    onSaveClient = { id, name, email, phone, address ->
                                        viewModel.saveClient(id, name, email, phone, address)
                                    },
                                    onDeleteClient = { id -> viewModel.deleteClient(id) }
                                )

                                DashboardTab.Items -> ItemsLibraryScreen(
                                    workspace = workspace,
                                    items = state.libraryItems,
                                    isLoading = state.isDashboardLoading,
                                    isMutating = state.isMutatingEntity,
                                    onSaveItem = { id, name, desc, price ->
                                        viewModel.saveLibraryItem(id, name, desc, price)
                                    },
                                    onDeleteItem = { id -> viewModel.deleteLibraryItem(id) }
                                )

                                DashboardTab.Settings -> SettingsScreen(
                                    workspace = workspace,
                                    defaultPaymentInstructions = state.defaultPaymentInstructions,
                                    supabaseConfig = state.config,
                                    isSaving = state.isMutatingEntity,
                                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                                    onSaveSettings = { companyName, logoUrl, brandColor, currency, taxRate, payDefaults ->
                                        viewModel.updateWorkspaceSettings(
                                            companyName, logoUrl, brandColor, currency, taxRate, payDefaults
                                        )
                                    },
                                    onConfigureSupabase = { viewModel.openConnectModal() }
                                )
                            }
                        }
                    }
                }
            }

            composable(Routes.QUOTE_NEW) {
                val workspace = state.workspace
                if (workspace == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    QuoteBuilderScreen(
                        workspace = workspace,
                        builder = state.quoteBuilder,
                        clients = state.clients,
                        libraryItems = state.libraryItems,
                        isMutatingClient = state.isMutatingEntity,
                        onUpdateBuilder = { transform -> viewModel.updateBuilder(transform) },
                        onAddBlankRow = { viewModel.addBlankLineItem() },
                        onAddFromLibrary = { item -> viewModel.addLineItemFromLibrary(item) },
                        onMoveRow = { index, delta -> viewModel.moveLineItem(index, delta) },
                        onRemoveRow = { id -> viewModel.removeLineItem(id) },
                        onInlineCreateClient = { name, email, phone, address, onDone ->
                            viewModel.saveClient(null, name, email, phone, address, onDone)
                        },
                        onSaveDraft = {
                            viewModel.saveQuote(sendMagicLink = false) {
                                navController.popBackStack()
                            }
                        },
                        onSaveAndGetMagicLink = {
                            viewModel.saveQuote(sendMagicLink = true) {}
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Routes.QUOTE_EDIT,
                arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
            ) {
                val workspace = state.workspace
                if (workspace == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    QuoteBuilderScreen(
                        workspace = workspace,
                        builder = state.quoteBuilder,
                        clients = state.clients,
                        libraryItems = state.libraryItems,
                        isMutatingClient = state.isMutatingEntity,
                        onUpdateBuilder = { transform -> viewModel.updateBuilder(transform) },
                        onAddBlankRow = { viewModel.addBlankLineItem() },
                        onAddFromLibrary = { item -> viewModel.addLineItemFromLibrary(item) },
                        onMoveRow = { index, delta -> viewModel.moveLineItem(index, delta) },
                        onRemoveRow = { id -> viewModel.removeLineItem(id) },
                        onInlineCreateClient = { name, email, phone, address, onDone ->
                            viewModel.saveClient(null, name, email, phone, address, onDone)
                        },
                        onSaveDraft = {
                            viewModel.saveQuote(sendMagicLink = false) {
                                navController.popBackStack()
                            }
                        },
                        onSaveAndGetMagicLink = {
                            viewModel.saveQuote(sendMagicLink = true) {}
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Routes.PUBLIC_QUOTE,
                arguments = listOf(navArgument("quoteId") { type = NavType.StringType })
            ) { entry ->
                val quoteId = entry.arguments?.getString("quoteId").orEmpty()
                PublicQuoteScreen(
                    quoteId = quoteId,
                    state = state.publicQuoteState,
                    isSubmittingSignature = state.isSubmittingSignature,
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onLoadQuote = { id -> viewModel.loadPublicQuote(id) },
                    onAcceptAndSign = { id, sig, name -> viewModel.acceptAndSignPublicQuote(id, sig, name) },
                    onDecline = { id -> viewModel.declinePublicQuote(id) },
                    onValidationToast = { msg -> viewModel.showToast(msg, isError = true) },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.LANDING) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        // Sleek "Connect Supabase" Setup Modal
        if (state.showConnectModal) {
            ConnectSupabaseDialog(
                initialUrl = state.config.url,
                initialKey = state.config.key,
                isLoading = state.isTestingConnection,
                errorMessage = state.connectionError,
                onTestAndSave = { url, key -> viewModel.testAndSaveSupabaseConfig(url, key) },
                onDismiss = if (state.config.isConfigured) {
                    { viewModel.dismissConnectModal() }
                } else null
            )
        }

        // Magic Link Share Modal after "Save & Get Magic Link"
        state.magicLinkShareQuote?.let { sentQuote ->
            MagicLinkShareDialog(
                quoteId = sentQuote.id,
                quoteNumber = sentQuote.quoteNumber,
                onPreviewPublicQuote = {
                    viewModel.dismissMagicLinkModal()
                    navController.navigate(Routes.publicQuote(sentQuote.id)) {
                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                    }
                },
                onDismiss = {
                    viewModel.dismissMagicLinkModal()
                    if (currentRoute == Routes.QUOTE_NEW || currentRoute == Routes.QUOTE_EDIT) {
                        navController.popBackStack()
                    }
                }
            )
        }

        // Floating Toast Notification Banner
        AnimatedVisibility(
            visible = state.toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            state.toast?.let { toast ->
                Surface(
                    color = if (toast.isError) Coral600 else Slate900,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (toast.isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (toast.isError) Color.White else Emerald600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = toast.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
}
}
