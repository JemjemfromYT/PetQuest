package com.example.petquest.data.local

import androidx.room.*
import com.example.petquest.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetQuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPet(pet: PetEntity): Long
    @Update suspend fun updatePet(pet: PetEntity)
    @Query("SELECT * FROM pets ORDER BY id ASC") fun getAllPets(): Flow<List<PetEntity>>
    @Query("SELECT * FROM pets WHERE id = :petId") suspend fun getPetById(petId: Int): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTask(task: TaskEntity)
    @Update suspend fun updateTask(task: TaskEntity)
    @Query("SELECT * FROM tasks WHERE dateAdded >= :startOfDay AND dateAdded <= :endOfDay")
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAchievements(list: List<AchievementEntity>)
    @Update suspend fun updateAchievement(achievement: AchievementEntity)
    @Query("SELECT * FROM achievements") fun getAllAchievements(): Flow<List<AchievementEntity>>
}