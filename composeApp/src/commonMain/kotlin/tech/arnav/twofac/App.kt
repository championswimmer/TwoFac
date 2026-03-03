package tech.arnav.twofac

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import tech.arnav.twofac.navigation.AccountDetail
import tech.arnav.twofac.navigation.Accounts
import tech.arnav.twofac.navigation.AddAccount
import tech.arnav.twofac.navigation.Home
import tech.arnav.twofac.navigation.Settings
import tech.arnav.twofac.screens.AccountDetailScreen
import tech.arnav.twofac.screens.AccountsScreen
import tech.arnav.twofac.screens.AddAccountScreen
import tech.arnav.twofac.screens.HomeScreen
import tech.arnav.twofac.screens.SettingsScreen
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    HOME("Home", Icons.Filled.Home, "Home"),
    ACCOUNTS("Accounts", Icons.Filled.ManageAccounts, "Accounts"),
    SETTINGS("Settings", Icons.Filled.Settings, "Settings"),
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            bottomBar = {
                if (currentDestination.shouldShowBottomBar()) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination.isSelected(destination),
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.contentDescription
                                    )
                                },
                                label = { Text(destination.label) }
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
                        }
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
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Settings> {
                    SettingsScreen()
                }

                composable<AddAccount> {
                    AddAccountScreen(
                        onNavigateBack = { navController.popBackStack() }
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
