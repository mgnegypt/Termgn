import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'graphics/theme/palette.dart';
import 'graphics/theme/typography.dart';
import 'screens/boot_screen.dart';
import 'storage/hive_boxes.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await GameStorage.init();
  runApp(const ProviderScope(child: MgnApp()));
}

class MgnApp extends StatelessWidget {
  const MgnApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MGN',
      debugShowCheckedModeBanner: false,
      theme: AppTypography.theme(),
      // Arabic, right-to-left first.
      locale: const Locale('ar'),
      supportedLocales: const <Locale>[Locale('ar'), Locale('en')],
      localizationsDelegates: const <LocalizationsDelegate<Object>>[
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      builder: (BuildContext context, Widget? child) => Directionality(
        textDirection: TextDirection.rtl,
        child: ColoredBox(
          color: Palette.black,
          child: child ?? const SizedBox.shrink(),
        ),
      ),
      home: const BootScreen(),
    );
  }
}
