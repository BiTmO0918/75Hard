package com.cmu.a75hard.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


// Esquema de cores para o tema claro
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000), // Preto para textos e ícones principais
    secondary = Color(0xFF757575), // Cinza para elementos secundários
    tertiary = Color(0xFFF7F7F7), // Fundo dos cards
    background = Color(0xFFFFFFFF), // Branco para o fundo principal
    surface = Color(0xFFF7F7F7), // Fundo para superfícies
    onPrimary = Color.White, // Texto em botões primários
    onSecondary = Color.Black, // Texto em elementos secundários
    onBackground = Color.Black, // Texto no fundo branco
    onSurface = Color.Black // Texto em superfície
)

// Esquema de cores para o tema escuro
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF), // Branco para textos e ícones principais
    secondary = Color(0xFFB0B0B0), // Cinza claro para elementos secundários
    tertiary = Color(0xFF2C2C2C), // Fundo dos cards
    background = Color(0xFF121212), // Preto para o fundo principal
    surface = Color(0xFF1E1E1E), // Fundo para superfícies
    onPrimary = Color.Black, // Texto em botões primários
    onSecondary = Color.White, // Texto em elementos secundários
    onBackground = Color.White, // Texto no fundo preto
    onSurface = Color.White // Texto em superfícies
)

@Composable
fun _75HardTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
