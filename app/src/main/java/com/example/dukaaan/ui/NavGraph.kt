package com.example.dukaaan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.dukaaan.viewmodel.LoginType
import com.example.dukaaan.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object OwnerDashboard : Screen("owner_dashboard/{shopId}") {
        fun createRoute(shopId: String) = "owner_dashboard/$shopId"
    }
    object StaffDashboard : Screen("staff_dashboard/{shopId}/{staffId}/{staffName}") {
        fun createRoute(shopId: String, staffId: String, staffName: String) = "staff_dashboard/$shopId/$staffId/$staffName"
    }
    object ProductSalesHistory : Screen("product_sales_history/{stockId}/{stockName}") {
        fun createRoute(stockId: String, stockName: String) = "product_sales_history/$stockId/$stockName"
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel = viewModel<AuthViewModel>()
    
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen { userId, loginType, shopId, staffName ->
                when (loginType) {
                    is LoginType.Owner -> navController.navigate(Screen.OwnerDashboard.createRoute(userId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                    is LoginType.Staff -> {
                        val safeStaffName = if (staffName.isNullOrBlank()) "Staff" else staffName
                        navController.navigate(Screen.StaffDashboard.createRoute(shopId, userId, safeStaffName)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }
        }
        composable(
            route = Screen.OwnerDashboard.route,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId") ?: ""
            OwnerDashboard(
                shopId = shopId,
                ownerName = "",
                onLogout = { 
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) } 
                },
                navController = navController
            )
        }
        composable(
            route = Screen.StaffDashboard.route,
            arguments = listOf(
                navArgument("shopId") { type = NavType.StringType },
                navArgument("staffId") { type = NavType.StringType },
                navArgument("staffName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId") ?: ""
            val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
            val staffName = backStackEntry.arguments?.getString("staffName") ?: ""
            StaffDashboard(
                shopId = shopId,
                staffId = staffId,
                staffName = staffName,
                onLogout = { 
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) } 
                },
                navController = navController
            )
        }
        composable(
            route = Screen.ProductSalesHistory.route,
            arguments = listOf(
                navArgument("stockId") { type = NavType.StringType },
                navArgument("stockName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val stockId = backStackEntry.arguments?.getString("stockId") ?: ""
            val stockName = backStackEntry.arguments?.getString("stockName") ?: ""
            ProductSalesHistoryScreen(
                stockId = stockId,
                stockName = stockName,
                onBack = { navController.popBackStack() }
            )
        }
    }
} 