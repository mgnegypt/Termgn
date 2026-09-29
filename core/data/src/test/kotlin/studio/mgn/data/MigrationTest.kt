package studio.mgn.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import studio.mgn.data.save.MIGRATION_1_2
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simulates the schemaVersion 1 -> 2 transition: builds a real v1 database
 * file, runs the production [MIGRATION_1_2], and verifies the new column
 * exists with its default while old rows survive intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private fun openV1(): SupportSQLiteOpenHelper {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration-test.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS saves (" +
                            "id INTEGER PRIMARY KEY NOT NULL, " +
                            "payload_json TEXT NOT NULL, " +
                            "schema_version INTEGER NOT NULL, " +
                            "country_name TEXT NOT NULL, " +
                            "turn_number INTEGER NOT NULL, " +
                            "date_iso TEXT NOT NULL, " +
                            "saved_at INTEGER NOT NULL)",
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            })
            .build()
        // Fresh file for every run.
        context.deleteDatabase("migration-test.db")
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    @Test
    fun `migration 1 to 2 adds playtime with default and keeps rows`() {
        val helper = openV1()
        val db = helper.writableDatabase
        db.execSQL(
            "INSERT INTO saves (id, payload_json, schema_version, country_name, " +
                "turn_number, date_iso, saved_at) VALUES " +
                "(0, '{}', 1, 'المجد', 7, '2025-08-01', 123456789)",
        )

        MIGRATION_1_2.migrate(db)

        db.query("SELECT * FROM saves WHERE id = 0").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val cols = (0 until cursor.columnCount).associateBy { cursor.getColumnName(it) }
            assertTrue("playtime_seconds" in cols, "new column missing after migration")
            assertEquals(7, cursor.getInt(cols.getValue("turn_number")))
            assertEquals("المجد", cursor.getString(cols.getValue("country_name")))
            assertEquals(0L, cursor.getLong(cols.getValue("playtime_seconds")))
        }
        db.close()
        helper.close()
    }
}
