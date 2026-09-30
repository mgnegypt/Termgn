package studio.mgn.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Macrobenchmarks for the 60fps goal. These need a real device or an
 * emulator and NEVER run on CI — run them locally with:
 *
 *   ./gradlew :benchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=studio.mgn.benchmark.StartupBenchmark
 *
 * Recorded numbers go in the PLAN 5 report (docs/perf.md).
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCold() = rule.measureRepeated(
        packageName = "studio.mgn.mgn",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
