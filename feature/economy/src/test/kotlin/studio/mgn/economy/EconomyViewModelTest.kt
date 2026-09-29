package studio.mgn.economy

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import studio.mgn.content.ContentLoader
import studio.mgn.data.GameRepository
import studio.mgn.data.GameSetup
import studio.mgn.data.save.MgnDatabase
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun idleMain() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}

private fun <T> awaitValue(
    flow: StateFlow<T>,
    timeoutMs: Long = 8000,
    predicate: (T) -> Boolean,
): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    var value = flow.value
    while (!predicate(value)) {
        idleMain()
        Thread.sleep(15)
        check(System.currentTimeMillis() < deadline) { "timed out" }
        value = flow.value
    }
    return value
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EconomyViewModelTest {

    private lateinit var repo: GameRepository
    private val pack = ContentLoader.load()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(
            db,
            worldProvider = {
                mapOf(
                    "arzan" to DiplomacyState(
                        countryId = "arzan",
                        nameAr = "أرزان",
                        behavior = AiBehavior.TRADER,
                    ),
                )
            },
        )
    }

    private fun viewModel() = EconomyViewModel(repo, pack)

    @Test
    fun `draft previews finance before confirm`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = viewModel()
        awaitValue(vm.state) { !it.isLoading && it.finance != null }
        val base = vm.state.value.finance!!.totalIncome

        vm.onEvent(EconomyEvent.Draft(tax = 0.5f, military = null, subsidy = null))
        val preview = awaitValue(vm.state) {
            it.preview != null && it.preview.totalIncome != base
        }
        assertTrue(preview.preview!!.totalIncome > base)
        // Save untouched until confirm.
        val untouched = repo.load() as studio.mgn.data.LoadResult.Ok
        assertEquals(0.22, untouched.state.taxRate)
        vm.onEvent(EconomyEvent.ConfirmDials)
        val confirmed = awaitValue(vm.state) { it.state?.taxRate == 0.5 }
        assertEquals(0.5, confirmed.state!!.taxRate)
    }

    @Test
    fun `invest succeeds with funds and fails without`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = viewModel()
        awaitValue(vm.state) { !it.isLoading && it.investOptions.isNotEmpty() }
        assertTrue(vm.state.value.investOptions.all { it.affordable })

        // Drain the treasury via the repository state, then invest must fail.
        val drained = repo.load().let {
            (it as studio.mgn.data.LoadResult.Ok).state.copy(treasuryCash = 10.0)
        }
        repo.save(drained)
        vm.onEvent(EconomyEvent.Refresh)
        awaitValue(vm.state) { it.state?.treasuryCash == 10.0 }
        vm.onEvent(EconomyEvent.Invest("agriculture"))
        awaitValue(vm.state) { it.investError }
    }

    @Test
    fun `loans are capped and warned at high ratios`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = viewModel()
        awaitValue(vm.state) { !it.isLoading && it.finance != null }

        vm.onEvent(EconomyEvent.Loan(9999999.0))
        val indebted = awaitValue(vm.state) { it.state?.debt ?: 0.0 > 0 }
        assertTrue(indebted.state!!.debt <= 400000.0 + 1e-6)

        vm.onEvent(EconomyEvent.Repay(9999999.0))
        val clear = awaitValue(vm.state) { it.state?.debt == 0.0 }
        assertEquals(0.0, clear.state!!.debt)

        assertTrue(EconomyViewModel.warnDebt(5.0))
        assertFalse(EconomyViewModel.warnDebt(1.0))
    }
}
