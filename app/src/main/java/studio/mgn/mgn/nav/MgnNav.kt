package studio.mgn.mgn.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import studio.mgn.design.MgnTheme
import studio.mgn.mgn.MgnApp
import studio.mgn.mgn.game.GameScreen
import studio.mgn.mgn.game.GameViewModel
import studio.mgn.mgn.menu.MenuScreen
import studio.mgn.mgn.menu.MenuViewModel
import studio.mgn.mgn.more.AchievementsScreen
import studio.mgn.mgn.more.AchievementsViewModel
import studio.mgn.mgn.more.SettingsScreen
import studio.mgn.mgn.more.SettingsViewModel
import studio.mgn.mgn.splash.SplashScreen
import studio.mgn.mgn.splash.SplashViewModel
import studio.mgn.setup.SetupStrings
import studio.mgn.setup.SetupViewModel
import studio.mgn.setup.SetupWizardScreen

private const val TRANSITION_MS = 300

/**
 * PLAN 2 navigation: splash → menu → setup → game, plus settings
 * and achievements. Fade + light slide on every transition.
 */
@Composable
fun MgnNav(app: MgnApp, reduceMotion: Boolean) {
    val nav = rememberNavController()
    val enter = if (reduceMotion) {
        fadeIn()
    } else {
        fadeIn(animationSpec = tween(TRANSITION_MS)) +
            slideInHorizontally(animationSpec = tween(TRANSITION_MS)) { it / 6 }
    }
    val exit = if (reduceMotion) {
        fadeOut()
    } else {
        fadeOut(animationSpec = tween(TRANSITION_MS)) +
            slideOutHorizontally(animationSpec = tween(TRANSITION_MS)) { -it / 6 }
    }
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        composable(Routes.SPLASH) {
            val vm: SplashViewModel = viewModel {
                SplashViewModel(loadContent = { app.content })
            }
            SplashScreen(
                viewModel = vm,
                onReady = {
                    nav.navigate(Routes.MENU) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                reduceMotion = reduceMotion,
            )
        }
        composable(Routes.MENU) {
            val vm: MenuViewModel = viewModel { MenuViewModel(app.repository) }
            MenuScreen(
                viewModel = vm,
                onNavigateSetup = { nav.navigate(Routes.SETUP) },
                onNavigateGame = { nav.navigate(Routes.GAME) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                reduceMotion = reduceMotion,
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel {
                SettingsViewModel(app.settings, app.repository)
            }
            SettingsScreen(viewModel = vm)
        }
        composable(Routes.ACHIEVEMENTS) {
            val vm: AchievementsViewModel = viewModel {
                AchievementsViewModel(app.content, app.repository)
            }
            AchievementsScreen(viewModel = vm)
        }
        composable(Routes.SETUP) {
            val vm: SetupViewModel = viewModel { SetupViewModel(app.repository) }
            val strings = remember {
                SetupStrings(
                    title = app.getString(studio.mgn.mgn.R.string.setup_title),
                    next = app.getString(studio.mgn.mgn.R.string.setup_next),
                    back = app.getString(studio.mgn.mgn.R.string.setup_back),
                    confirm = app.getString(studio.mgn.mgn.R.string.setup_confirm),
                    stepName = app.getString(studio.mgn.mgn.R.string.setup_step_name),
                    nameHint = app.getString(studio.mgn.mgn.R.string.setup_name_hint),
                    stepTitle = app.getString(studio.mgn.mgn.R.string.setup_step_title),
                    rulerHint = app.getString(studio.mgn.mgn.R.string.setup_ruler_hint),
                    stepFlag = app.getString(studio.mgn.mgn.R.string.setup_step_flag),
                    stepTurn = app.getString(studio.mgn.mgn.R.string.setup_step_turn),
                    turnDay = app.getString(studio.mgn.mgn.R.string.setup_turn_day),
                    turnWeek = app.getString(studio.mgn.mgn.R.string.setup_turn_week),
                    turnMonth = app.getString(studio.mgn.mgn.R.string.setup_turn_month),
                    stepAxes = app.getString(studio.mgn.mgn.R.string.setup_step_axes),
                    axisEconomic = app.getString(studio.mgn.mgn.R.string.setup_axis_economic),
                    axisSocial = app.getString(studio.mgn.mgn.R.string.setup_axis_social),
                    axisForeign = app.getString(studio.mgn.mgn.R.string.setup_axis_foreign),
                    stepReview = app.getString(studio.mgn.mgn.R.string.setup_step_review),
                    emptyError = app.getString(studio.mgn.mgn.R.string.setup_empty_error),
                )
            }
            SetupWizardScreen(
                viewModel = vm,
                onDone = {
                    nav.navigate(Routes.GAME) {
                        popUpTo(Routes.MENU)
                    }
                },
                onBack = { nav.popBackStack() },
                reduceMotion = reduceMotion,
                strings = strings,
            )
        }
        composable(Routes.GAME) {
            val vm: GameViewModel = viewModel {
                GameViewModel(app.repository) { holder ->
                    app.sessionHolder = holder
                }
            }
            GameScreen(viewModel = vm)
        }
    }
}
