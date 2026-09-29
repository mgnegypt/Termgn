package studio.mgn.development

/** Every user-facing string on the development screen; built by the app layer. */
data class DevelopmentStrings(
    val title: String,
    val tabAvailable: String,
    val tabBuilding: String,
    val tabBuilt: String,
    val emptyAvailable: String,
    val emptyBuilding: String,
    val emptyBuilt: String,
    val buildNow: String,
    val rushLabel: String,
    val builtLabel: String,
    val remainingTurns: String,
    val costLabel: String,
    val durationLabel: String,
    val requiresLabel: String,
    val gemsSuffix: String,
    val keyLabels: Map<String, String>,
)
