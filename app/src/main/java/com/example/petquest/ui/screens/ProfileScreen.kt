package com.example.petquest.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.data.model.AchievementEntity
import com.example.petquest.data.model.PetEntity
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.Rarity
import com.example.petquest.data.model.Virtue
import com.example.petquest.data.repository.FirebaseRepository
import com.example.petquest.data.repository.PublicProfile
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel
import com.example.petquest.worker.ReminderWorker
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PetQuestViewModel,
    firebaseRepository: FirebaseRepository,
    onAddPetClick: () -> Unit,
    onAdminClick: () -> Unit,
    onNavigateToCollection: () -> Unit = {}
) {
    val context = LocalContext.current

    val pets                 by viewModel.allPets.collectAsState()
    val userLevel            by viewModel.userLevel.collectAsState()
    val totalBondPoints      by viewModel.totalBondPoints.collectAsState()
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()
    val allAchievements      by viewModel.allAchievements.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationHour     by viewModel.notificationHour.collectAsState()
    val notificationMinute   by viewModel.notificationMinute.collectAsState()
    val userStreak           by viewModel.userStreak.collectAsState()

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

    var tapCount      by remember { mutableIntStateOf(0) }
    var lastTapMs     by remember { mutableLongStateOf(0L) }
    var showAdminHint by remember { mutableStateOf(false) }

    val scope            = rememberCoroutineScope()
    var isSharingProfile by remember { mutableStateOf(false) }

    fun onLevelTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapMs > 3000L) tapCount = 0
        tapCount++
        lastTapMs = now
        if (tapCount >= 5) { tapCount = 0; onAdminClick() }
        else if (tapCount >= 3) showAdminHint = true
    }

    LaunchedEffect(showAdminHint) {
        if (showAdminHint) {
            kotlinx.coroutines.delay(1500)
            showAdminHint = false
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

    // ── FIXED: share now uses an https:// GitHub Pages link so it shows as
    // a tappable blue hyperlink in WhatsApp, Messages, Discord, etc.
    // The page (docs/share.html) auto-redirects to petquest://share?uid=...
    val shareProfile: () -> Unit = {
        scope.launch {
            isSharingProfile = true
            try {
                val profile = PublicProfile(
                    trainerName  = if (pets.isNotEmpty()) pets.first().name else "PetQuest Trainer",
                    level        = userLevel,
                    streak       = userStreak,
                    bondPoints   = totalBondPoints,
                    petCount     = pets.size,
                    speciesCount = speciesCount
                )
                firebaseRepository.pushProfile(profile)
                val uid = firebaseRepository.getMyUid() ?: return@launch

                // https:// so messaging apps render it as a tappable link
                val shareLink = "https://jemjemfromyt.github.io/PetQuest/share?uid=$uid"

                val shareText = buildString {
                    append("🐾 Check out my PetQuest profile!\n\n")
                    append("Trainer: ${profile.trainerName}\n")
                    append("Level: ${profile.level}  |  Streak: ${profile.streak} days\n")
                    append("Bond Points: ${profile.bondPoints}\n")
                    append("Pets: ${profile.petCount}  |  Species: ${profile.speciesCount} discovered\n\n")
                    append("Tap to view my live profile 👇\n")
                    append(shareLink)
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share your PetQuest profile"))
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't share — check your connection.", Toast.LENGTH_SHORT).show()
            } finally {
                isSharingProfile = false
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
            title   = { Text("Reminder Time") },
            text    = {
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
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        IconButton(onClick = onAdminClick) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = "Admin",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
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

            // ── Trainer level card ────────────────────────────────────────────
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
                            Card(
                                shape     = MaterialTheme.shapes.medium,
                                colors    = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.25f)
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Image(
                                    painter            = painterResource(R.drawable.profile_banner),
                                    contentDescription = null,
                                    modifier           = Modifier.size(72.dp).padding(4.dp),
                                    contentScale       = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f).clickable { onLevelTap() }) {
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
                                                modifier   = Modifier.padding(
                                                    horizontal = 7.dp, vertical = 3.dp
                                                ),
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = Color.White
                                            )
                                        }
                                    }
                                }

                                if (showAdminHint) {
                                    Text(
                                        "Keep tapping...",
                                        fontSize = 10.sp,
                                        color    = Color.White.copy(alpha = 0.70f)
                                    )
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

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier  = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$totalBondPoints",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF1B5E20)
                            )
                            Text(
                                "Total Bond Pts",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Card(
                        modifier  = Modifier.weight(1f),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${pets.size}",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF4A148C)
                            )
                            Text(
                                "Active Pets",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Color(0xFF6A1B9A)
                            )
                        }
                    }
                }
            }

            // ── Event Badges ──────────────────────────────────────────────────
            item { EventBadgesSection(eventBadges = eventBadges) }

            // ── Species collection card ───────────────────────────────────────
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

            // ── My Pets heading ───────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("My Pets (${pets.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(onClick = onAddPetClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Pet", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Pet grid ─────────────────────────────────────────────────────
            if (pets.isEmpty()) {
                item {
                    EmptyStateCard(
                        imageRes    = R.drawable.empty_collection,
                        title       = "No Pets Yet",
                        description = "Tap + to add your first pet.",
                        actionLabel = "Add a Pet",
                        onAction    = onAddPetClick
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pets.chunked(2).forEach { rowPets ->
                            Row(
                                modifier              = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPets.forEach { pet ->
                                    PetCollectionCard(pet = pet, modifier = Modifier.weight(1f).fillMaxHeight())
                                }
                                if (rowPets.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Settings ──────────────────────────────────────────────────────
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

// ──────────────────────────────────────────────────────────────────────────────
// Event Badges Section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EventBadgesSection(eventBadges: List<AchievementEntity>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (eventBadges.isNotEmpty()) Color(0xFFFFFDE7)
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
                    color      = if (eventBadges.isNotEmpty()) Color(0xFF5D4037)
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

// ──────────────────────────────────────────────────────────────────────────────
// Settings Card
// ──────────────────────────────────────────────────────────────────────────────

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

// ──────────────────────────────────────────────────────────────────────────────
// Flippable Pet Collection Card
// ──────────────────────────────────────────────────────────────────────────────

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

// ──────────────────────────────────────────────────────────────────────────────
// Front face
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PetCardFront(pet: PetEntity, rarityColor: Color) {
    val hasGoldBorder = pet.bondLevel >= 20

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
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
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(listOf(rarityColor, rarityColor.copy(alpha = 0.45f)))
                    )
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
                            if (hasGoldBorder)
                                Modifier.border(2.dp, Color(0xFFFFD700), MaterialTheme.shapes.small)
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

                    if (pet.isVerified) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.padding(3.dp).size(14.dp)
                        )
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
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Back face
// ──────────────────────────────────────────────────────────────────────────────

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

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier  = Modifier.fillMaxWidth().fillMaxHeight(),
        shape     = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(gradStart, gradMid, gradEnd))
                )
        ) {
            Image(
                painter            = painterResource(virtueInfo.emblemRes),
                contentDescription = null,
                modifier           = Modifier
                    .size(140.dp)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 0.13f },
                contentScale       = ContentScale.Fit
            )

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

            val words = earnedTitle.split(" ")
            Column(
                modifier            = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (words.size >= 2) {
                    Text(
                        text          = words.first(),
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Light,
                        color         = Color.White.copy(alpha = 0.75f),
                        textAlign     = TextAlign.Center,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text          = words.drop(1).joinToString(" ").uppercase(),
                        fontSize      = 21.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color.White,
                        textAlign     = TextAlign.Center,
                        letterSpacing = (-0.5).sp,
                        lineHeight    = 23.sp,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text          = earnedTitle.uppercase(),
                        fontSize      = 21.sp,
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
