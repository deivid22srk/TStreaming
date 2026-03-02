package com.rk.terminal.ui.screens.bot

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.normal.Preference
import com.rk.settings.Settings
import com.rk.terminal.ui.routes.MainActivityRoutes
import org.json.JSONObject
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotSettings(navController: NavController) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = JSONObject().apply {
                    put("bot_api_id", Settings.bot_api_id)
                    put("bot_api_hash", Settings.bot_api_hash)
                    put("bot_owner_id", Settings.bot_owner_id)
                    put("bot_allowed_ids", Settings.bot_allowed_ids)
                    put("bot_username", Settings.bot_username)
                    put("bot_token", Settings.bot_token)
                    put("bot_channel_id", Settings.bot_channel_id)
                    put("bot_secret_length", Settings.bot_secret_length)
                    put("bot_base_url", Settings.bot_base_url)
                    put("bot_bind_addr", Settings.bot_bind_addr)
                    put("bot_port", Settings.bot_port)
                }
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(json.toString(4).toByteArray())
                }
                Toast.makeText(context, "Configurações exportadas!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { `is` ->
                    val json = JSONObject(`is`.bufferedReader().readText())
                    if (json.has("bot_api_id")) Settings.bot_api_id = json.getString("bot_api_id")
                    if (json.has("bot_api_hash")) Settings.bot_api_hash = json.getString("bot_api_hash")
                    if (json.has("bot_owner_id")) Settings.bot_owner_id = json.getString("bot_owner_id")
                    if (json.has("bot_allowed_ids")) Settings.bot_allowed_ids = json.getString("bot_allowed_ids")
                    if (json.has("bot_username")) Settings.bot_username = json.getString("bot_username")
                    if (json.has("bot_token")) Settings.bot_token = json.getString("bot_token")
                    if (json.has("bot_channel_id")) Settings.bot_channel_id = json.getString("bot_channel_id")
                    if (json.has("bot_secret_length")) Settings.bot_secret_length = json.getString("bot_secret_length")
                    if (json.has("bot_base_url")) Settings.bot_base_url = json.getString("bot_base_url")
                    if (json.has("bot_bind_addr")) Settings.bot_bind_addr = json.getString("bot_bind_addr")
                    if (json.has("bot_port")) Settings.bot_port = json.getString("bot_port")
                }
                Toast.makeText(context, "Configurações importadas!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                navController.navigate(MainActivityRoutes.BotSettings.route)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    PreferenceLayout(
        label = "Configurações do Bot",
        backArrowVisible = true
    ) {
        PreferenceGroup(heading = "Backup") {
            Preference(
                text = { Text("Exportar Configurações") },
                secondaryText = { Text("Salva suas configurações em um arquivo JSON") },
                onClick = { exportLauncher.launch("bot_settings.json") }
            )
            Preference(
                text = { Text("Importar Configurações") },
                secondaryText = { Text("Carrega configurações de um arquivo JSON") },
                onClick = { importLauncher.launch("application/json") }
            )
        }

        PreferenceGroup(heading = "Telegram Configuration") {
            BotSettingEntry(title = "API ID", value = Settings.bot_api_id) { Settings.bot_api_id = it }
            BotSettingEntry(title = "API Hash", value = Settings.bot_api_hash) { Settings.bot_api_hash = it }
            BotSettingEntry(title = "Owner ID", value = Settings.bot_owner_id) { Settings.bot_owner_id = it }
            BotSettingEntry(title = "Allowed User IDs", value = Settings.bot_allowed_ids) { Settings.bot_allowed_ids = it }
            BotSettingEntry(title = "Bot Username", value = Settings.bot_username) { Settings.bot_username = it }
            BotSettingEntry(title = "Bot Token", value = Settings.bot_token) { Settings.bot_token = it }
            BotSettingEntry(title = "Channel ID", value = Settings.bot_channel_id) { Settings.bot_channel_id = it }
        }
        PreferenceGroup(heading = "Server Configuration") {
            BotSettingEntry(title = "Secret Code Length", value = Settings.bot_secret_length) { Settings.bot_secret_length = it }
            BotSettingEntry(title = "Base URL", value = Settings.bot_base_url) { Settings.bot_base_url = it }
            BotSettingEntry(title = "Bind Address", value = Settings.bot_bind_addr) { Settings.bot_bind_addr = it }
            BotSettingEntry(title = "Port", value = Settings.bot_port) { Settings.bot_port = it }
        }
    }
}

@Composable
fun BotSettingEntry(title: String, value: String, onValueChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    Preference(
        text = { Text(title) },
        secondaryText = { Text(value) },
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(tempValue)
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
