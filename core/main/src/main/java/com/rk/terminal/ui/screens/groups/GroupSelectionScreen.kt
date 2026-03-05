package com.rk.terminal.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.terminal.database.DatabaseManager
import com.rk.terminal.telegram.TelegramManager
import org.drinkless.tdlib.TdApi
import java.io.File

@Composable
fun GroupSelectionScreen(navController: NavController) {
    val telegramManager = TelegramManager.getInstance()
    var chats by remember { mutableStateOf<List<TdApi.Chat>>(emptyList()) }

    LaunchedEffect(Unit) {
        telegramManager.getChats { chatsResponse ->
            val chatList = mutableListOf<TdApi.Chat>()
            chatsResponse.chatIds.forEach { chatId ->
                telegramManager.getChat(chatId) { chat ->
                    chatList.add(chat)
                    if (chatList.size == chatsResponse.chatIds.size) {
                        chats = chatList
                    }
                }
            }
        }
    }

    var selectedChat by remember { mutableStateOf<TdApi.Chat?>(null) }
    val context = navController.context
    val dbManager = remember { DatabaseManager(context) }

    Scaffold(
        topBar = { Text("Database Storage", modifier = Modifier.padding(16.dp)) }
    ) { padding ->
        if (selectedChat == null) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(chats) { chat ->
                    ListItem(
                        headlineContent = { Text(chat.title) },
                        modifier = Modifier.clickable {
                            selectedChat = chat
                        }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text("Group: ${selectedChat?.title}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val zip = dbManager.createZipBackup()
                    telegramManager.uploadDatabase(zip, selectedChat!!.id) { success ->
                        if (success) {
                            navController.navigate("media_list")
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Backup to Telegram")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    telegramManager.downloadDatabase(selectedChat!!.id) { file ->
                        if (file != null) {
                            dbManager.restoreFromZip(file)
                            navController.navigate("media_list")
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore from Telegram")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { selectedChat = null }) {
                    Text("Back to groups")
                }
            }
        }
    }
}
