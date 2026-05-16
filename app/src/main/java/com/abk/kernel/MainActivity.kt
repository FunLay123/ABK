package com.abk.kernel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abk.kernel.ui.screens.AuthGateScreen
import com.abk.kernel.ui.screens.BuildScreen
import com.abk.kernel.ui.screens.FlashScreen
import com.abk.kernel.ui.screens.InstalledModulesScreen
import com.abk.kernel.ui.screens.ModuleRepositoryScreen
import com.abk.kernel.ui.screens.RootAuthorizationScreen
import com.abk.kernel.ui.screens.RuntimeHomeScreen
import com.abk.kernel.ui.screens.SettingsScreen
import com.abk.kernel.ui.screens.StatusScreen
import com.abk.kernel.ui.theme.AbkTheme
import com.abk.kernel.ui.theme.LocalUiSurfaceAlpha
import com.abk.kernel.ui.theme.uiSurfaceColor
import com.abk.kernel.viewmodel.AuthStep
import com.abk.kernel.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private var pendingModuleInstallUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingModuleInstallUri = extractModuleInstallUri(intent)?.toString()

        setContent {
            val vm: MainViewModel = viewModel()
            val state by vm.uiState.collectAsState()

            LaunchedEffect(state.termsAccepted) {
                if (state.termsAccepted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AbkTheme(
                themeMode = state.themeMode,
                dynamicColorEnabled = state.dynamicColorEnabled,
                customThemeColorArgb = state.customThemeColorArgb,
                customAccentColorArgb = state.customAccentColorArgb
            ) {
                AppBackgroundHost(
                    backgroundUri = state.customBackgroundUri,
                    backgroundEnabled = state.backgroundImageEnabled,
                    uiSurfaceAlpha = state.uiSurfaceAlpha
                ) {
                    when {
                        !state.termsLoaded -> Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {}
                        !state.termsAccepted -> TermsAgreementDialog(
                            onAccept = vm::acceptTerms,
                            onDecline = { finishAffinity() }
                        )
                        state.authStep != AuthStep.READY -> AuthGateScreen(vm)
                        else -> AbkMainScaffold(
                            vm = vm,
                            pendingModuleInstallUri = pendingModuleInstallUri,
                            onModuleInstallUriConsumed = { pendingModuleInstallUri = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingModuleInstallUri = extractModuleInstallUri(intent)?.toString()
    }
}

@Composable
private fun AppBackgroundHost(
    backgroundUri: String?,
    backgroundEnabled: Boolean,
    uiSurfaceAlpha: Float,
    content: @Composable () -> Unit
) {
    val hasBackground = backgroundEnabled && !backgroundUri.isNullOrBlank()
    val colorScheme = MaterialTheme.colorScheme
    val scrimColor = if (colorScheme.surface.luminance() > 0.5f) {
        colorScheme.surface.copy(alpha = 0.28f)
    } else {
        Color.Black.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        if (hasBackground) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
            )
        }
        CompositionLocalProvider(
            LocalUiSurfaceAlpha provides if (hasBackground) uiSurfaceAlpha.coerceIn(0f, 1f) else 1f
        ) {
            content()
        }
    }
}

@Composable
private fun TermsAgreementDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val scrollState = rememberScrollState()
    val canAccept by remember {
        derivedStateOf { scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue }
    }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "ABK Terms of Use & Disclaimer",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TermsText("Version: 1")
                TermsText("Effective date: 2026-05-07")
                TermsText("Please read this agreement in full. By clicking \"Agree and Continue\" you confirm that you have read and accepted all terms. If you do not agree, click \"Decline and Exit\".")

                TermsSection(
                    "1. Purpose",
                    "ABK is used to trigger GitHub Actions builds, download, flash, or install GKI KernelSU / SUSFS artifacts, and provides Root checking, GitHub authorization, fork checking, custom external module injection, build progress sync, and artifact management.",
                    "ABK is intended for learning, research, personal use, and debugging on lawfully authorized devices. No guarantees are made regarding compatibility, successful boot, successful flashing, Root availability, detection bypass, or long-term stability."
                )
                TermsSection(
                    "2. High-Risk Operation Warning",
                    "Building, modifying, or flashing boot, init_boot, vendor_boot, kernel images, AnyKernel3 packages, or installing low-level modules are high-risk operations that may result in bootloop, boot failure, data corruption, partition errors, unavailable system services, or voided warranty.",
                    "In the event of an issue, you may need to restore official images, re-flash your device, wipe data, or unlock/re-lock the bootloader. All associated costs and consequences are your sole responsibility.",
                    "Do not proceed with building or flashing if you are unsure about your device model, partition layout, Android version, kernel version, security patch level, KMI compatibility, or recovery plan."
                )
                TermsSection(
                    "3. Lawful Use Restrictions",
                    "You may only use ABK on devices, accounts, repositories, and network environments that you own or have explicit authorization for.",
                    "It is prohibited to use ABK, its workflows, patches, custom external modules, or build artifacts for grey/black market activities, unauthorized access, bypassing risk controls, cheating, malicious concealment, data theft, service disruption, audit evasion, mass abuse, infringement of others' rights, or any other illegal or prohibited purposes.",
                    "You are responsible for ensuring compliance with applicable laws and regulations, platform rules, device manufacturer terms, and upstream project license requirements."
                )
                TermsSection(
                    "4. Third-Party Projects & External Modules",
                    "ABK aggregates multiple third-party projects, patches, scripts, and download sources. The code, licenses, stability, security, and compatibility of third-party components are the responsibility of their respective upstream maintainers.",
                    "Enabling custom external modules will clone an external repository and execute the setup.sh in its root directory. Before enabling, you should review the script contents, commit history, source trustworthiness, and permission implications.",
                    "External modules may modify kernel source code, defconfig, build scripts, or artifact contents. Any build failures, device issues, security risks, or compliance problems caused by external modules are the responsibility of the enabler and module provider."
                )
                TermsSection(
                    "5. Authorization, Privacy & Account Risk",
                    "ABK uses GitHub Device Flow to obtain an authorization token for checking or managing forks, triggering Actions, reading build status, and downloading artifacts. You should understand the authorization scope and manage your GitHub account security accordingly.",
                    "ABK may request Root access when flashing, installing modules, or identifying the local device state. Granting Root increases system risk; you should verify the necessity of each operation yourself.",
                    "You must not expose tokens, keys, private data, sensitive device information, or non-public build artifacts in custom modules, public repositories, logs, or issues."
                )
                TermsSection(
                    "6. Disclaimer",
                    "To the fullest extent permitted by law, the developers, maintainers, and contributors of ABK are not liable for any device damage, data loss, account risk, service interruption, compliance issues, third-party claims, or any direct/indirect losses arising from the use, modification, distribution, or reliance on ABK or its build artifacts.",
                    "ABK is provided as-is, with no guarantees of being defect-free, uninterrupted, or free of security risks, and no guarantee of availability for any specific device, system version, kernel branch, or third-party module.",
                    "By continuing to use ABK, you confirm that you have the necessary knowledge, backup, and recovery capabilities, and that you accept full responsibility for all associated risks."
                )
                TermsText("You may click \"Agree and Continue\" only after reading to the end of this agreement.")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline and Exit")
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = canAccept
            ) {
                Text(if (canAccept) "Agree and Continue" else "Scroll to bottom")
            }
        }
    )
}

