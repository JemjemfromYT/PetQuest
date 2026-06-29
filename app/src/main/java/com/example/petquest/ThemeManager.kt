// HOW TO APPLY:
// 1. In Android Studio, right-click your package folder (com.example.petquest)
// 2. New → Kotlin Class/File → Object
// 3. Name it "ThemeManager", select "Object"
// 4. Replace ALL content with everything below the dashed line
// ─────────────────────────────────────────────────────────────────

package com.example.petquest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var themeMode: String by mutableStateOf("LIGHT")
}
