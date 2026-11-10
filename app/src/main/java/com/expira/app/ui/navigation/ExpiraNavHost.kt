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
import com.expira.app.extraction.ConfidenceLevel
import com.expira.app.ui.camera.CameraRoute
import com.expira.app.ui.components.BottomTab
import com.expira.app.ui.components.ExpiraBottomNav
import com.expira.app.ui.correction.CorrectionRoute
import com.expira.app.ui.correction.CorrectionViewModel
import com.expira.app.ui.evaluation.EvaluationScreen
import com.expira.app.ui.history.HistoryScreen
import com.expira.app.ui.home.HomeScreen
import com.expira.app.ui.processing.ProcessingRoute
import com.expira.app.ui.processing.ProcessingViewModel
import com.expira.app.ui.review.ConfirmRoute
import com.expira.app.ui.review.FailedScreen
import com.expira.app.ui.review.ResultRoute
import com.expira.app.ui.theme.ExpiraColors

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val EVALUATION = "evaluation"
    const val CAMERA = "camera"
    const val RESULT = "result"
    const val CONFIRM = "confirm"
    const val FAILED = "failed"
    const val CORRECTION = "correction?${CorrectionViewModel.ARG_PREFILL}={${CorrectionViewModel.ARG_PREFILL}}"

    fun correction(prefill: Boolean): String = "correction?${CorrectionViewModel.ARG_PREFILL}=$prefill"
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
                    onTakePhoto = { navController.navigate(Routes.CAMERA) },
                    onImagePicked = { uri -> navController.navigate(Routes.processing(uri)) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.EVALUATION) { EvaluationScreen() }
            composable(Routes.CAMERA) {
                CameraRoute(
                    onClose = { navController.popBackStack() },
                    onImageReady = { uri ->
                        navController.navigate(Routes.processing(uri)) {
                            popUpTo(Routes.CAMERA) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.PROCESSING,
                arguments = listOf(navArgument(ProcessingViewModel.ARG_IMAGE_URI) { type = NavType.StringType }),
            ) {
                ProcessingRoute(
                    onBack = { navController.popBackStack() },
                    onFinished = { level ->
                        val next = when (level) {
                            ConfidenceLevel.HIGH -> Routes.RESULT
                            ConfidenceLevel.MEDIUM -> Routes.CONFIRM
                            ConfidenceLevel.LOW -> Routes.FAILED
                        }
                        navController.navigate(next) { popUpTo(Routes.PROCESSING) { inclusive = true } }
                    },
                )
            }
            composable(Routes.RESULT) {
                ResultRoute(
                    onBack = { navController.popBackStack() },
                    onCorrect = { navController.navigate(Routes.correction(prefill = true)) },
                    onSaved = navController::backToHome,
                )
            }
            composable(Routes.CONFIRM) {
                ConfirmRoute(
                    onBack = { navController.popBackStack() },
                    onManualInput = { prefill -> navController.navigate(Routes.correction(prefill)) },
                    onSaved = navController::backToHome,
                )
            }
            composable(Routes.FAILED) {
                FailedScreen(
                    onBack = { navController.popBackStack() },
                    onRetake = {
                        navController.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) }
                    },
                    onManualInput = { navController.navigate(Routes.correction(prefill = false)) },
                )
            }
            composable(
                route = Routes.CORRECTION,
                arguments = listOf(
                    navArgument(CorrectionViewModel.ARG_PREFILL) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                CorrectionRoute(
                    onBack = { navController.popBackStack() },
                    onSaved = navController::backToHome,
                )
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

/** After saving, return to Beranda where the new entry shows under "Pemindaian terakhir". */
private fun NavHostController.backToHome() {
    popBackStack(Routes.HOME, inclusive = false)
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
