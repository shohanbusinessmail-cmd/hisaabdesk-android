
package com.hisaabdesk.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisaabdesk.app.ui.MainViewModel
import com.hisaabdesk.app.ui.screens.*

private data class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val bottom = listOf(
        Dest("dashboard", "ড্যাশবোর্ড", Icons.Default.Home),
        Dest("transactions", "লেনদেন", Icons.Default.List),
        Dest("accounts", "অ্যাকাউন্ট", Icons.Default.AccountBalanceWallet),
        Dest("more", "আরও", Icons.Default.MoreHoriz)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            bottom.forEach { d ->
                NavigationBarItem(selected = current?.startsWith(d.route) == true,
                    onClick = { nav.navigate(d.route) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(d.icon, contentDescription = d.label) }, label = { Text(d.label) })
            }
        }
    }) { pad ->
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.padding(pad)) {
            composable("dashboard") { DashboardScreen(vm, nav) }
            composable("transactions?add={add}", arguments = listOf(navArgument("add") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                TransactionsScreen(vm, e.arguments?.getString("add"))
            }
            composable("transactions") { TransactionsScreen(vm) }
            composable("accounts?transfer={transfer}", arguments = listOf(navArgument("transfer") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
                AccountsScreen(vm, openTransfer = e.arguments?.getString("transfer") == "1")
            }
            composable("accounts") { AccountsScreen(vm) }
            composable("more") { MoreScreen { route -> nav.navigate(route) } }
            composable("shops") { ShopsScreen(vm) }
            composable("debts") { DebtsScreen(vm) }
            composable("loans") { LoansScreen(vm) }
            composable("savings") { SavingsBudgetScreen(vm) }
            composable("categories") { CategoriesScreen(vm) }
            composable("reports") { ReportsScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
            composable("about") { AboutScreen() }
        }
    }
}

@Composable
private fun MoreScreen(go: (String) -> Unit) {
    val items = listOf(
        "shops" to "দোকানের বাকি", "debts" to "ব্যক্তিগত ধার-দেনা", "loans" to "এনজিও ও ব্যাংক ঋণ",
        "savings" to "সঞ্চয় ও বাজেট", "categories" to "ক্যাটাগরি", "reports" to "রিপোর্ট",
        "settings" to "সেটিংস ও ব্যাকআপ", "about" to "সম্পর্কে"
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("আরও", style = MaterialTheme.typography.headlineSmall)
        items.forEach { (route, label) ->
            Card(Modifier.fillMaxWidth()) {
                TextButton(onClick = { go(route) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
