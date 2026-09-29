package studio.mgn.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import studio.mgn.model.Achievement
import studio.mgn.model.AiBehavior
import studio.mgn.model.CmpOp
import studio.mgn.model.Condition
import studio.mgn.model.DiplomacyState
import studio.mgn.model.Era
import studio.mgn.model.EventCategory
import studio.mgn.model.EventChoice
import studio.mgn.model.GameEvent
import studio.mgn.model.LandmarkDef
import studio.mgn.model.LandmarkKind
import studio.mgn.model.MissionDef

/** Raw event JSON shape (conditions stay as JSON until mapped). */
@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val weight: Int = 10,
    val minTurn: Int = 0,
    val oncePerGame: Boolean = false,
    val cooldownTurns: Int? = null,
    val imageKey: String = "",
    val condition: JsonObject? = null,
    val choices: List<ChoiceDto>,
)

@Serializable
data class ChoiceDto(
    val label: String,
    val resultText: String,
    val costCash: Double = 0.0,
    val costGems: Int = 0,
    val stateEffects: Map<String, Double> = emptyMap(),
    val relationEffects: Map<String, Double> = emptyMap(),
    val allRelationsDelta: Double = 0.0,
)

@Serializable
data class LandmarkDto(
    val id: String,
    val nameAr: String,
    val descriptionAr: String,
    val kind: String,
    val costCash: Double,
    val buildTurns: Int,
    val onCompleteEffects: Map<String, Double> = emptyMap(),
    val perTurnEffects: Map<String, Double> = emptyMap(),
    val tourismIncomePerTurn: Double = 0.0,
    val maintenancePerTurn: Double = 0.0,
    val requirements: Map<String, Double> = emptyMap(),
    val era: String = "modern",
)

@Serializable
data class CountryDto(
    val id: String,
    val nameAr: String,
    val behavior: String,
    val relation: Double = 0.0,
    val economicPower: Double = 50.0,
    val militaryPower: Double = 50.0,
)

@Serializable
data class AchievementDto(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val gemReward: Int = 0,
    val hidden: Boolean = false,
    val condition: JsonObject,
)

@Serializable
data class MissionDto(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val goalKey: String,
    val goalValue: Double,
    val gemReward: Int = 0,
)

@Serializable
data class LandmarkPositionDto(
    val id: String,
    val x: Double,
    val y: Double,
    val decor: Boolean = false,
    val noteAr: String = "",
)

/** Full content pack handed to the engines. */
data class ContentPack(
    val events: List<GameEvent>,
    val landmarks: List<LandmarkDef>,
    val countries: List<CountryDto>,
    val achievements: List<Achievement>,
    val missions: List<MissionDef>,
    val landmarkPositions: List<LandmarkPosition>,
) {
    val landmarkCatalog: Map<String, LandmarkDef> get() = landmarks.associateBy { it.id }

    fun initialWorld(): Map<String, DiplomacyState> = countries.associate { dto ->
        dto.id to DiplomacyState(
            countryId = dto.id,
            nameAr = dto.nameAr,
            behavior = AiBehavior.valueOf(dto.behavior.uppercase()),
            relation = dto.relation,
            economicPower = dto.economicPower,
            militaryPower = dto.militaryPower,
        )
    }

    fun eventById(id: String): GameEvent? = events.firstOrNull { it.id == id }
    fun landmarkById(id: String): LandmarkDef? = landmarkCatalog[id]
}

/** Maps raw JSON conditions to the [Condition] tree. */
object ConditionMapper {
    fun map(node: JsonObject?): Condition? {
        if (node == null) return null
        node["all"]?.let { arr ->
            return Condition.All(arr.jsonArray.map { map(it.jsonObject)!! })
        }
        node["any"]?.let { arr ->
            return Condition.Any(arr.jsonArray.map { map(it.jsonObject)!! })
        }
        node["not"]?.let {
            return Condition.Not(map(it.jsonObject)!!)
        }
        node["historyContains"]?.let {
            return Condition.HistoryContains(it.jsonPrimitive.content)
        }
        val key = node["key"]!!.jsonPrimitive.content
        val op = CmpOp.fromSymbol(node["op"]!!.jsonPrimitive.content)
        val value = node["value"]!!.jsonPrimitive.double
        return Condition.Cmp(key, op, value)
    }
}

