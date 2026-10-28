package com.sarpai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(
        "Chat" to Icons.Filled.Chat, "History" to Icons.Filled.History,
        "আপনার তথ্য দিন" to Icons.Filled.Person, "Notes" to Icons.Filled.Checklist, "Settings" to Icons.Filled.Settings
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(if (i == 2) "তথ্য" else label, maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ChatScreen(vm, goSettings = { tab = 4 })
                1 -> HistoryScreen(vm, openChat = { vm.open(it); tab = 0 })
                2 -> MemoryScreen(vm)
                3 -> NotesScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}
