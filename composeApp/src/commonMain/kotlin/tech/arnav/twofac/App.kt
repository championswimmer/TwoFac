package tech.arnav.twofac

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.jetbrains.compose.ui.tooling.preview.Preview
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

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Home
        ) {
            composable<Home> {
                HomeScreen(
                    onNavigateToAccounts = { navController.navigate(Accounts) },
                    onNavigateToSettings = { navController.navigate(Settings) }
                )
            }

            composable<Accounts> {
                AccountsScreen(
                    onNavigateToAddAccount = { navController.navigate(AddAccount) },
                    onNavigateToAccountDetail = { accountId ->
                        navController.navigate(AccountDetail(accountId))
                    },
                    onNavigateBack = { navController.popBackStack() }
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
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AddAccount> {
                AddAccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}