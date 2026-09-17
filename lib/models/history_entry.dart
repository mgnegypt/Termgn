/// A single entry in the reign's chronicle, shown in the stats/timeline screen
/// and in the yearly report.
class HistoryEntry {
  HistoryEntry({
    required this.turnNumber,
    required this.date,
    required this.title,
    this.detail = '',
    this.tag = HistoryTag.info,
    this.deltas = const <String, double>{},
  });

  final int turnNumber;
  final DateTime date;
  final String title;
  final String detail;
  final HistoryTag tag;

  /// Notable state changes attached to the entry, for the timeline painter.
  final Map<String, double> deltas;

  Map<String, Object?> toMap() => <String, Object?>{
        'turnNumber': turnNumber,
        'date': date.toIso8601String(),
        'title': title,
        'detail': detail,
        'tag': tag.name,
        'deltas': deltas,
      };

  static HistoryEntry fromMap(Map<String, Object?> map) {
    return HistoryEntry(
      turnNumber: (map['turnNumber'] as num?)?.toInt() ?? 0,
      date: DateTime.tryParse((map['date'] as String?) ?? '') ?? DateTime(2025),
      title: (map['title'] as String?) ?? '',
      detail: (map['detail'] as String?) ?? '',
      tag: HistoryTag.values.firstWhere(
        (HistoryTag t) => t.name == map['tag'],
        orElse: () => HistoryTag.info,
      ),
      deltas: <String, double>{
        for (final MapEntry<Object?, Object?> e
            in ((map['deltas'] as Map<Object?, Object?>?) ??
                    const <Object?, Object?>{})
                .entries)
          e.key.toString(): (e.value as num).toDouble(),
      },
    );
  }
}

enum HistoryTag {
  info,
  decision,
  crisis,
  war,
  treaty,
  construction,
  achievement,
  yearlyReport,
}
