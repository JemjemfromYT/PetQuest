// ============================================================
// FILE: app/src/main/java/com/example/petquest/viewmodel/PetQuestViewModel.kt
//       (COMPLETE FILE — replace fully)
// CHANGES (V1.5):
//   1. Added notificationsEnabled, notificationHour, notificationMinute StateFlows
//   2. Added setNotificationsEnabled() function
//   3. Added setReminderTime() function
//   Note: WorkManager scheduling is intentionally handled by the UI layer
//   (ProfileScreen) which owns Context. The ViewModel only manages DataStore.
// ============================================================

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

    // ─── Existing StateFlows ───────────────────────────────────────────────

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

    // ─── V1.2 Collection flows ─────────────────────────────────────────────

    val collectedSpecies: StateFlow<Set<PetType>> = allPets.map { pets ->
        pets.map { it.type }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val collectionPercentage: StateFlow<Int> = collectedSpecies.map { species ->
        val total = PetType.entries.size
        if (total == 0) 0 else (species.size * 100) / total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rarityCollectionStats: StateFlow<Map<Rarity, Pair<Int, Int>>> =
        collectedSpecies.map { species ->
            Rarity.entries.associateWith { rarity ->
                val total     = PetType.entries.count { it.rarity == rarity }
                val collected = species.count { it.rarity == rarity }
                collected to total
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─── V1.5 Notification StateFlows ─────────────────────────────────────

    /** Whether the user has enabled daily reminder notifications. */
    val notificationsEnabled: StateFlow<Boolean> =
        prefsRepository.notificationsEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Hour component of the scheduled reminder (24-hour clock). Default 8. */
    val notificationHour: StateFlow<Int> =
        prefsRepository.notificationHour
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    /** Minute component of the scheduled reminder. Default 0. */
    val notificationMinute: StateFlow<Int> =
        prefsRepository.notificationMinute
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    // ─── Helpers ──────────────────────────────────────────────────────────

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
            val pets = allPets.first { it.isNotEmpty() || petRepository.allPets.first().isEmpty() }
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
                val pets = allPets.first { list -> list.any { it.name == pet.name && it.type == pet.type } }
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
                val pet = allPets.value.find { it.id == task.petId } ?: return@launch

                val points   = if (task.type == TaskType.CORE) 10 else 5
                val oldLevel = pet.bondLevel
                val newPoints = pet.bondPoints + points
                val newLevel  = (newPoints / 100) + 1

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
        val last  = prefsRepository.lastStreakDate.first()
        val current = prefsRepository.userStreak.first()
        if (last != today) {
            val yesterday = yesterdayString()
            val newStreak = if (last == yesterday) current + 1 else 1
            prefsRepository.updateStreak(newStreak)
            prefsRepository.updateLastStreakDate(today)
        }
    }

    // ─── V1.5 Notification actions ─────────────────────────────────────────

    /**
     * Persist the notification enabled/disabled preference.
     * The caller (ProfileScreen) is responsible for scheduling or cancelling
     * the WorkManager job, since WorkManager requires a Context.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                prefsRepository.setNotificationsEnabled(enabled)
            } catch (e: Exception) {
                Log.e("PetQuestVM", "setNotificationsEnabled error", e)
            }
        }
    }

    /**
     * Persist the reminder time preference.
     * The caller (ProfileScreen) is responsible for rescheduling the
     * WorkManager job with the new time.
     */
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                prefsRepository.setReminderTime(hour, minute)
            } catch (e: Exception) {
                Log.e("PetQuestVM", "setReminderTime error", e)
            }
        }
    }

    // ─── Personality-aware task generation ────────────────────────────────

    private suspend fun generateTasksForPet(pet: PetEntity) {
        if (petRepository.hasTasksForPet(pet.id)) return

        val universalCore = listOf(
            "Feed ${pet.name}",
            "Give water to ${pet.name}"
        )
        val personalityCore = when (pet.personality) {
            Personality.PLAYFUL     -> listOf("Play with ${pet.name}", "Chase toy with ${pet.name}")
            Personality.LAZY        -> listOf("Relax with ${pet.name}", "Give ${pet.name} a cozy rest area")
            Personality.CURIOUS     -> listOf("Explore something new with ${pet.name}", "Let ${pet.name} investigate a safe object")
            Personality.FRIENDLY    -> listOf("Spend social time with ${pet.name}", "Introduce ${pet.name} to someone new")
            Personality.SHY         -> listOf("Spend quiet time with ${pet.name}", "Create a calm space for ${pet.name}")
            Personality.MISCHIEVOUS -> listOf("Give ${pet.name} an enrichment activity", "Redirect ${pet.name}'s energy positively")
        }

        (universalCore + personalityCore).forEach { title ->
            petRepository.insertTask(TaskEntity(petId = pet.id, title = title, type = TaskType.CORE))
        }

        listOf(
            "Brush ${pet.name}",
            "Give ${pet.name} a treat",
            "Take a photo of ${pet.name}",
            "Clean ${pet.name}'s area"
        ).forEach { title ->
            petRepository.insertTask(TaskEntity(petId = pet.id, title = title, type = TaskType.OPTIONAL))
        }
    }

    private suspend fun checkAndUnlockAchievements() {
        val pets         = allPets.value
        val achievements = allAchievements.value
        if (achievements.isEmpty()) return

        suspend fun unlock(title: String) {
            val a = achievements.find { it.title == title }
            if (a != null && !a.isUnlocked) petRepository.unlockAchievement(a.id)
        }

        val distinctTypes = pets.map { it.type }.toSet()
        val ownedRarities = distinctTypes.map { it.rarity }.toSet()

        if (pets.isNotEmpty())                       unlock("First Pet")
        if (pets.any { it.isVerified })              unlock("First Verification")
        if (pets.size >= 3)                          unlock("Pet Lover")
        if (pets.any { it.bondLevel >= 5 })          unlock("Bond Master")
        if (prefsRepository.userStreak.first() >= 7) unlock("7-Day Streak")

        if (pets.any { it.type.rarity == Rarity.EPIC }) unlock("Epic Tamer")
        if (Rarity.entries.all { it in ownedRarities }) unlock("Rarity Hunter")
        if (distinctTypes.size >= 5)                    unlock("Species Collector")
        if (distinctTypes.size >= 10)                   unlock("Animal Explorer")
    }

    // ─── Admin / Debug Functions ───────────────────────────────────────────

    fun adminAddBondPoints(petId: Int, points: Int) {
        viewModelScope.launch {
            try {
                val pet = allPets.value.find { it.id == petId } ?: return@launch
                val oldLevel  = pet.bondLevel
                val newPoints = pet.bondPoints + points
                val newLevel  = (newPoints / 100) + 1
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
                val tasks           = todaysTasks.value
                val incompleteTasks = tasks.filter { !it.isCompleted }
                incompleteTasks.forEach { task -> petRepository.completeTask(task.id) }
                val pets = allPets.value
                pets.forEach { pet ->
                    val totalPoints = incompleteTasks
                        .filter { it.petId == pet.id }
                        .sumOf { if (it.type == TaskType.CORE) 10 else 5 as Int }
                    if (totalPoints > 0) {
                        val oldLevel  = pet.bondLevel
                        val newPoints = pet.bondPoints + totalPoints
                        val newLevel  = (newPoints / 100) + 1
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
                allPets.value.forEach { generateTasksForPet(it) }
                prefsRepository.updateLastTaskDate(todayString())
            } catch (e: Exception) {
                Log.e("PetQuestVM", "adminResetTasks error", e)
            }
        }
    }

    fun adminUnlockAllAchievements() {
        viewModelScope.launch {
            try {
                allAchievements.value
                    .filter { !it.isUnlocked }
                    .forEach { petRepository.unlockAchievement(it.id) }
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
                allPets.value
                    .filter { !it.isVerified }
                    .forEach { pet -> petRepository.verifyPet(pet.id, "admin_verified") }
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
