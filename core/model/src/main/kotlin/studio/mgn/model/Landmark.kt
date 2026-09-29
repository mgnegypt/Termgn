package studio.mgn.model

/** Catalogue definition of a buildable landmark. Immutable. */
data class LandmarkDef(
    val id: String,
    val nameAr: String,
    val descriptionAr: String,
    val kind: LandmarkKind,
    val costCash: Double,
    val buildTurns: Int,
    val onCompleteEffects: Map<String, Double> = emptyMap(),
    val perTurnEffects: Map<String, Double> = emptyMap(),
    val tourismIncomePerTurn: Double = 0.0,
    val maintenancePerTurn: Double = 0.0,
    val requirements: Map<String, Double> = emptyMap(),
    val era: Era = Era.MODERN,
)

/** A queued construction with remaining build turns. */
@kotlinx.serialization.Serializable
data class OngoingBuild(
    val id: String,
    val turnsRemaining: Int,
)

/** A completed landmark (design data re-hydrated from the catalogue). */
@kotlinx.serialization.Serializable
data class BuiltLandmark(
    val id: String,
)
