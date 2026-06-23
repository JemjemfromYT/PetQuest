package com.example.petquest.ui

import androidx.annotation.DrawableRes
import com.example.petquest.R
import com.example.petquest.data.model.Virtue

/**
 * Holds the display identity for a single Virtue.
 *
 * The `title` field ("The Sage", etc.) is retained in the data model for
 * potential future use but is NO LONGER displayed anywhere in the UI.
 * The emblem image is the primary identity; the virtue name is secondary.
 *
 * To swap an emblem, drop a PNG file named exactly:
 *   wisdom.png / diligence.png / temperance.png / courage.png / compassion.png
 * into  app/src/main/res/drawable/
 * No code changes are needed — the drawable resource IDs are mapped here.
 */
data class VirtueInfo(
    val virtue: Virtue,
    /** Retained for data completeness, NOT displayed in UI. */
    val title: String,
    /** One-sentence description shown on the Pet Detail screen. */
    val description: String,
    /** Drawable resource ID for the virtue's emblem PNG. */
    @DrawableRes val emblemRes: Int
)

/**
 * Central source of truth for all Virtue identity data.
 *
 * Supported virtues: WISDOM, DILIGENCE, TEMPERANCE, COURAGE, COMPASSION.
 * Fantasy titles are stored but hidden; the emblem is always the main identity.
 *
 * Access via:  VirtueConfig[pet.virtue]
 */
object VirtueConfig {

    private val map: Map<Virtue, VirtueInfo> = mapOf(
        Virtue.WISDOM to VirtueInfo(
            virtue      = Virtue.WISDOM,
            title       = "The Sage",          // not displayed
            description = "Observant, thoughtful, and eager to learn.",
            emblemRes   = R.drawable.wisdom
        ),
        Virtue.DILIGENCE to VirtueInfo(
            virtue      = Virtue.DILIGENCE,
            title       = "The Steward",       // not displayed
            description = "Reliable, disciplined, and hardworking.",
            emblemRes   = R.drawable.diligence
        ),
        Virtue.TEMPERANCE to VirtueInfo(
            virtue      = Virtue.TEMPERANCE,
            title       = "The Balanced",      // not displayed
            description = "Patient, calm, and self-controlled.",
            emblemRes   = R.drawable.temperance
        ),
        Virtue.COURAGE to VirtueInfo(
            virtue      = Virtue.COURAGE,
            title       = "The Guardian",      // not displayed
            description = "Brave, confident, and protective.",
            emblemRes   = R.drawable.courage
        ),
        Virtue.COMPASSION to VirtueInfo(
            virtue      = Virtue.COMPASSION,
            title       = "The Companion",     // not displayed
            description = "Kind, caring, and supportive.",
            emblemRes   = R.drawable.compassion
        )
    )

    /** Returns the [VirtueInfo] for the given [virtue]. Always non-null. */
    operator fun get(virtue: Virtue): VirtueInfo = map.getValue(virtue)
}
