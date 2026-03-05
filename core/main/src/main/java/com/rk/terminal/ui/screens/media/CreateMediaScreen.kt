package com.rk.terminal.ui.screens.media

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.terminal.database.DatabaseManager
import com.rk.terminal.model.Media
import com.rk.terminal.model.MediaType
import java.util.UUID

@Composable
fun CreateMediaScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MediaType.MOVIE) }
    var selectedVideoId by remember { mutableLongStateOf(0L) }
    var selectedChatId by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val dbManager = remember { DatabaseManager(context) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Create New Content", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = type == MediaType.MOVIE, onClick = { type = MediaType.MOVIE })
            Text("Movie", modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = type == MediaType.SERIES, onClick = { type = MediaType.SERIES })
            Text("Series")
        }

        FilledTonalButton(
            onClick = {
                // Navigate to video picker and expect result
                navController.navigate("video_picker")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Video from Telegram")
        }

        // In a real app, you'd retrieve the result from the savedStateHandle
        val navBackStackEntry = navController.currentBackStackEntry
        val videoId = navBackStackEntry?.savedStateHandle?.get<Long>("videoId")
        val chatId = navBackStackEntry?.savedStateHandle?.get<Long>("chatId")
        if (videoId != null && chatId != null) {
            selectedVideoId = videoId
            selectedChatId = chatId
            Text("Selected Video ID: $selectedVideoId")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val newMedia = Media(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                type = type,
                telegramFileId = selectedVideoId,
                chatId = selectedChatId
            )
            val db = dbManager.loadDatabase()
            dbManager.saveDatabase(db.copy(medias = db.medias + newMedia))
            navController.popBackStack()
        }, enabled = title.isNotEmpty() && selectedVideoId != 0L) {
            Text("Save")
        }
    }
}
