package com.rk.terminal.ui.routes

sealed class MainActivityRoutes(val route: String) {
    data object Settings : MainActivityRoutes("settings")
    data object Customization : MainActivityRoutes("customization")
    data object MainScreen : MainActivityRoutes("main")

    data object Login : MainActivityRoutes("login")
    data object GroupSelection : MainActivityRoutes("group_selection")
    data object MediaList : MainActivityRoutes("media_list")
    data object CreateMedia : MainActivityRoutes("create_media")
    data object VideoPicker : MainActivityRoutes("video_picker")
}