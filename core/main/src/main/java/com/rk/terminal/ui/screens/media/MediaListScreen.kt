package com.rk.terminal.ui.screens.media

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.terminal.database.DatabaseManager
import com.rk.terminal.model.Media

@Composable
fun MediaListScreen(navController: NavController) {
    val context = LocalContext.current
    val dbManager = remember { DatabaseManager(context) }
    var database by remember { mutableStateOf(dbManager.loadDatabase()) }

    Scaffold(
        topBar = { Text("Movies & Series", modifier = Modifier.padding(16.dp)) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create_media") }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(database.medias) { media ->
                ListItem(
                    headlineContent = { Text(media.title) },
                    supportingContent = { Text(media.type.name) }
                )
            }
        }
    }
}
