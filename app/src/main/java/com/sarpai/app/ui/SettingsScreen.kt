package com.sarpai.app.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sarpai.app.ai.ProviderRegistry

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val s = vm.repo.settings
    val provider by vm.provider.collectAsState()
    val keyVer by vm.keyVersion.collectAsState()
    val theme by vm.theme.collectAsState()
    val test by vm.testStatus.collectAsState()
    val p = ProviderRegistry.get(provider)
    val hasKey = remember(provider, keyVer) { vm.repo.keys.has(provider) }
    var keyInput by remember(provider) { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var model by remember(provider) { mutableStateOf(s.model(provider, p.defaultModel)) }
    var endpoint by remember { mutableStateOf(s.endpoint) }
    var instr by remember { mutableStateOf(s.customInstructions) }
    var memOn by remember { mutableStateOf(s.memoryEnabled) }
    var maxTok by remember { mutableStateOf(s.maxTokens.toString()) }
    var temp by remember { mutableFloatStateOf(s.temperature) }
    var erase by remember { mutableStateOf(false) }
    var sizeTick by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI provider", style = MaterialTheme.typography.titleLarge)
        ProviderRegistry.all.forEach { pr ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(provider == pr.id, { vm.setProvider(pr.id) }); Text(pr.label)
            }
        }
        Text(if (hasKey) "Key saved for ${p.label} (stored encrypted on this device)." else "No key saved for ${p.label}.",
            color = if (hasKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        OutlinedTextField(keyInput, { keyInput = it }, Modifier.fillMaxWidth(), label = { Text("API key") }, singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") }
            Button(onClick = { vm.saveKey(provider, keyInput); keyInput = "" }, enabled = keyInput.isNotBlank()) { Text(if (hasKey) "Replace" else "Save") }
            OutlinedButton(onClick = { vm.deleteKey(provider) }, enabled = hasKey) { Text("Delete") }
        }
        OutlinedTextField(model, { model = it; s.setModel(provider, it) }, Modifier.fillMaxWidth(), label = { Text("Model name") }, singleLine = true,
            supportingText = { Text("Default: ${p.defaultModel}. Model names change over time — check your provider's docs. Not verified until you run the test.") })
        if (provider == "openai") OutlinedTextField(endpoint, { endpoint = it; s.endpoint = it }, Modifier.fillMaxWidth(),
            label = { Text("Custom base URL (optional, https://…/v1)") }, singleLine = true)
        OutlinedTextField(maxTok, { maxTok = it.filter(Char::isDigit).take(5); maxTok.toIntOrNull()?.let { n -> if (n in 16..32000) s.maxTokens = n } },
            Modifier.fillMaxWidth(), label = { Text("Max response tokens") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Text("Temperature: ${"%.1f".format(temp)}")
        Slider(temp, { temp = it; s.temperature = it }, valueRange = 0f..1f)
        Button(onClick = { vm.testConnection() }) { Text("Test connection") }
        test?.let { Text(it) }
        Text("Warning: a key stored on a phone cannot be protected against a determined attacker with device access. Use a key with spending limits.", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()
        Text("Personalization", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system", "light", "dark").forEach { t -> FilterChip(theme == t, { vm.setTheme(t) }, { Text(t) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(memOn, { memOn = it; s.memoryEnabled = it }); Spacer(Modifier.width(8.dp)); Text("Use saved information in AI requests")
        }
        OutlinedTextField(instr, { instr = it; s.customInstructions = it }, Modifier.fillMaxWidth(), label = { Text("Custom instructions (sent with every request)") }, minLines = 3)

        HorizontalDivider()
        Text("Storage & privacy", style = MaterialTheme.typography.titleLarge)
        val fmt = { b: Long -> Formatter.formatShortFileSize(ctx, b) }
        key(sizeTick) {
            Text("Database: ${fmt(vm.dbSizeBytes())} (measured file size)")
            Text("Cache: ${fmt(vm.cacheSizeBytes())} (measured)")
        }
        OutlinedButton(onClick = { vm.clearCache(); sizeTick++ }) { Text("Clear cache (keeps your data)") }
        Text("Data leaves the device only when you send a chat message to your chosen provider. No analytics. No backup to the cloud in v0.1.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { erase = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Erase ALL local data") }

        HorizontalDivider()
        Text("SARP AI v0.1.0 — Your Personal AI Assistant", style = MaterialTheme.typography.bodySmall)
        Text("Not yet implemented: streaming, voice, attachments, reminders, Google Drive/Gmail/Calendar, Cloudinary, app lock. See docs/STATUS.md.", style = MaterialTheme.typography.bodySmall)
    }
    if (erase) ConfirmDialog("Erase everything?", "Deletes conversations, saved information, notes, API keys and settings from this device. This cannot be undone.",
        { vm.eraseEverything(); erase = false }, { erase = false })
}
