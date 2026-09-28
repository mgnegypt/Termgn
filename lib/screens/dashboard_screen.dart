import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../graphics/theme/palette.dart';
import '../graphics/theme/typography.dart';
import '../models/enums.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// Main overview: status, treasury, turn advancement, reports, indicators.
class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key, required this.onGoToDecisions});

  final VoidCallback onGoToDecisions;

  static final NumberFormat _money = NumberFormat.decimalPattern('ar');

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const Center(child: Text('لا توجد لعبة نشطة'));
    }
    final s = session.state;
    final finance = ref.watch(financeProvider);
    final bool blocked = session.awaitingDecision;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        if (s.isCollapsed || s.hasWon)
          Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: s.isCollapsed
                  ? Palette.negative.withValues(alpha: 0.15)
                  : Palette.positive.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: s.isCollapsed ? Palette.negative : Palette.positive,
              ),
            ),
            child: Text(
              s.isCollapsed
                  ? 'انهيار الدولة: الشرعية والرضا والخزينة منهارة. يمكنك المواصلة لمحاولة الإنقاذ أو بدء حكم جديد من الإعدادات.'
                  : 'نصر عظيم: تقييم ${s.nationScore.toStringAsFixed(1)} بشرعية ${s.legitimacy.toStringAsFixed(0)}. يمكنك المواصلة أو بدء تحدٍ جديد.',
              style: AppTypography.body,
            ),
          ),
        Panel(
          highlightGold: true,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text('${s.rulerTitle} ${s.countryName}',
                  style: AppTypography.titleLarge),
              const SizedBox(height: 4),
              Text(
                'الدور ${s.turnNumber} — ${s.inGameDate.year}/${s.inGameDate.month} — الحالة: ${s.statusLabel}',
                style: AppTypography.caption,
              ),
              const Divider(height: 24),
              Row(
                children: <Widget>[
                  StatChip(
                      label: 'الخزينة',
                      value: _money.format(s.treasuryCash.round())),
                  const SizedBox(width: 8),
                  StatChip(label: 'الجواهر', value: '${s.gems}'),
                  const SizedBox(width: 8),
                  StatChip(
                      label: 'الدين', value: _money.format(s.debt.round())),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: <Widget>[
                  StatChip(
                      label: 'التقييم',
                      value: s.nationScore.toStringAsFixed(1)),
                  const SizedBox(width: 8),
                  StatChip(
                      label: 'الشرعية',
                      value: s.legitimacy.toStringAsFixed(0)),
                  const SizedBox(width: 8),
                  StatChip(
                      label: 'السكان',
                      value: _money.format(s.population)),
                ],
              ),
              if (finance != null) ...<Widget>[
                const SizedBox(height: 12),
                Text(
                  'صافي الدور: ${finance.net >= 0 ? '+' : ''}'
                  '${_money.format(finance.net.round())}',
                  style: AppTypography.numeric.copyWith(
                    color: Palette.forDelta(finance.net),
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (blocked)
          FilledButton(
            onPressed: onGoToDecisions,
            child: Text(
                'لديك ${session.pendingEvents.length} قرار معلق — انتقل للقرارات'),
          )
        else
          FilledButton(
            onPressed: () =>
                ref.read(gameProvider.notifier).nextTurn(),
            child: const Text('الدور القادم'),
          ),
        const SizedBox(height: 16),
        if (session.lastResolution != null)
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
        if (session.lastReport != null) ...<Widget>[
          const SizedBox(height: 12),
          Panel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const SectionTitle('تقرير الدور'),
                if (session.lastReport!.completedLandmarks.isNotEmpty)
                  Text(
                    'اكتمل: ${session.lastReport!.completedLandmarks.map((e) => e.nameAr).join('، ')}',
                    style: AppTypography.bodySecondary,
                  ),
                if (!session.lastReport!.diplomacy.isEmpty)
                  Text(
                    'الدبلوماسية: ${session.lastReport!.diplomacy.warResults.length} معارك • '
                    '${session.lastReport!.diplomacy.newSanctions.length} عقوبات جديدة',
                    style: AppTypography.bodySecondary,
                  ),
                if (session.lastReport!.yearlyReport != null)
                  Text(
                    'تقرير سنوي: ${session.lastReport!.yearlyReport!.nationScore.toStringAsFixed(1)} '
                    '(+${session.lastReport!.yearlyReport!.gemsAwarded} جواهر)',
                    style: AppTypography.bodySecondary,
                  ),
                if (session.lastReport!.referendum != null)
                  Text(
                    session.lastReport!.referendum!.passed
                        ? 'تم تجديد الثقة في الاستفتاء'
                        : 'فشل الاستفتاء — تراجعت الشرعية',
                    style: AppTypography.bodySecondary.copyWith(
                      color: session.lastReport!.referendum!.passed
                          ? Palette.positive
                          : Palette.negative,
                    ),
                  ),
                if (session.lastReport!.newAchievements.isNotEmpty)
                  Text(
                    'إنجازات: ${session.lastReport!.newAchievements.map((e) => e.titleAr).join('، ')}',
                    style: AppTypography.bodySecondary,
                  ),
              ],
            ),
          ),
        ],
        const SizedBox(height: 16),
        const SectionTitle('المؤشرات'),
        for (final String k in StateKeys.indicators)
          IndicatorBar(label: labelFor(k), value: s.readKey(k)),
        const SizedBox(height: 8),
        const SectionTitle('الأحداث الأخيرة'),
        for (final h in s.history.reversed.take(6))
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Text('• ${h.title}', style: AppTypography.caption),
          ),
      ],
    );
  }
}
