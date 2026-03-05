package com.rk.terminal.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramManager {
    private var client: Client? = null

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState: StateFlow<TdApi.AuthorizationState?> = _authorizationState

    private val resultHandler = Client.ResultHandler { object_ ->
        when (object_.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val update = object_ as TdApi.UpdateAuthorizationState
                _authorizationState.value = update.authorizationState
            }
        }
    }

    init {
        // Initialize TDLib client
        System.loadLibrary("tdjni")
        client = Client.create(resultHandler, null, null)
    }

    fun setTdlibParameters(apiId: Int, apiHash: String, databaseDirectory: String) {
        val parameters = TdApi.SetTdlibParameters().apply {
            this.apiId = apiId
            this.apiHash = apiHash
            this.useMessageDatabase = true
            this.useSecretChats = true
            this.systemLanguageCode = "en"
            this.deviceModel = "Android"
            this.applicationVersion = "1.0"
            this.databaseDirectory = databaseDirectory
        }
        client?.send(parameters) {
            // Handle result
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), {
            // Handle result
        })
    }

    fun checkAuthenticationCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), {
            // Handle result
        })
    }

    fun uploadDatabase(file: File, chatId: Long, callback: (Boolean) -> Unit) {
        val inputLocalFile = TdApi.InputFileLocal(file.absolutePath)
        val content = TdApi.InputMessageDocument(inputLocalFile, null, false, TdApi.FormattedText("Database Backup", null))
        client?.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { object_ ->
            callback(object_ is TdApi.Message)
        }
    }

    fun downloadDatabase(chatId: Long, callback: (File?) -> Unit) {
        // Find the last document message in the chat that looks like a backup
        getMessages(chatId, limit = 10) { messages ->
            val backupMessage = messages.messages.firstOrNull {
                it.content is TdApi.MessageDocument && (it.content as TdApi.MessageDocument).document.fileName == "database_backup.zip"
            }
            if (backupMessage != null) {
                val document = (backupMessage.content as TdApi.MessageDocument).document
                client?.send(TdApi.DownloadFile(document.document.id, 1, 0, 0, true)) { object_ ->
                    if (object_ is TdApi.File) {
                        callback(File(object_.local.path))
                    } else {
                        callback(null)
                    }
                }
            } else {
                callback(null)
            }
        }
    }

    fun send(function: TdApi.Function<out TdApi.Object>, handler: Client.ResultHandler) {
        client?.send(function, handler)
    }

    // Helper to get chats (groups)
    fun getChats(limit: Int = 100, callback: (TdApi.Chats) -> Unit) {
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { object_ ->
            if (object_ is TdApi.Chats) {
                callback(object_)
            }
        }
    }

    fun getChat(chatId: Long, callback: (TdApi.Chat) -> Unit) {
        client?.send(TdApi.GetChat(chatId)) { object_ ->
            if (object_ is TdApi.Chat) {
                callback(object_)
            }
        }
    }

    fun getMessages(chatId: Long, fromMessageId: Long = 0, limit: Int = 50, callback: (TdApi.Messages) -> Unit) {
        client?.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) { object_ ->
            if (object_ is TdApi.Messages) {
                callback(object_)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TelegramManager? = null

        fun getInstance(): TelegramManager {
            return instance ?: synchronized(this) {
                instance ?: TelegramManager().also { instance = it }
            }
        }
    }
}
