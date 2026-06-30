// HOW TO APPLY: Open ProfileScreen.kt → Ctrl+A → Delete → Paste this entire file
// CHANGES from original:
//   1. Tab icons replaced — emoji text removed, PNG icons added (paw / trophy / event)
//      If build fails with "unresolved reference: paw/trophy/event", copy the matching
//      PNG files from docs/icons/ into app/src/main/res/drawable/ with those exact names.

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║  ⚠️  SYNC CONTRACT — READ BEFORE EDITING  ⚠️                                ║
// ║                                                                              ║
// ║  This file (Profile tab — owner's own view) must stay in sync with:         ║
// ║    • SharedProfileScreen.kt  → in-app public Trainer Profile view           ║
// ║    • docs/share.html         → WEBSITE Trainer Profile (GitHub Pages)       ║
// ║                                                                              ║
// ║  RULES — if you change one, update ALL THREE:                               ║
// ║   1. Only VERIFIED pets shown: verifiedPets = pets.filter { it.isVerified } ║
// ║   2. Pet count header and stats use verifiedPets.size, NOT pets.size        ║
// ║   3. Card FRONT: photo/avatar + rarity bar + name + rarity chip + Lv.N     ║
// ║         — NO tier star icon on the front card                               ║
// ║   4. Card BACK: virtue image + bond status text + virtue title (GUARDIAN/   ║
// ║         FRIEND/COMPANION) — NO LV.XX badge; aura/ring animations stay      ║
// ║   5. buildCurrentProfile() syncs to Firebase — MUST use verifiedPets only  ║
// ╚══════════════════════════════════════════════════════════════════════════════╝
package com.example.petquest.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.data.model.AchievementEntity
import com.example.petquest.data.model.PetEntity
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.Rarity
import com.example.petquest.data.model.Virtue
import com.example.petquest.data.repository.SupabaseRepository
import com.example.petquest.data.repository.PetSummary
import com.example.petquest.data.repository.PublicProfile
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel
import com.example.petquest.worker.ReminderWorker
import java.util.Locale
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Achievement categories (mirrors SharedProfileScreen — keep in sync)
// ─────────────────────────────────────────────────────────────────────────────

private data class PsAchievCategoryDef(
    val label  : String,
    val icon   : ImageVector,
    val color  : Color,
    val titles : List<String>
)

val PROFILE_BANNERS: List<Int> = listOf(
    R.drawable.profile_banner1,
    R.drawable.profile_banner2,
    R.drawable.profile_banner3,
    R.drawable.profile_banner4,
    R.drawable.profile_banner5
    // Add more here when ready: R.drawable.profile_banner6 … R.drawable.profile_banner10
)

