package studio.mgn.economy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import studio.mgn.content.ContentPack
import studio.mgn.data.GameRepository
import studio.mgn.data.LoadResult
import studio.mgn.engine.BalanceConfig
import studio.mgn.engine.EconomyEngine
import studio.mgn.engine.TurnFinance
import studio.mgn.model.GameState

data class EconomyUiState(
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val state: GameState? = null,
    /** Finance for the saved dials. */
    val finance: TurnFinance? = null,
    val draftTax: Float = 0.22f,
    val draftMilitary: Float = 0.35f,
    val draftSubsidy: Float = 0.4f,
    /** Finance for the draft dials (preview before confirm). */
    val preview: TurnFinance? = null,
    val investError: Boolean = false,
    /** Debt relative to one turn's income (>1 means stressed). */
    val debtRatio: Double = 0.0,
    /** Per-sector invest button state (+3 points). */
    val investOptions: List<InvestOption> = emptyList(),
)

data class InvestOption(
    val sector: String,
    val level: Double,
    val cost: Double,
    val affordable: Boolean,
)

sealed interface EconomyEvent {
    data class Draft(val tax: Float?, val military: Float?, val subsidy: Float?) : EconomyEvent
    data object ConfirmDials : EconomyEvent
    data class Invest(val sector: String) : EconomyEvent
    data class Loan(val amount: Double) : EconomyEvent
    data class Repay(val amount: Double) : EconomyEvent
    data object ClearError : EconomyEvent
    data object Refresh : EconomyEvent
}

/**
 * Economy screen logic. Draft dials preview finance through
 * [EconomyEngine] without mutating the save until confirmed.
 */
class EconomyViewModel(
    private val repository: GameRepository,
    private val content: ContentPack,
) : ViewModel() {

    private val _state = MutableStateFlow(EconomyUiState())
    val state: StateFlow<EconomyUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: EconomyEvent) {
        when (event) {
            is EconomyEvent.Draft -> {
                val s = _state.value
                val tax = event.tax ?: s.draftTax
                val military = event.military ?: s.draftMilitary
                val subsidy = event.subsidy ?: s.draftSubsidy
                val base = s.state
                _state.value = s.copy(
                    draftTax = tax,
                    draftMilitary = military,
                    draftSubsidy = subsidy,
                    preview = base?.let {
                        EconomyEngine.computeFinance(
                            it.copy(
                                taxRate = tax.toDouble(),
                                militarySpendingLevel = military.toDouble(),
                                subsidyLevel = subsidy.toDouble(),
                            ),
                            content.landmarkCatalog,
                        )
                    },
                )
            }
            EconomyEvent.ConfirmDials -> mutate { state ->
                state.copy(
                    taxRate = _state.value.draftTax.toDouble(),
                    militarySpendingLevel = _state.value.draftMilitary.toDouble(),
                    subsidyLevel = _state.value.draftSubsidy.toDouble(),
                ).clamped()
            }
            is EconomyEvent.Invest -> {
                val current = _state.value.state ?: return
                val next = EconomyEngine.investInSector(current, event.sector, 3.0)
                if (next == null) {
                    _state.value = _state.value.copy(investError = true)
                } else {
                    persist(next)
                }
            }
            is EconomyEvent.Loan -> mutate { state ->
                EconomyEngine.takeLoan(state, event.amount)
            }
            is EconomyEvent.Repay -> mutate { state ->
                EconomyEngine.repayDebt(state, event.amount)
            }
            EconomyEvent.ClearError ->
                _state.value = _state.value.copy(investError = false)
            EconomyEvent.Refresh -> refresh()
        }
    }

    private fun mutate(block: (GameState) -> GameState) {
        val current = _state.value.state ?: return
        viewModelScope.launch {
            persist(block(current))
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            when (val loaded = repository.load()) {
                is LoadResult.Ok -> adopt(loaded.state)
                is LoadResult.RecoveredFromBackup -> adopt(loaded.state)
                else -> _state.value = EconomyUiState(isLoading = false, missing = true)
            }
        }
    }

    private suspend fun persist(next: GameState) {
        repository.save(next)
        adopt(next)
    }

    private fun adopt(state: GameState) {
        val finance = EconomyEngine.computeFinance(state, content.landmarkCatalog)
        val ratio = if (finance.totalIncome > 0) {
            state.debt / finance.totalIncome
        } else {
            if (state.debt > 0) Double.MAX_VALUE else 0.0
        }
        _state.value = EconomyUiState(
            isLoading = false,
            state = state,
            finance = finance,
            draftTax = state.taxRate.toFloat(),
            draftMilitary = state.militarySpendingLevel.toFloat(),
            draftSubsidy = state.subsidyLevel.toFloat(),
            preview = finance,
            investError = _state.value.investError,
            debtRatio = ratio,
            investOptions = studio.mgn.model.StateKeys.INVESTABLE_SECTORS.map { sector ->
                val cost = EconomyEngine.sectorInvestmentCost(state, sector, 3.0)
                InvestOption(
                    sector = sector,
                    level = state.readKey(sector),
                    cost = cost,
                    affordable = state.treasuryCash >= cost,
                )
            },
        )
    }

    companion object {
        /** Debt ratio above 75% of the stress threshold triggers the warning. */
        fun warnDebt(ratio: Double): Boolean =
            ratio > BalanceConfig.DEBT_STRESS_THRESHOLD * 0.75
    }
}
