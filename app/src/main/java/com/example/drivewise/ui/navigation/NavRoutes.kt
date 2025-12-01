package com.example.drivewise.ui.navigation

enum class NavRoutes(val route: String) {
    LOGIN("login"),
    REGISTER("register"),
    ADMIN_HOME("admin_home"),
    CLIENT_HOME("client_home"),
    CREATE_POST("create_post"),
    POST_DETAILS("post_details/{postId}");

    companion object {
        const val POST_ID_ARG = "postId"
        fun postDetailsRoute(postId: String) = POST_DETAILS.route.replace("{$POST_ID_ARG}", postId)
    }
}

