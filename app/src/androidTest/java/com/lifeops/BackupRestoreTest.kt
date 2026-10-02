package com.lifeops

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifeops.core.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Exercises the public ZIP/ContentResolver API and its real Room transaction.
 * Cache-file URIs intentionally avoid dependence on a particular system picker.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreTest : WorkspaceTest() {
    private fun file(name: String) = File(context.cacheDir, name)
    private fun root(items: List<ItemEntity>, events: List<FollowUpHistoryEntity> = emptyList()) =
        JSONObject().put("format", "LifeOps").put("version", 1)
            .put("items", JSONArray().apply { items.forEach { put(ItemCodec.encode(it)) } })
            .put("history", JSONArray().apply { events.forEach {
                put(JSONObject().put("id", it.id).put("itemId", it.itemId).put("text", it.text).put("at", it.at))
            } })

    private fun zip(text: String, entry: String = "lifeops.json"): Uri {
        val output = file("fixture.zip")
        ZipOutputStream(output.outputStream()).use {
            it.putNextEntry(ZipEntry(entry)); it.write(text.toByteArray(Charsets.UTF_8)); it.closeEntry()
        }
        return Uri.fromFile(output)
    }

    private suspend fun rejects(uri: Uri, message: String? = null) {
        val failure = runCatching { Backup.import(context, uri) }.exceptionOrNull()
        assertNotNull("Invalid backup must fail", failure)
        if (message != null) assertTrue("Actual error: ${failure?.message}", failure!!.message.orEmpty().contains(message))
    }

    @Test fun exportAndRestoreRoundTripPreservesAllKindsLinksHistoryAndReminders() = runBlocking {
        val future = System.currentTimeMillis() + 7 * 86_400_000L
        val project = ItemEntity(id = "project", kind = Kind.PROJECT, title = "Project", status = "Active", progress = 40, startAt = 1000)
        val task = ItemEntity(id = "task", title = "தமிழ் task", body = "Line one\nLine two • ✓", status = "Planned",
            priority = 3, nextAction = "Draft", due = future, reminderAt = future - 60_000,
            recurrence = "Weekly", projectId = project.id, tags = "work,தமிழ்", pinned = true)
        val follow = ItemEntity(id = "follow", kind = Kind.FOLLOW, title = "Refund", contact = "Service desk", category = "Refund",
            lastAction = "Called", waitingSince = 2000, status = "Waiting", projectId = project.id)
        val note = ItemEntity(id = "note", kind = Kind.NOTE, body = "Archived note", deleted = true, sample = true)
        val expense = ItemEntity(id = "expense", kind = Kind.EXPENSE, title = "Lunch", amountMinor = 166025, payment = "Cash", category = "Food")
        val document = ItemEntity(id = "document", kind = Kind.DOCUMENT, title = "Receipt", projectId = follow.id,
            uri = "content://ungranted.provider/receipt")
        val records = listOf(project, task, follow, note, expense, document)
        val history = listOf(FollowUpHistoryEntity("older", follow.id, "Opened", 3000), FollowUpHistoryEntity("newer", follow.id, "Called", 4000))
        dao.putAll(records); dao.events(history)
        val output = file("roundtrip.zip")
        Backup.export(context, Uri.fromFile(output))
        ZipInputStream(output.inputStream()).use {
            assertEquals("lifeops.json", it.nextEntry.name)
            val json = JSONObject(it.readBytes().toString(Charsets.UTF_8))
            assertEquals("LifeOps", json.getString("format"))
            assertEquals(1, json.getInt("version"))
            assertEquals(6, json.getJSONArray("items").length())
            assertNull(it.nextEntry)
        }
        db.clearAllTables()
        assertEquals(6, Backup.import(context, Uri.fromFile(output)))
        assertEquals(records.map { if (it.id == document.id) it.copy(uri = "") else it }.sortedBy { it.id }, dao.all().sortedBy { it.id })
        assertEquals(history, dao.allHistory())
        assertEquals(history.reversed(), dao.history(follow.id).first())
        assertEquals(listOf(note), dao.archived().first())
        assertEquals(5, dao.observe().first().size)
        assertEquals(ReminderEntity(task.id, future - 60_000), dao.reminderFor(task.id))
        assertEquals(0, Backup.import(context, Uri.fromFile(output)))
        assertEquals(2, dao.allHistory().size)
    }

    @Test fun mergeUpdatesOnlyNewerIdsPreservesLocalOnlyAndIsIdempotent() = runBlocking {
        val newerLocal = ItemEntity(id = "newer-local", title = "Keep newer local", updatedAt = 300)
        val equal = ItemEntity(id = "equal", title = "Keep equal local", updatedAt = 200)
        val olderLocal = ItemEntity(id = "older-local", title = "Replace older local", updatedAt = 100)
        val localOnly = ItemEntity(id = "local-only", title = "Untouched", updatedAt = 100)
        dao.putAll(listOf(newerLocal, equal, olderLocal, localOnly))
        val imported = olderLocal.copy(title = "New backup value", updatedAt = 200, deleted = true)
        val newRecord = ItemEntity(id = "new", title = "Added", updatedAt = 200)
        val uri = zip(root(listOf(newerLocal.copy(title = "Stale", updatedAt = 200), equal.copy(title = "Equal backup"), imported, newRecord)).toString())
        assertEquals(2, Backup.import(context, uri))
        assertEquals(listOf(newerLocal, equal, imported, localOnly, newRecord).sortedBy { it.id }, dao.all().sortedBy { it.id })
        assertEquals(0, Backup.import(context, uri))
        assertEquals(5, dao.all().size)
    }

    @Test fun invalidTimelineRollsBackInsertedAndUpdatedItemsAndKeepsHistory() = runBlocking {
        val existing = ItemEntity(id = "existing", title = "Original", updatedAt = 100)
        val history = FollowUpHistoryEntity("history", existing.id, "Original history", 50)
        dao.put(existing); dao.event(history)
        val incoming = listOf(existing.copy(title = "Must roll back", updatedAt = 200), ItemEntity(id = "new", title = "Must not remain"))
        val invalid = FollowUpHistoryEntity("bad", "missing-parent", "Orphan", 200)
        rejects(zip(root(incoming, listOf(invalid)).toString()), "Invalid timeline link")
        assertEquals(listOf(existing), dao.all())
        assertEquals(listOf(history), dao.allHistory())
    }

    @Test fun duplicateItemIdsAndInvalidValuesDoNotChangeExistingData() = runBlocking {
        val sentinel = ItemEntity(id = "sentinel", title = "Keep", updatedAt = 100)
        dao.put(sentinel)
        val item = ItemEntity(id = "incoming", title = "Valid")
        rejects(zip(root(listOf(item, item.copy(title = "Duplicate"))).toString()), "Duplicate IDs")
        val invalid = listOf(item.copy(kind = "Unknown"), item.copy(priority = 4), item.copy(progress = 101),
            item.copy(amountMinor = -1), item.copy(id = ""), item.copy(updatedAt = 0))
        invalid.forEach { bad ->
            rejects(zip(root(listOf(sentinel.copy(title = "Do not overwrite", updatedAt = 200), bad)).toString()), "Invalid record")
            assertEquals(listOf(sentinel), dao.all())
        }
    }

    @Test fun unsupportedOrMalformedZipIsRejectedWithoutDataLoss() = runBlocking {
        val sentinel = ItemEntity(id = "sentinel", title = "Keep")
        dao.put(sentinel)
        val cases = listOf(
            root(emptyList()).put("version", 2).toString() to "Unsupported backup format",
            root(emptyList()).put("format", "Other").toString() to "Unsupported backup format",
            "{broken json" to null
        )
        cases.forEach { (text, message) ->
            rejects(zip(text), message)
            assertEquals(listOf(sentinel), dao.all())
        }
        rejects(zip(root(emptyList()).toString(), "not-lifeops.json"), "Not a LifeOps backup")
        assertEquals(listOf(sentinel), dao.all())
    }

    @Test fun decompressedSizeLimitRejectsSmallZipWithOversizedPayload() = runBlocking {
        val sentinel = ItemEntity(id = "sentinel", title = "Keep")
        dao.put(sentinel)
        val output = file("oversized.zip")
        ZipOutputStream(output.outputStream()).use { stream ->
            stream.putNextEntry(ZipEntry("lifeops.json"))
            val block = ByteArray(8192) { 'a'.code.toByte() }
            repeat(Backup.MAX_BYTES / block.size) { stream.write(block) }
            stream.write('a'.code)
            stream.closeEntry()
        }
        assertTrue(output.length() < Backup.MAX_BYTES)
        rejects(Uri.fromFile(output), "Backup exceeds 20 MB")
        assertEquals(listOf(sentinel), dao.all())
    }

    @Test fun roomFlowsTrackArchiveRestoreAndKeepPinnedOrdering() = runBlocking {
        val pinned = ItemEntity(id = "pinned", title = "Pinned", pinned = true, updatedAt = 100)
        val recent = ItemEntity(id = "recent", title = "Recent", updatedAt = 300)
        val old = ItemEntity(id = "old", title = "Old", updatedAt = 200)
        dao.putAll(listOf(old, recent, pinned))
        assertEquals(listOf(pinned, recent, old), dao.observe().first())
        val archived = recent.copy(deleted = true)
        dao.put(archived)
        assertEquals(listOf(pinned, old), dao.observe().first())
        assertEquals(listOf(archived), dao.archived().first())
        dao.put(recent)
        assertEquals(listOf(pinned, recent, old), dao.observe().first())
        assertTrue(dao.archived().first().isEmpty())
    }
}
