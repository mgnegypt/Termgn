package studio.mgn.development

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
class DevelopmentViewModelTest {

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

    @Test
    fun `tabs split catalogue by build state`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(treasuryCash = 5000000.0, gems = 100)
        repo.save(state)
        val vm = DevelopmentViewModel(repo, pack)
        val ui = awaitValue(vm.state) { !it.isLoading && it.available.isNotEmpty() }
        assertTrue(ui.available.isNotEmpty())
        assertTrue(ui.catalog.size == pack.landmarks.size)

        vm.onEvent(DevelopmentEvent.Build("national_museum"))
        val building = awaitValue(vm.state) {
            it.state!!.underConstruction.any { b -> b.id == "national_museum" }
        }
        assertEquals(1, building.state!!.underConstruction.size)

        vm.onEvent(DevelopmentEvent.Tab(LandmarkTab.BUILT))
        assertEquals(LandmarkTab.BUILT, vm.state.value.tab)

        vm.onEvent(DevelopmentEvent.Rush("national_museum"))
        val rushed = awaitValue(vm.state) {
            val q = it.state!!.underConstruction.firstOrNull { b ->
                b.id == "national_museum"
            }
            q == null || q.turnsRemaining < 6
        }
        assertTrue(
            rushed.state!!.underConstruction.none { it.id == "national_museum" } ||
                rushed.state.underConstruction.first { it.id == "national_museum" }
                    .turnsRemaining < 6,
        )
    }
}
