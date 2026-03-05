package com.rk.terminal.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.terminal.telegram.TelegramManager
import org.drinkless.tdlib.TdApi

@Composable
fun LoginScreen(navController: NavController) {
    val telegramManager = TelegramManager.getInstance()
    val authState by telegramManager.authorizationState.collectAsState()

    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Telegram Login", style = MaterialTheme.typography.headlineLarge)

        when (authState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                OutlinedTextField(
                    value = apiId,
                    onValueChange = { apiId = it },
                    label = { Text("API ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiHash,
                    onValueChange = { apiHash = it },
                    label = { Text("API Hash") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val id = apiId.toIntOrNull()
                        if (id != null) {
                            telegramManager.setTdlibParameters(id, apiHash, navController.context.filesDir.absolutePath + "/tdlib")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next")
                }
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                // Handle password if set
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { telegramManager.setPhoneNumber(phoneNumber) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Code")
                }
            }
            is TdApi.AuthorizationStateWaitCode -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { telegramManager.checkAuthenticationCode(code) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
            is TdApi.AuthorizationStateReady -> {
                LaunchedEffect(Unit) {
                    navController.navigate("group_selection")
                }
            }
            else -> {
                Text("Waiting for state: ${authState?.javaClass?.simpleName}")
            }
        }
    }
}
