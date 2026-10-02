package com.lifeops

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifeops.core.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreWorkflowsTest : WorkspaceTest() {
    @get:Rule val ui = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After fun closeActivity() { scenario?.close() }

    private fun launch(capture: Boolean = false) {
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java)
            .putExtra("capture", capture))
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("nav:Home").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun capture(kind: String) {
        ui.onNodeWithContentDescription("Quick capture").performClick()
        ui.onNodeWithTag("capture:$kind").performClick()
        ui.onNodeWithTag("editor").assertExists()
    }

    private fun field(label: String, value: String) {
        ui.onNodeWithTag("field:$label").performScrollTo().performTextReplacement(value)
    }

    private fun editorButton(text: String) {
        // IME insets animate outside Compose's test clock. Close the keyboard
        // before positioning/tapping controls near the bottom of the sheet.
        closeSoftKeyboard()
        ui.waitForIdle()
        ui.onNodeWithText(text).performScrollTo().performClick()
    }

    private fun save(kind: String, editing: Boolean = false) {
        editorButton(if (editing) "Save changes" else "Add ${kind.lowercase()}")
        try {
            ui.waitUntil(10_000) { ui.onAllNodesWithTag("editor").fetchSemanticsNodes().isEmpty() }
        } catch (failure: Throwable) {
            dumpUi()
            throw failure
        }
    }

    private fun awaitItem(predicate: (ItemEntity) -> Boolean): ItemEntity {
        var found: ItemEntity? = null
        ui.waitUntil(10_000) {
            found = runBlocking { dao.all().firstOrNull(predicate) }
            found != null
        }
        return requireNotNull(found)
    }

    private fun navigate(tab: String) { ui.onNodeWithTag("nav:$tab").performClick() }

    private fun showTasks() {
        navigate("Tasks")
        ui.onNodeWithText("All", substring = false).performScrollTo().performClick()
    }

    private fun scrollTo(container: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        try {
            val list = ui.onNodeWithTag(container)
            list.performScrollToNode(matcher)
            val target = ui.onNode(matcher)
            // A merely visible control can still sit under the capture FAB.
            // Position it in the upper viewport before sending a real tap.
            val offset = target.fetchSemanticsNode().boundsInRoot.top -
                list.fetchSemanticsNode().boundsInRoot.top - 80f
            if (offset > 0f) list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, offset) }
            return ui.onNode(matcher).assertIsDisplayed()
        } catch (failure: Throwable) {
            dumpUi()
            throw failure
        }
    }

    private fun dumpUi() {
        val roots = ui.onAllNodes(isRoot(), useUnmergedTree = true)
        roots.fetchSemanticsNodes().indices.forEach { roots[it].printToLog("LifeOpsTest") }
    }

    private fun detailText(text: String) = scrollTo("detail", hasText(text))

    @Test fun taskCreationAndEditingPersistAcrossActivityRelaunch() {
        launch()
        capture(Kind.TASK)
        field("Title", "  Prepare review  ")
        field("Next action", "  Draft agenda  ")
        save(Kind.TASK)
        val created = awaitItem { it.title == "Prepare review" }
        assertEquals("Draft agenda", created.nextAction)
        assertEquals("Inbox", created.status)
        showTasks()
        ui.onNodeWithText("Prepare review").performClick()
        ui.onNodeWithContentDescription("Edit item").performClick()
        field("Title", "Review release")
        field("Next action", "Send agenda")
        ui.onNodeWithTag("select:Status").performScrollTo().performClick()
        ui.onNodeWithText("Planned").performClick()
        editorButton("More details")
        ui.onNodeWithTag("select:Priority").performScrollTo().performClick()
        ui.onNodeWithText("High").performClick()
        save(Kind.TASK, editing = true)
        val edited = awaitItem { it.id == created.id && it.title == "Review release" }
        assertEquals("Send agenda", edited.nextAction)
        assertEquals("Planned", edited.status)
        assertEquals(2, edited.priority)
        assertEquals(created.createdAt, edited.createdAt)
        assertEquals(1, runBlocking { dao.all().size })
        assertTrue(runBlocking { dao.allHistory().any { it.itemId == edited.id && it.text == "Status changed to Planned" } })

        scenario?.close()
        launch()
        showTasks()
        ui.onNodeWithText("Review release").performClick()
        detailText("Send agenda").assertIsDisplayed()
        detailText("Planned").assertIsDisplayed()
    }

    @Test fun blankTaskIsRejectedAndDiscardedDraftIsNotSaved() {
        launch()
        capture(Kind.TASK)
        editorButton("Add task")
        ui.onNodeWithText("Please enter a title.").assertIsDisplayed()
        assertTrue(runBlocking { dao.all().isEmpty() })
        field("Title", "Unsaved draft")
        scenario!!.recreate()
        ui.onNodeWithTag("field:Title").assertTextContains("Unsaved draft")
        ui.onNodeWithContentDescription("Close editor").performScrollTo().performClick()
        ui.onNodeWithText("Discard").performClick()
        ui.onNodeWithTag("editor").assertDoesNotExist()
        assertTrue(runBlocking { dao.all().isEmpty() })
    }

    @Test fun followUpCreationTimelineAndResolutionPersist() {
        launch()
        capture(Kind.FOLLOW)
        field("Title", "Service refund")
        field("Next action", "Request reference")
        field("Person / company", "Service desk")
        save(Kind.FOLLOW)
        val follow = awaitItem { it.title == "Service refund" }
        assertEquals("Waiting", follow.status)
        assertNotNull(follow.waitingSince)
        assertEquals("Service desk", follow.contact)
        navigate("More")
        ui.onNodeWithText("Follow-ups").performClick()
        ui.onNodeWithText("Service refund").performClick()
        scrollTo("detail", hasTestTag("field:Add an update")).performTextInput("Called service desk")
        detailText("Add to timeline").performClick()
        awaitItem { it.id == follow.id && it.lastAction == "Called service desk" }
        ui.waitUntil(10_000) { runBlocking { dao.allHistory().any { it.itemId == follow.id && it.text == "Called service desk" } } }
        detailText("Mark resolved").performClick()
        awaitItem { it.id == follow.id && it.status == "Resolved" }
        assertEquals(2, runBlocking { dao.history(follow.id).first().size })
        scrollTo("detail", hasContentDescription("Back")).performClick()
        ui.onNodeWithText("Service refund").assertDoesNotExist()
        ui.onNodeWithText("Resolved").performScrollTo().performClick()
        ui.onNodeWithText("Service refund").assertIsDisplayed()
    }

    @Test fun universalCaptureCreatesTitlelessNoteWithUnicodeBody() {
        launch()
        capture(Kind.NOTE)
        editorButton("Add note")
        ui.onNodeWithText("Write a note or add a title.").assertIsDisplayed()
        field("Your note", "தமிழ் குறிப்பு • remember the idea")
        save(Kind.NOTE)
        val note = awaitItem { it.kind == Kind.NOTE }
        assertEquals("", note.title)
        assertEquals("தமிழ் குறிப்பு • remember the idea", note.body)
        navigate("More")
        ui.onNodeWithText("Notes").performClick()
        ui.onNodeWithText(note.body).assertIsDisplayed()
    }

    @Test fun universalCaptureCreatesProjectAndExpenseWithValidation() {
        launch()
        capture(Kind.PROJECT)
        field("Title", "Home repairs")
        field("Next action", "Request quotation")
        save(Kind.PROJECT)
        assertEquals("Planning", awaitItem { it.title == "Home repairs" }.status)
        navigate("Projects")
        ui.onNodeWithText("Home repairs").assertIsDisplayed()
        capture(Kind.EXPENSE)
        field("Description", "Supplies")
        field("Amount (₹)", "-12")
        editorButton("Add expense")
        ui.onNodeWithText("Enter a valid positive amount.").assertIsDisplayed()
        assertFalse(runBlocking { dao.all().any { it.kind == Kind.EXPENSE } })
        field("Amount (₹)", "1660.25")
        save(Kind.EXPENSE)
        assertEquals(166025L, awaitItem { it.kind == Kind.EXPENSE }.amountMinor)
    }

    @Test fun universalCaptureRequiresDocumentAttachment() {
        launch()
        capture(Kind.DOCUMENT)
        field("Title", "Invoice")
        editorButton("Add document")
        ui.onNodeWithText("Choose a file to attach.").assertIsDisplayed()
        assertTrue(runBlocking { dao.all().isEmpty() })
        ui.onNodeWithContentDescription("Close editor").performScrollTo().performClick()
        ui.onNodeWithText("Discard").performClick()
        ui.onNodeWithTag("editor").assertDoesNotExist()
    }

    @Test fun captureIntentOpensPickerAndDoesNotReopenAfterRecreation() {
        launch(capture = true)
        ui.onNodeWithTag("capture:${Kind.TASK}").assertIsDisplayed()
        ui.onNodeWithTag("capture:${Kind.TASK}").performClick()
        field("Title", "From shortcut")
        save(Kind.TASK)
        scenario!!.recreate()
        ui.onNodeWithTag("capture:${Kind.TASK}").assertDoesNotExist()
        assertEquals(1, runBlocking { dao.all().size })
    }

    @Test fun searchMatchesAllKindsAndMetadataAndPreservesQueryOnBack() {
        val records = listOf(
            ItemEntity(id = "search-task", title = "Needle task"),
            ItemEntity(id = "search-follow", kind = Kind.FOLLOW, title = "Contact result", contact = "Needle person"),
            ItemEntity(id = "search-note", kind = Kind.NOTE, title = "Body result", body = "Needle in body"),
            ItemEntity(id = "search-project", kind = Kind.PROJECT, title = "Action result", nextAction = "Needle action"),
            ItemEntity(id = "search-expense", kind = Kind.EXPENSE, title = "Category result", category = "Needle category"),
            ItemEntity(id = "search-document", kind = Kind.DOCUMENT, title = "Tag result", tags = "Needle tag"),
            ItemEntity(id = "archived", title = "Needle archived", deleted = true),
            ItemEntity(id = "unrelated", title = "Unrelated task")
        )
        runBlocking { dao.putAll(records) }
        launch()
        ui.onNodeWithContentDescription("Search everything").performClick()
        ui.onNodeWithTag("search:query").performTextInput("  nEeDlE  ")
        records.take(6).forEach { record ->
            scrollTo("search", hasText("${record.kind} · 1")).assertIsDisplayed()
            scrollTo("search", hasText(record.title)).assertIsDisplayed()
        }
        ui.onNodeWithText("Needle archived").assertDoesNotExist()
        ui.onNodeWithText("Unrelated task").assertDoesNotExist()
        scrollTo("search", hasText("Contact result")).performClick()
        detailText("Needle person").assertIsDisplayed()
        scrollTo("detail", hasContentDescription("Back")).performClick()
        scrollTo("search", hasTestTag("search:query")).assertTextContains("  nEeDlE  ")
        ui.onNodeWithTag("search:query").performTextReplacement("no-such-record")
        ui.onNodeWithText("No results for “no-such-record”").assertIsDisplayed()
        ui.onNodeWithTag("search:query").performTextClearance()
        ui.onNodeWithText("A thought, a project, a person…").assertIsDisplayed()
    }

    @Test fun archiveConfirmationAndSettingsRestorePreserveRecordAndHistory() {
        val original = ItemEntity(id = "archive-task", title = "Keep this task", status = "Planned", nextAction = "Still needed")
        val event = FollowUpHistoryEntity(id = "event", itemId = original.id, text = "Original history")
        runBlocking { dao.put(original); dao.event(event) }
        launch()
        showTasks()
        ui.onNodeWithText(original.title).performClick()
        ui.onNodeWithContentDescription("Archive item").performClick()
        ui.onNodeWithText("Cancel").performClick()
        assertFalse(runBlocking { dao.get(original.id)!!.deleted })
        ui.onNodeWithContentDescription("Archive item").performClick()
        ui.onNodeWithText("Archive", substring = false).performClick()
        awaitItem { it.id == original.id && it.deleted }
        ui.waitUntil(10_000) { ui.onAllNodesWithText(original.title).fetchSemanticsNodes().isEmpty() }
        assertTrue(runBlocking { dao.observe().first().isEmpty() })
        navigate("More")
        scrollTo("more", hasText("Settings")).performClick()
        scrollTo("settings", hasTestTag("archive:toggle"))
        ui.waitUntil(10_000) { ui.onAllNodesWithText("Archive · 1").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("archive:toggle").performClick()
        ui.onNodeWithTag("archive:toggle").assertTextContains("Hide")
        scrollTo("settings", hasText(original.title)).assertIsDisplayed()
        scrollTo("settings", hasTestTag("archive:restore:${original.id}")).performClick()
        val restored = awaitItem { it.id == original.id && !it.deleted }
        assertEquals(original.copy(updatedAt = restored.updatedAt), restored)
        assertEquals(listOf(event), runBlocking { dao.history(original.id).first() })
        assertTrue(runBlocking { dao.archived().first().isEmpty() })
        showTasks()
        ui.onNodeWithText(original.title).assertIsDisplayed()
    }
}
