import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../engine/economy_calculator.dart';
import '../graphics/theme/typography.dart';
import '../models/enums.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// Treasury dials, sector investment, loans.
class SectorsScreen extends ConsumerStatefulWidget {
  const SectorsScreen({super.key});

  @override
  ConsumerState<SectorsScreen> createState() => _SectorsScreenState();
}

class _SectorsScreenState extends ConsumerState<SectorsScreen> {
  final TextEditingController _loan = TextEditingController(text: '50000');
  static final NumberFormat _money = NumberFormat.decimalPattern('ar');

  @override
  void dispose() {
    _loan.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const Center(child: Text('لا توجد لعبة نشطة'));
    }
    final s = session.state;
    final finance = ref.watch(financeProvider);
    final ctl = ref.read(gameProvider.notifier);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        Panel(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const SectionTitle('الخزينة'),
              Row(
                children: <Widget>[
                  StatChip(
                      label: 'الرصيد',
                      value: _money.format(s.treasuryCash.round())),
                  const SizedBox(width: 8),
                  StatChip(
                      label: 'الدين', value: _money.format(s.debt.round())),
                ],
              ),
              if (finance != null) ...<Widget>[
                const SizedBox(height: 8),
                Text('الدخل: ${_money.format(finance.totalIncome.round())} • '
                    'المصروفات: ${_money.format(finance.totalExpenses.round())}'),
              ],
              const SizedBox(height: 8),
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextField(
                      controller: _loan,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: 'مبلغ القرض/السداد',
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  FilledButton(
                    onPressed: () {
                      final double v =
                          double.tryParse(_loan.text) ?? 0;
                      if (v > 0) ctl.takeLoan(v);
                    },
                    child: const Text('اقتراض'),
                  ),
                  const SizedBox(width: 8),
                  OutlinedButton(
                    onPressed: () {
                      final double v =
                          double.tryParse(_loan.text) ?? 0;
                      if (v > 0) ctl.repayDebt(v);
                    },
                    child: const Text('سداد'),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        const SectionTitle('السياسات'),
        _Dial(
          label: 'الضرائب',
          value: s.taxRate,
          onChanged: ctl.setTaxRate,
        ),
        _Dial(
          label: 'الإنفاق العسكري',
          value: s.militarySpendingLevel,
          onChanged: ctl.setMilitarySpending,
        ),
        _Dial(
          label: 'الدعم',
          value: s.subsidyLevel,
          onChanged: ctl.setSubsidyLevel,
        ),
        const SizedBox(height: 12),
        const SectionTitle('الاستثمار في القطاعات (+3 نقاط)'),
        for (final String key in StateKeys.investableSectors)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: IndicatorBar(
                      label: labelFor(key), value: s.readKey(key)),
                ),
                const SizedBox(width: 8),
                OutlinedButton(
                  onPressed: () async {
                    final bool ok =
                        await ctl.investInSector(key, 3);
                    if (!ok && context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                            content: Text('لا يكفي الرصيد للاستثمار')),
                      );
                    }
                  },
                  child: Text(_money.format(
                      EconomyCalculator.sectorInvestmentCost(s, key, 3)
                          .round())),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _Dial extends StatelessWidget {
  const _Dial({
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final String label;
  final double value;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        SizedBox(width: 110, child: Text(label)),
        Expanded(
          child: Slider(
            value: value.clamp(0.0, 1.0),
            onChanged: onChanged,
          ),
        ),
        SizedBox(
          width: 44,
          child: Text('${(value * 100).round()}٪',
              style: AppTypography.caption),
        ),
      ],
    );
  }
}