/** A building sign on the city scene: relative 0..1 coords, or pure decor. */
data class LandmarkPosition(
    val id: String,
    val x: Double,
    val y: Double,
    val decor: Boolean = false,
    val noteAr: String = "",
)

fun MissionDto.toModel(): MissionDef = MissionDef(
    id = id,
    titleAr = titleAr,
    descriptionAr = descriptionAr,
    goalKey = goalKey,
    goalValue = goalValue,
    gemReward = gemReward,
)

fun LandmarkPositionDto.toModel(): LandmarkPosition = LandmarkPosition(
    id = id,
    x = x,
    y = y,
    decor = decor,
    noteAr = noteAr,
)

fun EventDto.toModel(): GameEvent = GameEvent(
    id = id,
    title = title,
    description = description,
    category = EventCategory.valueOf(category.uppercase()),
    choices = choices.map { it.toModel() },
    condition = ConditionMapper.map(condition),
    weight = weight,
    cooldownTurns = cooldownTurns,
    oncePerGame = oncePerGame,
    minTurn = minTurn,
    imageKey = imageKey,
)

fun ChoiceDto.toModel(): EventChoice = EventChoice(
    label = label,
    resultText = resultText,
    stateEffects = stateEffects,
    costCash = costCash,
    costGems = costGems,
    relationEffects = relationEffects,
    allRelationsDelta = allRelationsDelta,
)

fun LandmarkDto.toModel(): LandmarkDef = LandmarkDef(
    id = id,
    nameAr = nameAr,
    descriptionAr = descriptionAr,
    kind = LandmarkKind.valueOf(kind.uppercase()),
    costCash = costCash,
    buildTurns = buildTurns,
    onCompleteEffects = onCompleteEffects,
    perTurnEffects = perTurnEffects,
    tourismIncomePerTurn = tourismIncomePerTurn,
    maintenancePerTurn = maintenancePerTurn,
    requirements = requirements,
    era = Era.valueOf(era.uppercase()),
)

fun AchievementDto.toModel(): Achievement = Achievement(
    id = id,
    titleAr = titleAr,
    descriptionAr = descriptionAr,
    condition = ConditionMapper.map(condition)!!,
    gemReward = gemReward,
    hidden = hidden,
)

/** Loads the JSON content shipped in this module's resources. */
object ContentLoader {
    private val json = Json { ignoreUnknownKeys = false }

    fun load(): ContentPack {
        fun read(name: String): String =
            ContentLoader::class.java.getResource("/$name")?.readText()
                ?: throw IllegalStateException("Missing content resource: $name")

        val events = listOf("events_classic.json", "events_modern.json", "events_crisis.json")
            .flatMap { name ->
                json.decodeFromString<List<EventDto>>(read(name)).map { it.toModel() }
            }
        val landmarks = json.decodeFromString<List<LandmarkDto>>(read("landmarks.json"))
            .map { it.toModel() }
        val countries = json.decodeFromString<List<CountryDto>>(read("countries.json"))
        val achievements = json.decodeFromString<List<AchievementDto>>(read("achievements.json"))
            .map { it.toModel() }
        val missions = json.decodeFromString<List<MissionDto>>(read("missions.json"))
            .map { it.toModel() }
        val positions =
            json.decodeFromString<List<LandmarkPositionDto>>(read("landmark_positions.json"))
                .map { it.toModel() }
        return ContentPack(events, landmarks, countries, achievements, missions, positions)
    }
}
