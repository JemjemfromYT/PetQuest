package com.example.petquest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.petquest.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.petquest.data.model.PetEntity
import com.example.petquest.data.model.PetType
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel
import com.example.petquest.worker.ReminderWorker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PetQuestViewModel,
    onAddPetClick: () -> Unit,
    onAdminClick: () -> Unit,
    onNavigateToCollection: () -> Unit = {}
) {
    val context = LocalContext.current

    val pets                 by viewModel.allPets.collectAsState()
    val userLevel            by viewModel.userLevel.collectAsState()
    val totalBondPoints      by viewModel.totalBondPoints.collectAsState()
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationHour     by viewModel.notificationHour.collectAsState()
    val notificationMinute   by viewModel.notificationMinute.collectAsState()

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

    var tapCount      by remember { mutableIntStateOf(0) }
    var lastTapMs     by remember { mutableLongStateOf(0L) }
    var showAdminHint by remember { mutableStateOf(false) }

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
        contract = ActivityResultContracts.RequestPermission()
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
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
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

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = notificationHour,
            initialMinute = notificationMinute,
            is24Hour      = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title            = { Text("Reminder Time") },
            text             = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
            TopAppBar(
                title   = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Admin",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
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
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile illustration
                        Image(
                            painter            = painterResource(id = R.drawable.profile_banner),
                            contentDescription = null,
                            modifier           = Modifier.size(72.dp),
                            contentScale       = ContentScale.Fit
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onLevelTap() }
                        ) {
                            // Level + title chip
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Level $userLevel",
                                    fontSize   = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.primary
                                )
                                val title = trainerTitle(userLevel)
                                if (title.isNotEmpty()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            title,
                                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (showAdminHint) {
                                Text(
                                    "Keep tapping...",
                                    fontSize = 10.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // XP progress
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "XP",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$xpInLevel / 100",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                            )
                        }
                    }
                }
            }

            // ── Trainer stats row — bond points + pets ────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total bond points
                    Card(
                        modifier = Modifier.weight(1f),
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$totalBondPoints",
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF1B5E20)
                            )
                            Text(
                                "Total Bond Pts",
                                fontSize = 10.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Pet count
                    Card(
                        modifier = Modifier.weight(1f),
                        colors   = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3E5F5)
                        )
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${pets.size}",
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF4A148C)
                            )
                            Text(
                                "Active Pets",
                                fontSize = 10.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Species collection card ───────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCollection() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "$speciesCount / $totalSpecies",
                                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 12.sp,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { speciesProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
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

            // ── My Pets heading + add button ──────────────────────────────────
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
                    val rows = pets.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowPets ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPets.forEach { pet ->
                                    PetCollectionCard(pet = pet, modifier = Modifier.weight(1f))
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

@Composable
private fun SettingsCard(
    notificationsEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
    onToggle: (Boolean) -> Unit,
    onChangeTime: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                            imageVector        = if (notificationsEnabled)
                                Icons.Default.Notifications
                            else
                                Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint               = if (notificationsEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "Daily Reminders",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                        Text(
                            if (notificationsEnabled) "Enabled" else "Disabled",
                            fontSize = 11.sp,
                            color    = if (notificationsEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
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
                            fontSize   = 15.sp,
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

@Composable
private fun PetCollectionCard(pet: PetEntity, modifier: Modifier = Modifier) {
    val rarityColor = when (pet.type.rarity.name) {
        "COMMON"   -> Color(0xFF9E9E9E)
        "UNCOMMON" -> MaterialTheme.colorScheme.secondary
        "RARE"     -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.error
    }
    val hasGoldBorder = pet.bondLevel >= 20

    Card(
        modifier  = modifier,
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rarity accent strip — thicker for more impact
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(rarityColor)
            )

            Column(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pet photo
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(
                            if (hasGoldBorder)
                                Modifier.border(
                                    width = 2.dp,
                                    color = Color(0xFFFFD700),
                                    shape = MaterialTheme.shapes.small
                                )
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
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.padding(3.dp).size(14.dp)
                        )
                    }
                }

                // Name
                Text(
                    pet.name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.fillMaxWidth()
                )

                // Rarity chip + level inline
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = rarityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            pet.type.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = rarityColor
                        )
                    }
                    Text(
                        "Lv.${pet.bondLevel}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
