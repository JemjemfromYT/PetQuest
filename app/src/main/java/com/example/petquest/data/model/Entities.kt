package com.example.petquest.data.model

import androidx.room.*

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: PetType,
    val virtue: Virtue,
    val bondPoints: Int = 0,
    val bondLevel: Int = 1,
    val isVerified: Boolean = false,
    val photoUri: String? = null
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petId: Int,
    val title: String,
    val type: TaskType,
    val isCompleted: Boolean = false,
    val date: String = ""
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
