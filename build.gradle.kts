// ROOT build.gradle.kts  (the one at the very top of your project, NOT inside app/)
// HOW TO APPLY: Open this file in Android Studio → Ctrl+A → Delete → Paste this entire file → Sync Now
// CHANGE: Removed "com.google.gms.google-services" (Firebase plugin no longer needed)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
}
