package studio.mgn.model

import kotlinx.serialization.Serializable
import studio.mgn.model.serializers.LocalDateSerializer
import java.time.LocalDate

/** A single entry in the reign's chronicle. Immutable. */
@Serializable
data class HistoryEntry(
    val turnNumber: Int,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val title: String,
    val detail: String = "",
    val tag: HistoryTag = HistoryTag.INFO,
    val deltas: Map<String, Double> = emptyMap(),
)
