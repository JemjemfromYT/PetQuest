package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.data.model.Virtue
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.LevelUpEvent
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: Int,
    viewModel: PetQuestViewModel,
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val pets by viewModel.allPets.collectAsState()
    val pet  = pets.find { it.id == petId }

    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var levelUpEvent     by remember { mutableStateOf<LevelUpEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.levelUpEvent.collect { event ->
            if (event.petName == pet?.name) levelUpEvent = event
        }
    }

    if (showEditDialog && pet != null) {
        EditPetDialog(
            currentName = pet.name,
            onDismiss   = { showEditDialog = false },
            onConfirm   = { newName ->
                viewModel.editPet(pet.id, newName)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && pet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete ${pet.name}?", fontWeight = FontWeight.Bold) },
            text             = {
                Text(
                    "This will permanently remove ${pet.name} and all their tasks. This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePet(pet.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    levelUpEvent?.let { event ->
        LevelUpDialog(event = event, onDismiss = { levelUpEvent = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Pet Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pet != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit pet")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete pet",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (pet == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val rarityColor = when (pet.type.rarity.name) {
            "COMMON"   -> MaterialTheme.colorScheme.tertiary
            "UNCOMMON" -> MaterialTheme.colorScheme.secondary
            "RARE"     -> MaterialTheme.colorScheme.primary
            else       -> MaterialTheme.colorScheme.error
        }

        var virtueVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(200)
            virtueVisible = true
        }
        val virtueAlpha by animateFloatAsState(
            targetValue   = if (virtueVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label         = "virtue_alpha"
        )

        val bondProgress by animateFloatAsState(
            targetValue   = (pet.bondPoints % 100) / 100f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            label         = "bond_progress"
        )

        // Bond title milestone
        val bondTitle: Pair<String, Int>? = when {
            pet.bondLevel >= 20 -> "Master Companion" to 20
            pet.bondLevel >= 15 -> "Lifelong Partner"  to 15
            pet.bondLevel >= 10 -> "Loyal Friend"      to 10
            pet.bondLevel >= 5  -> "Trusted Companion" to 5
            else                -> null
        }

        // Next reward milestone — used for progression visibility
        val nextReward: Pair<Int, String>? = when {
            pet.bondLevel < 5  -> 5  to "+1 Daily Task"
            pet.bondLevel < 10 -> 10 to "Achievement Tier Unlock"
            pet.bondLevel < 15 -> 15 to "Special Bond Badge"
            pet.bondLevel < 20 -> 20 to "Premium Profile Border"
            else               -> null
        }

        // Gold border for Level 20 (Premium Profile Border reward)
        val hasGoldBorder = pet.bondLevel >= 20

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Pet photo card — gold border at Level 20 ───────────────────────
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier  = Modifier
                        .size(200.dp)
                        .then(
                            if (hasGoldBorder)
                                Modifier.border(
                                    width = 4.dp,
                                    color = Color(0xFFFFD700),
                                    shape = MaterialTheme.shapes.medium
                                )
                            else Modifier
                        ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    if (pet.photoUri != null) {
                        AsyncImage(
                            model              = pet.photoUri,
                            contentDescription = "Pet photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(petEmoji(pet.type.name), fontSize = 56.sp)
                                Text(
                                    "No Photo Yet",
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Badge(containerColor = rarityColor) {
                Text(
                    " ${pet.type.rarity.name} ",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Level 15 Bond Badge
            if (pet.bondLevel >= 15) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.EmojiEvents,
                            contentDescription = "Bond Badge",
                            tint               = MaterialTheme.colorScheme.tertiary,
                            modifier           = Modifier.size(14.dp)
                        )
                        Text(
                            "Bond Badge",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progression: Level, Bond Points, Bond Points ───────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    Modifier.weight(1f),
                    "Lv.${pet.bondLevel}",
                    "Bond Level",
                    iconRes = R.drawable.ic_level
                )
                StatCard(
                    Modifier.weight(1f),
                    pet.type.name.replace("_", " "),
                    "Type"
                )
                StatCard(
                    Modifier.weight(1f),
                    "${pet.bondPoints}",
                    "Bond Pts",
                    iconRes = R.drawable.ic_bondpoints
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Bond progress bar ──────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { bondProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(
                "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Bond Title (unlocked milestone) ────────────────────────────────
            if (bondTitle != null) {
                Spacer(Modifier.height(10.dp))
                BondTitleCard(title = bondTitle.first, unlockedAt = bondTitle.second)
            }

            // ── Next Reward (progression motivation) ───────────────────────────
            if (nextReward != null) {
                Spacer(Modifier.height(10.dp))
                NextRewardCard(level = nextReward.first, reward = nextReward.second)
            }

            Spacer(Modifier.height(20.dp))

            // ── Virtue Identity Card ───────────────────────────────────────────
            Box(modifier = Modifier.alpha(virtueAlpha)) {
                VirtueIdentityCard(virtue = pet.virtue)
            }

            Spacer(Modifier.height(20.dp))

            // ── Verification Status Section ────────────────────────────────────
            if (pet.isVerified) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter            = painterResource(id = R.drawable.ic_verified),
                            contentDescription = "Verified",
                            modifier           = Modifier.size(56.dp),
                            contentScale       = ContentScale.Fit
                        )
                        Text(
                            "Pet Verified",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "${pet.name} is fully active — tasks, bond points, and streaks are all enabled.",
                            fontSize  = 13.sp,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Tasks", "Bond Pts", "Streaks").forEach { label ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        label,
                                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(petEmoji(pet.type.name), fontSize = 48.sp)
                        Text(
                            "Verification Required",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "${pet.name} is locked until verified. Take a photo to unlock all features.",
                            fontSize  = 13.sp,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onErrorContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)
                        )
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Complete tasks", "Earn bond points", "Increase streaks", "Unlock achievements")
                                .forEach { feature ->
                                    Text(
                                        "• $feature",
                                        fontSize = 13.sp,
                                        color    = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick  = onVerifyClick,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Verify My Pet",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Bond Title Milestone Card ─────────────────────────────────────────────────

@Composable
private fun BondTitleCard(title: String, unlockedAt: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Bond Title",
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
            )
            Text(
                title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Unlocked at Level $unlockedAt",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
            )
        }
    }
}

// ─── Next Reward Card ──────────────────────────────────────────────────────────

@Composable
private fun NextRewardCard(level: Int, reward: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = "Next reward",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Next Reward",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                )
                Text(
                    "Level $level — $reward",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ─── Virtue Identity Card ──────────────────────────────────────────────────────

@Composable
fun VirtueIdentityCard(virtue: Virtue, modifier: Modifier = Modifier) {
    val info = VirtueConfig[virtue]

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter            = painterResource(id = info.emblemRes),
                contentDescription = "${virtue.name} emblem",
                modifier           = Modifier.size(96.dp),
                contentScale       = ContentScale.Fit
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = virtue.name,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary
            )
            Text(
                text      = info.description,
                fontSize  = 13.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Edit Pet Dialog ───────────────────────────────────────────────────────────

@Composable
private fun EditPetDialog(
    currentName: String,
    onDismiss:   () -> Unit,
    onConfirm:   (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pet", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Pet Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text(
                    "Note: pet type and virtue cannot be changed after creation.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Level-Up Celebration Dialog ──────────────────────────────────────────────

@Composable
fun LevelUpDialog(event: LevelUpEvent, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "level_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label         = "level_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = {}) {
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.ic_level),
                    contentDescription = "Level Up",
                    modifier           = Modifier.size(88.dp).scale(iconScale),
                    contentScale       = ContentScale.Fit
                )
                Text(
                    "Level Up!",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.alpha(contentAlpha)
                )
                Text(
                    "${event.petName} reached Bond Level ${event.newLevel}!",
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.alpha(contentAlpha)
                )
                Row(
                    modifier              = Modifier.alpha(contentAlpha),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Lv.${event.oldLevel}  →  Lv.${event.newLevel}",
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha)
                ) {
                    Text("Keep Going!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
