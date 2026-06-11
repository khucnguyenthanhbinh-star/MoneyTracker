package com.moneytracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneytracker.ui.screens.add.AddTransactionScreen
import com.moneytracker.ui.screens.home.HomeScreen
import com.moneytracker.ui.screens.settings.SettingsScreen

@Composable
fun MoneyTrackerNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddTransaction = { navController.navigate("add") },
                onEditTransaction = { id -> navController.navigate("edit/$id") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("add") {
            AddTransactionScreen(onBack = { navController.popBackStack() })
        }
        composable("edit/{txId}", arguments = listOf(navArgument("txId") { type = NavType.LongType })) {
            val txId = it.arguments?.getLong("txId") ?: 0L
            AddTransactionScreen(transactionId = txId, onBack = { navController.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
