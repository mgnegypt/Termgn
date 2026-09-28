import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/achievements_data.dart';
import '../graphics/theme/typography.dart';
import '../state/game_provider.dart';
import '../storage/hive_boxes.dart';
import '../widgets/common.dart';

/// Settings, achievements, full history, danger zone.
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final GameSession? session = ref.watch(gameProvider);
    final bool sound = GameStorage.soundEnabled;
    final bool anims = GameStorage.animationsEnabled;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        const SectionTitle('الإعدادات'),
        SwitchListTile(
          title: const Text('الصوت'),
          value: sound,
          onChanged: (bool v) async {
            await GameStorage.setSoundEnabled(v);
            if (context.mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(v ? 'الصوت مفعّل' : 'الصوت متوقف')),
              );
            }
          },
        ),
        SwitchListTile(
          title: const Text('الأنيميشن'),
          value: anims,
          onChanged: (bool v) async {
            await GameStorage.setAnimationsEnabled(v);
            if (context.mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                    content:
                        Text(v ? 'الأنيميشن مفعّل' : 'الأنيميشن متوقف')),
              );
            }
          },
        ),
        const SizedBox(height: 12),
        if (session != null) ...<Widget>[
          SectionTitle(
              'الإنجازات (${session.state.unlockedAchievements.length}/${AchievementsData.all.length})'),
          for (final a in AchievementsData.all)
            ListTile(
              dense: true,
              title: Text(a.titleAr),
              subtitle: Text(a.descriptionAr,
                  style: AppTypography.caption),
              trailing: Text(
                session.state.unlockedAchievements.contains(a.id)
                    ? '✓'
                    : '+${a.gemReward}',
                style: AppTypography.numeric,
              ),
            ),
          const SizedBox(height: 12),
          const SectionTitle('السجل الكامل'),
          for (final h in session.state.history.reversed.take(30))
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Text('• ${h.title} — ${h.detail}',
                  style: AppTypography.caption),
            ),
          const SizedBox(height: 16),
          OutlinedButton(
            onPressed: () =>
                ref.read(gameProvider.notifier).deleteSave(),
            child: const Text('حذف الحفظ وبدء حكم جديد'),
          ),
        ] else
          const Text('لا توجد لعبة نشطة',
              style: AppTypography.bodySecondary),
      ],
    );
  }
}
