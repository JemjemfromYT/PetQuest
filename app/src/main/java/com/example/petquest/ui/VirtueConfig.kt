package com.example.petquest.ui

import androidx.annotation.DrawableRes
import com.example.petquest.R
import com.example.petquest.data.model.Virtue

/**
 * Holds the display identity for a single Virtue.
 *
 * To swap or add an emblem, drop a PNG file named exactly:
 *   wisdom.png / diligence.png / temperance.png / courage.png / compassion.png
 * into  app/src/main/res/drawable/
 * No code changes are needed — the drawable resource IDs are mapped here.
 */
data class VirtueInfo(
    val virtue: Virtue,
    /** Short title shown beneath the emblem on cards. E.g. "The Sage". */
    val title: String,
    /** One-sentence description shown on the Pet Detail screen. */
    val description: String,
    /** Drawable resource ID for the virtue's emblem PNG. */
    @DrawableRes val emblemRes: Int
)

/**
 * Central source of truth for all Virtue identity data.
 * Access via:  VirtueConfig[pet.virtue]
 */
object VirtueConfig {

    private val map: Map<Virtue, VirtueInfo> = mapOf(
        Virtue.WISDOM to VirtueInfo(
            virtue      = Virtue.WISDOM,
            title       = "The Sage",
            description = "Observant, thoughtful, and eager to learn.",
            emblemRes   = R.drawable.wisdom
        ),
        Virtue.DILIGENCE to VirtueInfo(
            virtue      = Virtue.DILIGENCE,
            title       = "The Steward",
            description = "Reliable, disciplined, and hardworking.",
            emblemRes   = R.drawable.diligence
        ),
        Virtue.TEMPERANCE to VirtueInfo(
            virtue      = Virtue.TEMPERANCE,
            title       = "The Balanced",
            description = "Patient, calm, and self-controlled.",
            emblemRes   = R.drawable.temperance
        ),
        Virtue.COURAGE to VirtueInfo(
            virtue      = Virtue.COURAGE,
            title       = "The Guardian",
            description = "Brave, confident, and protective.",
            emblemRes   = R.drawable.courage
        ),
        Virtue.COMPASSION to VirtueInfo(
            virtue      = Virtue.COMPASSION,
            title       = "The Companion",
            description = "Kind, caring, and supportive.",
            emblemRes   = R.drawable.compassion
        )
    )

    /** Returns the [VirtueInfo] for the given [virtue]. Always non-null. */
    operator fun get(virtue: Virtue): VirtueInfo = map.getValue(virtue)
}
