import 'package:flutter/material.dart';

/// MGN STUDIO palette: deep black, gold and teal — "luxury meets technical".
///
/// Every colour used anywhere in the app must come from here.
abstract final class Palette {
  // Backgrounds, darkest to lightest.
  static const Color black = Color(0xFF07070A);
  static const Color surface = Color(0xFF0E0F14);
  static const Color surfaceRaised = Color(0xFF16181F);
  static const Color surfaceOverlay = Color(0xFF1E212A);

  // Brand accents.
  static const Color gold = Color(0xFFD4AF37);
  static const Color goldSoft = Color(0xFFE8CC7A);
  static const Color goldDeep = Color(0xFF8A6F1E);
  static const Color teal = Color(0xFF2DD4BF);
  static const Color tealDeep = Color(0xFF0F766E);

  // Text.
  static const Color textPrimary = Color(0xFFF5F3EE);
  static const Color textSecondary = Color(0xFFA8A69E);
  static const Color textMuted = Color(0xFF6B6A66);

  // Semantic indicators.
  static const Color positive = Color(0xFF34D399);
  static const Color warning = Color(0xFFF5A524);
  static const Color negative = Color(0xFFEF4444);
  static const Color info = Color(0xFF60A5FA);

  // Hairlines and dividers.
  static const Color border = Color(0xFF262A33);
  static const Color borderGold = Color(0x66D4AF37);

  /// Colour for a 0-100 indicator value: red → amber → teal → green.
  static Color forIndicator(double value) {
    if (value < 25) return negative;
    if (value < 45) return warning;
    if (value < 70) return teal;
    return positive;
  }

  /// Colour for a signed delta, used in "what changed" summaries.
  static Color forDelta(double delta) {
    if (delta > 0) return positive;
    if (delta < 0) return negative;
    return textMuted;
  }

  static const LinearGradient goldSheen = LinearGradient(
    colors: <Color>[goldDeep, gold, goldSoft],
    begin: Alignment.bottomRight,
    end: Alignment.topLeft,
  );

  static const LinearGradient panel = LinearGradient(
    colors: <Color>[surfaceRaised, surface],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );
}
