// ============================================================
// FILE: app/src/main/java/com/example/petquest/viewmodel/PetQuestViewModel.kt
// CHANGES: Added FirebaseRepository + getCurrentUserUid() + syncProfileToFirebase()
// Everything else is UNCHANGED from your original file.
// ============================================================

package com.example.petquest.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.petquest.data.model.*
import com.example.petquest.data.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class LevelUpEvent(
    val petName: String,
    val oldLevel: Int,
    val newLevel: Int
)

class PetQuestViewModel(
    private val petRepository: PetRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    // ─── Firebase (for share link photo sync) ─────────────────────────────────
    private val firebaseRepository = FirebaseRepository()

    /** Returns the current user's Firebase UID, or null if not signed in. */
    fun getCurrentUserUid(): String? = firebaseRepository.getMyUid()

    /**
     * Builds the PublicProfile from current local data and pushes it to Firestore.
     * For each pet that has a local content:// photo URI, this will also upload
     * the photo to Firebase Storage so the share-link web page and
     * SharedProfileScreen can display the real photo instead of an emoji.
     *
     * Call this:
     *   - On Profile screen ON_RESUME (already wired in ProfileScreen.kt)
     *   - When the user taps the Share button (already wired in ProfileScreen.kt)
     */
    fun syncProfileToFirebase() {
        viewModelScope.launch {
            try {
                val uid        = firebaseRepository.ensureSignedIn()
                val pets       = allPets.value
                val total      = totalBondPoints.value
                val level      = userLevel.value
                val streak     = prefsRepository.userStreak.first()
                val specCount  = collectedSpecies.value.size
                val badgeTitles = allAchievements.value
                    .filter { it.isUnlocked }
                    .map { it.title }

                // Use the first pet's name as the trainer name (matches ProfileScreen logic)
                val trainerName = pets.firstOrNull()?.name ?: "Trainer"

                val petSummaries = pets.take(6).mapIndexed { index, pet ->
                    PetSummary(
                        name       = pet.name,
                        species    = pet.type.name.uppercase(),
                        rarity     = pet.type.rarity.name.uppercase(),
                        bondLevel  = pet.bondLevel,
                        isVerified = pet.isVerified,
                        photoUri   = pet.photoUri,   // FirebaseRepository will upload if content://
                        virtue     = pet.virtue.name.uppercase()
                    )
                }

                val profile = PublicProfile(
                    uid                 = uid,
                    trainerName         = trainerName,
                    level               = level,
                    streak              = streak,
                    bondPoints          = total,
                    petCount            = pets.size,
                    speciesCount        = specCount,
                    pets                = petSummaries,
                    unlockedBadgeTitles = badgeTitles
                )

                firebaseRepository.pushProfile(profile)
                Log.d("PetQuestVM", "syncProfileToFirebase: pushed profile for uid=$uid")
            } catch (e: Exception) {
                Log.e("PetQuestVM", "syncProfileToFirebase error", e)
            }
        }
    }

    // ─── StateFlows ────────────────────────────────────────────────────────────

    val allPets: StateFlow<List<PetEntity>> =
        petRepository.allPets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysTasks: StateFlow<List<TaskEntity>> =
        petRepository.getTodaysTasks(todayString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<AchievementEntity>> =
        petRepository.allAchievements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStreak: StateFlow<Int> =
        prefsRepository.userStreak.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val personalBestStreak: StateFlow<Int> =
        prefsRepository.personalBestStreak.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasOnboarded: StateFlow<Boolean?> =
        prefsRepository.hasOnboarded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasSeenOnboarding: StateFlow<Boolean> =
        prefsRepository.hasSeenOnboarding.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasSeenTutorial: StateFlow<Boolean> =
        prefsRepository.hasSeenTutorial
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val totalBondPoints: StateFlow<Int> = allPets.map { pets ->
        pets.sumOf { it.bondPoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userLevel: StateFlow<Int> = totalBondPoints.map { points ->
        (points / 100) + 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

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

    val notificationsEnabled: StateFlow<Boolean> =
        prefsRepository.notificationsEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationHour: StateFlow<Int> =
        prefsRepository.notificationHour
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    val notificationMinute: StateFlow<Int> =
        prefsRepository.notificationMinute
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalTasksCompleted: StateFlow<Int> =
        prefsRepository.totalTasksCompleted
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastEventClaimDate: StateFlow<String> =
        prefsRepository.lastEventClaimDate
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val todayEventBonusClaimed: StateFlow<Boolean> = lastEventClaimDate.map { date ->
        date == todayString()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _levelUpEvent = MutableSharedFlow<LevelUpEvent>()
    val levelUpEvent: SharedFlow<LevelUpEvent> = _levelUpEvent.asSharedFlow()

    private val _pendingVerificationPetId = MutableStateFlow<Int?>(null)
    val pendingVerificationPetId: StateFlow<Int?> = _pendingVerificationPetId.asStateFlow()

    fun clearPendingVerificationPetId() {
        _pendingVerificationPetId.value = null
    }

    init {
        viewModelScope.launch {
            try {
                checkDailyReset()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "daily reset error", e)
            }
        }
    }

    // ─── Date helpers ──────────────────────────────────────────────────────────

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun yesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    // ─── Daily reset ───────────────────────────────────────────────────────────

    private suspend fun checkDailyReset() {
        val today        = todayString()
        val lastTaskDate = prefsRepository.lastTaskDate.first()
        if (lastTaskDate != today) {
            petRepository.clearAllTasks()
            val pets = allPets.first { it.isNotEmpty() || petRepository.allPets.first().isEmpty() }
            pets.forEach { pet -> generateTasksForPet(pet) }
            prefsRepository.updateLastTaskDate(today)
        }
    }

    // ─── Public actions ────────────────────────────────────────────────────────

    fun addPet(pet: PetEntity) {
        viewModelScope.launch {
            try {
                petRepository.insertPet(pet)
                prefsRepository.setOnboarded()
                val pets     = allPets.first { list -> list.any { it.name == pet.name && it.type == pet.type } }
                val inserted = pets.maxByOrNull { it.id } ?: return@launch
                generateTasksForPet(inserted)
                checkAndUnlockAchievements()
                _pendingVerificationPetId.value = inserted.id
            } catch (e: Exception) {
                Log.e("PetQuestVM", "addPet error", e)
            }
        }
    }

    fun editPet(petId: Int, newName: String) {
        viewModelScope.launch {
            try {
                petRepository.updatePet(petId, newName.trim())
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
                val pet = allPets.value.find { it.id == task.petId } ?: return@launch
                if (!pet.isVerified) return@launch

                petRepository.completeTask(task.id)
                prefsRepository.incrementTasksCompleted()

                val points   = if (task.type == TaskType.CORE || task.type == TaskType.VIRTUE) 10 else 5
                val oldLevel  = pet.bondLevel
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

    fun markOnboardingSeen() {
        viewModelScope.launch {
            try {
                prefsRepository.markOnboardingSeen()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "markOnboardingSeen error", e)
            }
        }
    }

    fun markTutorialSeen() {
        viewModelScope.launch {
            try {
                prefsRepository.markTutorialSeen()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "markTutorialSeen error", e)
            }
        }
    }

    // ─── Event bonus claim ─────────────────────────────────────────────────────

    fun claimEventBonus(badgeTitle: String, badgeDescription: String) {
        viewModelScope.launch {
            try {
                allPets.value.filter { it.isVerified }.forEach { pet ->
                    val oldLevel  = pet.bondLevel
                    val newPoints = pet.bondPoints + 20
                    val newLevel  = (newPoints / 100) + 1
                    petRepository.addBondPoints(pet.id, 20, pet.bondPoints, pet.bondLevel)
                    if (newLevel > oldLevel) {
                        _levelUpEvent.emit(LevelUpEvent(pet.name, oldLevel, newLevel))
                    }
                }

                petRepository.insertAchievementIfAbsent(
                    AchievementEntity(title = badgeTitle, description = badgeDescription)
                )
                delay(200)
                val target = allAchievements.value.find { it.title == badgeTitle }
                if (target != null && !target.isUnlocked) {
                    petRepository.unlockAchievement(target.id)
                }

                prefsRepository.updateLastEventClaimDate(todayString())
                checkAndUnlockAchievements()
            } catch (e: Exception) {
                Log.e("PetQuestVM", "claimEventBonus error", e)
            }
        }
    }

    private suspend fun updateStreak() {
        val today   = todayString()
        val last    = prefsRepository.lastStreakDate.first()
        val current = prefsRepository.userStreak.first()
        if (last != today) {
            val yesterday = yesterdayString()
            val newStreak = if (last == yesterday) current + 1 else 1
            prefsRepository.updateStreak(newStreak)
            prefsRepository.updateLastStreakDate(today)
            val best = prefsRepository.personalBestStreak.first()
            if (newStreak > best) {
                prefsRepository.updatePersonalBestStreak(newStreak)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                prefsRepository.setNotificationsEnabled(enabled)
            } catch (e: Exception) {
                Log.e("PetQuestVM", "setNotificationsEnabled error", e)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                prefsRepository.setReminderTime(hour, minute)
            } catch (e: Exception) {
                Log.e("PetQuestVM", "setReminderTime error", e)
            }
        }
    }

    // ─── Task generation ───────────────────────────────────────────────────────

    private suspend fun generateTasksForPet(pet: PetEntity) {
        if (petRepository.hasTasksForPet(pet.id)) return

        val today = todayString()
        val seed  = (today.hashCode().toLong() * 31L) + pet.id.toLong()
        val rng   = Random(seed)

        val coreTasks = TaskPools.UNIVERSAL_CORE
            .map { it.replace("{name}", pet.name) }
            .shuffled(rng)
            .take(3)

        val virtueTask = TaskPools.virtueCorePool(pet.virtue)
            .map { it.replace("{name}", pet.name) }
            .shuffled(rng)
            .take(1)

        val optionalCount      = if (pet.bondLevel >= 5) 4 else 3
        val speciesOptionals   = TaskPools.speciesOptionalPool(pet.type)
            .map { it.replace("{name}", pet.name) }
        val universalOptionals = TaskPools.UNIVERSAL_OPTIONAL
            .map { it.replace("{name}", pet.name) }
        val optionalTasks = (speciesOptionals + universalOptionals)
            .shuffled(rng)
            .take(optionalCount)

        coreTasks.forEach { title ->
            petRepository.insertTask(
                TaskEntity(petId = pet.id, title = title, type = TaskType.CORE, date = today)
            )
        }
        virtueTask.forEach { title ->
            petRepository.insertTask(
                TaskEntity(petId = pet.id, title = title, type = TaskType.VIRTUE, date = today)
            )
        }
        optionalTasks.forEach { title ->
            petRepository.insertTask(
                TaskEntity(petId = pet.id, title = title, type = TaskType.OPTIONAL, date = today)
            )
        }
    }

    // ─── Achievement unlocking ─────────────────────────────────────────────────

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
        val verifiedCount = pets.count { it.isVerified }
        val streak        = prefsRepository.userStreak.first()
        val totalPoints   = totalBondPoints.value
        val totalTasks    = prefsRepository.totalTasksCompleted.first()
        val trainerLevel  = userLevel.value

        if (pets.isNotEmpty())                             unlock("First Pet")
        if (verifiedCount >= 1)                            unlock("First Verification")
        if (pets.size >= 3)                                unlock("Pet Lover")
        if (pets.size >= 5)                                unlock("Own 5 Pets")
        if (pets.size >= 10)                               unlock("Own 10 Pets")
        if (verifiedCount >= 3)                            unlock("Verify 3 Pets")
        if (verifiedCount >= 5)                            unlock("Verify 5 Pets")
        if (verifiedCount >= 10)                           unlock("Verify 10 Pets")

        if (streak >= 3)                                   unlock("3-Day Streak")
        if (streak >= 7)                                   unlock("7-Day Streak")
        if (streak >= 14)                                  unlock("14-Day Streak")
        if (streak >= 30)                                  unlock("30-Day Streak")
        if (streak >= 60)                                  unlock("60-Day Streak")
        if (streak >= 100)                                 unlock("100-Day Streak")

        if (totalTasks >= 10)                              unlock("Complete 10 Tasks")
        if (totalTasks >= 25)                              unlock("Complete 25 Tasks")
        if (totalTasks >= 50)                              unlock("Complete 50 Tasks")
        if (totalTasks >= 100)                             unlock("Complete 100 Tasks")
        if (totalTasks >= 250)                             unlock("Complete 250 Tasks")
        if (totalTasks >= 500)                             unlock("Complete 500 Tasks")

        if (totalPoints >= 100)                            unlock("Earn 100 Bond Points")
        if (totalPoints >= 250)                            unlock("Earn 250 Bond Points")
        if (totalPoints >= 500)                            unlock("Earn 500 Bond Points")
        if (totalPoints >= 1000)                           unlock("Earn 1000 Bond Points")
        if (totalPoints >= 2500)                           unlock("Earn 2500 Bond Points")
        if (totalPoints >= 5000)                           unlock("Earn 5000 Bond Points")

        if (pets.any { it.bondLevel >= 5 })                unlock("Bond Master")
        if (pets.any { it.bondLevel >= 10 })               unlock("Bond Veteran")
        if (pets.any { it.bondLevel >= 15 })               unlock("Level 10 Companion")
        if (pets.any { it.bondLevel >= 20 })               unlock("Virtue Master")
        if (pets.any { it.bondLevel >= 30 })               unlock("Dedicated Caregiver")
        if (pets.any { it.bondLevel >= 50 })               unlock("Elite Trainer")

        if (trainerLevel >= 10)                            unlock("Reach Level 10")
        if (trainerLevel >= 20)                            unlock("Reach Level 20")
        if (trainerLevel >= 30)                            unlock("Reach Level 30")
        if (trainerLevel >= 50)                            unlock("Reach Level 50")

        if (distinctTypes.size >= 5)                       unlock("Species Collector")
        if (distinctTypes.size >= 10)                      unlock("Animal Explorer")
        if (distinctTypes.size >= 15)                      unlock("Master Explorer")
        if (pets.any { it.type.rarity == Rarity.EPIC })    unlock("Epic Tamer")
        if (Rarity.entries.all { it in ownedRarities })    unlock("Rarity Hunter")
    }

    // ─── Admin / Debug ─────────────────────────────────────────────────────────

    fun adminAddBondPoints(petId: Int, points: Int) {
        viewModelScope.launch {
            try {
                val pet       = allPets.value.find { it.id == petId } ?: return@launch
                val oldLevel  = pet.bondLevel
                petRepository.addBondPoints(petId, points, pet.bondPoints, pet.bondLevel)
                val newPoints = pet.bondPoints + points
                val newLevel  = (newPoints / 100) + 1
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
                if (incompleteTasks.isNotEmpty()) {
                    prefsRepository.incrementTasksCompleted(incompleteTasks.size)
                }
                val pets = allPets.value
                pets.forEach { pet ->
                    val totalPoints = incompleteTasks
                        .filter { it.petId == pet.id }
                        .fold(0) { acc, task ->
                            acc + if (task.type == TaskType.CORE || task.type == TaskType.VIRTUE) 10 else 5
                        }
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
                val best = prefsRepository.personalBestStreak.first()
                if (streak > best) {
                    prefsRepository.updatePersonalBestStreak(streak)
                }
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
