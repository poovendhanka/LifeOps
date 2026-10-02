package com.lifeops

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.lifeops.core.database.LifeDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import java.util.concurrent.TimeUnit

/** Uses the same on-disk Room database as the app, never mocks of the DAO/VM. */
abstract class WorkspaceTest {
    protected val context: Context get() = ApplicationProvider.getApplicationContext()
    protected val db: LifeDatabase get() = LifeDatabase.get(context)
    protected val dao get() = db.dao()

    @Before
    fun resetWorkspace() {
        // Orchestrator also clears app data/process state between cases. Keep this
        // reset so a single test can be run directly from Android Studio as well.
        WorkManager.getInstance(context).cancelAllWork().result.get(10, TimeUnit.SECONDS)
        runBlocking { db.clearAllTables() }
    }
}
