package tech.arnav.twofac

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.nav_home
import twofac.composeapp.generated.resources.nav_accounts
import twofac.composeapp.generated.resources.nav_settings
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import tech.arnav.twofac.navigation.AccountDetail
import tech.arnav.twofac.navigation.Accounts
import tech.arnav.twofac.navigation.AddAccount
import tech.arnav.twofac.navigation.ExportAccountQr
import tech.arnav.twofac.navigation.Home
import tech.arnav.twofac.navigation.OnboardingGuide
import tech.arnav.twofac.navigation.Settings
import tech.arnav.twofac.screens.AccountDetailScreen
import tech.arnav.twofac.screens.AccountsScreen
import tech.arnav.twofac.screens.AddAccountScreen
import tech.arnav.twofac.screens.ExportQrScreen
import tech.arnav.twofac.screens.HomeScreen
import tech.arnav.twofac.screens.OnboardingGuideScreen
import tech.arnav.twofac.screens.SettingsScreen
import tech.arnav.twofac.theme.TwoFacTheme
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val icon: ImageVector,
) {
    HOME(Icons.Rounded.Home),
    ACCOUNTS(Icons.Rounded.ManageAccounts),
    SETTINGS(Icons.Rounded.Settings),
}

@Composable
@Preview
fun App(onQuit: (() -> Unit)? = null) {
    TwoFacTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (currentDestination.shouldShowBottomBar()) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val label = when (destination) {
                                TopLevelDestination.HOME -> stringResource(Res.string.nav_home)
                                TopLevelDestination.ACCOUNTS -> stringResource(Res.string.nav_accounts)
                                TopLevelDestination.SETTINGS -> stringResource(Res.string.nav_settings)
                            }
                            NavigationBarItem(
                                selected = currentDestination.isSelected(destination),
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Home,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Home> {
                    HomeScreen(
                        onNavigateToAccounts = {
                            navController.navigateToTopLevel(TopLevelDestination.ACCOUNTS)
                        },
                        onNavigateToOnboarding = { unseenOnly ->
                            navController.navigate(OnboardingGuide(unseenOnly = unseenOnly))
                        },
                    )
                }

                composable<Accounts> {
                    AccountsScreen(
                        onNavigateToAddAccount = { navController.navigate(AddAccount) },
                        onNavigateToAccountDetail = { accountId ->
                            navController.navigate(AccountDetail(accountId))
                        }
                    )
                }

                composable<AccountDetail> { backStackEntry ->
                    val accountDetail = backStackEntry.toRoute<AccountDetail>()
                    AccountDetailScreen(
                        accountId = accountDetail.accountId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToExportQr = { accountId ->
                            navController.navigate(ExportAccountQr(accountId))
                        },
                    )
                }

                composable<ExportAccountQr> { backStackEntry ->
                    val route = backStackEntry.toRoute<ExportAccountQr>()
                    ExportQrScreen(
                        accountId = route.accountId,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

                composable<Settings> {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToOnboarding = {
                            navController.navigate(OnboardingGuide())
                        },
                        onQuit = onQuit
                    )
                }

                composable<AddAccount> {
                    AddAccountScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<OnboardingGuide> { backStackEntry ->
                    val route = backStackEntry.toRoute<OnboardingGuide>()
                    OnboardingGuideScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddAccount = { navController.navigate(AddAccount) },
                        onNavigateToAccounts = {
                            navController.navigateToTopLevel(TopLevelDestination.ACCOUNTS)
                        },
                        onNavigateToSettings = {
                            navController.navigateToTopLevel(TopLevelDestination.SETTINGS)
                        },
                        unseenOnly = route.unseenOnly,
                    )
                }
            }
        }
    }
}

private fun NavDestination?.shouldShowBottomBar(): Boolean {
    return this.matchesRoute(Home::class) ||
        this.matchesRoute(Accounts::class) ||
        this.matchesRoute(Settings::class)
}

private fun NavDestination?.isSelected(destination: TopLevelDestination): Boolean {
    return when (destination) {
        TopLevelDestination.HOME -> this.matchesRoute(Home::class)
        TopLevelDestination.ACCOUNTS -> this.matchesRoute(Accounts::class)
        TopLevelDestination.SETTINGS -> this.matchesRoute(Settings::class)
    }
}

private fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    val route = when (destination) {
        TopLevelDestination.HOME -> Home
        TopLevelDestination.ACCOUNTS -> Accounts
        TopLevelDestination.SETTINGS -> Settings
    }
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.matchesRoute(routeClass: KClass<*>): Boolean {
    val currentRoute = this?.route ?: return false
    val routeName = routeClass.qualifiedName ?: return false
    return currentRoute == routeName || currentRoute.startsWith("$routeName/")
}
