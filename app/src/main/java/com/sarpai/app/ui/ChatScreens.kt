package com.sarpai.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sarpai.app.ai.ProviderRegistry
import com.sarpai.app.data.ConversationEntity
import java.text.DateFormat
import java.util.Date

private fun copy(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SARP AI", text))
}
private fun share(ctx: Context, text: String) {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AppViewModel, goSettings: () -> Unit) {
    val ctx = LocalContext.current
    val msgs by vm.messages.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()
    val provider by vm.provider.collectAsState()
    val keyVer by vm.keyVersion.collectAsState()
    val hasKey = remember(provider, keyVer) { vm.repo.keys.has(provider) }
    val input by vm.draft.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Column { Text("SARP AI"); Text(ProviderRegistry.get(provider).label + if (hasKey) "" else " · no key", style = MaterialTheme.typography.labelSmall) } },
            actions = { IconButton(onClick = { vm.newChat() }) { Icon(Icons.Filled.Add, "New conversation") } }
        )
        if (!hasKey) {
            Card(Modifier.padding(12.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Add an API key to start chatting.", style = MaterialTheme.typography.titleSmall)
                    Text("Notes, tasks and saved information still work without one.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = goSettings) { Text("Open Settings") }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            SelectionContainer { Text(m.content) }
                            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.createdAt)), style = MaterialTheme.typography.labelSmall)
                            if (!mine) Row {
                                IconButton(onClick = { copy(ctx, m.content) }) { Icon(Icons.Filled.ContentCopy, "Copy") }
                                IconButton(onClick = { share(ctx, m.content) }) { Icon(Icons.Filled.Share, "Share") }
                            }
                        }
                    }
                }
            }
            if (sending) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Waiting for response…") } }
        }
        error?.let {
            Card(Modifier.padding(12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    if (msgs.isNotEmpty()) TextButton(onClick = { vm.retryOrRegenerate() }) { Text("Retry") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { vm.draft.value = it }, Modifier.weight(1f), placeholder = { Text("Message SARP AI") }, maxLines = 5, shape = RoundedCornerShape(24.dp))
            Spacer(Modifier.width(8.dp))
            if (sending) FilledIconButton(onClick = { vm.cancel() }) { Icon(Icons.Filled.Stop, "Cancel") }
            else FilledIconButton(onClick = { vm.send(input); if (hasKey) vm.draft.value = "" }, enabled = input.isNotBlank()) { Icon(Icons.Filled.Send, "Send") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: AppViewModel, openChat: (Long) -> Unit) {
    val list by vm.conversations.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleting by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleteAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), label = { Text("Search conversations") }, singleLine = true)
            IconButton(onClick = { deleteAll = true }, enabled = list.isNotEmpty()) { Icon(Icons.Filled.DeleteSweep, "Delete all") }
        }
        val shown = list.filter { it.title.contains(query, ignoreCase = true) }
        if (shown.isEmpty()) Text("No conversations yet.", Modifier.padding(24.dp))
        LazyColumn {
            items(shown, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.title, maxLines = 1) },
                    supportingContent = { Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(c.updatedAt))) },
                    trailingContent = { Row {
                        IconButton(onClick = { renaming = c }) { Icon(Icons.Filled.Edit, "Rename") }
                        IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, "Delete") }
                    } },
                    modifier = Modifier.fillMaxWidth().clickable { openChat(c.id) }
                )
                HorizontalDivider()
            }
        }
    }
    renaming?.let { c ->
        var t by remember { mutableStateOf(c.title) }
        AlertDialog(onDismissRequest = { renaming = null }, title = { Text("Rename") },
            text = { OutlinedTextField(t, { t = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(c.id, t); renaming = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } })
    }
    deleting?.let { c ->
        ConfirmDialog("Delete this conversation?", "\"${c.title}\" and its messages will be deleted from this device.", { vm.deleteConversation(c.id); deleting = null }, { deleting = null })
    }
    if (deleteAll) ConfirmDialog("Delete ALL conversations?", "This cannot be undone. Notes, memory and settings are not affected.", { vm.deleteAllConversations(); deleteAll = false }, { deleteAll = false })
}

@Composable
fun ConfirmDialog(title: String, text: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(onDismissRequest = onNo, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onYes) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onNo) { Text("Cancel") } })
}
