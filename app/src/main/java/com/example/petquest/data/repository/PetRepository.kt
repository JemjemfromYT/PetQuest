package com.example.petquest.data.repository

import com.example.petquest.data.local.PetQuestDao
import com.example.petquest.data.model.*
import kotlinx.coroutines.flow.Flow

class PetRepository(private val dao: PetQuestDao) {

    val allPets: Flow<List<PetEntity>> = dao.getAllPets()
    val todaysTasks: Flow<List<TaskEntity>> = dao.getTodaysTasks()
    val allAchievements: Flow<List<AchievementEntity>> = dao.getAllAchievements()

    suspend fun insertPet(pet: PetEntity) = dao.insertPet(pet)
    suspend fun insertTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun completeTask(taskId: Int) = dao.completeTask(taskId)
    suspend fun clearAllTasks() = dao.clearAllTasks()
    suspend fun verifyPet(petId: Int, photoUri: String) = dao.verifyPet(petId, photoUri)
    suspend fun unlockAchievement(id: Int) = dao.unlockAchievement(id)

    suspend fun addBondPoints(
        petId: Int,
        pointsToAdd: Int,
        currentBondPoints: Int,
        currentBondLevel: Int
    ) {
        val newPoints = currentBondPoints + pointsToAdd
        val newLevel = (newPoints / 100) + 1
        dao.updateBondPoints(petId, newPoints, newLevel)
    }

    suspend fun hasTasksForPet(petId: Int): Boolean {
        return dao.getTaskCountForPet(petId) > 0
    }

    suspend fun seedAchievements() {
        if (dao.getAchievementCount() > 0) return
        listOf(
            AchievementEntity(title = "First Pet",          description = "Add your first pet"),
            AchievementEntity(title = "First Verification", description = "Verify a pet with a photo"),
            AchievementEntity(title = "Pet Lover",          description = "Own 3 or more pets"),
            AchievementEntity(title = "Bond Master",        description = "Reach Bond Level 5 with a pet"),
            AchievementEntity(title = "7-Day Streak",       description = "Maintain a 7-day streak")
        ).forEach { dao.insertAchievement(it) }
    }
}