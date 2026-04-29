package com.formbuddy.android.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.formbuddy.android.R
import com.formbuddy.android.ui.components.ios.FillinActionMenu
import com.formbuddy.android.ui.components.ios.FillinTab
import com.formbuddy.android.ui.components.ios.FillinTabBar
import com.formbuddy.android.ui.screens.docs.DocsScreen
import com.formbuddy.android.ui.screens.filling.FillingScreen
import com.formbuddy.android.ui.screens.filling.editor.EditorScreen
import com.formbuddy.android.ui.screens.library.FormsLibraryScreen
import com.formbuddy.android.ui.screens.onboarding.OnboardingScreen
import com.formbuddy.android.ui.screens.paywall.PaywallScreen
import com.formbuddy.android.ui.screens.privacy.PrivacyAuditScreen
import com.formbuddy.android.ui.screens.profiles.BusinessProfileScreen
import com.formbuddy.android.ui.screens.profiles.ProfileChatScreen
import com.formbuddy.android.ui.screens.profiles.ProfileScreen
import com.formbuddy.android.ui.screens.profiles.ProfilesScreen
import com.formbuddy.android.ui.screens.profiles.SignatureScreen
import com.formbuddy.android.ui.screens.scanner.ScannerScreen
import com.formbuddy.android.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Docs : Screen("docs")
    data object Profiles : Screen("profiles")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object Scanner : Screen("scanner")
    data object Filling : Screen("filling/{source}?uri={uri}&formId={formId}") {
        fun createRoute(source: String, uri: String? = null, formId: String? = null): String {
            var route = "filling/$source"
            val params = mutableListOf<String>()
            uri?.let { params.add("uri=${Uri.encode(it)}") }
            formId?.let { params.add("formId=$it") }
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
    }
    data object Editor : Screen("editor/{formId}") {
        fun createRoute(formId: String) = "editor/$formId"
    }
    data object Profile : Screen("profile/{profileId}?isFamily={isFamily}") {
        fun createRoute(profileId: String, isFamily: Boolean = false) = "profile/$profileId?isFamily=$isFamily"
    }
    data object BusinessProfile : Screen("business_profile/{profileId}") {
        fun createRoute(profileId: String) = "business_profile/$profileId"
    }
    data object ProfileChat : Screen("profile_chat")
    data object Signature : Screen("signature/{profileId}") {
        fun createRoute(profileId: String) = "signature/$profileId"
    }
    data object FormsLibrary : Screen("forms_library")
    data object Paywall : Screen("paywall")
    data object PrivacyAudit : Screen("privacy_audit")
}