@Composable
private fun TermsSection(title: String, vararg paragraphs: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    paragraphs.forEach { paragraph ->
        TermsText(paragraph)
    }
}

@Composable
private fun TermsText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private enum class AbkTab(val label: String) {
    Status("Status"),
    Build("Build"),
    Modules("Modules"),
    Flash("Flash"),
    RuntimeHome("Home"),
    InstalledModules("Installed Modules"),
    RootAuth("Superuser"),
    Settings("Settings")
}

@Composable
private fun AbkMainScaffold(
    vm: MainViewModel,
    pendingModuleInstallUri: String? = null,
    onModuleInstallUriConsumed: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(AbkTab.Status) }
    var flashDetailPageVisible by rememberSaveable { mutableStateOf(false) }
    var settingsThemePageVisible by rememberSaveable { mutableStateOf(false) }
    var buildPlanPageVisible by rememberSaveable { mutableStateOf(false) }
    var moduleRepositoryPageVisible by rememberSaveable { mutableStateOf(false) }
    var rootAuthDetailPageVisible by rememberSaveable { mutableStateOf(false) }
    var managerPatchPageVisible by rememberSaveable { mutableStateOf(false) }
    var lastBackAt by remember { mutableStateOf(0L) }
    val runtimeNativeManagerActive = state.hasNativeManagerPermission
    val visibleTabs = remember(state.runtimeNavigationEnabled, state.rootGranted, runtimeNativeManagerActive) {
        if (state.runtimeNavigationEnabled) {
            buildList {
                add(AbkTab.RuntimeHome)
                if (state.rootGranted) add(AbkTab.InstalledModules)
                if (runtimeNativeManagerActive) add(AbkTab.RootAuth)
                add(AbkTab.Settings)
            }
        } else {
            listOf(AbkTab.Status, AbkTab.Build, AbkTab.Modules, AbkTab.Flash, AbkTab.Settings)
        }
    }
    val activeTab = if (selectedTab in visibleTabs) selectedTab else visibleTabs.first()
    val motionScheme = MaterialTheme.motionScheme
    val hideBottomBar = when (activeTab) {
        AbkTab.Build -> buildPlanPageVisible
        AbkTab.Modules -> moduleRepositoryPageVisible
        AbkTab.Flash -> flashDetailPageVisible
        AbkTab.Settings -> settingsThemePageVisible
        AbkTab.RootAuth -> rootAuthDetailPageVisible
        AbkTab.RuntimeHome -> managerPatchPageVisible
        else -> false
    }

    LaunchedEffect(pendingModuleInstallUri) {
        if (!pendingModuleInstallUri.isNullOrBlank()) {
            if (!state.runtimeNavigationEnabled) vm.setRuntimeNavigationEnabled(true)
            selectedTab = AbkTab.InstalledModules
        }
    }

    LaunchedEffect(activeTab) {
        when (activeTab) {
            AbkTab.Build -> {
                moduleRepositoryPageVisible = false
                flashDetailPageVisible = false
                settingsThemePageVisible = false
                rootAuthDetailPageVisible = false
                managerPatchPageVisible = false
            }
            AbkTab.Flash -> {
                buildPlanPageVisible = false
                moduleRepositoryPageVisible = false
                settingsThemePageVisible = false
                rootAuthDetailPageVisible = false
                managerPatchPageVisible = false
            }
            AbkTab.Modules -> {
                buildPlanPageVisible = false
                flashDetailPageVisible = false
                settingsThemePageVisible = false
                rootAuthDetailPageVisible = false
                managerPatchPageVisible = false
            }
            AbkTab.Settings -> {
                buildPlanPageVisible = false
                moduleRepositoryPageVisible = false
                flashDetailPageVisible = false
                rootAuthDetailPageVisible = false
                managerPatchPageVisible = false
            }
            AbkTab.RootAuth -> {
                buildPlanPageVisible = false
                moduleRepositoryPageVisible = false
                flashDetailPageVisible = false
                settingsThemePageVisible = false
                managerPatchPageVisible = false
            }
            AbkTab.RuntimeHome -> {
                buildPlanPageVisible = false
                moduleRepositoryPageVisible = false
                flashDetailPageVisible = false
                settingsThemePageVisible = false
                rootAuthDetailPageVisible = false
            }
            else -> {
                buildPlanPageVisible = false
                moduleRepositoryPageVisible = false
                flashDetailPageVisible = false
                settingsThemePageVisible = false
                rootAuthDetailPageVisible = false
                managerPatchPageVisible = false
            }
        }
    }

    LaunchedEffect(visibleTabs, selectedTab, state.runtimeNavigationEnabled) {
        if (selectedTab !in visibleTabs) {
            selectedTab = if (state.runtimeNavigationEnabled) AbkTab.RuntimeHome else AbkTab.Status
        }
    }

    fun handleTopLevelBack() {
        val now = System.currentTimeMillis()
        if (now - lastBackAt <= EXIT_BACK_INTERVAL_MS) {
            context.findActivity()?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "Press again to exit AnyBase Kernel", Toast.LENGTH_SHORT).show()
        }
    }

    if (!hideBottomBar) {
        BackHandler(onBack = ::handleTopLevelBack)
    }

    Scaffold(
        containerColor = uiSurfaceColor(MaterialTheme.colorScheme.surface),
        bottomBar = {
            AnimatedVisibility(
                visible = !hideBottomBar,
                enter = fadeIn(animationSpec = motionScheme.fastEffectsSpec()) +
                    slideInVertically(animationSpec = motionScheme.fastSpatialSpec()) { height -> height },
                exit = ExitTransition.None
            ) {
                NavigationBar(
                    containerColor = uiSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
                    tonalElevation = 0.dp
                ) {
                    visibleTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { selectedTab = tab },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        AbkTab.Status -> Icons.Default.Home
                                        AbkTab.Build -> Icons.Default.RocketLaunch
                                        AbkTab.Modules -> Icons.Default.LibraryBooks
                                        AbkTab.Flash -> if (state.rootGranted) Icons.Default.FlashOn else Icons.Default.FolderOpen
                                        AbkTab.RuntimeHome -> Icons.Default.Memory
                                        AbkTab.InstalledModules -> Icons.Default.Extension
                                        AbkTab.RootAuth -> Icons.Default.AdminPanelSettings
                                        AbkTab.Settings -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.displayLabel(state.rootGranted)
                                )
                            },
                            label = {
                                Text(text = tab.displayLabel(state.rootGranted))
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val contentPadding = if (hideBottomBar) PaddingValues(0.dp) else padding
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (
                        fadeIn(animationSpec = motionScheme.defaultEffectsSpec()) +
                            slideInHorizontally(
                                animationSpec = motionScheme.defaultSpatialSpec()
                            ) { width -> direction * width / 4 }
                        ) togetherWith (
                        fadeOut(animationSpec = motionScheme.fastEffectsSpec()) +
                            slideOutHorizontally(
                                animationSpec = motionScheme.fastSpatialSpec()
                            ) { width -> -direction * width / 6 }
                        )
                },
                label = "abk-tab"
            ) { tab ->
                when (tab) {
                    AbkTab.Status -> StatusScreen(
                        vm = vm,
                        runtimeNavigationEnabled = state.runtimeNavigationEnabled,
                        onToggleRuntimeNavigation = { vm.setRuntimeNavigationEnabled(true) }
                    )
                    AbkTab.Build -> BuildScreen(
                        vm = vm,
                        outerPadding = contentPadding,
                        onPlanPageVisibleChange = { buildPlanPageVisible = it }
                    )
                    AbkTab.Modules -> ModuleRepositoryScreen(
                        vm = vm,
                        outerPadding = contentPadding,
                        onRepositoryPageVisibleChange = { moduleRepositoryPageVisible = it }
                    )
                    AbkTab.Flash -> FlashScreen(
                        vm = vm,
                        outerPadding = contentPadding,
                        onDetailPageVisibleChange = { flashDetailPageVisible = it }
                    )
                    AbkTab.RuntimeHome -> RuntimeHomeScreen(
                        vm = vm,
                        onSwitchToClassic = { vm.setRuntimeNavigationEnabled(false) },
                        onManagerPatchPageVisibleChange = { managerPatchPageVisible = it }
                    )
                    AbkTab.InstalledModules -> InstalledModulesScreen(
                        vm = vm,
                        pendingModuleInstallUri = pendingModuleInstallUri,
                        onPendingModuleInstallUriConsumed = onModuleInstallUriConsumed
                    )
                    AbkTab.RootAuth -> RootAuthorizationScreen(
                        vm = vm,
                        outerPadding = contentPadding,
                        onDetailPageVisibleChange = { rootAuthDetailPageVisible = it }
                    )
                    AbkTab.Settings -> SettingsScreen(
                        vm = vm,
                        outerPadding = contentPadding,
                        onThemePageVisibleChange = { settingsThemePageVisible = it },
                        onOpenInstalledModules = {
                            if (!state.runtimeNavigationEnabled) vm.setRuntimeNavigationEnabled(true)
                            selectedTab = if (state.rootGranted) {
                                AbkTab.InstalledModules
                            } else {
                                AbkTab.RuntimeHome
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun AbkTab.displayLabel(rootGranted: Boolean): String = when (this) {
    AbkTab.Flash -> if (rootGranted) label else "Files"
    else -> label
}

private fun extractModuleInstallUri(intent: Intent?): Uri? {
    if (intent == null) return null
    val uri = when (intent.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> intent.streamUri() ?: intent.firstClipUri()
        Intent.ACTION_SEND_MULTIPLE -> intent.streamUris().firstOrNull() ?: intent.firstClipUri()
        else -> null
    } ?: return null
    return uri.takeIf { isLikelyModuleZipIntent(intent.type, it) }
}

private fun Intent.streamUri(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    }

private fun Intent.streamUris(): List<Uri> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }

private fun Intent.firstClipUri(): Uri? =
    clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

private fun isLikelyModuleZipIntent(mimeType: String?, uri: Uri): Boolean {
    val cleanMime = mimeType?.lowercase().orEmpty()
    val path = uri.toString().lowercase()
    return cleanMime in MODULE_ZIP_MIME_TYPES || path.endsWith(".zip")
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val EXIT_BACK_INTERVAL_MS = 2_000L
private val MODULE_ZIP_MIME_TYPES = setOf(
    "application/zip",
    "application/x-zip",
    "application/x-zip-compressed",
    "application/octet-stream"
)
