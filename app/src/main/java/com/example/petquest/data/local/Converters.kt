package com.example.petquest.data.model

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromPetType(value: PetType): String = value.name
    @TypeConverter fun toPetType(value: String): PetType = PetType.valueOf(value)

    @TypeConverter fun fromPersonality(value: Personality): String = value.name
    @TypeConverter fun toPersonality(value: String): Personality = Personality.valueOf(value)

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType = TaskType.valueOf(value)
}