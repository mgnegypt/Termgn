package studio.mgn.model

import kotlinx.serialization.Serializable

/** The player's live relationship with one AI country. Immutable. */
@Serializable
data class DiplomacyState(
    val countryId: String,
    val nameAr: String,
    val behavior: AiBehavior,
    val relation: Double = 0.0,
    val economicPower: Double = 50.0,
    val militaryPower: Double = 50.0,
    val treaties: Set<TreatyType> = emptySet(),
    val atWar: Boolean = false,
    val warTurns: Int = 0,
    val sanctioned: Boolean = false,
    val lastOfferTurn: Int = -999,
) {
    val hasEmbassy: Boolean get() = TreatyType.EMBASSY in treaties
    val hasTradeDeal: Boolean get() = TreatyType.TRADE in treaties
    val isAlly: Boolean get() = TreatyType.DEFENSIVE in treaties
    val intelUnlocked: Boolean get() = hasEmbassy
}
