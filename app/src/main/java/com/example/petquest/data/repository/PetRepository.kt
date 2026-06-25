package com.example.petquest.data.repository

import com.example.petquest.data.local.PetQuestDao
import com.example.petquest.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class PetRepository(private val dao: PetQuestDao) {

    val allPets: Flow<List<PetEntity>> = dao.getAllPets()
    val allAchievements: Flow<List<AchievementEntity>> = dao.getAllAchievements()

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun getTodaysTasks(date: String): Flow<List<TaskEntity>> =
        dao.getTodaysTasks(date)

    suspend fun insertPet(pet: PetEntity) = dao.insertPet(pet)
    suspend fun insertTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun completeTask(taskId: Int) = dao.completeTask(taskId)
    suspend fun clearAllTasks() = dao.clearAllTasks()
    suspend fun verifyPet(petId: Int, photoUri: String) = dao.verifyPet(petId, photoUri)
    suspend fun unlockAchievement(id: Int) = dao.unlockAchievement(id)

    suspend fun updatePet(petId: Int, name: String) =
        dao.updatePet(petId, name)

    suspend fun deletePet(petId: Int) {
        dao.deleteTasksForPet(petId)
        dao.deletePetById(petId)
    }

    suspend fun addBondPoints(
        petId: Int,
        pointsToAdd: Int,
        currentBondPoints: Int,
        currentBondLevel: Int
    ) {
        val newPoints = currentBondPoints + pointsToAdd
        val newLevel  = (newPoints / 100) + 1
        dao.updateBondPoints(petId, newPoints, newLevel)
    }

    suspend fun hasTasksForPet(petId: Int): Boolean {
        return dao.getTaskCountForPet(petId, todayString()) > 0
    }

    suspend fun seedAchievements() {
        listOf(
            // ── Original achievements ──────────────────────────────────────────
            AchievementEntity(title = "First Pet",          description = "Add your first pet"),
            AchievementEntity(title = "First Verification", description = "Verify a pet with a photo"),
            AchievementEntity(title = "Pet Lover",          description = "Own 3 or more pets"),
            AchievementEntity(title = "Bond Master",        description = "Reach Bond Level 5 with a pet"),
            AchievementEntity(title = "7-Day Streak",       description = "Maintain a 7-day streak"),
            AchievementEntity(title = "Epic Tamer",         description = "Own a pet of EPIC rarity"),
            AchievementEntity(title = "Rarity Hunter",      description = "Own a pet of every rarity tier"),
            AchievementEntity(title = "Species Collector",  description = "Collect 5 different species"),
            AchievementEntity(title = "Animal Explorer",    description = "Collect 10 different species"),
            // ── Streaks ───────────────────────────────────────────────────────
            AchievementEntity(title = "3-Day Streak",       description = "Maintain a 3-day streak"),
            AchievementEntity(title = "30-Day Streak",      description = "Maintain a 30-day streak"),
            // ── Tasks ─────────────────────────────────────────────────────────
            AchievementEntity(title = "Complete 25 Tasks",  description = "Complete 25 tasks in total"),
            AchievementEntity(title = "Complete 50 Tasks",  description = "Complete 50 tasks in total"),
            AchievementEntity(title = "Complete 100 Tasks", description = "Complete 100 tasks in total"),
            AchievementEntity(title = "Complete 250 Tasks", description = "Complete 250 tasks in total"),
            // ── Bond Points ───────────────────────────────────────────────────
            AchievementEntity(title = "Earn 250 Bond Points",  description = "Accumulate 250 total bond points"),
            AchievementEntity(title = "Earn 500 Bond Points",  description = "Accumulate 500 total bond points"),
            AchievementEntity(title = "Earn 1000 Bond Points", description = "Accumulate 1000 total bond points"),
            // ── Pet Levels ────────────────────────────────────────────────────
            AchievementEntity(title = "Reach Level 10", description = "Reach Bond Level 10 with any pet"),
            AchievementEntity(title = "Reach Level 20", description = "Reach Bond Level 20 with any pet"),
            // ── Pet Collection ────────────────────────────────────────────────
            AchievementEntity(title = "Own 5 Pets",    description = "Own 5 or more pets at once"),
            AchievementEntity(title = "Verify 3 Pets", description = "Verify 3 or more pets"),
            AchievementEntity(title = "Verify 5 Pets", description = "Verify 5 or more pets"),
            // ── Pet Bond Level milestones ─────────────────────────────────────
            AchievementEntity(title = "Bond Veteran",        description = "Reach Bond Level 10 with any pet"),
            AchievementEntity(title = "Level 10 Companion",  description = "Reach Bond Level 15 with any pet"),
            AchievementEntity(title = "Virtue Master",       description = "Reach Bond Level 20 with any pet"),
            AchievementEntity(title = "Dedicated Caregiver", description = "Reach Bond Level 30 with any pet"),
            AchievementEntity(title = "Elite Trainer",       description = "Reach Bond Level 50 with any pet"),
            // ── New streaks ───────────────────────────────────────────────────
            AchievementEntity(title = "14-Day Streak",  description = "Maintain a 14-day streak"),
            AchievementEntity(title = "60-Day Streak",  description = "Maintain a 60-day streak"),
            AchievementEntity(title = "100-Day Streak", description = "Maintain a 100-day streak"),
            // ── New tasks ─────────────────────────────────────────────────────
            AchievementEntity(title = "Complete 10 Tasks",  description = "Complete 10 tasks in total"),
            AchievementEntity(title = "Complete 500 Tasks", description = "Complete 500 tasks in total"),
            // ── New bond points ───────────────────────────────────────────────
            AchievementEntity(title = "Earn 100 Bond Points",  description = "Accumulate 100 total bond points"),
            AchievementEntity(title = "Earn 2500 Bond Points", description = "Accumulate 2500 total bond points"),
            AchievementEntity(title = "Earn 5000 Bond Points", description = "Accumulate 5000 total bond points"),
            // ── New trainer levels ────────────────────────────────────────────
            AchievementEntity(title = "Reach Level 30", description = "Reach Trainer Level 30"),
            AchievementEntity(title = "Reach Level 50", description = "Reach Trainer Level 50"),
            // ── New collection ────────────────────────────────────────────────
            AchievementEntity(title = "Own 10 Pets",    description = "Own 10 or more pets at once"),
            AchievementEntity(title = "Verify 10 Pets", description = "Verify 10 or more pets"),
            AchievementEntity(title = "Master Explorer", description = "Collect 15 different species")
        ).forEach { dao.insertAchievement(it) }
    }
}
