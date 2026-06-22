package com.example.petquest.data.model

import androidx.room.*
import com.example.petquest.data.model.Converters

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: PetType,
    val personality: Personality,
    val bondPoints: Int = 0,
    val bondLevel: Int = 1,
    val isVerified: Boolean = false,
    val photoUri: String? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petId: Int,
    val title: String,
    val type: TaskType,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["title"], unique = true)]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false
)