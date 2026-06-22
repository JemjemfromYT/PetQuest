package com.example.petquest.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary                = PetOrange40,
    onPrimary              = PetNeutral99,
    primaryContainer       = PetOrange90,
    onPrimaryContainer     = PetOrange10,

    secondary              = PetGreen40,
    onSecondary            = PetNeutral99,
    secondaryContainer     = PetGreen90,
    onSecondaryContainer   = PetGreen10,

    tertiary               = PetPink40,
    onTertiary             = PetNeutral99,
    tertiaryContainer      = PetPink90,
    onTertiaryContainer    = PetPink10,

    error                  = PetError40,
    errorContainer         = PetError90,

    background             = PetNeutral99,
    onBackground           = PetNeutral10,
    surface                = PetNeutral99,
    onSurface              = PetNeutral10,
    surfaceVariant         = PetNeutral90,
    onSurfaceVariant       = PetNeutral20,
    outline                = PetNeutral20
)

private val DarkColorScheme = darkColorScheme(
    primary                = PetOrange80,
    onPrimary              = PetOrange20,
    primaryContainer       = PetOrange40,
    onPrimaryContainer     = PetOrange90,

    secondary              = PetGreen80,
    onSecondary            = PetGreen20,
    secondaryContainer     = PetGreen40,
    onSecondaryContainer   = PetGreen90,

    tertiary               = PetPink80,
    onTertiary             = PetPink20,
    tertiaryContainer      = PetPink40,
    onTertiaryContainer    = PetPink90,

    error                  = PetError80,
    errorContainer         = PetError40,

    background             = PetNeutral10,
    onBackground           = PetNeutral90,
    surface                = PetNeutral10,
    onSurface              = PetNeutral90,
    surfaceVariant         = PetNeutral20,
    onSurfaceVariant       = PetNeutral90,
    outline                = PetNeutral90
)

@Composable
fun PetQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = Typography,
        content      = content
    )
}
