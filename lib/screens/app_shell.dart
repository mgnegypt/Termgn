import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../state/game_provider.dart';
import 'dashboard_screen.dart';
import 'decision_screen.dart';
import 'diplomacy_screen.dart';
import 'landmarks_screen.dart';
import 'sectors_screen.dart';
import 'settings_screen.dart';
import 'setup_screen.dart';

/// Root shell: setup when no save, tabbed game when a session exists.
class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  int _tab = 0;

  void _goToDecisions() => setState(() => _tab = 1);

  @override
  Widget build(BuildContext context) {
    final GameSession? session = ref.watch(gameProvider);
    if (session == null) {
      return const SetupScreen();
    }

    final int badge = session.pendingEvents.length;
    final List<Widget> pages = <Widget>[
      DashboardScreen(onGoToDecisions: _goToDecisions),
      const DecisionScreen(),
      const SectorsScreen(),
      const DiplomacyScreen(),
      const LandmarksScreen(),
      const SettingsScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(session.state.countryName),
        actions: <Widget>[
          Padding(
            padding: const EdgeInsetsDirectional.only(end: 12),
            child: Center(
              child: Text(
                'دور ${session.state.turnNumber} • 💎 ${session.state.gems}',
              ),
            ),
          ),
        ],
      ),
      body: IndexedStack(index: _tab, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (int i) => setState(() => _tab = i),
        destinations: <NavigationDestination>[
          const NavigationDestination(
              icon: Icon(Icons.dashboard), label: 'الرئيسية'),
          NavigationDestination(
            icon: badge > 0
                ? Badge(label: Text('$badge'), child: const Icon(Icons.how_to_vote))
                : const Icon(Icons.how_to_vote),
            label: 'القرارات',
          ),
          const NavigationDestination(
              icon: Icon(Icons.factory), label: 'القطاعات'),
          const NavigationDestination(
              icon: Icon(Icons.public), label: 'الدبلوماسية'),
          const NavigationDestination(
              icon: Icon(Icons.account_balance), label: 'المعالم'),
          const NavigationDestination(
              icon: Icon(Icons.settings), label: 'المزيد'),
        ],
      ),
    );
  }
}
