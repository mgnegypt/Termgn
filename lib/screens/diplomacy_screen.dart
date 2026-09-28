import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../graphics/theme/palette.dart';
import '../graphics/theme/typography.dart';
import '../models/diplomacy_state.dart';
import '../models/enums.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// World relations, treaties, war stances.
class DiplomacyScreen extends ConsumerWidget {
  const DiplomacyScreen({super.key});

  String _treatyLabel(TreatyType t) => switch (t) {
        TreatyType.trade => 'تجارية',
        TreatyType.defensive => 'دفاعية',
        TreatyType.nonAggression => 'عدم اعتداء',
        TreatyType.embassy => 'سفارة',
      };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const Center(child: Text('لا توجد لعبة نشطة'));
    }
    final s = session.state;
    final ctl = ref.read(gameProvider.notifier);
    final List<DiplomacyState> countries = s.countries.values.toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        Text(
          'في حرب: ${s.warEnemies.length} • عقوبات: ${s.sanctionCount}',
          style: AppTypography.bodySecondary,
        ),
        const SizedBox(height: 12),
        for (final DiplomacyState c in countries)
          Panel(
            highlightGold: c.atWar,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Expanded(
                      child: Text(c.nameAr,
                          style: AppTypography.titleMedium),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: c.atWar
                            ? Palette.negative.withValues(alpha: 0.15)
                            : Palette.surfaceOverlay,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        c.atWar
                            ? 'حرب'
                            : 'علاقة ${c.relation.toStringAsFixed(0)}',
                        style: TextStyle(
                          color: c.atWar
                              ? Palette.negative
                              : Palette.forIndicator(
                                  (c.relation + 100) / 2),
                        ),
                      ),
                    ),
                  ],
                ),
                if (c.sanctioned)
                  const Text('عقوبات دولية مفروضة',
                      style: TextStyle(color: Palette.warning)),
                if (c.treaties.isNotEmpty)
                  Text(
                    'المعاهدات: ${c.treaties.map(_treatyLabel).join('، ')}',
                    style: AppTypography.bodySecondary,
                  ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: <Widget>[
                    for (final TreatyType t in TreatyType.values)
                      if (!c.treaties.contains(t) && !c.atWar)
                        OutlinedButton(
                          onPressed: ctl.diplomacy.canSign(s, c.countryId, t)
                              ? () => ctl.signTreaty(c.countryId, t)
                              : null,
                          child: Text(_treatyLabel(t)),
                        )
                      else if (c.treaties.contains(t))
                        OutlinedButton(
                          onPressed: () =>
                              ctl.breakTreaty(c.countryId, t),
                          child: Text('فسخ ${_treatyLabel(t)}'),
                        ),
                    if (!c.atWar)
                      OutlinedButton(
                        onPressed: () => ctl.declareWar(c.countryId),
                        child: const Text('إعلان حرب'),
                      )
                    else
                      DropdownButton<WarStance>(
                        value: ctl.stanceFor(c.countryId),
                        items: const <DropdownMenuItem<WarStance>>[
                          DropdownMenuItem(
                              value: WarStance.defensive,
                              child: Text('دفاع')),
                          DropdownMenuItem(
                              value: WarStance.offensive,
                              child: Text('هجوم')),
                          DropdownMenuItem(
                              value: WarStance.negotiate,
                              child: Text('تفاوض')),
                        ],
                        onChanged: (WarStance? v) {
                          if (v != null) {
                            ctl.setWarStance(c.countryId, v);
                          }
                        },
                      ),
                  ],
                ),
              ],
            ),
          ),
        const SizedBox(height: 8),
      ],
    );
  }
}
