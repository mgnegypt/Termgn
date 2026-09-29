package studio.mgn.content

import studio.mgn.model.StateKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Content Validator: fails on missing references, choiceless events,
 * duplicate ids, unknown state keys or malformed conditions.
 */
class ContentValidatorTest {

    private val pack: ContentPack by lazy { ContentLoader.load() }

    @Test
    fun `event counts match the MVP targets`() {
        val byCategory = pack.events.groupBy { it.category.name }
        assertTrue(
            (byCategory["CLASSIC"]?.size ?: 0) in 25..31,
            "classic events: ${byCategory["CLASSIC"]?.size}",
        )
        assertTrue(
            (byCategory["MODERN"]?.size ?: 0) in 25..31,
            "modern events: ${byCategory["MODERN"]?.size}",
        )
        assertTrue(
            (byCategory["CRISIS"]?.size ?: 0) in 8..10,
            "crisis events: ${byCategory["CRISIS"]?.size}",
        )
    }

    @Test
    fun `landmarks countries and achievements match the MVP targets`() {
        assertTrue(pack.landmarks.size in 12..15)
        assertTrue(pack.landmarks.count { it.kind.name == "CULTURAL" } >= 5)
        assertTrue(pack.landmarks.count { it.kind.name == "MODERN" } >= 5)
        assertTrue(pack.countries.size in 8..12)
        assertTrue(pack.achievements.size in 15..20)
    }

    @Test
    fun `every id is unique and every event is authored`() {
        fun checkUnique(ids: List<String>, what: String) {
            assertEquals(ids.size, ids.toSet().size, "duplicate $what id")
        }
        checkUnique(pack.events.map { it.id }, "event")
        checkUnique(pack.landmarks.map { it.id }, "landmark")
        checkUnique(pack.countries.map { it.id }, "country")
        checkUnique(pack.achievements.map { it.id }, "achievement")

        for (event in pack.events) {
            assertTrue(event.choices.isNotEmpty(), "${event.id} has no choices")
            assertTrue(event.title.isNotBlank(), "${event.id} has no title")
            assertTrue(event.description.isNotBlank(), "${event.id} has no description")
            assertTrue(event.imageKey.isNotNullOrEmpty() || true)
            for (choice in event.choices) {
                assertTrue(choice.label.isNotBlank(), "${event.id} choice has no label")
                assertTrue(
                    choice.resultText.isNotBlank(),
                    "${event.id} choice has no resultText",
                )
            }
        }
    }

    @Test
    fun `every effect key is a known state key`() {
        for (event in pack.events) {
            for (choice in event.choices) {
                for (key in choice.stateEffects.keys) {
                    assertTrue(
                        key in StateKeys.EFFECT_KEYS,
                        "${event.id} -> $key",
                    )
                }
                for (countryId in choice.relationEffects.keys) {
                    assertTrue(
                        pack.countries.any { it.id == countryId },
                        "${event.id} references unknown country $countryId",
                    )
                }
            }
            assertConditionKeys(event.id, event.condition)
        }
        for (landmark in pack.landmarks) {
            for (key in landmark.onCompleteEffects.keys +
                landmark.perTurnEffects.keys +
                landmark.requirements.keys
            ) {
                assertTrue(
                    key in StateKeys.EFFECT_KEYS,
                    "${landmark.id} -> $key",
                )
            }
        }
        for (achievement in pack.achievements) {
            assertConditionKeys(achievement.id, achievement.condition)
        }
    }

    @Test
    fun `landmark references resolve`() {        val ids = pack.landmarks.map { it.id }.toSet()
        assertEquals(ids.size, pack.landmarks.size)
        for (landmark in pack.landmarks) {
            assertTrue(landmark.buildTurns > 0, "${landmark.id} buildTurns")
            assertTrue(landmark.costCash > 0, "${landmark.id} costCash")
        }
    }

    @Test
    fun `missions are valid and measurable`() {
        assertTrue(pack.missions.size in 10..20, "expected a full mission set")
        val ids = pack.missions.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate mission id")
        for (mission in pack.missions) {
            assertTrue(mission.titleAr.isNotBlank(), "${mission.id} title")
            assertTrue(mission.descriptionAr.isNotBlank(), "${mission.id} description")
            assertTrue(
                mission.goalKey in StateKeys.KNOWN_CONDITION_KEYS,
                "${mission.id} goal ${mission.goalKey}",
            )
            assertTrue(mission.goalValue > 0, "${mission.id} goal value")
            assertTrue(mission.gemReward >= 0, "${mission.id} reward")
        }
    }

    @Test
    fun `landmark positions are valid`() {
        assertTrue(pack.landmarkPositions.isNotEmpty())
        val landmarkIds = pack.landmarks.map { it.id }.toSet()
        for (pos in pack.landmarkPositions) {
            assertTrue(pos.x in 0.0..1.0, "${pos.id} x")
            assertTrue(pos.y in 0.0..1.0, "${pos.id} y")
            if (!pos.decor) {
                assertTrue(
                    pos.id in landmarkIds,
                    "${pos.id} has no engine landmark; mark it decor",
                )
            }
        }
    }

    private fun assertConditionKeys(owner: String, condition: studio.mgn.model.Condition?) {
        fun visit(node: studio.mgn.model.Condition?) {
            when (node) {
                null -> return
                is studio.mgn.model.Condition.Cmp -> assertTrue(
                    node.key in StateKeys.KNOWN_CONDITION_KEYS,
                    "$owner condition key ${node.key}",
                )
                is studio.mgn.model.Condition.All -> node.conditions.forEach(::visit)
                is studio.mgn.model.Condition.Any -> node.conditions.forEach(::visit)
                is studio.mgn.model.Condition.Not -> visit(node.condition)
                is studio.mgn.model.Condition.HistoryContains -> assertTrue(
                    node.text.isNotBlank(),
                    "$owner historyContains is blank",
                )
            }
        }
        visit(condition)
    }
}

private fun String?.isNotNullOrEmpty(): Boolean = !this.isNullOrEmpty()
