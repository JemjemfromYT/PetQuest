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
    WOLF(Rarity.EPIC),
    DRAGON(Rarity.EPIC),
    DINOSAUR(Rarity.EPIC)
}

enum class Virtue { WISDOM, DILIGENCE, TEMPERANCE, COURAGE, COMPASSION }

enum class Trait(val virtue: Virtue) {
    // Wisdom
    CURIOUS(Virtue.WISDOM),
    OBSERVANT(Virtue.WISDOM),
    ANALYTICAL(Virtue.WISDOM),
    REFLECTIVE(Virtue.WISDOM),
    INSIGHTFUL(Virtue.WISDOM),
    // Diligence
    DISCIPLINED(Virtue.DILIGENCE),
    METICULOUS(Virtue.DILIGENCE),
    CONSCIENTIOUS(Virtue.DILIGENCE),
    RELIABLE(Virtue.DILIGENCE),
    HARDWORKING(Virtue.DILIGENCE),
    // Temperance
    CALM(Virtue.TEMPERANCE),
    MODERATE(Virtue.TEMPERANCE),
    SELF_CONTROLLED(Virtue.TEMPERANCE),
    PATIENT(Virtue.TEMPERANCE),
    BALANCED(Virtue.TEMPERANCE),
    // Courage
    BRAVE(Virtue.COURAGE),
    PROTECTIVE(Virtue.COURAGE),
    CONFIDENT(Virtue.COURAGE),
    BOLD(Virtue.COURAGE),
    FEARLESS(Virtue.COURAGE),
    // Compassion
    CARING(Virtue.COMPASSION),
    FRIENDLY(Virtue.COMPASSION),
    LOYAL(Virtue.COMPASSION),
    AFFECTIONATE(Virtue.COMPASSION),
    EMPATHETIC(Virtue.COMPASSION)
}

// VIRTUE must remain at the END so existing ordinal-based data stays valid.
// Converters.kt stores TaskType by name (string), so adding VIRTUE here is
// fully backward-compatible with existing Room rows.
enum class TaskType { CORE, OPTIONAL, VIRTUE }
