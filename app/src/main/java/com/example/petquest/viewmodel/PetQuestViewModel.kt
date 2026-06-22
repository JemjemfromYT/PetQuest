package com.example.petquest.viewmodel

import androidx.lifecycle.*
import com.example.petquest.data.model.*
import com.example.petquest.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class PetQuestViewModel(
    private val petRepository: PetRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val allPets: StateFlow<List<PetEntity>> =
        petRepository.allPets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysTasks: StateFlow<List<TaskEntity>> =
        petRepository.todaysTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<AchievementEntity>> =
        petRepository.allAchievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStreak: StateFlow<Int> =
        prefsRepository.userStreak.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasOnboarded: StateFlow<Boolean> =
        prefsRepository.hasOnboarded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val totalBondPoints: StateFlow<Int> = allPets.map { pets ->
        pets.sumOf { it.bondPoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevel: StateFlow<Int> = totalBondPoints.map { points ->
        (points / 100) + 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun addPet(pet: PetEntity) {
        viewModelScope.launch {
            petRepository.insertPet(pet)
            prefsRepository.setOnboarded()
            // Re-fetch pets to get the inserted pet with its real ID
            val pets = petRepository.allPets.first()
            val inserted = pets.lastOrNull() ?: return@launch
            generateTasksForPet(inserted)
            checkAndUnlockAchievements()
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            petRepository.completeTask(task.id)
            val pet = allPets.value.find { it.id == task.petId } ?: return@launch
            val points = if (task.type == TaskType.CORE) 10 else 5
            petRepository.addBondPoints(pet.id, points, pet.bondPoints, pet.bondLevel)
            updateStreak()
            checkAndUnlockAchievements()
        }
    }

    fun verifyPet(petId: Int, photoUri: String) {
        viewModelScope.launch {
            petRepository.verifyPet(petId, photoUri)
            checkAndUnlockAchievements()
        }
    }

    private suspend fun updateStreak() {
        val today = LocalDate.now().toString()
        val last = prefsRepository.lastStreakDate.first()
        val current = prefsRepository.userStreak.first()
        if (last != today) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            val newStreak = if (last == yesterday) current + 1 else 1
            prefsRepository.updateStreak(newStreak)
            prefsRepository.updateLastStreakDate(today)
        }
    }

    private suspend fun generateTasksForPet(pet: PetEntity) {
        if (petRepository.hasTasksForPet(pet.id)) return
        val coreTasks = listOf(
            "Feed ${pet.name}",
            "Give water to ${pet.name}",
            "Spend time with ${pet.name}",
            "Play with ${pet.name}"
        )
        val optionalTasks = listOf(
            "Brush ${pet.name}",
            "Give ${pet.name} a treat",
            "Take a photo of ${pet.name}",
            "Clean ${pet.name}'s area"
        )
        coreTasks.forEach { title ->
            petRepository.insertTask(TaskEntity(petId = pet.id, title = title, type = TaskType.CORE))
        }
        optionalTasks.forEach { title ->
            petRepository.insertTask(TaskEntity(petId = pet.id, title = title, type = TaskType.OPTIONAL))
        }
    }

    private suspend fun checkAndUnlockAchievements() {
        val pets = allPets.value
        val achievements = allAchievements.value

        fun unlock(title: String) {
            val a = achievements.find { it.title == title }
            if (a != null && !a.isUnlocked) {
                viewModelScope.launch { petRepository.unlockAchievement(a.id) }
            }
        }

        if (pets.isNotEmpty()) unlock("First Pet")
        if (pets.any { it.isVerified }) unlock("First Verification")
        if (pets.size >= 3) unlock("Pet Lover")
        if (pets.any { it.bondLevel >= 5 }) unlock("Bond Master")
        if (prefsRepository.userStreak.first() >= 7) unlock("7-Day Streak")
    }
}

class PetQuestViewModelFactory(
    private val petRepository: PetRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PetQuestViewModel(petRepository, userPreferencesRepository) as T
    }
}