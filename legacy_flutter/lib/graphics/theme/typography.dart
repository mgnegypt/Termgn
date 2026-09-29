import 'package:flutter/material.dart';

import 'palette.dart';

/// Typography and the app-wide dark theme.
///
/// Arabic-first: the default font family is left to the platform's Arabic
/// face so no font binaries ship in the APK, while weights and spacing follow
/// the MGN STUDIO identity (display headings, monospaced numerals).
abstract final class AppTypography {
  /// Numerals and codes, mirroring DM Mono in the studio identity.
  static const String monoFamily = 'monospace';

  static const TextStyle displayLarge = TextStyle(
    fontSize: 30,
    fontWeight: FontWeight.w700,
    height: 1.3,
    letterSpacing: 0.2,
    color: Palette.textPrimary,
  );

  static const TextStyle titleLarge = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.w700,
    height: 1.35,
    color: Palette.textPrimary,
  );

  static const TextStyle titleMedium = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.4,
    color: Palette.textPrimary,
  );

  static const TextStyle body = TextStyle(
    fontSize: 15,
    height: 1.6,
    color: Palette.textPrimary,
  );

  static const TextStyle bodySecondary = TextStyle(
    fontSize: 14,
    height: 1.6,
    color: Palette.textSecondary,
  );

  static const TextStyle caption = TextStyle(
    fontSize: 12,
    height: 1.4,
    color: Palette.textMuted,
  );

  /// Treasury figures, dates and indicator values.
  static const TextStyle numeric = TextStyle(
    fontFamily: monoFamily,
    fontSize: 16,
    fontWeight: FontWeight.w600,
    color: Palette.textPrimary,
  );

  static const TextStyle numericLarge = TextStyle(
    fontFamily: monoFamily,
    fontSize: 24,
    fontWeight: FontWeight.w700,
    color: Palette.gold,
  );

  /// Small uppercase-style label above a value, in gold.
  static const TextStyle label = TextStyle(
    fontSize: 11,
    fontWeight: FontWeight.w600,
    letterSpacing: 1.2,
    color: Palette.gold,
  );

  static ThemeData theme() {
    final ThemeData base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: Palette.black,
      colorScheme: base.colorScheme.copyWith(
        surface: Palette.surface,
        primary: Palette.gold,
        secondary: Palette.teal,
        error: Palette.negative,
        onPrimary: Palette.black,
        onSurface: Palette.textPrimary,
      ),
      dividerColor: Palette.border,
      cardTheme: const CardThemeData(
        color: Palette.surfaceRaised,
        elevation: 0,
        margin: EdgeInsets.zero,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Palette.black,
        foregroundColor: Palette.textPrimary,
        elevation: 0,
        centerTitle: false,
      ),
      textTheme: base.textTheme.copyWith(
        displayLarge: displayLarge,
        titleLarge: titleLarge,
        titleMedium: titleMedium,
        bodyMedium: body,
        bodySmall: bodySecondary,
        labelSmall: caption,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: Palette.gold,
          foregroundColor: Palette.black,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: Palette.gold,
          side: const BorderSide(color: Palette.borderGold),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
        ),
      ),
    );
  }
}
