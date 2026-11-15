package com.example.edu_kt_hw_06.data

import android.content.Context
import androidx.core.content.edit

class AuthPreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getLogin(): String? = preferences.getString(KEY_LOGIN, null)

    fun getPassword(): String? = preferences.getString(KEY_PASSWORD, null)

    fun save(login: String, password: String) {
        preferences.edit {
            putString(KEY_LOGIN, login)
            putString(KEY_PASSWORD, password)
        }
    }

    fun clear() {
        preferences.edit { clear() }
    }

    companion object {
        private const val PREFERENCES_NAME = "auth"
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
    }
}
