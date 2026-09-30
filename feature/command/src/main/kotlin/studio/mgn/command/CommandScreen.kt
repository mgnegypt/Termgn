package studio.mgn.command

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio.mgn.audio.AudioManager
import studio.mgn.audio.HapticStrength
import studio.mgn.audio.NoopAudioManager
import studio.mgn.audio.SoundKey
import studio.mgn.content.ContentPack
import studio.mgn.design.AnimationKeys
import studio.mgn.design.GameAnimationSpot
import studio.mgn.design.GameButton
import studio.mgn.design.GoldFramePanel
import studio.mgn.design.MgnTheme
import studio.mgn.engine.BalanceConfig

/**
 * Command screen: the primary face of the game in landscape.
 * Wide (≥900dp): full grid. Narrow: indicators/missions collapse into a
 * bottom pull panel and the map shrinks (see [CompactCommand]).
 */
@Composable
fun CommandScreen(
    viewModel: CommandViewModel,
    content: ContentPack,
    strings: CommandStrings,
    reduceMotion: Boolean,
    audio: AudioManager = NoopAudioManager(),
) {
    val ui by viewModel.state.collectAsState()
    val state = ui.state
    if (ui.isLoading || state == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MgnTheme.colors.bgBase),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "…",
                color = MgnTheme.colors.textSecondary,
            )
        }
        return
    }
    if (ui.showDecisions) {
        DecisionScreen(
            pending = ui.pending,
            gems = state.gems,
            rerollCost = BalanceConfig.GEMS_PER_DECISION_REROLL,
            choiceStates = ui.choiceStates,
            lastResolution = ui.lastResolution,
            strings = strings,
            onChoose = { id, index ->
                audio.play(SoundKey.BUTTON)
                viewModel.onEvent(CommandEvent.Choose(id, index))
            },
            onReroll = {
                audio.play(SoundKey.REWARD)
                viewModel.onEvent(CommandEvent.Reroll)
            },
            onClose = { viewModel.onEvent(CommandEvent.ShowDecisions(false)) },
        )
        return
    }
    ui.ending?.let { ending ->
        androidx.compose.runtime.LaunchedEffect(ending) {
            if (ending == GameEndingScreen.COLLAPSE) {
                audio.vibrate(HapticStrength.HEAVY)
            }
        }
        EndingScreen(
            ending = ending,
            state = state,
            summary = ReignSummary(
                turns = state.turnNumber,
                score = state.nationScore,
                achievements = state.unlockedAchievements.size,
                achievementsTotal = content.achievements.size,
                bestDecisionTitle = viewModel.bestDecision()?.event?.title,
                worstDecisionTitle = viewModel.worstDecision()?.event?.title,
                bestTurn = viewModel.bestTurnInfo(),
                worstTurn = viewModel.worstTurnInfo(),
            ),
            strings = strings.ending,
            canContinue = ending != GameEndingScreen.COLLAPSE,
            animate = !reduceMotion,
            onMenu = { viewModel.onEvent(CommandEvent.GoToMenu) },
            onNewGame = { viewModel.onEvent(CommandEvent.GoToMenu) },
            onContinue = { viewModel.onEvent(CommandEvent.DismissEnding) },
        )
        return
    }
    CommandContent(
        ui = ui,
        content = content,
        strings = strings,
        reduceMotion = reduceMotion,
        audio = audio,
        onEvent = viewModel::onEvent,
    )
    when (ui.overlays.firstOrNull()) {
        ReportOverlay.REPORT -> ui.lastReport?.let { report ->
            TurnReportSheet(
                report = report,
                before = ui.previous,
                strings = strings,
                onClose = { viewModel.onEvent(CommandEvent.DismissReport) },
            )
        }
        ReportOverlay.YEARLY -> ui.lastReport?.yearlyReport?.let { yearly ->
            YearlyReportSheet(
                report = yearly,
                best = viewModel.bestDecision(),
                worst = viewModel.worstDecision(),
                strings = strings.yearly,
                onClose = { viewModel.onEvent(CommandEvent.DismissReport) },
            )
        }
        ReportOverlay.REFERENDUM -> ui.lastReport?.referendum?.let { referendum ->
            ReferendumSheet(
                result = referendum,
                threshold = ui.referendumThreshold,
                animate = !reduceMotion,
                strings = strings.referendum,
                onClose = { viewModel.onEvent(CommandEvent.DismissReport) },
            )
        }
        null -> Unit
    }
    ui.selectedIndicator?.let { key ->
        IndicatorSheet(
            key = key,
            state = state,
            previous = ui.previous,
            finance = ui.lastReport?.finance,
            strings = strings,
            onDismiss = { viewModel.onEvent(CommandEvent.ShowIndicator(null)) },
        )
    }
    ui.selectedLandmark?.let { id ->
        val def = content.landmarkById(id)
        val pos = content.landmarkPositions.firstOrNull { it.id == id }
        LandmarkSheet(
            def = def,
            isDecor = pos?.decor ?: (def == null),
            decorNote = pos?.noteAr ?: "",
            isBuilt = state.builtLandmarks.any { it.id == id },
            turnsRemaining = state.underConstruction.firstOrNull { it.id == id }
                ?.turnsRemaining,
            buildTurns = def?.buildTurns ?: 0,
            canBuild = id in ui.buildable,
            canRush = state.gems >= BalanceConfig.GEMS_PER_CONSTRUCTION_TURN_SKIP,
            rushCost = BalanceConfig.GEMS_PER_CONSTRUCTION_TURN_SKIP,
            strings = strings,
            onBuild = {
                audio.play(SoundKey.BUILD)
                viewModel.onEvent(CommandEvent.StartConstruction(id))
            },
            onRush = {
                audio.play(SoundKey.REWARD)
                viewModel.onEvent(CommandEvent.RushConstruction(id))
            },
            onDismiss = { viewModel.onEvent(CommandEvent.ShowLandmark(null)) },
        )
    }
    ui.selectedCountry?.let { id ->
        state.countries[id]?.let { country ->
            CountrySheet(
                country = country,
                strings = strings,
                treatyLabel = { strings.keyLabels[it] ?: it },
                onDismiss = { viewModel.onEvent(CommandEvent.ShowCountry(null)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandContent(
    ui: CommandUiState,
    content: ContentPack,
    strings: CommandStrings,
    reduceMotion: Boolean,
    audio: AudioManager = NoopAudioManager(),
    onEvent: (CommandEvent) -> Unit,
) {
    if (ui.showUnlockToast && ui.overlays.isEmpty()) {
        androidx.compose.runtime.LaunchedEffect(ui.lastReport) {
            audio.play(SoundKey.ACHIEVEMENT)
            audio.vibrate(HapticStrength.LIGHT)
        }
    }
    val state = ui.state ?: return
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth < 900.dp) {
            CompactCommand(ui, content, strings, reduceMotion, onEvent)
        } else {
            WideCommand(ui, content, strings, reduceMotion, onEvent)
        }
    }
}

@Composable
private fun WideCommand(
    ui: CommandUiState,
    content: ContentPack,
    strings: CommandStrings,
    reduceMotion: Boolean,
    onEvent: (CommandEvent) -> Unit,
) {
    val state = ui.state ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        CommandTopBar(
            state = state,
            previous = ui.previous,
            strings = strings,
            animate = !reduceMotion,
            onCellClick = { onEvent(CommandEvent.ShowIndicator(it)) },
            onEndTurn = {
                audio.play(SoundKey.TURN_END)
                onEvent(CommandEvent.EndTurn)
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CommandSideRail(
                selected = ui.section,
                pendingCount = ui.pending.size,
                strings = strings,
                onSelect = { onEvent(CommandEvent.SelectSection(it)) },
            )
            if (ui.section == CommandSection.DASHBOARD) {
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (ui.showUnlockToast && ui.overlays.isEmpty()) {
                        UnlockToast(
                            count = (ui.lastReport?.newAchievements?.size ?: 0) +
                                (ui.lastReport?.newMissions?.size ?: 0),
                            strings = strings,
                            animate = !reduceMotion,
                        )
                    }
                    CityScene(
                        state = state,
                        positions = content.landmarkPositions,
                        signLabel = { id ->
                            content.landmarkById(id)?.nameAr
                                ?: content.landmarkPositions
                                    .firstOrNull { it.id == id }
                                    ?.noteAr?.take(20)
                                ?: id
                        },
                        progressOf = { id ->
                            val queued = state.underConstruction
                                .firstOrNull { it.id == id } ?: return@CityScene null
                            val total = content.landmarkById(id)?.buildTurns
                                ?.coerceAtLeast(1) ?: return@CityScene null
                            1f - queued.turnsRemaining / total.toFloat()
                        },
                        animate = !reduceMotion,
                        onSignClick = { onEvent(CommandEvent.ShowLandmark(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IndicatorsPanel(
                            state = state,
                            previous = ui.previous,
                            strings = strings,
                            animate = !reduceMotion,
                            onIndicatorClick = {
                                onEvent(CommandEvent.ShowIndicator(it))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MissionsPanel(
                            missions = ui.missions,
                            completedCount = ui.completedMissionCount,
                            strings = strings,
                            onShowAll = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WorldMap(
                        countries = state.countries.values.toList(),
                        playerName = state.countryName,
                        selectedId = ui.selectedCountry,
                        onSelect = { onEvent(CommandEvent.ShowCountry(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                    EventsPanel(
                        history = state.history,
                        pendingCount = ui.pending.size,
                        imageKeyOf = { "" },
                        strings = strings,
                        turnBadge = strings.turnBadge,
                        onOpenDecisions = {
                            onEvent(CommandEvent.ShowDecisions(true))
                        },
                    )
                }
            } else if (ui.section == CommandSection.HISTORY) {
                HistoryScreen(
                    history = state.history,
                    strings = strings.history,
                    onBack = { onEvent(CommandEvent.SelectSection(CommandSection.DASHBOARD)) },
                    modifier = Modifier.weight(2.4f),
                )
            } else if (ui.section == CommandSection.ACHIEVEMENTS) {
                AchievementsGrid(
                    achievements = content.achievements,
                    unlocked = state.unlockedAchievements,
                    strings = strings.achievementsGrid,
                    onBack = { onEvent(CommandEvent.SelectSection(CommandSection.DASHBOARD)) },
                    modifier = Modifier.weight(2.4f),
                )
            } else {
                SectionPlaceholder(
                    section = ui.section,
                    strings = strings,
                    modifier = Modifier.weight(2.4f),
                )
            }
        }
        EndTurnBar(
            processing = ui.processing,
            pendingCount = ui.pending.size,
            strings = strings,
            reduceMotion = reduceMotion,
            onEndTurn = {
                audio.play(SoundKey.TURN_END)
                onEvent(CommandEvent.EndTurn)
            },
        )
    }
}

/** Narrow layout: scene + mini-map on top, indicators/missions in a drag panel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactCommand(
    ui: CommandUiState,
    content: ContentPack,
    strings: CommandStrings,
    reduceMotion: Boolean,
    onEvent: (CommandEvent) -> Unit,
) {
    val state = ui.state ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        CommandTopBar(
            state = state,
            previous = ui.previous,
            strings = strings,
            animate = !reduceMotion,
            onCellClick = { onEvent(CommandEvent.ShowIndicator(it)) },
            onEndTurn = {
                audio.play(SoundKey.TURN_END)
                onEvent(CommandEvent.EndTurn)
            },
            modifier = Modifier.padding(8.dp),
        )
        if (ui.section == CommandSection.DASHBOARD) {
            BottomSheetScaffold(
                sheetContent = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        IndicatorsPanel(
                            state = state,
                            previous = ui.previous,
                            strings = strings,
                            animate = !reduceMotion,
                            onIndicatorClick = {
                                onEvent(CommandEvent.ShowIndicator(it))
                            },
                        )
                        MissionsPanel(
                            missions = ui.missions,
                            completedCount = ui.completedMissionCount,
                            strings = strings,
                            onShowAll = {},
                        )
                    }
                },
                sheetPeekHeight = 140.dp,
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CityScene(
                        state = state,
                        positions = content.landmarkPositions,
                        signLabel = { id -> content.landmarkById(id)?.nameAr ?: id },
                        progressOf = { null },
                        animate = !reduceMotion,
                        onSignClick = { onEvent(CommandEvent.ShowLandmark(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                    WorldMap(
                        countries = state.countries.values.toList(),
                        playerName = state.countryName,
                        selectedId = ui.selectedCountry,
                        onSelect = { onEvent(CommandEvent.ShowCountry(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                    EventsPanel(
                        history = state.history,
                        pendingCount = ui.pending.size,
                        imageKeyOf = { "" },
                        strings = strings,
                        turnBadge = strings.turnBadge,
                        onOpenDecisions = {
                            onEvent(CommandEvent.ShowDecisions(true))
                        },
                    )
                }
            }
        } else if (ui.section == CommandSection.HISTORY) {
            HistoryScreen(
                history = state.history,
                strings = strings.history,
                onBack = { onEvent(CommandEvent.SelectSection(CommandSection.DASHBOARD)) },
                modifier = Modifier.weight(1f),
            )
        } else if (ui.section == CommandSection.ACHIEVEMENTS) {
            AchievementsGrid(
                achievements = content.achievements,
                unlocked = state.unlockedAchievements,
                strings = strings.achievementsGrid,
                onBack = { onEvent(CommandEvent.SelectSection(CommandSection.DASHBOARD)) },
                modifier = Modifier.weight(1f),
            )
        } else {
            SectionPlaceholder(
                section = ui.section,
                strings = strings,
                modifier = Modifier.weight(1f),
            )
        }
        EndTurnBar(
            processing = ui.processing,
            pendingCount = ui.pending.size,
            strings = strings,
            reduceMotion = reduceMotion,
            onEndTurn = {
                audio.play(SoundKey.TURN_END)
                onEvent(CommandEvent.EndTurn)
            },
        )
    }
}

@Composable
private fun UnlockToast(count: Int, strings: CommandStrings, animate: Boolean) {
    if (count <= 0) return
    GoldFramePanel(elevated = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GameAnimationSpot(
                key = AnimationKeys.ACHIEVEMENT_UNLOCK,
                animate = animate,
                sizeDp = 96,
            )
            Text(
                text = "◆ ${strings.unlocksLabel} (+$count)",
                style = MgnTheme.typography.titleMedium,
                color = MgnTheme.colors.goldPrimary,
            )
        }
    }
}

@Composable
private fun SectionPlaceholder(
    section: CommandSection,
    strings: CommandStrings,
    modifier: Modifier = Modifier,
) {
    GoldFramePanel(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.sectionLabels[section.name] ?: section.name,
                style = MgnTheme.typography.titleLarge,
                color = MgnTheme.colors.goldPrimary,
            )
            Text(
                text = strings.placeholderSection,
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
fun EndTurnBar(
    processing: Boolean,
    pendingCount: Int,
    strings: CommandStrings,
    reduceMotion: Boolean,
    onEndTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MgnTheme.colors.bgPanel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (pendingCount > 0) {
            Text(
                text = "${strings.decisionsPending}: $pendingCount",
                style = MgnTheme.typography.bodyMedium,
                color = MgnTheme.colors.negative,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        GameButton(
            text = if (processing) strings.processing else strings.endTurn,
            onClick = onEndTurn,
            enabled = !processing,
            reduceMotion = reduceMotion,
            modifier = Modifier.width(280.dp),
        )
        Spacer(Modifier.weight(1f))
    }
}

// ── Previews at the required sizes ──

@Composable
private fun previewUi(): CommandUiState {
    val state = previewState()
    return CommandUiState(
        isLoading = false,
        state = state,
        previous = state.copy(turnNumber = 13, treasuryCash = 290000.0),
        pending = emptyList(),
        missions = emptyList(),
        completedMissionCount = 1,
    )
}

@Preview(name = "Command-800x360", widthDp = 800, heightDp = 360)
@Composable
private fun Command800Preview() {
    MgnTheme {
        CommandContent(
            ui = previewUi(),
            content = previewPack(),
            strings = previewStrings(),
            reduceMotion = true,
            onEvent = {},
        )
    }
}

@Preview(name = "Command-900x400", widthDp = 900, heightDp = 400)
@Composable
private fun Command900Preview() {
    MgnTheme {
        CommandContent(
            ui = previewUi(),
            content = previewPack(),
            strings = previewStrings(),
            reduceMotion = true,
            onEvent = {},
        )
    }
}

@Preview(name = "Command-1080x540", widthDp = 1080, heightDp = 540)
@Composable
private fun Command1080Preview() {
    MgnTheme {
        CommandContent(
            ui = previewUi(),
            content = previewPack(),
            strings = previewStrings(),
            reduceMotion = true,
            onEvent = {},
        )
    }
}

@Preview(name = "Command-tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun CommandTabletPreview() {
    MgnTheme {
        CommandContent(
            ui = previewUi(),
            content = previewPack(),
            strings = previewStrings(),
            reduceMotion = true,
            onEvent = {},
        )
    }
}
