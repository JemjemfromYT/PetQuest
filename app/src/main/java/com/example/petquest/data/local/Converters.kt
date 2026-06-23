package com.example.petquest.data.local

import androidx.room.TypeConverter
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.TaskType
import com.example.petquest.data.model.Virtue

class Converters {
    @TypeConverter fun fromPetType(value: PetType): String = value.name
    @TypeConverter fun toPetType(value: String): PetType = PetType.valueOf(value)

    @TypeConverter fun fromVirtue(value: Virtue): String = value.name
    @TypeConverter fun toVirtue(value: String): Virtue =
        runCatching { Virtue.valueOf(value) }.getOrDefault(Virtue.WISDOM)

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType = TaskType.valueOf(value)
}
