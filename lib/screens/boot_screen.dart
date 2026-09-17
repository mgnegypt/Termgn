import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../data/events_data.dart';
import '../data/landmarks_data.dart';
import '../engine/economy_calculator.dart';
import '../engine/event_engine.dart';
import '../graphics/theme/palette.dart';
import '../graphics/theme/typography.dart';
import '../models/enums.dart';
import '../models/event.dart';
import '../models/game_state.dart';
import '../models/history_entry.dart';
import '../state/game_provider.dart';

/// Temporary shell that exercises the full engine loop end to end.
///
/// The models, engines and content are complete; the designed screens
/// (dashboard, decision card, diplomacy, landmarks, sectors) replace this
/// screen in the UI phase. It is intentionally plain: no bespoke painters or
/// SVG yet, just the real game loop wired to the real state.
class BootScreen extends ConsumerWidget {
  const BootScreen({super.key});

  static final NumberFormat _money = NumberFormat.decimalPattern('ar');

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: session == null
              ? _NewGamePanel(onStart: () => _startGame(ref))
              : _SessionPanel(session: session),
        ),
      ),
    );
  }

  static Future<void> _startGame(WidgetRef ref) {
    return ref.read(gameProvider.notifier).startNewGame(
          countryName: 'المجد',
          rulerTitle: 'رئيس',
          turnLength: TurnLength.month,
          flagPrimaryColor: Palette.black.toARGB32(),
          flagSecondaryColor: Palette.gold.toARGB32(),
          flagSymbolId: 'crescent_star',
        );
  }
}

class _NewGamePanel extends StatelessWidget {
  const _NewGamePanel({required this.onStart});

  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          const Text('MGN', style: AppTypography.numericLarge),
          const SizedBox(height: 8),
          const Text('إنتاج MGN STUDIO', style: AppTypography.caption),
          const SizedBox(height: 32),
          const Text(
            'محاكاة إدارة دولة وبناء حضارة — بلا إنترنت، بلا سيرفر.',
            style: AppTypography.bodySecondary,
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 32),
          FilledButton(onPressed: onStart, child: const Text('ابدأ حكمًا جديدًا')),
          const SizedBox(height: 24),
          Text(
            'المحتوى الجاهز: ${EventsData.all.length} حدث • '
            '${LandmarksData.all.length} معلم',
            style: AppTypography.caption,
          ),
        ],
      ),
    );
  }
}

class _SessionPanel extends ConsumerWidget {
  const _SessionPanel({required this.session});

  final GameSession session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameState s = session.state;
    final TurnFinance? finance = ref.watch(financeProvider);

    return ListView(
      children: <Widget>[
        _Header(state: s, finance: finance),
        const SizedBox(height: 20),
        if (session.currentEvent != null)
          _DecisionCard(
            event: session.currentEvent!,
            state: s,
            onChoose: (EventChoice choice) => ref
                .read(gameProvider.notifier)
                .choose(session.currentEvent!, choice),
          )
        else
          _NextTurnButton(
            onPressed: () => ref.read(gameProvider.notifier).nextTurn(),
          ),
        const SizedBox(height: 24),
        _IndicatorSection(title: 'المؤشرات', keys: StateKeys.indicators, state: s),
        const SizedBox(height: 16),
        _IndicatorSection(title: 'القطاعات', keys: StateKeys.sectors, state: s),
        const SizedBox(height: 16),
        if (session.lastResolution != null)
          _ResolutionPanel(resolution: session.lastResolution!),
        _Chronicle(state: s),
        const SizedBox(height: 24),
        Center(
          child: OutlinedButton(
            onPressed: () => ref.read(gameProvider.notifier).deleteSave(),
            child: const Text('حذف الحفظ والبدء من جديد'),
          ),
        ),
      ],
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.state, required this.finance});

  final GameState state;
  final TurnFinance? finance;

  static const List<String> _seasons = <String>['ربيع', 'صيف', 'خريف', 'شتاء'];

  @override
  Widget build(BuildContext context) {
    final String season = _seasons[state.currentSeason.index];
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: Palette.panel,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Palette.borderGold),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text('${state.rulerTitle} ${state.countryName}',
              style: AppTypography.titleLarge),
          const SizedBox(height: 4),
          Text(
            'الدور ${state.turnNumber} — '
            '${state.inGameDate.year}/${state.inGameDate.month} — $season',
            style: AppTypography.caption,
          ),
          const Divider(height: 24),
          Row(
            children: <Widget>[
              _Stat(label: 'الخزينة', value: BootScreen._money.format(state.treasuryCash.round())),
              _Stat(label: 'الجواهر', value: '${state.gems}'),
              _Stat(label: 'الدين', value: BootScreen._money.format(state.debt.round())),
            ],
          ),
          if (finance != null) ...<Widget>[
            const SizedBox(height: 12),
            Text(
              'صافي الدور: ${finance!.net >= 0 ? '+' : ''}'
              '${BootScreen._money.format(finance!.net.round())}',
              style: AppTypography.numeric.copyWith(
                color: Palette.forDelta(finance!.net),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(label, style: AppTypography.label),
          const SizedBox(height: 2),
          Text(value, style: AppTypography.numeric),
        ],
      ),
    );
  }
}

