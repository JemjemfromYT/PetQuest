package com.example.petquest.data.local

import androidx.room.*
import com.example.petquest.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetQuestDao {

    // ─── Pets ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM pets ORDER BY id ASC")
    fun getAllPets(): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Query("UPDATE pets SET bondPoints = :points, bondLevel = :level WHERE id = :petId")
    suspend fun updateBondPoints(petId: Int, points: Int, level: Int)

    @Query("UPDATE pets SET isVerified = 1, photoUri = :uri WHERE id = :petId")
    suspend fun verifyPet(petId: Int, uri: String)

    @Query("UPDATE pets SET name = :name WHERE id = :petId")
    suspend fun updatePet(petId: Int, name: String)

    @Query("DELETE FROM pets WHERE id = :petId")
    suspend fun deletePetById(petId: Int)

    // ─── Tasks ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY type ASC, id ASC")
    fun getTodaysTasks(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun completeTask(taskId: Int)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("DELETE FROM tasks WHERE petId = :petId")
    suspend fun deleteTasksForPet(petId: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE petId = :petId AND date = :date")
    suspend fun getTaskCountForPet(petId: Int, date: String): Int

    // ─── Achievements ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlockAchievement(id: Int)

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getAchievementCount(): Int
}
