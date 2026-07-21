package com.scenescribe.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.scenescribe.app.data.api.models.UserDto

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("scenescribe_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUser(): UserDto? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, UserDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveAuth(token: String, user: UserDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
    }
}
