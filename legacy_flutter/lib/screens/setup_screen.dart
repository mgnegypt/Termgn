import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../data/events_data.dart';
import '../data/landmarks_data.dart';
import '../graphics/theme/typography.dart';
import '../models/enums.dart';
import '../state/game_provider.dart';
import '../widgets/common.dart';

/// New-game setup: country identity, turn length, flag preset, ideology axes.
class SetupScreen extends ConsumerStatefulWidget {
  const SetupScreen({super.key});

  @override
  ConsumerState<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends ConsumerState<SetupScreen> {
  final TextEditingController _country = TextEditingController(text: 'المجد');
  final TextEditingController _ruler = TextEditingController(text: 'رئيس');
  TurnLength _turnLength = TurnLength.month;
  int _preset = 0;
  double _axisEconomic = 0;
  double _axisSocial = 0;
  double _axisForeign = 0;
  bool _starting = false;

  static const List<Map<String, Object>> _presets = <Map<String, Object>>[
    <String, Object>{'a': 0xFF0B0B0D, 'b': 0xFFD4AF37, 's': 'crescent_star'},
    <String, Object>{'a': 0xFF0F766E, 'b': 0xFFF5F3EE, 's': 'eagle'},
    <String, Object>{'a': 0xFF7C2D12, 'b': 0xFFD4AF37, 's': 'tower'},
  ];

  @override
  void dispose() {
    _country.dispose();
    _ruler.dispose();
    super.dispose();
  }

  Future<void> _start() async {
    if (_starting) return;
    setState(() => _starting = true);
    final Map<String, Object> p = _presets[_preset];
    await ref.read(gameProvider.notifier).startNewGame(
          countryName:
              _country.text.trim().isEmpty ? 'المجد' : _country.text.trim(),
          rulerTitle: _ruler.text.trim().isEmpty ? 'رئيس' : _ruler.text.trim(),
          turnLength: _turnLength,
          flagPrimaryColor: p['a']! as int,
          flagSecondaryColor: p['b']! as int,
          flagSymbolId: p['s']! as String,
          axisEconomic: _axisEconomic,
          axisSocial: _axisSocial,
          axisForeign: _axisForeign,
        );
    if (mounted) setState(() => _starting = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('MGN — تأسيس الدولة')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: <Widget>[
          const Text('MGN', style: AppTypography.numericLarge),
          const SizedBox(height: 4),
          const Text('إنتاج MGN STUDIO — بلا إنترنت، تخزين محلي فقط',
              style: AppTypography.caption),
          const SizedBox(height: 20),
          const SectionTitle('اسم الدولة'),
          TextField(
            controller: _country,
            decoration: const InputDecoration(border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          const SectionTitle('لقب الحاكم'),
          TextField(
            controller: _ruler,
            decoration: const InputDecoration(border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          const SectionTitle('طول الدور'),
          SegmentedButton<TurnLength>(
            segments: const <ButtonSegment<TurnLength>>[
              ButtonSegment(value: TurnLength.day, label: Text('يوم')),
              ButtonSegment(value: TurnLength.week, label: Text('أسبوع')),
              ButtonSegment(value: TurnLength.month, label: Text('شهر')),
            ],
            selected: <TurnLength>{_turnLength},
            onSelectionChanged: (Set<TurnLength> s) =>
                setState(() => _turnLength = s.first),
          ),
          const SizedBox(height: 12),
          const SectionTitle('الراية (مولّدة بالكود)'),
          Row(
            children: <Widget>[
              for (int i = 0; i < _presets.length; i++)
                Padding(
                  padding: const EdgeInsetsDirectional.only(end: 8),
                  child: ChoiceChip(
                    label: Text('راية ${i + 1}'),
                    selected: _preset == i,
                    onSelected: (_) => setState(() => _preset = i),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          const SectionTitle('التوجهات (اختياري)'),
          _AxisSlider(
            label: 'اقتصادي',
            value: _axisEconomic,
            onChanged: (double v) => setState(() => _axisEconomic = v),
          ),
          _AxisSlider(
            label: 'اجتماعي',
            value: _axisSocial,
            onChanged: (double v) => setState(() => _axisSocial = v),
          ),
          _AxisSlider(
            label: 'خارجي',
            value: _axisForeign,
            onChanged: (double v) => setState(() => _axisForeign = v),
          ),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _starting ? null : _start,
            child: Text(_starting ? 'جارٍ التأسيس…' : 'ابدأ الحكم'),
          ),
          const SizedBox(height: 16),
          Text(
            'المحتوى: ${EventsData.all.length} حدث • '
            '${LandmarksData.all.length} معلم • 10 دول',
            style: AppTypography.caption,
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _AxisSlider extends StatelessWidget {
  const _AxisSlider({
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
        SizedBox(width: 70, child: Text(label)),
        Expanded(
          child: Slider(
            value: value,
            min: -100,
            max: 100,
            divisions: 40,
            label: value.toStringAsFixed(0),
            onChanged: onChanged,
          ),
        ),
        SizedBox(
          width: 44,
          child: Text(NumberFormat.decimalPattern('ar').format(value.round()),
              style: AppTypography.caption),
        ),
      ],
    );
  }
}
