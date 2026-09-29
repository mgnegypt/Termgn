import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../data/landmarks_data.dart';
import '../engine/balance_config.dart';
import '../engine/construction_engine.dart';
import '../graphics/theme/typography.dart';
import '../models/landmark.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// Landmark catalogue, queue and gem rush.
class LandmarksScreen extends ConsumerWidget {
  const LandmarksScreen({super.key});

  static final NumberFormat _money = NumberFormat.decimalPattern('ar');

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const Center(child: Text('لا توجد لعبة نشطة'));
    }
    final s = session.state;
    final ctl = ref.read(gameProvider.notifier);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        if (s.underConstruction.isNotEmpty) ...<Widget>[
          const SectionTitle('قيد البناء'),
          for (final Landmark b in s.underConstruction)
            Panel(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(b.nameAr, style: AppTypography.titleMedium),
                  Text('متبقٍ ${b.turnsRemaining ?? 0} دور',
                      style: AppTypography.bodySecondary),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: s.gems >=
                            BalanceConfig.gemsPerConstructionTurnSkip
                        ? () => ctl.rushConstruction(b.id, 1)
                        : null,
                    child: Text(
                        'تسريع (${BalanceConfig.gemsPerConstructionTurnSkip} جواهر — لديك ${s.gems})'),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 12),
        ],
        if (s.builtLandmarks.isNotEmpty) ...<Widget>[
          SectionTitle('المكتملة (${s.builtLandmarks.length})'),
          Text(
            s.builtLandmarks.map((Landmark l) => l.nameAr).join('، '),
            style: AppTypography.bodySecondary,
          ),
          const SizedBox(height: 12),
        ],
        const SectionTitle('الكتالوج'),
        for (final Landmark l in LandmarksData.all)
          Panel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(l.nameAr, style: AppTypography.titleMedium),
                const SizedBox(height: 4),
                Text(l.descriptionAr,
                    style: AppTypography.bodySecondary),
                const SizedBox(height: 6),
                Text(
                  '${_money.format(l.costCash.round())} • ${l.buildTurns} دور'
                  '${l.requirements.isNotEmpty ? ' • يتطلب: ${l.requirements.entries.map((e) => '${labelFor(e.key)} ${e.value.round()}').join('، ')}' : ''}',
                  style: AppTypography.caption,
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: ConstructionEngine.canStart(s, l)
                      ? () => ctl.startConstruction(l)
                      : null,
                  child: Text(
                    s.builtLandmarks.any((Landmark b) => b.id == l.id)
                        ? 'مكتمل'
                        : s.underConstruction
                                .any((Landmark b) => b.id == l.id)
                            ? 'قيد البناء'
                            : 'ابنِ الآن',
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
