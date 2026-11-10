package com.expira.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expira.app.ui.components.BottomTab
import com.expira.app.ui.components.ExpiraBottomNav
import com.expira.app.ui.evaluation.EvaluationScreen
import com.expira.app.ui.history.HistoryScreen
import com.expira.app.ui.home.HomeScreen
import com.expira.app.ui.processing.ProcessingRoute
import com.expira.app.ui.processing.ProcessingViewModel
import com.expira.app.ui.theme.ExpiraColors

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EVALUATION = "evaluation"
    const val PROCESSING = "processing?${ProcessingViewModel.ARG_IMAGE_URI}={${ProcessingViewModel.ARG_IMAGE_URI}}"

    fun processing(imageUri: Uri): String = "processing?${ProcessingViewModel.ARG_IMAGE_URI}=${Uri.encode(imageUri.toString())}"
}

private val tabRoutes = mapOf(
    BottomTab.Scan to Routes.HOME,
    BottomTab.History to Routes.HISTORY,
    BottomTab.Evaluation to Routes.EVALUATION,
)

@Composable
fun ExpiraNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = tabRoutes.entries.firstOrNull { it.value == currentRoute }?.key

    Column(modifier = Modifier.fillMaxSize().background(ExpiraColors.Background)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onTakePhoto = {},
                    onImagePicked = { uri -> navController.navigate(Routes.processing(uri)) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.EVALUATION) { EvaluationScreen() }
            composable(
                route = Routes.PROCESSING,
                arguments = listOf(navArgument(ProcessingViewModel.ARG_IMAGE_URI) { type = NavType.StringType }),
            ) {
                ProcessingRoute(onBack = { navController.popBackStack() })
            }
        }
        if (currentTab != null) {
            ExpiraBottomNav(
                selected = currentTab,
                onSelect = { tab -> navController.navigateToTab(tabRoutes.getValue(tab)) },
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
