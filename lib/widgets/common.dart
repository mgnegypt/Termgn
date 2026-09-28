import 'package:flutter/material.dart';

import '../graphics/theme/palette.dart';
import '../graphics/theme/typography.dart';

/// Small shared widgets: section titles, indicator bars, stat chips.
class SectionTitle extends StatelessWidget {
  const SectionTitle(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(text, style: AppTypography.label),
    );
  }
}

class IndicatorBar extends StatelessWidget {
  const IndicatorBar({
    super.key,
    required this.label,
    required this.value,
  });

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: <Widget>[
          SizedBox(
            width: 110,
            child: Text(label, style: AppTypography.bodySecondary),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: (value / 100).clamp(0.0, 1.0),
                minHeight: 8,
                backgroundColor: Palette.surfaceRaised,
                valueColor: AlwaysStoppedAnimation<Color>(
                  Palette.forIndicator(value),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          SizedBox(
            width: 32,
            child: Text(
              value.toStringAsFixed(0),
              style: AppTypography.caption,
              textAlign: TextAlign.end,
            ),
          ),
        ],
      ),
    );
  }
}

class StatChip extends StatelessWidget {
  const StatChip({super.key, required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
        decoration: BoxDecoration(
          color: Palette.surfaceRaised,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Palette.border),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(label, style: AppTypography.caption),
            const SizedBox(height: 2),
            Text(value, style: AppTypography.numeric),
          ],
        ),
      ),
    );
  }
}

class Panel extends StatelessWidget {
  const Panel({super.key, required this.child, this.highlightGold = false});

  final Widget child;
  final bool highlightGold;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: Palette.panel,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: highlightGold ? Palette.borderGold : Palette.border,
        ),
      ),
      child: child,
    );
  }
}

/// Arabic labels for state keys.
String labelFor(String key) => switch (key) {
      'economy' => 'الاقتصاد',
      'publicSatisfaction' => 'رضا الشعب',
      'digitalOpinion' => 'الرأي الرقمي',
      'militarySecurity' => 'الأمن العسكري',
      'cyberSecurity' => 'الأمن السيبراني',
      'environment' => 'البيئة',
      'culture' => 'الثقافة',
      'agriculture' => 'الزراعة',
      'industry' => 'الصناعة',
      'foodSecurity' => 'الأمن الغذائي',
      'energy' => 'الطاقة',
      'technology' => 'التقنية',
      'tourism' => 'السياحة',
      'health' => 'الصحة',
      'education' => 'التعليم',
      'treasuryCash' => 'الخزينة',
      'population' => 'السكان',
      _ => key,
    };
