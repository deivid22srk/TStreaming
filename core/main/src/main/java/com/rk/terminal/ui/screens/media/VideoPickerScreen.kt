package com.rk.terminal.ui.screens.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.terminal.telegram.TelegramManager
import org.drinkless.tdlib.TdApi

@Composable
fun VideoPickerScreen(navController: NavController) {
    val telegramManager = TelegramManager.getInstance()
    var chats by remember { mutableStateOf<List<TdApi.Chat>>(emptyList()) }
    var selectedChatId by remember { mutableLongStateOf(0L) }
    var messages by remember { mutableStateOf<List<TdApi.Message>>(emptyList()) }

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

    Scaffold(
        topBar = { Text(if (selectedChatId == 0L) "Select Group" else "Select Video", modifier = Modifier.padding(16.dp)) }
    ) { padding ->
        if (selectedChatId == 0L) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(chats) { chat ->
                    ListItem(
                        headlineContent = { Text(chat.title) },
                        modifier = Modifier.clickable {
                            selectedChatId = chat.id
                            telegramManager.getMessages(chat.id) { response ->
                                messages = response.messages.toList()
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(messages.filter { it.content is TdApi.MessageVideo || it.content is TdApi.MessageDocument }) { message ->
                    val title = when (val content = message.content) {
                        is TdApi.MessageVideo -> content.caption.text.ifEmpty { "Video ${message.id}" }
                        is TdApi.MessageDocument -> content.caption.text.ifEmpty { content.document.fileName }
                        else -> "Unknown"
                    }
                    ListItem(
                        headlineContent = { Text(title) },
                        modifier = Modifier.clickable {
                            navController.previousBackStackEntry?.savedStateHandle?.set("videoId", message.id)
                            navController.previousBackStackEntry?.savedStateHandle?.set("chatId", selectedChatId)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
