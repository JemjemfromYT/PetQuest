package com.example.petquest.data.repository

import com.example.petquest.data.local.PetQuestDao
import com.example.petquest.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PetRepository(private val dao: PetQuestDao) {
    val allPets: Flow<List<PetEntity>> = dao.getAllPets()
    val allAchievements: Flow<List<AchievementEntity>> = dao.getAllAchievements()

    suspend fun addPet(pet: PetEntity): Long = dao.insertPet(pet)
    suspend fun updatePet(pet: PetEntity) = dao.updatePet(pet)
    suspend fun getPetById(id: Int): PetEntity? = dao.getPetById(id)
    suspend fun addTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun updateAchievement(a: AchievementEntity) = dao.updateAchievement(a)

    fun getTodaysTasks(): Flow<List<TaskEntity>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        return dao.getTasksForDateRange(start, cal.timeInMillis)
    }

    suspend fun initializeDefaultAchievements() {
        dao.insertAchievements(listOf(
            AchievementEntity(title = "First Pet", description = "Add your first pet."),
            AchievementEntity(title = "First Verification", description = "Take a photo of your pet."),
            AchievementEntity(title = "Pet Lover", description = "Reach Bond Level 5 with any pet."),
            AchievementEntity(title = "Bond Master", description = "Reach Bond Level 10 with any pet."),
            AchievementEntity(title = "7-Day Streak", description = "Complete all core tasks 7 days in a row.")
        ))
    }
}