private val PS_ACHIEV_CATEGORIES = listOf(
    PsAchievCategoryDef(
        label  = "First Steps",
        icon   = Icons.Default.Pets,
        color  = Color(0xFF2E7D32),
        titles = listOf(
            "First Pet", "First Verification", "Pet Lover",
            "Own 5 Pets", "Own 10 Pets",
            "Verify 3 Pets", "Verify 5 Pets", "Verify 10 Pets"
        )
    ),
    PsAchievCategoryDef(
        label  = "Streak",
        icon   = Icons.Default.Whatshot,
        color  = Color(0xFFE65100),
        titles = listOf(
            "3-Day Streak", "7-Day Streak", "14-Day Streak",
            "30-Day Streak", "60-Day Streak", "100-Day Streak"
        )
    ),
    PsAchievCategoryDef(
        label  = "Tasks",
        icon   = Icons.Default.CheckCircle,
        color  = Color(0xFF1565C0),
        titles = listOf(
            "Complete 10 Tasks", "Complete 25 Tasks", "Complete 50 Tasks",
            "Complete 100 Tasks", "Complete 250 Tasks", "Complete 500 Tasks"
        )
    ),
    PsAchievCategoryDef(
        label  = "Bond Points",
        icon   = Icons.Default.Star,
        color  = Color(0xFFF9A825),
        titles = listOf(
            "Earn 100 Bond Points", "Earn 250 Bond Points", "Earn 500 Bond Points",
            "Earn 1000 Bond Points", "Earn 2500 Bond Points", "Earn 5000 Bond Points"
        )
    ),
    PsAchievCategoryDef(
        label  = "Pet Bond Level",
        icon   = Icons.Default.EmojiEvents,
        color  = Color(0xFF6A1B9A),
        titles = listOf(
            "Bond Master", "Bond Veteran", "Level 10 Companion",
            "Virtue Master", "Dedicated Caregiver", "Elite Trainer"
        )
    ),
    PsAchievCategoryDef(
        label  = "Trainer Level",
        icon   = Icons.Default.Stars,
        color  = Color(0xFFBF360C),
        titles = listOf(
            "Reach Level 10", "Reach Level 20", "Reach Level 30", "Reach Level 50"
        )
    ),
    PsAchievCategoryDef(
        label  = "Species & Rarity",
        icon   = Icons.Default.Explore,
        color  = Color(0xFF00695C),
        titles = listOf(
            "Species Collector", "Animal Explorer", "Master Explorer",
            "Rarity Hunter", "Epic Tamer"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel              : PetQuestViewModel,
    supabaseRepository     : SupabaseRepository,
    onAddPetClick          : () -> Unit,
    onAdminClick           : () -> Unit,
    onNavigateToCollection : () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    val pets                 by viewModel.allPets.collectAsState()
    val verifiedPets          = pets.filter { it.isVerified }
    val userLevel            by viewModel.userLevel.collectAsState()
    val totalBondPoints      by viewModel.totalBondPoints.collectAsState()
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()
    val allAchievements      by viewModel.allAchievements.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationHour     by viewModel.notificationHour.collectAsState()
    val notificationMinute   by viewModel.notificationMinute.collectAsState()
    val userStreak           by viewModel.userStreak.collectAsState()
    val profileBannerIndex   by viewModel.profileBannerIndex.collectAsState()

    val speciesCount = collectedSpecies.size
    val totalSpecies = PetType.entries.size
    val xpInLevel    = totalBondPoints % 100

    val xpProgress by animateFloatAsState(
        targetValue   = xpInLevel / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "xp_progress"
    )

    val speciesProgress by animateFloatAsState(
        targetValue   = if (totalSpecies == 0) 0f else speciesCount / totalSpecies.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label         = "species_progress"
    )

    val eventBadges: List<AchievementEntity> = remember(allAchievements) {
        allAchievements.filter { it.isUnlocked && it.title in EVENT_BADGE_TITLES }
    }

    val regularBadgeTitles: Set<String> = remember(allAchievements) {
        allAchievements.filter { it.isUnlocked && it.title !in EVENT_BADGE_TITLES }
            .map { it.title }.toSet()
    }

    val groupedAchievs: List<Pair<PsAchievCategoryDef, List<String>>> = remember(regularBadgeTitles) {
        PS_ACHIEV_CATEGORIES.mapNotNull { cat ->
            val earned = cat.titles.filter { it in regularBadgeTitles }
            if (earned.isEmpty()) null else cat to earned
        }
    }

    val scope            = rememberCoroutineScope()
    var isSharingProfile by remember { mutableStateOf(false) }
    var isSyncing        by remember { mutableStateOf(false) }
    var showBannerPicker by remember { mutableStateOf(false) }

    // ── Tab state ─────────────────────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }

    fun buildCurrentProfile() = PublicProfile(
        trainerName         = if (pets.isNotEmpty()) pets.first().name else "PetQuest Trainer",
        level               = userLevel,
        streak              = userStreak,
        bondPoints          = totalBondPoints,
        petCount            = verifiedPets.size,
        speciesCount        = speciesCount,
        pets                = verifiedPets.take(6).map { pet ->
            PetSummary(
                name       = pet.name,
                species    = pet.type.name,
                rarity     = pet.type.rarity.name,
                bondLevel  = pet.bondLevel,
                isVerified = pet.isVerified,
                photoUri   = pet.photoUri?.toString(),
                virtue     = pet.virtue.name
            )
        },
        unlockedBadgeTitles = allAchievements.filter { it.isUnlocked }.map { it.title }
    )

    // ── Background sync: runs in viewModelScope so navigation can't cancel it ──
    LaunchedEffect(pets.size, allAchievements.count { it.isUnlocked }) {
        kotlinx.coroutines.delay(800)
        if (pets.isNotEmpty() || allAchievements.any { it.isUnlocked }) {
            viewModel.syncProfileToFirebase()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (pets.isNotEmpty() || allAchievements.any { it.isUnlocked }) {
                    viewModel.syncProfileToFirebase()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Hidden admin sequence state (silent, zero visual feedback) ──────────────
    var hiddenStep     by remember { mutableIntStateOf(0) }
    var hiddenTapA     by remember { mutableIntStateOf(0) }
    var hiddenTapB     by remember { mutableIntStateOf(0) }
    var hiddenStepMs   by remember { mutableLongStateOf(0L) }

    fun onBondCardTap() {
        val now = System.currentTimeMillis()
        if (hiddenStep == 1 && now - hiddenStepMs > 12000L) {
            hiddenStep = 0; hiddenTapA = 0; hiddenTapB = 0
        }
        if (hiddenStep == 0) {
            hiddenTapA++
            if (hiddenTapA == 1) hiddenStepMs = System.currentTimeMillis()
            if (hiddenTapA >= 3) {
                hiddenStep = 1; hiddenTapA = 0
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    fun onActivePetsCardTap() {
        val now = System.currentTimeMillis()
        if (hiddenStep != 1) return
        if (now - hiddenStepMs > 12000L) {
            hiddenStep = 0; hiddenTapA = 0; hiddenTapB = 0; return
        }
        hiddenTapB++
        if (hiddenTapB >= 3) {
            hiddenStep = 0; hiddenTapA = 0; hiddenTapB = 0
            onAdminClick()
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
            ReminderWorker.schedule(context, notificationHour, notificationMinute)
        }
    }

    fun toggleNotifications(enable: Boolean) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.setNotificationsEnabled(true)
                    ReminderWorker.schedule(context, notificationHour, notificationMinute)
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                viewModel.setNotificationsEnabled(true)
                ReminderWorker.schedule(context, notificationHour, notificationMinute)
            }
        } else {
            viewModel.setNotificationsEnabled(false)
            ReminderWorker.cancel(context)
        }
    }

    // ── Share profile ─────────────────────────────────────────────────────────
    val shareProfile: () -> Unit = {
        if (!isSharingProfile) {
            scope.launch {
                isSharingProfile = true
                try {
                    val profile = buildCurrentProfile()

                    var uid: String? = null
                    try { uid = supabaseRepository.ensureSignedIn() } catch (_: Exception) {}

                    viewModel.syncProfileToFirebase()

                    val shareLink = if (uid != null) {
                        "https://jemjemfromyt.github.io/PetQuest/share.html?uid=$uid"
                    } else {
                        "https://jemjemfromyt.github.io/PetQuest/share.html"
                    }

                    val petWord = if (profile.petCount != 1) "pets" else "pet"
                    val shareText = buildString {
                        appendLine("🐾 Check out my PetQuest profile!")
                        append("Level ${profile.level} Trainer")
                        if (profile.streak > 0) append(" • 🔥 ${profile.streak} day streak")
                        appendLine()
                        append("💚 ${profile.bondPoints} Bond Points")
                        append(" • 🐾 ${profile.petCount} $petWord")
                        appendLine()
                        appendLine()
                        append(shareLink)
                    }

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_TITLE, "${profile.trainerName}'s PetQuest Profile")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share your PetQuest profile"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Couldn't share — please try again.", Toast.LENGTH_SHORT).show()
                } finally {
                    isSharingProfile = false
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = notificationHour,
            initialMinute = notificationMinute,
            is24Hour      = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder Time") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    viewModel.setReminderTime(h, m)
                    if (notificationsEnabled) ReminderWorker.schedule(context, h, m)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF8C42), Color(0xFFFFB77A))
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Profile",
                        fontSize      = 24.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Row {
                        if (isSharingProfile) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(24.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = shareProfile) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share Profile",
                                    tint               = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Sync banner ────────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = isSyncing,
                    enter   = fadeIn(tween(300)),
                    exit    = fadeOut(tween(600))
                ) {
                    SyncBanner()
                }
            }

            // ── Level card ─────────────────────────────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape     = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF8C42), Color(0xFFFFCFA8))
                                )
                            )
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { showBannerPicker = true },
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                val bannerRes = PROFILE_BANNERS.getOrElse(profileBannerIndex - 1) { PROFILE_BANNERS[0] }
                                Image(
                                    painter            = painterResource(bannerRes),
                                    contentDescription = "Profile banner",
                                    modifier           = Modifier.fillMaxSize().padding(4.dp),
                                    contentScale       = ContentScale.Fit
                                )
                                // Pencil badge at bottom-right corner
                                Surface(
                                    modifier        = Modifier.padding(4.dp),
                                    shape           = CircleShape,
                                    color           = Color(0xFFFF6D00),
                                    shadowElevation = 2.dp
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Change banner",
                                        tint     = Color.White,
                                        modifier = Modifier.size(20.dp).padding(4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Level $userLevel",
                                        fontSize   = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = Color.White
                                    )
                                    val title = trainerTitle(userLevel)
                                    if (title.isNotEmpty()) {
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = Color.White.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                title,
                                                modifier = Modifier.padding(
                                                    horizontal = 7.dp, vertical = 3.dp
                                                ),
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "XP to next level",
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = Color.White.copy(alpha = 0.80f)
                                    )
                                    Text(
                                        "$xpInLevel / 100",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = Color.White
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(xpProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Bond Pts + Active Pets (admin tap targets preserved) ───────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier  = Modifier.weight(1f).clickable { onBondCardTap() },
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF0D2E1A) else Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$totalBondPoints",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF1B5E20)
                            )
                            Text(
                                "Total Bond Pts",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = if (isSystemInDarkTheme()) Color(0xFF66BB6A) else Color(0xFF2E7D32)
                            )
                        }
                    }
                    Card(
                        modifier  = Modifier.weight(1f).clickable { onActivePetsCardTap() },
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF2A1040) else Color(0xFFF3E5F5))
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${verifiedPets.size}",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = if (isSystemInDarkTheme()) Color(0xFFCE93D8) else Color(0xFF4A148C)
                            )
                            Text(
                                "Active Pets",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = if (isSystemInDarkTheme()) Color(0xFFBA68C8) else Color(0xFF6A1B9A)
                            )
                        }
                    }
                }
            }

            // ── Streak banner (always visible when streak > 0) ─────────────────
            if (userStreak > 0) {
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF3D1F00) else Color(0xFFFBE9E7))
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Whatshot, null,
                                tint     = if (isSystemInDarkTheme()) Color(0xFFFF8A65) else Color(0xFFE64A19),
                                modifier = Modifier.size(28.dp))
                            Column {
                                Text("$userStreak Day Streak",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = if (isSystemInDarkTheme()) Color(0xFFFF7043) else Color(0xFFBF360C))
                                Text("Consistent trainer — logging in every day!",
                                    fontSize = 11.sp,
                                    color    = if (isSystemInDarkTheme()) Color(0xFFFF8A65).copy(alpha = 0.85f) else Color(0xFFE64A19).copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            }

            // ── Species collected ──────────────────────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().clickable { onNavigateToCollection() },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Species Collected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 13.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                ) {
                                    Text(
                                        "$speciesCount / $totalSpecies",
                                        modifier   = Modifier.padding(
                                            horizontal = 10.dp, vertical = 3.dp
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 12.sp,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(speciesProgress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF1A6B44), Color(0xFF70C99A))
                                            )
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector        = Icons.Default.ChevronRight,
                            contentDescription = "View collection",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Tab row — PNG icons instead of emoji ───────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick  = { selectedTab = 0 },
                        icon = {
                            Image(
                                painter            = painterResource(R.drawable.paw),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp),
                                contentScale       = ContentScale.Fit
                            )
                        },
                        text = { Text("My Pets", fontSize = 11.sp, maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick  = { selectedTab = 1 },
                        icon = {
                            Image(
                                painter            = painterResource(R.drawable.trophy),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp),
                                contentScale       = ContentScale.Fit
                            )
                        },
                        text = { Text("Achievements", fontSize = 11.sp, maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick  = { selectedTab = 2 },
                        icon = {
                            Image(
                                painter            = painterResource(R.drawable.event),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp),
                                contentScale       = ContentScale.Fit
                            )
                        },
                        text = { Text("Events", fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }

            // ── Tab content ────────────────────────────────────────────────────
            item {
                when (selectedTab) {

                    // ── My Pets ─────────────────────────────────────────────
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "My Pets (${verifiedPets.size})",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalIconButton(
                                    onClick  = onAddPetClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Pet",
                                        modifier           = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (verifiedPets.isEmpty()) {
                                EmptyStateCard(
                                    imageRes    = R.drawable.empty_collection,
                                    title       = "No Pets Yet",
                                    description = "Tap + to add your first pet.",
                                    actionLabel = "Add a Pet",
                                    onAction    = onAddPetClick
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    verifiedPets.chunked(2).forEach { rowPets ->
                                        Row(
                                            modifier              = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowPets.forEach { pet ->
                                                PetCollectionCard(
                                                    pet      = pet,
                                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                                )
                                            }
                                            if (rowPets.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Achievements ────────────────────────────────────────
                    1 -> {
                        PsAchievementsSection(groupedAchievs)
                    }

                    // ── Events ──────────────────────────────────────────────
                    2 -> {
                        EventBadgesSection(eventBadges = eventBadges)
                    }
                }
            }

            // ── Settings card (always below tabs) ─────────────────────────────
            item {
                SettingsCard(
                    notificationsEnabled = notificationsEnabled,
                    notificationHour     = notificationHour,
                    notificationMinute   = notificationMinute,
                    onToggle             = { toggleNotifications(it) },
                    onChangeTime         = { showTimePicker = true }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Achievements section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PsAchievementsSection(grouped: List<Pair<PsAchievCategoryDef, List<String>>>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (grouped.isNotEmpty()) (if (isSystemInDarkTheme()) Color(0xFF2C2200) else Color(0xFFFFF8E1))
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint     = if (grouped.isNotEmpty()) Color(0xFFF9A825)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Achievements",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (grouped.isNotEmpty()) (if (isSystemInDarkTheme()) Color(0xFFFFCC80) else Color(0xFF5D4037))
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (grouped.isNotEmpty()) {
                    val totalCount = grouped.sumOf { it.second.size }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF9A825).copy(alpha = 0.20f)
                    ) {
                        Text(
                            "$totalCount unlocked",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFF9A825)
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                Text(
                    "No achievements unlocked yet.\nComplete tasks and care for your pets to earn badges!",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            } else {
                grouped.forEachIndexed { index, (category, titles) ->
                    PsAchievCategoryRow(category, titles)
                    if (index < grouped.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PsAchievCategoryRow(category: PsAchievCategoryDef, titles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(14.dp))
            Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = category.color)
        }
        titles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { title ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        color    = category.color.copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(14.dp))
                            Text(
                                title,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = category.color,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                                lineHeight = 13.sp,
                                modifier   = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sync Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SyncBanner() {
    val transition = rememberInfiniteTransition(label = "syncPulse")
    val bannerAlpha by transition.animateFloat(
        initialValue  = 0.60f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerAlpha"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = bannerAlpha },
        shape = RoundedCornerShape(12.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF3D1F00) else Color(0xFFFFF3E0)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color       = if (isSystemInDarkTheme()) Color(0xFFFF8A65) else Color(0xFFE65100)
            )
            Text(
                "Syncing photos to share link...",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (isSystemInDarkTheme()) Color(0xFFFF7043) else Color(0xFFBF360C)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Event Badges Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EventBadgesSection(eventBadges: List<AchievementEntity>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (eventBadges.isNotEmpty()) (if (isSystemInDarkTheme()) Color(0xFF2C2200) else Color(0xFFFFFDE7))
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint               = if (eventBadges.isNotEmpty()) Color(0xFFF9A825)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    "Event Badges",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (eventBadges.isNotEmpty()) (if (isSystemInDarkTheme()) Color(0xFFFFCC80) else Color(0xFF5D4037))
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (eventBadges.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF9A825).copy(alpha = 0.20f)
                    ) {
                        Text(
                            "${eventBadges.size}",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFF9A825)
                        )
                    }
                }
            }

            if (eventBadges.isEmpty()) {
                Text(
                    "Complete seasonal event challenges to earn exclusive badges that display here.",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            } else {
                eventBadges.chunked(2).forEach { rowBadges ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            val matchingEvent = ALL_SEASONAL_EVENTS.find { it.badgeTitle == badge.title }
                            EventBadgeChip(
                                badge       = badge,
                                eventEmoji  = matchingEvent?.badgeEmoji ?: "🏅",
                                accentColor = matchingEvent?.accentColor ?: Color(0xFFF9A825),
                                modifier    = Modifier.weight(1f)
                            )
                        }
                        if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventBadgeChip(
    badge       : AchievementEntity,
    eventEmoji  : String,
    accentColor : Color,
    modifier    : Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(12.dp), color = accentColor.copy(alpha = 0.12f), modifier = modifier) {
        Row(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(eventEmoji, fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    badge.title,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = accentColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text("Seasonal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    notificationsEnabled : Boolean,
    notificationHour     : Int,
    notificationMinute   : Int,
    onToggle             : (Boolean) -> Unit,
    onChangeTime         : () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (notificationsEnabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector        = if (notificationsEnabled) Icons.Default.Notifications
                            else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint               = if (notificationsEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column {
                        Text("Daily Reminders", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            if (notificationsEnabled) "Enabled" else "Disabled",
                            fontSize = 11.sp,
                            color    = if (notificationsEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = notificationsEnabled, onCheckedChange = onToggle)
            }

            if (notificationsEnabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Reminder Time", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            formatTime(notificationHour, notificationMinute),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(onClick = onChangeTime) { Text("Change") }
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h12  = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
}

// ─────────────────────────────────────────────────────────────────────────────
// Flippable Pet Collection Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PetCollectionCard(pet: PetEntity, modifier: Modifier = Modifier) {
    var flipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue   = if (flipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "flip_${pet.id}"
    )

    val rarityColor = when (pet.type.rarity) {
        Rarity.COMMON   -> Color(0xFF9E9E9E)
        Rarity.UNCOMMON -> Color(0xFF43A047)
        Rarity.RARE     -> Color(0xFF1E88E5)
        Rarity.EPIC     -> Color(0xFF8E24AA)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY      = rotation
                cameraDistance = 14f * density
            }
            .clickable { flipped = !flipped }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                rotationY = 180f
                alpha     = if (rotation > 90f) 1f else 0f
            }
        ) {
            PetCardBack(pet = pet, rarityColor = rarityColor)
        }

        Box(
            modifier = Modifier.graphicsLayer {
                alpha = if (rotation <= 90f) 1f else 0f
            }
        ) {
            PetCardFront(pet = pet, rarityColor = rarityColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Front face
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PetCardFront(pet: PetEntity, rarityColor: Color) {

    val tier = when {
        pet.bondLevel >= 50 -> 5
        pet.bondLevel >= 30 -> 4
        pet.bondLevel >= 20 -> 3
        pet.bondLevel >= 10 -> 2
        pet.bondLevel >= 5  -> 1
        else                -> 0
    }

    val borderColors: List<Color> = when (tier) {
        1 -> listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFB0BEC5))
        2 -> listOf(Color(0xFF1565C0), Color(0xFF64B5F6), Color(0xFF1565C0))
        3 -> listOf(Color(0xFFFFD700), Color(0xFFFFF8DC), Color(0xFFFFB300), Color(0xFFFFD700))
        4 -> listOf(Color(0xFFE040FB), Color(0xFFFFD700), Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFE040FB))
        5 -> listOf(Color(0xFFFF1744), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF1744))
        else -> emptyList()
    }

    val borderWidth = when (tier) {
        0 -> 0.dp; 1 -> 1.5.dp; 2 -> 2.dp; 3 -> 2.5.dp; 4 -> 3.dp; else -> 3.5.dp
    }

    val lvColor: Color = when (tier) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> Color(0xFFB0BEC5)
        2 -> Color(0xFF42A5F5)
        3 -> Color(0xFFFFD700)
        4 -> Color(0xFFE040FB)
        else -> Color(0xFFFF6D00)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${pet.id}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = if (tier >= 3) 0.65f else 1f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (tier >= 4) 800 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pa"
    )

    val borderBrush: Brush? = when {
        tier == 0 -> null
        tier >= 3  -> Brush.linearGradient(borderColors.map { it.copy(alpha = pulseAlpha) })
        else       -> Brush.linearGradient(borderColors)
    }

    val stripeColors = if (borderColors.isNotEmpty())
        listOf(borderColors.first(), rarityColor, borderColors.last())
    else
        listOf(rarityColor, rarityColor.copy(alpha = 0.45f))

    Card(
        elevation = CardDefaults.cardElevation((4 + tier).dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((5 + tier).dp)
                    .background(Brush.horizontalGradient(stripeColors))
            )

            Column(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(
                            if (borderBrush != null)
                                Modifier.border(borderWidth, borderBrush, MaterialTheme.shapes.small)
                            else Modifier
                        )
                        .clip(MaterialTheme.shapes.small),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (pet.photoUri != null) {
                        AsyncImage(
                            model              = pet.photoUri,
                            contentDescription = "${pet.name} photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier         = Modifier
                                .fillMaxSize()
                                .background(rarityColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(petEmoji(pet.type.name), fontSize = 40.sp)
                        }
                    }

                    Column(
                        modifier            = Modifier.padding(3.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (pet.isVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Text(
                    pet.name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.fillMaxWidth()
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = rarityColor.copy(alpha = 0.13f)
                    ) {
                        Text(
                            pet.type.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = rarityColor
                        )
                    }
                    Text(
                        "Lv.${pet.bondLevel}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = lvColor
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Back face
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PetCardBack(pet: PetEntity, rarityColor: Color) {
    val virtueInfo = VirtueConfig[pet.virtue]

    val (gradStart, gradMid, gradEnd) = when (pet.virtue) {
        Virtue.COURAGE    -> Triple(Color(0xFF7B0000), Color(0xFFBF360C), Color(0xFFE53935))
        Virtue.WISDOM     -> Triple(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF5C6BC0))
        Virtue.DILIGENCE  -> Triple(Color(0xFF4E2800), Color(0xFF8D4B00), Color(0xFFEF6C00))
        Virtue.TEMPERANCE -> Triple(Color(0xFF004D40), Color(0xFF00695C), Color(0xFF26A69A))
        Virtue.COMPASSION -> Triple(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFEC407A))
    }

    val tier = when {
        pet.bondLevel >= 50 -> 5
        pet.bondLevel >= 30 -> 4
        pet.bondLevel >= 20 -> 3
        pet.bondLevel >= 10 -> 2
        pet.bondLevel >= 5  -> 1
        else                -> 0
    }

    val haloColor = when (tier) {
        0, 1 -> Color.White
        2    -> Color(0xFF90CAF9)
        3    -> Color(0xFFFFE082)
        4    -> Color(0xFFCE93D8)
        else -> Color(0xFFFF8A65)
    }

    val emblemAlpha = when {
        pet.bondLevel >= 50 -> 0.55f
        pet.bondLevel >= 30 -> 0.42f
        pet.bondLevel >= 20 -> 0.32f
        pet.bondLevel >= 10 -> 0.22f
        pet.bondLevel >= 5  -> 0.17f
        else                -> 0.13f
    }

    val emblemSize = when {
        pet.bondLevel >= 50 -> 160.dp
        pet.bondLevel >= 30 -> 150.dp
        pet.bondLevel >= 20 -> 145.dp
        else                -> 140.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "backpulse_${pet.id}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = if (tier >= 2) 0.4f else 1f,
        targetValue   = if (tier >= 2) 0.9f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bp"
    )

    val cornerDotAlpha = if (tier >= 3) pulseAlpha * 0.7f else 0f

    Card(
        elevation = CardDefaults.cardElevation((4 + tier).dp),
        modifier  = Modifier.fillMaxWidth().fillMaxHeight(),
        shape     = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(gradStart, gradMid, gradEnd)))
        ) {

            if (tier >= 2) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 32.dp)
                        .align(Alignment.Center)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    haloColor.copy(alpha = pulseAlpha * 0.18f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            if (tier >= 4) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 60.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 1.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    haloColor.copy(alpha = pulseAlpha * 0.5f),
                                    Color.Transparent,
                                    haloColor.copy(alpha = pulseAlpha * 0.5f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            if (tier >= 3) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 10.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    haloColor.copy(alpha = pulseAlpha * 0.7f),
                                    Color.Transparent,
                                    haloColor.copy(alpha = pulseAlpha * 0.7f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            Image(
                painter            = painterResource(virtueInfo.emblemRes),
                contentDescription = null,
                modifier           = Modifier
                    .size(emblemSize)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = emblemAlpha },
                contentScale       = ContentScale.Fit
            )

            if (tier >= 3) {
                val dotChar = if (tier >= 5) "✦" else if (tier >= 4) "✦" else "·"
                val dotSize = if (tier >= 4) 13.sp else 11.sp
                Text(dotChar, Modifier.align(Alignment.TopEnd).padding(10.dp),
                    color = haloColor.copy(alpha = cornerDotAlpha), fontSize = dotSize)
                Text(dotChar, Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = haloColor.copy(alpha = cornerDotAlpha), fontSize = dotSize)
                if (tier >= 4) {
                    Text(dotChar, Modifier.align(Alignment.TopStart).padding(10.dp),
                        color = haloColor.copy(alpha = cornerDotAlpha * 0.6f), fontSize = dotSize)
                    Text(dotChar, Modifier.align(Alignment.BottomEnd).padding(10.dp),
                        color = haloColor.copy(alpha = cornerDotAlpha * 0.6f), fontSize = dotSize)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(7.dp)
            ) {
                Image(
                    painter            = painterResource(virtueInfo.emblemRes),
                    contentDescription = pet.virtue.name,
                    modifier           = Modifier.size(16.dp)
                )
            }

            val earnedTitle: String = when {
                pet.bondLevel >= 50 -> "Eternal Companion"
                pet.bondLevel >= 40 -> "Soul Bonded"
                pet.bondLevel >= 30 -> "Elite Protector"
                pet.bondLevel >= 25 -> "Bonded Guardian"
                pet.bondLevel >= 20 -> "Master Companion"
                pet.bondLevel >= 15 -> "Lifelong Partner"
                pet.bondLevel >= 10 -> "Loyal Friend"
                pet.bondLevel >= 5  -> "Trusted Companion"
                else                -> "New Companion"
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))
                    )
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val words = earnedTitle.split(" ")
                if (words.size >= 2) {
                    Text(
                        text          = words.first(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Light,
                        color         = Color.White.copy(alpha = 0.70f),
                        textAlign     = TextAlign.Center,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text          = words.drop(1).joinToString(" ").uppercase(),
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        textAlign     = TextAlign.Center,
                        letterSpacing = (-0.5).sp,
                        lineHeight    = 21.sp,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text          = earnedTitle.uppercase(),
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        textAlign     = TextAlign.Center,
                        letterSpacing = (-0.5).sp,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner Picker Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BannerPickerSheet(
    currentIndex : Int,
    onSelect     : (Int) -> Unit,
    onDismiss    : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "Choose Profile Banner",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier   = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    items(PROFILE_BANNERS.size) { i ->
                        val idx      = i + 1
                        val selected = idx == currentIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.6f)
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color(0xFFFF6D00) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable { onSelect(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter            = painterResource(PROFILE_BANNERS[i]),
                                contentDescription = "Banner $idx",
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Fit
                            )
                            if (selected) {
                                Box(
                                    modifier         = Modifier.fillMaxSize().background(Color(0xFFFF6D00).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint     = Color.White,
                                        modifier = Modifier.size(20.dp).padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = Color(0xFFFF6D00))
                }
            }
        }
    }
}