@Composable
fun FormBuddyNavHost(
    importUri: Uri? = null,
    importMimeType: String? = null,
    deepLink: com.formbuddy.android.data.share.DeepLink.Parsed =
        com.formbuddy.android.data.share.DeepLink.Parsed.None
) {
    val navController = rememberNavController()
    var isMenuExpanded by remember { mutableStateOf(false) }

    val tabs = remember {
        listOf(
            FillinTab("docs", "Docs", Icons.Filled.Description, Icons.Outlined.Description),
            FillinTab("profiles", "Profiles", Icons.Filled.Person, Icons.Outlined.Person),
            FillinTab("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }
    val tabRouteByKey = mapOf(
        "docs" to Screen.Docs.route,
        "profiles" to Screen.Profiles.route,
        "settings" to Screen.Settings.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = remember { setOf(Screen.Docs.route, Screen.Profiles.route, Screen.Settings.route) }
    val showBottomBar = currentRoute in bottomBarRoutes

    // Deep-link routing for AppLinks (referral / share-for-fill).
    val deepLinkActions = androidx.hilt.navigation.compose.hiltViewModel<DeepLinkViewModel>()
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is com.formbuddy.android.data.share.DeepLink.Parsed.Referral ->
                deepLinkActions.consumeReferral(deepLink.referrerId, deepLink.planCode)
            is com.formbuddy.android.data.share.DeepLink.Parsed.ShareForFill ->
                deepLinkActions.consumeShareForFill(deepLink.token) { uri ->
                    navController.navigate(Screen.Filling.createRoute("share-fill", uri = uri))
                }
            com.formbuddy.android.data.share.DeepLink.Parsed.None -> Unit
        }
    }

    LaunchedEffect(importUri) {
        if (importUri != null) {
            navController.navigate(Screen.Filling.createRoute("upload", uri = importUri.toString()))
        }
    }

    val selectedKey = when (currentRoute) {
        Screen.Profiles.route -> "profiles"
        Screen.Settings.route -> "settings"
        else -> "docs"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Docs.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
            }
        ) {
            composable(Screen.Docs.route) {
                DocsScreen(
                    navController = navController,
                    onAddDocument = { isMenuExpanded = true }
                )
            }

            composable(Screen.Profiles.route) {
                ProfilesScreen(navController = navController)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }

            composable(Screen.Scanner.route) {
                ScannerScreen(navController = navController)
            }

            composable(
                route = Screen.Filling.route,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("formId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                FillingScreen(
                    navController = navController,
                    source = backStackEntry.arguments?.getString("source") ?: "upload",
                    uri = backStackEntry.arguments?.getString("uri"),
                    formId = backStackEntry.arguments?.getString("formId")
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("formId") { type = NavType.StringType })
            ) { backStackEntry ->
                EditorScreen(
                    navController = navController,
                    formId = backStackEntry.arguments?.getString("formId") ?: ""
                )
            }

            composable(
                route = Screen.Profile.route,
                arguments = listOf(
                    navArgument("profileId") { type = NavType.StringType },
                    navArgument("isFamily") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                ProfileScreen(
                    navController = navController,
                    profileId = backStackEntry.arguments?.getString("profileId") ?: "",
                    isFamily = backStackEntry.arguments?.getBoolean("isFamily") ?: false
                )
            }

            composable(
                route = Screen.BusinessProfile.route,
                arguments = listOf(navArgument("profileId") { type = NavType.StringType })
            ) { backStackEntry ->
                BusinessProfileScreen(
                    navController = navController,
                    profileId = backStackEntry.arguments?.getString("profileId") ?: ""
                )
            }

            composable(Screen.ProfileChat.route) {
                ProfileChatScreen(navController = navController)
            }

            composable(
                route = Screen.Signature.route,
                arguments = listOf(navArgument("profileId") { type = NavType.StringType })
            ) { backStackEntry ->
                SignatureScreen(
                    navController = navController,
                    profileId = backStackEntry.arguments?.getString("profileId") ?: ""
                )
            }

            composable(Screen.FormsLibrary.route) {
                FormsLibraryScreen(navController = navController)
            }

            composable(Screen.Paywall.route) {
                PaywallScreen(navController = navController)
            }

            composable(Screen.PrivacyAudit.route) {
                PrivacyAuditScreen(navController = navController)
            }
        }

        // Floating arc menu (Scan / Upload / Library) appears above the tab bar.
        if (showBottomBar) {
            FillinActionMenu(
                visible = isMenuExpanded,
                onScan = {
                    isMenuExpanded = false
                    navController.navigate(Screen.Scanner.route)
                },
                onUpload = {
                    isMenuExpanded = false
                    // Upload itself is delegated to host activity for content URI picking.
                    navController.navigate(Screen.Filling.createRoute("upload"))
                },
                onLibrary = {
                    isMenuExpanded = false
                    navController.navigate(Screen.FormsLibrary.route)
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Floating capsule tab bar + standalone FAB (mirrors iOS ActionButtonTabBar).
        if (showBottomBar) {
            FillinTabBar(
                tabs = tabs,
                selectedKey = selectedKey,
                isMenuExpanded = isMenuExpanded,
                onTabSelected = { key ->
                    isMenuExpanded = false
                    val route = tabRouteByKey[key] ?: return@FillinTabBar
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onFabTap = { isMenuExpanded = !isMenuExpanded },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
