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

private fun playMood(app: MgnApp, mood: studio.mgn.audio.MusicKey?) {
    app.musicMood = mood
    app.audio.setMusic(mood)
}
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
            androidx.compose.runtime.LaunchedEffect(Unit) {
                playMood(app, studio.mgn.audio.MusicKey.MENU)
            }
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
                    reviewLine = { name, title ->
                        app.getString(
                            studio.mgn.mgn.R.string.setup_review_line, name, title,
                        )
                    },
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
            androidx.compose.runtime.LaunchedEffect(Unit) {
                playMood(app, studio.mgn.audio.MusicKey.CALM)
            }
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
            androidx.compose.runtime.LaunchedEffect(vm) {
                vm.signals.collect { signal ->
                    when (signal) {
                        studio.mgn.command.CommandSignal.DecisionsOpened -> Unit
                        studio.mgn.command.CommandSignal.NavigateMenu -> {
                            nav.navigate(Routes.MENU) {
                                popUpTo(Routes.GAME) { inclusive = true }
                            }
                        }
                        studio.mgn.command.CommandSignal.NavigateEconomy ->
                            nav.navigate(Routes.ECONOMY)
                        studio.mgn.command.CommandSignal.NavigateDiplomacy ->
                            nav.navigate(Routes.DIPLOMACY)
                        studio.mgn.command.CommandSignal.NavigateDevelopment ->
                            nav.navigate(Routes.DEVELOPMENT)
                        studio.mgn.command.CommandSignal.NavigateSettings ->
                            nav.navigate(Routes.SETTINGS)
                    }
                }
            }
            CommandScreen(
                viewModel = vm,
                content = app.content,
                strings = rememberCommandStrings(app),
                reduceMotion = reduceMotion,
            )
        }
        composable(Routes.ECONOMY) {
            val vm: studio.mgn.economy.EconomyViewModel = viewModel {
                studio.mgn.economy.EconomyViewModel(app.repository, app.content)
            }
            studio.mgn.economy.EconomyScreen(
                viewModel = vm,
                strings = rememberEconomyStrings(app),
                reduceMotion = reduceMotion,
            )
        }
        composable(Routes.DIPLOMACY) {
            val vm: studio.mgn.diplomacy.DiplomacyViewModel = viewModel {
                studio.mgn.diplomacy.DiplomacyViewModel(app.repository, app.content)
            }
            studio.mgn.diplomacy.DiplomacyScreen(
                viewModel = vm,
                strings = rememberDiplomacyStrings(app),
            )
        }
        composable(Routes.DEVELOPMENT) {
            val vm: studio.mgn.development.DevelopmentViewModel = viewModel {
                studio.mgn.development.DevelopmentViewModel(app.repository, app.content)
            }
            studio.mgn.development.DevelopmentScreen(
                viewModel = vm,
                strings = rememberDevelopmentStrings(app),
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
            unlocksLabel = s(studio.mgn.mgn.R.string.cmd_unlocks),
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
            turnBadge = { turn ->
                res.getString(studio.mgn.mgn.R.string.cmd_turn_badge, turn)
            },
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
            history = studio.mgn.command.HistoryStrings(
                title = s(studio.mgn.mgn.R.string.hist_title),
                empty = s(studio.mgn.mgn.R.string.hist_empty),
                turnLabel = s(studio.mgn.mgn.R.string.hist_turn),
                back = s(studio.mgn.mgn.R.string.hist_back),
                filterAll = s(studio.mgn.mgn.R.string.hist_all),
                filterEconomy = s(studio.mgn.mgn.R.string.hist_economy),
                filterDiplomacy = s(studio.mgn.mgn.R.string.hist_diplomacy),
                filterEvents = s(studio.mgn.mgn.R.string.hist_events),
            ),
            achievementsGrid = studio.mgn.command.AchievementsGridStrings(
                title = s(studio.mgn.mgn.R.string.section_achievements),
                empty = s(studio.mgn.mgn.R.string.hist_empty),
                close = s(studio.mgn.mgn.R.string.yr_close),
                gemsSuffix = s(studio.mgn.mgn.R.string.cmd_gems),
                progress = { a, b ->
                    res.getString(
                        studio.mgn.mgn.R.string.achievements_progress,
                        a,
                        b,
                        if (b == 0) 0 else a * 100 / b,
                    )
                },
            ),
            yearly = studio.mgn.command.YearlyStrings(
                title = s(studio.mgn.mgn.R.string.yr_title),
                score = s(studio.mgn.mgn.R.string.yr_score),
                gems = s(studio.mgn.mgn.R.string.yr_gems),
                bestDecision = s(studio.mgn.mgn.R.string.yr_best),
                worstDecision = s(studio.mgn.mgn.R.string.yr_worst),
                close = s(studio.mgn.mgn.R.string.yr_close),
            ),
            referendum = studio.mgn.command.ReferendumStrings(
                title = s(studio.mgn.mgn.R.string.ref_title),
                passed = s(studio.mgn.mgn.R.string.ref_passed),
                failed = s(studio.mgn.mgn.R.string.ref_failed),
                close = s(studio.mgn.mgn.R.string.ref_close),
            ),
            ending = studio.mgn.command.EndingStrings(
                victoryTitle = s(studio.mgn.mgn.R.string.end_victory),
                collapseTitle = s(studio.mgn.mgn.R.string.end_collapse),
                continuationTitle = s(studio.mgn.mgn.R.string.end_continuation),
                turnsLabel = s(studio.mgn.mgn.R.string.end_turns),
                scoreLabel = s(studio.mgn.mgn.R.string.end_score),
                achievementsLabel = s(studio.mgn.mgn.R.string.end_achievements),
                bestDecision = s(studio.mgn.mgn.R.string.end_best),
                worstDecision = s(studio.mgn.mgn.R.string.end_worst),
                bestTurn = s(studio.mgn.mgn.R.string.end_best_turn),
                worstTurn = s(studio.mgn.mgn.R.string.end_worst_turn),
                menu = s(studio.mgn.mgn.R.string.end_menu),
                newGame = s(studio.mgn.mgn.R.string.end_new),
                continuePlaying = s(studio.mgn.mgn.R.string.end_continue),
            ),
        )
    }
}