class _NextTurnButton extends StatelessWidget {
  const _NextTurnButton({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: FilledButton(
        onPressed: onPressed,
        child: const Text('الدور القادم'),
      ),
    );
  }
}

class _DecisionCard extends StatelessWidget {
  const _DecisionCard({
    required this.event,
    required this.state,
    required this.onChoose,
  });

  final GameEvent event;
  final GameState state;
  final ValueChanged<EventChoice> onChoose;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Palette.surfaceRaised,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: event.isCrisis ? Palette.negative : Palette.border,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(event.isCrisis ? 'أزمة' : 'قرار', style: AppTypography.label),
          const SizedBox(height: 6),
          Text(event.title, style: AppTypography.titleMedium),
          const SizedBox(height: 8),
          Text(event.description, style: AppTypography.bodySecondary),
          const SizedBox(height: 16),
          for (final EventChoice choice in event.choices)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: choice.isSelectable(state)
                      ? () => onChoose(choice)
                      : null,
                  child: Text(
                    choice.effectiveCostCash > 0
                        ? '${choice.label} '
                            '(${BootScreen._money.format(choice.effectiveCostCash.round())})'
                        : choice.label,
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ResolutionPanel extends StatelessWidget {
  const _ResolutionPanel({required this.resolution});

  final EventResolution resolution;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Palette.surfaceOverlay,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(resolution.event.title, style: AppTypography.label),
          const SizedBox(height: 6),
          Text(resolution.choice.resultText, style: AppTypography.bodySecondary),
          const SizedBox(height: 10),
          Wrap(
            spacing: 12,
            children: <Widget>[
              for (final MapEntry<String, double> e
                  in resolution.appliedEffects.entries)
                Text(
                  '${_labelFor(e.key)} ${e.value > 0 ? '+' : ''}'
                  '${e.value.toStringAsFixed(1)}',
                  style: AppTypography.caption
                      .copyWith(color: Palette.forDelta(e.value)),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _IndicatorSection extends StatelessWidget {
  const _IndicatorSection({
    required this.title,
    required this.keys,
    required this.state,
  });

  final String title;
  final List<String> keys;
  final GameState state;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(title, style: AppTypography.label),
        const SizedBox(height: 8),
        for (final String key in keys)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: <Widget>[
                SizedBox(
                  width: 120,
                  child: Text(_labelFor(key), style: AppTypography.bodySecondary),
                ),
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: state.readKey(key) / 100,
                      minHeight: 8,
                      backgroundColor: Palette.surfaceRaised,
                      valueColor: AlwaysStoppedAnimation<Color>(
                        Palette.forIndicator(state.readKey(key)),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  state.readKey(key).toStringAsFixed(0),
                  style: AppTypography.caption,
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _Chronicle extends StatelessWidget {
  const _Chronicle({required this.state});

  final GameState state;

  @override
  Widget build(BuildContext context) {
    final List<HistoryEntry> recent =
        state.history.reversed.take(6).toList(growable: false);
    if (recent.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const Text('السجل', style: AppTypography.label),
        const SizedBox(height: 8),
        for (final HistoryEntry entry in recent)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Text(
              '• ${entry.title}',
              style: AppTypography.caption,
            ),
          ),
      ],
    );
  }
}

/// Arabic labels for state keys, used until the localisation layer lands.
String _labelFor(String key) => switch (key) {
      StateKeys.economy => 'الاقتصاد',
      StateKeys.publicSatisfaction => 'رضا الشعب',
      StateKeys.digitalOpinion => 'الرأي الرقمي',
      StateKeys.militarySecurity => 'الأمن العسكري',
      StateKeys.cyberSecurity => 'الأمن السيبراني',
      StateKeys.environment => 'البيئة',
      StateKeys.culture => 'الثقافة',
      StateKeys.agriculture => 'الزراعة',
      StateKeys.industry => 'الصناعة',
      StateKeys.foodSecurity => 'الأمن الغذائي',
      StateKeys.energy => 'الطاقة',
      StateKeys.technology => 'التقنية',
      StateKeys.tourism => 'السياحة',
      StateKeys.health => 'الصحة',
      StateKeys.education => 'التعليم',
      StateKeys.treasuryCash => 'الخزينة',
      StateKeys.population => 'السكان',
      _ => key,
    };
