package studio.mgn.diplomacy

import studio.mgn.model.TreatyType
import studio.mgn.model.WarStance

/** Every user-facing string on the diplomacy screen; built by the app layer. */
data class DiplomacyStrings(
    val title: String,
    val relationLabel: String,
    val treatiesLabel: String,
    val noTreaties: String,
    val signTreaty: String,
    val breakTreaty: String,
    val declareWar: String,
    val warConfirmTitle: String,
    val warConfirmMessage: String,
    val warConfirmOk: String,
    val cancel: String,
    val stanceLabel: String,
    val stanceDefensive: String,
    val stanceOffensive: String,
    val stanceNegotiate: String,
    val atWarLabel: String,
    val sanctionedLabel: String,
    val warCostNote: String,
    val treatyNames: Map<String, String>,
    val stanceNames: Map<String, String>,
)

fun treatyName(type: TreatyType, strings: DiplomacyStrings): String =
    strings.treatyNames[type.name] ?: type.name

fun stanceName(stance: WarStance, strings: DiplomacyStrings): String =
    strings.stanceNames[stance.name] ?: stance.name
