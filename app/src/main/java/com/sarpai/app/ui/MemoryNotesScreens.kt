package com.sarpai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(vm: AppViewModel) {
    val list by vm.memory.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("আপনার তথ্য দিন", style = MaterialTheme.typography.headlineSmall)
            Text("Saved only on this device. Only entries relevant to your message are sent to your AI provider.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("Search") }, singleLine = true)
                TextButton(onClick = { clearing = true }, enabled = list.isNotEmpty()) { Text("Clear all") }
            }
            val shown = list.filter { query.isBlank() || it.text.contains(query, true) || it.category.contains(query, true) }
            if (shown.isEmpty()) Text("Nothing saved yet. Tap + to add information about yourself.", Modifier.padding(24.dp))
            LazyColumn {
                items(shown, key = { it.id }) { m ->
                    ListItem(
                        overlineContent = { Text(m.category) },
                        headlineContent = { Text(m.text) },
                        trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(m.enabled, { vm.setMemoryEnabled(m.id, it) })
                            IconButton(onClick = { vm.deleteMemory(m.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                        } }
                    )
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Filled.Add, "Add") }
    }
    if (adding) {
        var cat by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text("Add information") },
            text = { Column {
                OutlinedTextField(cat, { cat = it }, label = { Text("Category (e.g. Name, Study, Goals)") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, { text = it }, label = { Text("Information") }, minLines = 3)
            } },
            confirmButton = { TextButton(onClick = { vm.addMemory(cat, text); adding = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } })
    }
    if (clearing) ConfirmDialog("Clear all saved information?", "This cannot be undone.", { vm.clearMemory(); clearing = false }, { clearing = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(vm: AppViewModel) {
    val list by vm.notes.collectAsState()
    var adding by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text("Notes & Tasks", style = MaterialTheme.typography.headlineSmall)
            Text("Reminders/notifications are not implemented yet in v0.1.", style = MaterialTheme.typography.bodySmall)
            if (list.isEmpty()) Text("No notes or tasks yet.", Modifier.padding(24.dp))
            LazyColumn {
                items(list, key = { it.id }) { n ->
                    ListItem(
                        leadingContent = { if (n.isTask) Checkbox(n.done, { vm.setDone(n.id, it) }) },
                        headlineContent = { Text(n.title.ifBlank { n.body.take(40) }) },
                        supportingContent = { if (n.title.isNotBlank() && n.body.isNotBlank()) Text(n.body, maxLines = 2) },
                        trailingContent = { IconButton(onClick = { vm.deleteNote(n.id) }) { Icon(Icons.Filled.Delete, "Delete") } }
                    )
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Filled.Add, "Add") }
    }
    if (adding) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var task by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text("New entry") },
            text = { Column {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(body, { body = it }, label = { Text("Details") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(task, { task = it }); Text("This is a task") }
            } },
            confirmButton = { TextButton(onClick = { vm.addNote(title, body, task); adding = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } })
    }
}
