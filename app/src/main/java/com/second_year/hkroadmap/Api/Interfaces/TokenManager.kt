package com.second_year.finalproject.Api.Interfaces

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "token_prefs"
    private const val TOKEN_KEY = "token_key"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }
}