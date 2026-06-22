package com.example.petquest.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.petquest.data.model.*
import com.example.petquest.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── Level-up event payload ────────────────────────────────────────────────
data class LevelUpEvent(
    val petName: String,
    val oldLevel: Int,
    val newLevel: Int
)

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

    val hasOnboarded: StateFlow<Boolean?> =
        prefsRepository.hasOnboarded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalBondPoints: StateFlow<Int> = allPets.map { pets ->
        pets.sumOf { it.bondPoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevel: StateFlow<Int> = totalBondPoints.map { points ->
        (points / 100) + 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // ─── Level-up event stream ─────────────────────────────────────────────
    private val _levelUpEvent = MutableSharedFlow<LevelUpEvent>()
    val levelUpEvent: SharedFlow<LevelUpEvent> = _levelUpEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                checkDailyReset()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "daily reset error", e)
            }
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun yesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private suspend fun checkDailyReset() {
        val today = todayString()
        val lastTaskDate = prefsRepository.lastTaskDate.first()
        if (lastTaskDate != today) {
            petRepository.clearAllTasks()
            val pets = petRepository.allPets.first()
            pets.forEach { pet -> generateTasksForPet(pet) }
            prefsRepository.updateLastTaskDate(today)
        }
    }

    // ─── Public actions ────────────────────────────────────────────────────

    fun addPet(pet: PetEntity) {
        viewModelScope.launch {
            try {
                petRepository.insertPet(pet)
                prefsRepository.setOnboarded()
                val pets = petRepository.allPets.first()
                val inserted = pets.maxByOrNull { it.id } ?: return@launch
                generateTasksForPet(inserted)
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "addPet error", e)
            }
        }
    }

    fun editPet(petId: Int, newName: String, newPersonality: Personality) {
        viewModelScope.launch {
            try {
                petRepository.updatePet(petId, newName.trim(), newPersonality)
            } catch (e: Exception) {
                Log.e("PetQuestVM", "editPet error", e)
            }
        }
    }

    fun deletePet(petId: Int) {
        viewModelScope.launch {
            try {
                petRepository.deletePet(petId)
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "deletePet error", e)
            }
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                petRepository.completeTask(task.id)
                val pets = petRepository.allPets.first()
                val pet = pets.find { it.id == task.petId } ?: return@launch

                val points = if (task.type == TaskType.CORE) 10 else 5
                val oldLevel = pet.bondLevel
                val newPoints = pet.bondPoints + points
                val newLevel = (newPoints / 100) + 1

                petRepository.addBondPoints(pet.id, points, pet.bondPoints, pet.bondLevel)

                if (newLevel > oldLevel) {
                    _levelUpEvent.emit(LevelUpEvent(pet.name, oldLevel, newLevel))
                }

                updateStreak()
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "completeTask error", e)
            }
        }
    }

    fun verifyPet(petId: Int, photoUri: String) {
        viewModelScope.launch {
            try {
                petRepository.verifyPet(petId, photoUri)
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "verifyPet error", e)
            }
        }
    }

    private suspend fun updateStreak() {
        val today = todayString()
        val last = prefsRepository.lastStreakDate.first()
        val current = prefsRepository.userStreak.first()
        if (last != today) {
            val yesterday = yesterdayString()
            val newStreak = if (last == yesterday) current + 1 else 1
            prefsRepository.updateStreak(newStreak)
            prefsRepository.updateLastStreakDate(today)
        }
    }

    // ─── Personality-aware task generation ────────────────────────────────
    private suspend fun generateTasksForPet(pet: PetEntity) {
        if (petRepository.hasTasksForPet(pet.id)) return

        // 2 universal core tasks + 2 personality-specific core tasks
        val universalCore = listOf(
            "Feed ${pet.name}",
            "Give water to ${pet.name}"
        )
        val personalityCore = when (pet.personality) {
            Personality.PLAYFUL     -> listOf(
                "Play with ${pet.name}",
                "Chase toy with ${pet.name}"
            )
            Personality.LAZY        -> listOf(
                "Relax with ${pet.name}",
                "Give ${pet.name} a cozy rest area"
            )
            Personality.CURIOUS     -> listOf(
                "Explore something new with ${pet.name}",
                "Let ${pet.name} investigate a safe object"
            )
            Personality.FRIENDLY    -> listOf(
                "Spend social time with ${pet.name}",
                "Introduce ${pet.name} to someone new"
            )
            Personality.SHY         -> listOf(
                "Spend quiet time with ${pet.name}",
                "Create a calm space for ${pet.name}"
            )
            Personality.MISCHIEVOUS -> listOf(
                "Give ${pet.name} an enrichment activity",
                "Redirect ${pet.name}'s energy positively"
            )
        }

        (universalCore + personalityCore).forEach { title ->
            petRepository.insertTask(
                TaskEntity(petId = pet.id, title = title, type = TaskType.CORE)
            )
        }

        // 4 universal optional tasks
        listOf(
            "Brush ${pet.name}",
            "Give ${pet.name} a treat",
            "Take a photo of ${pet.name}",
            "Clean ${pet.name}'s area"
        ).forEach { title ->
            petRepository.insertTask(
                TaskEntity(petId = pet.id, title = title, type = TaskType.OPTIONAL)
            )
        }
    }

    private suspend fun checkAndUnlockAchievements() {
        val pets = petRepository.allPets.first()
        val achievements = petRepository.allAchievements.first()
        if (achievements.isEmpty()) return

        suspend fun unlock(title: String) {
            val a = achievements.find { it.title == title }
            if (a != null && !a.isUnlocked) petRepository.unlockAchievement(a.id)
        }

        if (pets.isNotEmpty()) unlock("First Pet")
        if (pets.any { it.isVerified }) unlock("First Verification")
        if (pets.size >= 3) unlock("Pet Lover")
        if (pets.any { it.bondLevel >= 5 }) unlock("Bond Master")
        if (prefsRepository.userStreak.first() >= 7) unlock("7-Day Streak")
    }

    // ─── Admin / Debug Functions ───────────────────────────────────────────

    fun adminAddBondPoints(petId: Int, points: Int) {
        viewModelScope.launch {
            try {
                val pet = petRepository.allPets.first().find { it.id == petId } ?: return@launch
                val oldLevel = pet.bondLevel
                val newPoints = pet.bondPoints + points
                val newLevel = (newPoints / 100) + 1
                petRepository.addBondPoints(petId, points, pet.bondPoints, pet.bondLevel)
                if (newLevel > oldLevel) {
                    _levelUpEvent.emit(LevelUpEvent(pet.name, oldLevel, newLevel))
                }
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminAddBondPoints error", e)
            }
        }
    }

    fun adminCompleteAllTasks() {
        viewModelScope.launch {
            try {
                val tasks = petRepository.todaysTasks.first()
                val incompleteTasks = tasks.filter { !it.isCompleted }
                incompleteTasks.forEach { task -> petRepository.completeTask(task.id) }
                val pets = petRepository.allPets.first()
                pets.forEach { pet ->
                    val totalPoints = incompleteTasks
                        .filter { it.petId == pet.id }
                        .sumOf { if (it.type == TaskType.CORE) 10 else 5 as Int }
                    if (totalPoints > 0) {
                        val oldLevel = pet.bondLevel
                        val newPoints = pet.bondPoints + totalPoints
                        val newLevel = (newPoints / 100) + 1
                        petRepository.addBondPoints(pet.id, totalPoints, pet.bondPoints, pet.bondLevel)
                        if (newLevel > oldLevel) {
                            _levelUpEvent.emit(LevelUpEvent(pet.name, oldLevel, newLevel))
                        }
                    }
                }
                updateStreak()
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminCompleteAllTasks error", e)
            }
        }
    }

    fun adminResetTasks() {
        viewModelScope.launch {
            try {
                petRepository.clearAllTasks()
                val pets = petRepository.allPets.first()
                pets.forEach { generateTasksForPet(it) }
                prefsRepository.updateLastTaskDate(todayString())
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminResetTasks error", e)
            }
        }
    }

    fun adminUnlockAllAchievements() {
        viewModelScope.launch {
            try {
                val achievements = petRepository.allAchievements.first()
                achievements.filter { !it.isUnlocked }.forEach {
                    petRepository.unlockAchievement(it.id)
                }
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminUnlockAll error", e)
            }
        }
    }

    fun adminSetStreak(streak: Int) {
        viewModelScope.launch {
            try {
                prefsRepository.updateStreak(streak)
                prefsRepository.updateLastStreakDate(todayString())
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminSetStreak error", e)
            }
        }
    }

    fun adminVerifyAllPets() {
        viewModelScope.launch {
            try {
                val pets = petRepository.allPets.first()
                pets.filter { !it.isVerified }.forEach { pet ->
                    petRepository.verifyPet(pet.id, "admin_verified")
                }
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminVerifyAll error", e)
            }
        }
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
