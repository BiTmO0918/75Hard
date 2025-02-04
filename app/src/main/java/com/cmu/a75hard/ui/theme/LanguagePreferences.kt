// LanguagePreferences.kt
package com.cmu.a75hard.utils

import android.content.Context
import android.content.SharedPreferences

class LanguagePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Salva o idioma preferido
    fun saveLanguagePreference(languageCode: String) {
        sharedPreferences.edit().putString("language", languageCode).apply()
    }

    // Recupera o idioma preferido
    fun getLanguagePreference(): String {
        return sharedPreferences.getString("language", "en") ?: "en" // Padrão: Inglês
    }

    // Verifica se é a primeira execução
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("is_first_launch", true)
    }

    // Define se é ou não a primeira execução
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPreferences.edit().putBoolean("is_first_launch", isFirst).apply()
    }
}


