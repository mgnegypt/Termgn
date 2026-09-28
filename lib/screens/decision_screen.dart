import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../engine/balance_config.dart';
import '../graphics/theme/palette.dart';
import '../graphics/theme/typography.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// Pending decisions queue with gem reroll.
class DecisionScreen extends ConsumerWidget {
  const DecisionScreen({super.key});

  static final NumberFormat _money = NumberFormat.decimalPattern('ar');

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const Center(child: Text('لا توجد لعبة نشطة'));
    }
    if (session.pendingEvents.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Text('لا قرارات معلقة', style: AppTypography.titleMedium),
              const SizedBox(height: 8),
              const Text('تقدّم دورًا لتظهر أحداث جديدة',
                  style: AppTypography.bodySecondary),
              if (session.lastResolution != null) ...<Widget>[
                const SizedBox(height: 16),
                Panel(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(session.lastResolution!.event.title,
                          style: AppTypography.label),
                      const SizedBox(height: 6),
                      Text(session.lastResolution!.choice.resultText,
                          style: AppTypography.bodySecondary),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      );
    }
    final event = session.currentEvent!;
    final s = session.state;
    final bool canReroll =
        s.gems >= BalanceConfig.gemsPerDecisionReroll;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        Text('قرار ${session.pendingEvents.length} معلق',
            style: AppTypography.caption),
        const SizedBox(height: 8),
        Panel(
          highlightGold: event.isCrisis,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(event.isCrisis ? 'أزمة' : 'قرار',
                  style: AppTypography.label.copyWith(
                    color: event.isCrisis
                        ? Palette.negative
                        : Palette.gold,
                  )),
              const SizedBox(height: 6),
              Text(event.title, style: AppTypography.titleMedium),
              const SizedBox(height: 8),
              Text(event.description, style: AppTypography.bodySecondary),
              const SizedBox(height: 16),
              for (final choice in event.choices)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: SizedBox(
                    width: double.infinity,
                    child: OutlinedButton(
                      onPressed: choice.isSelectable(s)
                          ? () => ref
                              .read(gameProvider.notifier)
                              .choose(event, choice)
                          : null,
                      child: Text(
                        choice.effectiveCostCash > 0
                            ? '${choice.label} (${_money.format(choice.effectiveCostCash.round())})'
                            : choice.label,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                ),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: canReroll
                    ? () =>
                        ref.read(gameProvider.notifier).rerollDecision()
                    : null,
                child: Text(
                    'استبدال القرار (${BalanceConfig.gemsPerDecisionReroll} جواهر — لديك ${s.gems})'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
