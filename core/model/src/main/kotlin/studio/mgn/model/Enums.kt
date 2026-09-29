package studio.mgn.model

import kotlinx.serialization.Serializable

/** Shared enumerations. Persisted by name, never by ordinal. */
@Serializable
enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

@Serializable
enum class TurnLength { DAY, WEEK, MONTH }

@Serializable
enum class Era { FOUNDING, INDUSTRIAL, DIGITAL, MODERN }

@Serializable
enum class EventCategory { CLASSIC, MODERN, ECONOMIC, DIPLOMATIC, CRISIS }

@Serializable
enum class AiBehavior { HOSTILE, TRADER, NEUTRAL, ISOLATIONIST }

@Serializable
enum class TreatyType { TRADE, DEFENSIVE, NON_AGGRESSION, EMBASSY }

@Serializable
enum class WarStance { DEFENSIVE, OFFENSIVE, NEGOTIATE }

@Serializable
enum class WarOutcome { ONGOING, VICTORY, DEFEAT, STALEMATE, NEGOTIATED_PEACE }

@Serializable
enum class LandmarkKind { CULTURAL, MODERN }

@Serializable
enum class HistoryTag {
    INFO, DECISION, CRISIS, WAR, TREATY, CONSTRUCTION, ACHIEVEMENT, YEARLY_REPORT
}
