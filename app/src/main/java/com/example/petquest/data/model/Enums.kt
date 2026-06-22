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
    CHICKEN(Rarity.COMMON),
    GUINEA_PIG(Rarity.COMMON),
    FERRET(Rarity.UNCOMMON),
    HORSE(Rarity.EPIC),
    DUCK(Rarity.COMMON),
    FROG(Rarity.UNCOMMON),
    CRAB(Rarity.UNCOMMON),
    MONKEY(Rarity.EPIC),
    FOX(Rarity.EPIC),
    OWL(Rarity.RARE),
    PENGUIN(Rarity.RARE),
    PANDA(Rarity.EPIC),
    GOAT(Rarity.COMMON),
    PIG(Rarity.COMMON),
    COW(Rarity.COMMON),
    SHEEP(Rarity.COMMON),
    DEER(Rarity.RARE),
    BEAR(Rarity.EPIC),
    WOLF(Rarity.EPIC)
}

enum class Personality { PLAYFUL, LAZY, CURIOUS, FRIENDLY, SHY, MISCHIEVOUS }

enum class TaskType { CORE, OPTIONAL }
