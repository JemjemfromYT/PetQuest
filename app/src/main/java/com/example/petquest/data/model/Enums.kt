package com.example.petquest.data.model

enum class Rarity { COMMON, UNCOMMON, RARE, EPIC }

enum class PetType(val rarity: Rarity) {
    DOG(Rarity.UNCOMMON),
    CAT(Rarity.RARE),
    RABBIT(Rarity.UNCOMMON),
    HAMSTER(Rarity.COMMON),
    BIRD(Rarity.RARE),
    FISH(Rarity.COMMON),
    TURTLE(Rarity.EPIC),
    LIZARD(Rarity.UNCOMMON),
    SNAKE(Rarity.RARE),
    HEDGEHOG(Rarity.UNCOMMON),
    CHICKEN(Rarity.COMMON)
}

enum class Personality { PLAYFUL, LAZY, CURIOUS, FRIENDLY, SHY, MISCHIEVOUS }

enum class TaskType { CORE, OPTIONAL }