package com.example.petquest.data.local

import androidx.room.TypeConverter
import com.example.petquest.data.model.Personality
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.TaskType

class Converters {
    @TypeConverter fun fromPetType(v: PetType) = v.name
    @TypeConverter fun toPetType(v: String) = enumValueOf<PetType>(v)
    @TypeConverter fun fromPersonality(v: Personality) = v.name
    @TypeConverter fun toPersonality(v: String) = enumValueOf<Personality>(v)
    @TypeConverter fun fromTaskType(v: TaskType) = v.name
    @TypeConverter fun toTaskType(v: String) = enumValueOf<TaskType>(v)
}