package com.example.app

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
}