package studio.mgn.mgn

import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
import studio.mgn.data.settings.SettingsStore
import studio.mgn.mgn.menu.MenuEvent
import studio.mgn.mgn.menu.MenuNav
import studio.mgn.mgn.menu.MenuViewModel
import studio.mgn.mgn.more.AchievementsViewModel
import studio.mgn.mgn.more.SettingsEvent
import studio.mgn.mgn.more.SettingsViewModel
import studio.mgn.mgn.splash.SplashPhase
import studio.mgn.mgn.splash.SplashViewModel
import studio.mgn.model.AiBehavior
import studio.mgn.model.DiplomacyState
import studio.mgn.setup.SetupEvent
import studio.mgn.setup.SetupStep
import studio.mgn.setup.SetupViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun world() = mapOf(
    "arzan" to DiplomacyState(
        countryId = "arzan",
        nameAr = "أرزان",
        behavior = AiBehavior.TRADER,
    ),
)

private fun idleMain() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
}

/** Polls a StateFlow until [predicate] holds (background IO needs settling). */
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
        check(System.currentTimeMillis() < deadline) { "timed out waiting for state" }
        value = flow.value
    }
    return value
}

private fun <T> awaitFuture(
    future: java.util.concurrent.Future<T>,
    timeoutMs: Long = 8000,
): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!future.isDone) {
        idleMain()
        Thread.sleep(15)
        check(System.currentTimeMillis() < deadline) { "timed out waiting for event" }
    }
    return future.get()
}

private fun <T> collectFirstAsync(flow: kotlinx.coroutines.flow.Flow<T>) =
    java.util.concurrent.Executors.newSingleThreadExecutor().submit<T> {
        kotlinx.coroutines.runBlocking { flow.first() }
    }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppViewModelsTest {

    private lateinit var db: MgnDatabase
    private lateinit var repo: GameRepository
    private lateinit var settings: SettingsStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MgnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GameRepository(db, worldProvider = ::world)
        settings = SettingsStore(context)
    }

    // ── Splash ──

    @Test
    fun `splash becomes ready after content loads`() {
        val vm = SplashViewModel(loadContent = { ContentLoader.load() }, minDelayMs = 0)
        val ready = awaitValue(vm.state) { it.phase == SplashPhase.READY }
        assertTrue(ready.eventCount >= 60)
    }

    @Test
    fun `splash surfaces errors and recovers on retry`() {
        var fail = true
        val vm = SplashViewModel(
            loadContent = {
                if (fail) throw IllegalStateException("broken json")
                ContentLoader.load()
            },
            minDelayMs = 0,
        )
        awaitValue(vm.state) { it.phase == SplashPhase.ERROR }
        fail = false
        vm.retry()
        awaitValue(vm.state) { it.phase == SplashPhase.READY }
    }

    // ── Menu ──

    @Test
    fun `menu without save offers new game only`() {
        val vm = MenuViewModel(repo)
        val state = awaitValue(vm.state) { !it.isLoading }
        assertEquals(null, state.summary)
        assertFalse(state.corrupted)
    }

    @Test
    fun `menu shows save summary and guards overwrite`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val vm = MenuViewModel(repo)
        val navFuture = collectFirstAsync(vm.nav)

        val loaded = awaitValue(vm.state) { !it.isLoading && it.summary != null }
        assertEquals("المجد", loaded.summary!!.countryName)

        vm.onEvent(MenuEvent.NewGameClicked)
        awaitValue(vm.state) { it.showOverwriteConfirm }

        vm.onEvent(MenuEvent.OverwriteConfirmed)
        awaitValue(vm.state) { !it.isLoading && it.summary == null }
        assertEquals(MenuNav.SETUP, awaitFuture(navFuture))
    }

    @Test
    fun `menu flags a fully corrupt save without crashing`() = runTest {
        repo.newGame(GameSetup("المجد", "رئيس"))
        val dao = db.saveDao()
        val row = dao.get(0)!!
        dao.upsert(row.copy(payloadJson = "{broken"))
        dao.upsert(row.copy(id = 1, payloadJson = "{broken"))

        val vm = MenuViewModel(repo)
        val state = awaitValue(vm.state) { !it.isLoading }
        assertTrue(state.corrupted)

        vm.onEvent(MenuEvent.DeleteSaveClicked)
        awaitValue(vm.state) { !it.isLoading && !it.corrupted && it.summary == null }
    }

    // ── Settings ──

    @Test
    fun `settings toggles persist`() {
        val vm = SettingsViewModel(settings, repo)
        vm.onEvent(SettingsEvent.Music(false))
        awaitValue(vm.state) { !it.music }
        vm.onEvent(SettingsEvent.ReduceMotion(true))
        val state = awaitValue(vm.state) { it.reduceMotion }
        assertTrue(state.reduceMotion)
    }

    // ── Achievements ──

    @Test
    fun `achievements list reflects unlocked ids`() = runTest {
        var state = repo.newGame(GameSetup("المجد", "رئيس"))
        state = state.copy(unlockedAchievements = setOf("first_landmark"))
        repo.save(state)

        val vm = AchievementsViewModel(ContentLoader.load(), repo)
        val ui = awaitValue(vm.state) { !it.isLoading }
        assertTrue(ui.rows.isNotEmpty())
        assertEquals(1, ui.unlockedCount)
        assertTrue(ui.rows.first { it.id == "first_landmark" }.unlocked)
    }

    // ── Setup wizard ──

    @Test
    fun `setup wizard validates steps and creates the game`() = runTest {
        val vm = SetupViewModel(repo)
        assertEquals(SetupStep.NAME, vm.state.value.step)

        vm.onEvent(SetupEvent.Next)
        assertTrue(vm.state.value.showError)
        assertEquals(SetupStep.NAME, vm.state.value.step)

        vm.onEvent(SetupEvent.NameChanged("المجد"))
        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.TITLE, vm.state.value.step)

        vm.onEvent(SetupEvent.TitleChanged("رئيس"))
        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.FLAG, vm.state.value.step)

        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.TURN, vm.state.value.step)
        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.AXES, vm.state.value.step)
        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.REVIEW, vm.state.value.step)

        val done = collectFirstAsync(vm.done)
        vm.onEvent(SetupEvent.Confirm)
        awaitFuture(done)
        assertTrue(repo.hasSave())
        assertEquals("المجد", repo.peekSummary()!!.countryName)
    }

    @Test
    fun `setup back returns to previous step`() {
        val vm = SetupViewModel(repo)
        vm.onEvent(SetupEvent.NameChanged("X"))
        vm.onEvent(SetupEvent.Next)
        assertEquals(SetupStep.TITLE, vm.state.value.step)
        vm.onEvent(SetupEvent.Back)
        assertEquals(SetupStep.NAME, vm.state.value.step)
    }
}
