package com.lifeops.feature

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: LifeViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val archived by remember { vm.dao.archived() }.collectAsStateWithLifecycle(emptyList())
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var removeSamples by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.message(if (granted) "Notifications enabled" else "Notifications are blocked. You can enable them in Android settings.")
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    Backup.export(context, uri)
                    vm.message("Backup exported")
                } catch (_: Exception) {
                    vm.message("Could not export. Check storage access and free space.")
                } finally {
                    busy = false
                }
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri = it }

    LazyColumn(
        contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PageTitle("Settings", "Designed around your day.") }
        item {
            LifeOpsCard {
                Eyebrow("APPEARANCE")
                Text("LifeOps Black / Silver", fontSize = 20.sp)
                Text("Deep black. Quiet silver. Always yours.", color = Muted, fontSize = 13.sp)
            }
        }
        item { Section("Reminders") }
        item {
            LifeOpsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Notifications")
                        Text("Tasks and follow-ups", fontSize = 12.sp, color = Muted)
                    }
                    Switch(
                        checked = settings.notifications,
                        onCheckedChange = {
                            vm.updateSettings(settings.copy(notifications = it))
                            if (it && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
                SelectField(
                    "Default reminder hour",
                    "${settings.reminderHour.toString().padStart(2, '0')}:00",
                    (0..23).map { "${it.toString().padStart(2, '0')}:00" }
                ) { value -> vm.updateSettings(settings.copy(reminderHour = value.substringBefore(':').toInt())) }
                Text(
                    "Reminders use Android background scheduling and can be delayed by battery restrictions. Set a reminder explicitly on each item.",
                    color = Muted,
                    fontSize = 12.sp
                )
                TextButton(onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text("Android notification settings") }
            }
        }
        item {
            LifeOpsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Week starts on Monday", modifier = Modifier.weight(1f))
                    Switch(checked = settings.mondayFirst, onCheckedChange = { vm.updateSettings(settings.copy(mondayFirst = it)) })
                }
            }
        }
        item { Section("Your data") }
        item {
            LifeOpsCard {
                Text("Local backup", fontSize = 20.sp)
                Text("Save a ZIP with your records and activity history. Attached files stay in their original locations and may need to be selected again after restoring.", color = Muted, fontSize = 13.sp)
                LiquidButton(if (busy) "Working…" else "Export backup", { export.launch("LifeOps_Backup.zip") }, Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedButton(onClick = { import.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Import backup") }
                Text("Backups contain readable personal data. Save them somewhere private.", color = Muted, fontSize = 12.sp)
            }
        }
        item {
            LifeOpsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Archive · ${archived.size}")
                    TextButton(onClick = { showArchive = !showArchive }) { Text(if (showArchive) "Hide" else "Open") }
                }
                Text("Archived items are retained until restored. No permanent deletion is performed.", color = Muted, fontSize = 12.sp)
            }
        }
        if (showArchive) {
            items(archived, key = { it.id }) { item ->
                LifeOpsCard {
                    Text(item.title.ifBlank { "Untitled note" })
                    Text(item.kind, color = Muted, fontSize = 12.sp)
                    TextButton(onClick = { vm.restore(item) }) { Text("Restore") }
                }
            }
        }
        item { Section("Try it out") }
        item {
            LifeOpsCard {
                Text("Sample workspace", fontSize = 19.sp)
                Text("Explore realistic examples. Samples are separate from your own items.", color = Muted, fontSize = 13.sp)
                Row {
                    TextButton(onClick = { vm.seed() }) { Text("Load samples") }
                    TextButton(onClick = { removeSamples = true }) { Text("Remove samples") }
                }
            }
        }
        item { Section("Privacy & about") }
        item {
            LifeOpsCard {
                Eyebrow("LIFEOPS · 1.0.0")
                Text("One calm place for what’s next.", fontSize = 21.sp)
                Text("No accounts. No analytics. No servers. LifeOps does not request internet access. Records stay in the app’s private local database. Uninstalling removes app data, so export a backup first.", fontSize = 13.sp, color = Muted, lineHeight = 21.sp)
                Text("AI assistance and cloud sync are not enabled in this version.", color = Muted, fontSize = 12.sp)
            }
        }
    }

    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text("Merge this backup?") },
            text = { Text("New records will be added. Matching IDs are updated only when the backup record is newer. Make a backup first if you want to preserve the current state.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = importUri
                    importUri = null
                    if (uri != null) {
                        scope.launch {
                            busy = true
                            try {
                                val count = Backup.import(context, uri)
                                vm.message("Imported $count new or newer items")
                            } catch (e: Exception) {
                                vm.message("Import failed: ${e.message?.take(100) ?: "invalid backup"}")
                            } finally {
                                busy = false
                            }
                        }
                    }
                }) { Text("Merge backup") }
            },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("Cancel") } }
        )
    }
    if (removeSamples) {
        AlertDialog(
            onDismissRequest = { removeSamples = false },
            title = { Text("Remove sample items?") },
            text = { Text("Your own records will be kept. Sample items can be restored from the archive.") },
            confirmButton = { TextButton(onClick = { vm.clearSamples(); removeSamples = false }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removeSamples = false }) { Text("Cancel") } }
        )
    }
}