@Composable
private fun rememberEconomyStrings(app: MgnApp): studio.mgn.economy.EconomyStrings {
    val res = app.resources
    fun s(id: Int): String = res.getString(id)
    val keyLabels = mapOf(
        "agriculture" to s(studio.mgn.mgn.R.string.key_agriculture),
        "industry" to s(studio.mgn.mgn.R.string.key_industry),
        "energy" to s(studio.mgn.mgn.R.string.key_energy),
        "technology" to s(studio.mgn.mgn.R.string.key_technology),
        "tourism" to s(studio.mgn.mgn.R.string.key_tourism),
        "health" to s(studio.mgn.mgn.R.string.key_health),
        "education" to s(studio.mgn.mgn.R.string.key_education),
    )
    return remember {
        studio.mgn.economy.EconomyStrings(
            title = s(studio.mgn.mgn.R.string.eco_title),
            dialsTitle = s(studio.mgn.mgn.R.string.eco_dials),
            taxLabel = s(studio.mgn.mgn.R.string.eco_tax),
            militaryLabel = s(studio.mgn.mgn.R.string.eco_military),
            subsidyLabel = s(studio.mgn.mgn.R.string.eco_subsidy),
            incomeLabel = s(studio.mgn.mgn.R.string.eco_income),
            expensesLabel = s(studio.mgn.mgn.R.string.eco_expenses),
            netLabel = s(studio.mgn.mgn.R.string.eco_net),
            previewLabel = s(studio.mgn.mgn.R.string.eco_preview),
            confirmDials = s(studio.mgn.mgn.R.string.eco_confirm),
            investTitle = s(studio.mgn.mgn.R.string.eco_invest),
            investBlocked = s(studio.mgn.mgn.R.string.eco_invest_blocked),
            loanTitle = s(studio.mgn.mgn.R.string.eco_loan),
            loanAmountHint = s(studio.mgn.mgn.R.string.eco_loan_hint),
            borrowAction = s(studio.mgn.mgn.R.string.eco_borrow),
            repayAction = s(studio.mgn.mgn.R.string.eco_repay),
            debtLabel = s(studio.mgn.mgn.R.string.eco_debt),
            interestLabel = s(studio.mgn.mgn.R.string.eco_interest),
            debtWarning = s(studio.mgn.mgn.R.string.eco_debt_warning),
            chartTitle = s(studio.mgn.mgn.R.string.eco_chart),
            chartEmpty = s(studio.mgn.mgn.R.string.eco_chart_empty),
            percentSuffix = s(studio.mgn.mgn.R.string.eco_percent_suffix),
            keyLabels = keyLabels,
        )
    }
}

