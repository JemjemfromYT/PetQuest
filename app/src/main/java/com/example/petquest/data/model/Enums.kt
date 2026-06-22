package com.example.petquest.data.model

enum class Rarity { COMMON, UNCOMMON, RARE, EPIC }

enum class PetType(val rarity: Rarity) {
    CHICKEN(Rarity.COMMON),
    DOG(Rarity.UNCOMMON),
    RABBIT(Rarity.UNCOMMON),
    CAT(Rarity.RARE),
    BIRD(Rarity.RARE),
    TURTLE(Rarity.EPIC)
}

enum class Personality { PLAYFUL, LAZY, CURIOUS, FRIENDLY, SHY, MISCHIEVOUS }

enum class TaskType { CORE, OPTIONAL }