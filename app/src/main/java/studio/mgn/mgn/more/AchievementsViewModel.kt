package studio.mgn.mgn.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.mgn.content.ContentPack
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult

data class AchievementRow(
    val id: String,
    val title: String,
    val description: String,
    val gemReward: Int,
    val unlocked: Boolean,
)

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val rows: List<AchievementRow> = emptyList(),
) {
    val unlockedCount: Int get() = rows.count { it.unlocked }
    val percent: Int get() =
        if (rows.isEmpty()) 0 else (unlockedCount * 100 / rows.size)
}

/** Achievements list with locked/unlocked state and completion percent. */
class AchievementsViewModel(
    private val content: ContentPack,
    private val repository: GameRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AchievementsUiState())
    val state: StateFlow<AchievementsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val unlocked = when (val loaded = repository.load()) {
                is LoadResult.Ok -> loaded.state.unlockedAchievements
                is LoadResult.RecoveredFromBackup -> loaded.state.unlockedAchievements
                else -> emptySet()
            }
            _state.value = AchievementsUiState(
                isLoading = false,
                rows = content.achievements.map { achievement ->
                    AchievementRow(
                        id = achievement.id,
                        title = achievement.titleAr,
                        description = achievement.descriptionAr,
                        gemReward = achievement.gemReward,
                        unlocked = achievement.id in unlocked,
                    )
                },
            )
        }
    }
}