@Composable
private fun rememberDiplomacyStrings(app: MgnApp): studio.mgn.diplomacy.DiplomacyStrings {
    val res = app.resources
    fun s(id: Int): String = res.getString(id)
    return remember {
        studio.mgn.diplomacy.DiplomacyStrings(
            title = s(studio.mgn.mgn.R.string.dip_title),
            relationLabel = s(studio.mgn.mgn.R.string.dip_relation),
            treatiesLabel = s(studio.mgn.mgn.R.string.dip_treaties),
            noTreaties = s(studio.mgn.mgn.R.string.dip_no_treaties),
            signTreaty = s(studio.mgn.mgn.R.string.dip_sign),
            breakTreaty = s(studio.mgn.mgn.R.string.dip_break),
            declareWar = s(studio.mgn.mgn.R.string.dip_war),
            warConfirmTitle = s(studio.mgn.mgn.R.string.dip_war_title),
            warConfirmMessage = s(studio.mgn.mgn.R.string.dip_war_msg),
            warConfirmOk = s(studio.mgn.mgn.R.string.dip_war_ok),
            cancel = s(studio.mgn.mgn.R.string.dip_cancel),
            stanceLabel = s(studio.mgn.mgn.R.string.dip_stance),
            stanceDefensive = s(studio.mgn.mgn.R.string.dip_defensive),
            stanceOffensive = s(studio.mgn.mgn.R.string.dip_offensive),
            stanceNegotiate = s(studio.mgn.mgn.R.string.dip_negotiate),
            atWarLabel = s(studio.mgn.mgn.R.string.dip_at_war),
            sanctionedLabel = s(studio.mgn.mgn.R.string.dip_sanctioned),
            warCostNote = s(studio.mgn.mgn.R.string.dip_war_cost),
            treatyNames = mapOf(
                "TRADE" to s(studio.mgn.mgn.R.string.treaty_TRADE),
                "DEFENSIVE" to s(studio.mgn.mgn.R.string.treaty_DEFENSIVE),
                "NON_AGGRESSION" to s(studio.mgn.mgn.R.string.treaty_NON_AGGRESSION),
                "EMBASSY" to s(studio.mgn.mgn.R.string.treaty_EMBASSY),
            ),
            stanceNames = mapOf(
                "DEFENSIVE" to s(studio.mgn.mgn.R.string.dip_defensive),
                "OFFENSIVE" to s(studio.mgn.mgn.R.string.dip_offensive),
                "NEGOTIATE" to s(studio.mgn.mgn.R.string.dip_negotiate),
            ),
        )
    }
}

@Composable
private fun rememberDevelopmentStrings(app: MgnApp): studio.mgn.development.DevelopmentStrings {
    val res = app.resources
    fun s(id: Int): String = res.getString(id)
    val keyLabels = mapOf(
        "economy" to s(studio.mgn.mgn.R.string.key_economy),
        "publicSatisfaction" to s(studio.mgn.mgn.R.string.key_publicSatisfaction),
        "culture" to s(studio.mgn.mgn.R.string.key_culture),
        "education" to s(studio.mgn.mgn.R.string.key_education),
        "technology" to s(studio.mgn.mgn.R.string.key_technology),
        "tourism" to s(studio.mgn.mgn.R.string.key_tourism),
        "health" to s(studio.mgn.mgn.R.string.key_health),
        "energy" to s(studio.mgn.mgn.R.string.key_energy),
        "agriculture" to s(studio.mgn.mgn.R.string.key_agriculture),
        "industry" to s(studio.mgn.mgn.R.string.key_industry),
        "foodSecurity" to s(studio.mgn.mgn.R.string.key_foodSecurity),
        "environment" to s(studio.mgn.mgn.R.string.key_environment),
        "militarySecurity" to s(studio.mgn.mgn.R.string.key_militarySecurity),
        "digitalOpinion" to s(studio.mgn.mgn.R.string.key_digitalOpinion),
    )
    return remember {
        studio.mgn.development.DevelopmentStrings(
            title = s(studio.mgn.mgn.R.string.dev_title),
            tabAvailable = s(studio.mgn.mgn.R.string.dev_available),
            tabBuilding = s(studio.mgn.mgn.R.string.dev_building),
            tabBuilt = s(studio.mgn.mgn.R.string.dev_built),
            emptyAvailable = s(studio.mgn.mgn.R.string.dev_empty_available),
            emptyBuilding = s(studio.mgn.mgn.R.string.dev_empty_building),
            emptyBuilt = s(studio.mgn.mgn.R.string.dev_empty_built),
            buildNow = s(studio.mgn.mgn.R.string.dev_build),
            rushLabel = s(studio.mgn.mgn.R.string.dev_rush),
            builtLabel = s(studio.mgn.mgn.R.string.dev_built_done),
            remainingTurns = s(studio.mgn.mgn.R.string.dev_remaining),
            costLabel = s(studio.mgn.mgn.R.string.dev_cost),
            durationLabel = s(studio.mgn.mgn.R.string.dev_duration),
            requiresLabel = s(studio.mgn.mgn.R.string.dev_requires),
            gemsSuffix = s(studio.mgn.mgn.R.string.cmd_gems),
            keyLabels = keyLabels,
        )
    }
}
