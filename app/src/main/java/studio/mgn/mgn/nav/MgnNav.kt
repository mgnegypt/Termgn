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
import studio.mgn.command.CommandScreen
import studio.mgn.command.CommandStrings
import studio.mgn.command.CommandViewModel
import studio.mgn.model.GameState
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
            val vm: CommandViewModel = viewModel {
                CommandViewModel(
                    repository = app.repository,
                    content = app.content,
                    registrar = object : CommandViewModel.SessionRegistrar {
                        override fun registerSession(initial: GameState) {
                            app.sessionHolder =
                                studio.mgn.mgn.SessionHolder(app.repository, initial)
                        }

                        override fun session(): CommandViewModel.SessionHandle? {
                            val holder = app.sessionHolder ?: return null
                            return object : CommandViewModel.SessionHandle {
                                override fun update(state: GameState) {
                                    holder.update(state)
                                }
                            }
                        }
                    },
                )
            }
            CommandScreen(
                viewModel = vm,
                content = app.content,
                strings = rememberCommandStrings(app),
                reduceMotion = reduceMotion,
            )
        }
    }
}

@Composable
private fun rememberCommandStrings(app: MgnApp): CommandStrings {
    val res = app.resources
    fun s(id: Int): String = res.getString(id)
    return remember {
        CommandStrings(
            rulerTitlePrefix = s(studio.mgn.mgn.R.string.cmd_ruler_prefix),
            level = s(studio.mgn.mgn.R.string.cmd_level),
            turn = s(studio.mgn.mgn.R.string.cmd_turn),
            endTurn = s(studio.mgn.mgn.R.string.cmd_end_turn),
            processing = s(studio.mgn.mgn.R.string.cmd_processing),
            decisionsPending = s(studio.mgn.mgn.R.string.cmd_decisions_pending),
            councilTitle = s(studio.mgn.mgn.R.string.cmd_council),
            councilOpen = s(studio.mgn.mgn.R.string.cmd_council_open),
            latestEvents = s(studio.mgn.mgn.R.string.cmd_latest_events),
            indicatorsTitle = s(studio.mgn.mgn.R.string.cmd_indicators),
            missionsTitle = s(studio.mgn.mgn.R.string.cmd_missions),
            showAll = s(studio.mgn.mgn.R.string.cmd_show_all),
            reportTitle = s(studio.mgn.mgn.R.string.cmd_report),
            reportClose = s(studio.mgn.mgn.R.string.cmd_report_close),
            reportTreasury = s(studio.mgn.mgn.R.string.cmd_report_treasury),
            reportNewEvents = s(studio.mgn.mgn.R.string.cmd_report_new_events),
            reportAchievements = s(studio.mgn.mgn.R.string.cmd_report_achievements),
            reportMissions = s(studio.mgn.mgn.R.string.cmd_report_missions),
            decisionsTitle = s(studio.mgn.mgn.R.string.cmd_decisions),
            decisionsEmpty = s(studio.mgn.mgn.R.string.cmd_decisions_empty),
            reroll = s(studio.mgn.mgn.R.string.cmd_reroll),
            gemsSuffix = s(studio.mgn.mgn.R.string.cmd_gems),
            notEnoughGems = s(studio.mgn.mgn.R.string.cmd_not_enough_gems),
            chooseResult = s(studio.mgn.mgn.R.string.cmd_result),
            continueLabel = s(studio.mgn.mgn.R.string.cmd_continue),
            buildNow = s(studio.mgn.mgn.R.string.cmd_build),
            rushLabel = s(studio.mgn.mgn.R.string.cmd_rush),
            builtLabel = s(studio.mgn.mgn.R.string.cmd_built),
            underConstructionLabel = s(
                studio.mgn.mgn.R.string.cmd_under_construction,
            ),
            lockedSoon = s(studio.mgn.mgn.R.string.cmd_locked_soon),
            placeholderSection = s(studio.mgn.mgn.R.string.cmd_placeholder_plan4),
            relationLabel = s(studio.mgn.mgn.R.string.cmd_relation),
            treatiesLabel = s(studio.mgn.mgn.R.string.cmd_treaties),
            noTreaties = s(studio.mgn.mgn.R.string.cmd_no_treaties),
            remainingTurns = s(studio.mgn.mgn.R.string.cmd_remaining),
            costLabel = s(studio.mgn.mgn.R.string.cmd_cost),
            effectsPreview = s(studio.mgn.mgn.R.string.cmd_effects),
            keyLabels = mapOf(
                "treasuryCash" to s(studio.mgn.mgn.R.string.key_treasuryCash),
                "population" to s(studio.mgn.mgn.R.string.key_population),
                "legitimacy" to s(studio.mgn.mgn.R.string.key_legitimacy),
                "militarySecurity" to s(studio.mgn.mgn.R.string.key_militarySecurity),
                "economy" to s(studio.mgn.mgn.R.string.key_economy),
                "technology" to s(studio.mgn.mgn.R.string.key_technology),
                "culture" to s(studio.mgn.mgn.R.string.key_culture),
                "publicSatisfaction" to s(
                    studio.mgn.mgn.R.string.key_publicSatisfaction,
                ),
                "digitalOpinion" to s(studio.mgn.mgn.R.string.key_digitalOpinion),
                "cyberSecurity" to s(studio.mgn.mgn.R.string.key_cyberSecurity),
                "environment" to s(studio.mgn.mgn.R.string.key_environment),
                "foodSecurity" to s(studio.mgn.mgn.R.string.key_foodSecurity),
                "energy" to s(studio.mgn.mgn.R.string.key_energy),
                "tourism" to s(studio.mgn.mgn.R.string.key_tourism),
                "health" to s(studio.mgn.mgn.R.string.key_health),
                "education" to s(studio.mgn.mgn.R.string.key_education),
                "agriculture" to s(studio.mgn.mgn.R.string.key_agriculture),
                "industry" to s(studio.mgn.mgn.R.string.key_industry),
                "debt" to s(studio.mgn.mgn.R.string.key_debt),
                "gems" to s(studio.mgn.mgn.R.string.key_gems),
                "TRADE" to s(studio.mgn.mgn.R.string.treaty_TRADE),
                "DEFENSIVE" to s(studio.mgn.mgn.R.string.treaty_DEFENSIVE),
                "NON_AGGRESSION" to s(studio.mgn.mgn.R.string.treaty_NON_AGGRESSION),
                "EMBASSY" to s(studio.mgn.mgn.R.string.treaty_EMBASSY),
            ),
            sectionLabels = mapOf(
                "DASHBOARD" to s(studio.mgn.mgn.R.string.section_dashboard),
                "ECONOMY" to s(studio.mgn.mgn.R.string.section_economy),
                "DIPLOMACY" to s(studio.mgn.mgn.R.string.section_diplomacy),
                "DEVELOPMENT" to s(studio.mgn.mgn.R.string.section_development),
                "RESEARCH" to s(studio.mgn.mgn.R.string.section_research),
                "INTEL" to s(studio.mgn.mgn.R.string.section_intel),
                "HISTORY" to s(studio.mgn.mgn.R.string.section_history),
                "ACHIEVEMENTS" to s(studio.mgn.mgn.R.string.section_achievements),
                "SETTINGS" to s(studio.mgn.mgn.R.string.section_settings),
            ),
        )
    }
}
