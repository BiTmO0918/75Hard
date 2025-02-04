package com.cmu.a75hard.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Classe responsável pela gestão da preferência de tema do utilizador através do SharedPreferences.
 *
 * Esta classe permite guardar e recuperar a preferência do tema (claro ou escuro) do utilizador.
 * É útil para garantir que a escolha do tema persista entre sessões da aplicação.
 *
 * @constructor Cria uma instância de ThemePreferences.
 * @param context Contexto da aplicação utilizado para aceder as SharedPreferences.
 */
class ThemePreferences(context: Context) {

    // SharedPreferences utilizado para armazenar e recuperar as preferências do tema.
    private val preferences: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        // Chave utilizada para armazenar e recuperar a preferência de tema (claro ou escuro).
        private const val DARK_THEME_KEY = "dark_theme"
    }

    /**
     * Guarda a preferência de tema do utilizador.
     *
     * @param isDarkTheme Indica se o tema escuro está ativado.
     * Se true, a preferência salva será para tema escuro; caso contrário, será para tema claro.
     */
    fun saveThemePreference(isDarkTheme: Boolean) {
        preferences.edit().putBoolean(DARK_THEME_KEY, isDarkTheme).apply()
    }

    /**
     * Recupera a preferência de tema do utilizador.
     *
     * @return Retorna true se o tema escuro estiver ativado, ou false se o tema claro estiver ativado.
     * O valor padrão é false (tema claro), caso a preferência ainda não tenha sido guardada.
     */
    fun getThemePreference(): Boolean {
        return preferences.getBoolean(DARK_THEME_KEY, false) // Padrão: tema claro
    }
}
