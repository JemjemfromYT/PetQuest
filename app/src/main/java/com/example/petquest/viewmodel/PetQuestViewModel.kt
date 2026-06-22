package com.example.petquest.viewmodel

import androidx.lifecycle.*
import com.example.petquest.data.model.*
import com.example.petquest.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class PetQuestViewModel(
    private val petRepository: PetRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val allPets = petRepository.allPets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val todaysTasks = petRepository.getTodaysTasks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allAchievements = petRepository.allAchievements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val userStreak = prefsRepository.userStreak.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val userLevel = prefsRepository.userLevel.stateIn(viewModelScope, SharingStarted.Lazily, 1)
    val totalBondPoints = prefsRepository.totalBondPoints.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            petRepository.initializeDefaultAchievements()
            checkDailyStreak()
        }
    }

    private suspend fun checkDailyStreak() {
        val lastDate = prefsRepository.lastStreakDate.first()
        val today = startOfDay()
        val yesterday = today - 86_400_000L
        val streak = prefsRepository.userStreak.first()
        when {
            lastDate == 0L -> prefsRepository.updateLastStreakDate(today)
            lastDate >= today -> {}
            lastDate >= yesterday -> {
                prefsRepository.updateStreak(streak + 1)
                prefsRepository.updateLastStreakDate(today)
                if (streak + 1 >= 7) unlockAchievement("7-Day Streak")
            }
            else -> { prefsRepository.updateStreak(0); prefsRepository.updateLastStreakDate(today) }
        }
    }

    fun addPet(pet: PetEntity) = viewModelScope.launch {
        val id = petRepository.addPet(pet).toInt()
        listOf("Feed pet", "Give water", "Spend time together", "Play together").forEach {
            petRepository.addTask(TaskEntity(petId = id, title = it, type = TaskType.CORE))
        }
        listOf("Brush pet", "Give treat", "Take photo", "Clean area").forEach {
            petRepository.addTask(TaskEntity(petId = id, title = it, type = TaskType.OPTIONAL))
        }
        unlockAchievement("First Pet")
    }

    fun completeTask(task: TaskEntity) = viewModelScope.launch {
        petRepository.updateTask(task.copy(isCompleted = true))
        val points = if (task.type == TaskType.CORE) 10 else 5
        prefsRepository.addBondPoints(points)
        val pet = petRepository.getPetById(task.petId) ?: return@launch
        val newPoints = pet.bondPoints + points
        val newLevel = (newPoints / 100) + 1
        petRepository.updatePet(pet.copy(bondPoints = newPoints, bondLevel = newLevel))
        if (newLevel >= 5) unlockAchievement("Pet Lover")
        if (newLevel >= 10) unlockAchievement("Bond Master")
    }

    fun verifyPet(petId: Int, photoUri: String) = viewModelScope.launch {
        val pet = petRepository.getPetById(petId) ?: return@launch
        petRepository.updatePet(pet.copy(photoUri = photoUri, isVerified = true))
        unlockAchievement("First Verification")
    }

    fun getPetById(petId: Int): Flow<PetEntity?> = allPets.map { it.find { p -> p.id == petId } }

    private suspend fun unlockAchievement(title: String) {
        val a = petRepository.allAchievements.first().find { it.title == title && !it.isUnlocked } ?: return
        petRepository.updateAchievement(a.copy(isUnlocked = true, unlockDate = System.currentTimeMillis()))
    }

    private fun startOfDay() = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

class PetQuestViewModelFactory(
    private val repo: PetRepository, private val prefs: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PetQuestViewModel(repo, prefs) as T
    }
}