package com.example.petquest.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: PetType,
    val personality: Personality,
    val bondLevel: Int = 1,
    val bondPoints: Int = 0,
    val photoUri: String? = null,
    val isVerified: Boolean = false
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petId: Int,
    val title: String,
    val type: TaskType,
    val isCompleted: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockDate: Long? = null
)