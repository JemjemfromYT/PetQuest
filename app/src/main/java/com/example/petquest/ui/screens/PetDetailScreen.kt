package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// ─── Pet Memory model ──────────────────────────────────────────────────────────
private data class PetMemory(
    val icon    : ImageVector,
    val title   : String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId      : Int,
    viewModel  : PetQuestViewModel,
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val pets   by viewModel.allPets.collectAsState()
    val streak by viewModel.userStreak.collectAsState()
    val pet    = pets.find { it.id == petId }

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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
            // IMPROVED: gradient header consistent with all other screens.
            // Back button + pet name + edit/delete actions sit on the gradient.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF8C42), Color(0xFFFFB77A))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = Color.White
                        )
                    }
                    Text(
                        text       = pet?.name ?: "Pet Detail",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp,
                        color      = Color.White,
                        modifier   = Modifier.weight(1f)
                    )
                    if (pet != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit pet",
                                tint               = Color.White
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete pet",
                                // IMPROVED: delete icon is white on gradient (was red on peach — hard to see)
                                tint = Color.White.copy(alpha = 0.80f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (pet == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // ── Rarity accent colour ──────────────────────────────────────────────
        val rarityColor = when (pet.type.rarity.name) {
            "COMMON"   -> Color(0xFF9E9E9E)
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

        val bondTitle: Pair<String, Int>? = when {
            pet.bondLevel >= 50 -> "Eternal Companion" to 50
            pet.bondLevel >= 40 -> "Soul Bonded"        to 40
            pet.bondLevel >= 30 -> "Elite Protector"    to 30
            pet.bondLevel >= 25 -> "Bonded Guardian"    to 25
            pet.bondLevel >= 20 -> "Master Companion"   to 20
            pet.bondLevel >= 15 -> "Lifelong Partner"   to 15
            pet.bondLevel >= 10 -> "Loyal Friend"       to 10
            pet.bondLevel >= 5  -> "Trusted Companion"  to 5
            else                -> null
        }

        val nextReward: Pair<Int, String>? = when {
            pet.bondLevel < 5  -> 5  to "an extra daily task"
            pet.bondLevel < 10 -> 10 to "a new achievement tier opens up"
            pet.bondLevel < 15 -> 15 to "a special Bond Badge"
            pet.bondLevel < 20 -> 20 to "a gold profile border"
            pet.bondLevel < 25 -> 25 to "the Bonded Guardian title"
            pet.bondLevel < 30 -> 30 to "the Elite Protector title"
            pet.bondLevel < 40 -> 40 to "the Soul Bond Crest"
            pet.bondLevel < 50 -> 50 to "Legendary Bond Status"
            else               -> null
        }

        val hasGoldBorder = pet.bondLevel >= 20

        val memories = remember(pet, streak) {
            buildList {
                add(PetMemory(Icons.Default.Pets,        "First Steps",       "${pet.name} joined your family"))
                if (pet.isVerified)
                    add(PetMemory(Icons.Default.CameraAlt,   "Verified",          "${pet.name} is fully active"))
                if (pet.bondLevel >= 5)
                    add(PetMemory(Icons.Default.Star,        "Level 5 Reached",   "+1 daily task unlocked"))
                if (pet.bondLevel >= 10)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 10 Reached",  "Achievement tier unlocked"))
                if (pet.bondLevel >= 15)
                    add(PetMemory(Icons.Default.Favorite,    "Level 15 Reached",  "Bond Badge earned"))
                if (pet.bondLevel >= 20)
                    add(PetMemory(Icons.Default.Stars,       "Level 20 Reached",  "Gold border unlocked"))
                if (pet.bondLevel >= 25)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 25 Reached",  "Bonded Guardian title earned"))
                if (pet.bondLevel >= 30)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 30 Reached",  "Elite Protector title earned"))
                if (pet.bondLevel >= 40)
                    add(PetMemory(Icons.Default.Stars,       "Level 40 Reached",  "Soul Bond Crest unlocked"))
                if (pet.bondLevel >= 50)
                    add(PetMemory(Icons.Default.Stars,       "Level 50 Reached",  "Legendary Bond Status achieved"))
                if (streak >= 7)
                    add(PetMemory(Icons.Default.Whatshot,    "7-Day Streak",      "7 days of dedication"))
                if (streak >= 30)
                    add(PetMemory(Icons.Default.Whatshot,    "30-Day Streak",     "30 days of unwavering care"))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Pet photo card ────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier  = Modifier
                        .size(200.dp)
                        .then(
                            if (hasGoldBorder)
                                Modifier.border(
                                    width = 5.dp,
                                    color = Color(0xFFFFD700),
                                    shape = MaterialTheme.shapes.medium
                                )
                            else Modifier
                        ),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (pet.photoUri != null) Color.Transparent
                        else rarityColor.copy(alpha = 0.10f)
                    )
                ) {
                    if (pet.photoUri != null) {
                        AsyncImage(
                            model              = pet.photoUri,
                            contentDescription = "Pet photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier         = Modifier
                                .fillMaxSize()
                                .background(rarityColor.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(petEmoji(pet.type.name), fontSize = 64.sp)
                                Text(
                                    "No Photo Yet",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = rarityColor.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }

                // Gold sparkle for level 20+
                if (hasGoldBorder) {
                    Box(
                        modifier         = Modifier.size(220.dp).alpha(0.4f),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text("✨", fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Rarity badge ──────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = rarityColor
            ) {
                Text(
                    " ${pet.type.rarity.name} ",
                    modifier      = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // Level 15 Bond Badge
            if (pet.bondLevel >= 15) {
                Spacer(Modifier.height(8.dp))
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

            // ── Stat row: Bond Level / Type / Bond Points ─────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    value           = "Lv.${pet.bondLevel}",
                    label           = "Bond Level",
                    modifier        = Modifier.weight(1f),
                    iconRes         = R.drawable.ic_level,
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    accentContent   = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatCard(
                    value           = pet.type.name.replace("_", " "),
                    label           = "Type",
                    modifier        = Modifier.weight(1f),
                    accentContainer = MaterialTheme.colorScheme.secondaryContainer,
                    accentContent   = MaterialTheme.colorScheme.onSecondaryContainer
                )
                StatCard(
                    value           = "${pet.bondPoints}",
                    label           = "Bond Pts",
                    modifier        = Modifier.weight(1f),
                    iconRes         = R.drawable.ic_bondpoints,
                    accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    accentContent   = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.height(14.dp))

            // IMPROVED: bond progress bar replaced with gradient Box
            // LinearProgressIndicator was the thin default Android bar.
            // This is now 12dp with a gradient fill that uses the rarity color.
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(rarityColor.copy(alpha = 0.13f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bondProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(rarityColor, rarityColor.copy(alpha = 0.65f))
                                )
                            )
                    )
                    // Small dot at end of fill
                    if (bondProgress > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bondProgress.coerceIn(0f, 1f))
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(rarityColor)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // IMPROVED: show % progress on the right for high-level pets
                    Text(
                        "${((pet.bondPoints % 100))}%",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = rarityColor
                    )
                }
            }

            // ── Bond Title ────────────────────────────────────────────────────
            if (bondTitle != null) {
                Spacer(Modifier.height(12.dp))
                BondTitleCard(title = bondTitle.first, unlockedAt = bondTitle.second)
            }

            // ── Next Reward ───────────────────────────────────────────────────
            if (nextReward != null) {
                Spacer(Modifier.height(10.dp))
                NextRewardCard(level = nextReward.first, reward = nextReward.second)
            }

            Spacer(Modifier.height(20.dp))

            // ── Virtue identity card ──────────────────────────────────────────
            Box(modifier = Modifier.alpha(virtueAlpha)) {
                VirtueIdentityCard(virtue = pet.virtue)
            }

            Spacer(Modifier.height(20.dp))

            // ── Pet Memories Timeline ─────────────────────────────────────────
            if (memories.isNotEmpty()) {
                PetMemoryTimeline(petName = pet.name, memories = memories)
                Spacer(Modifier.height(20.dp))
            }

            // ── Verification Status ───────────────────────────────────────────
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
                        Icon(
                            imageVector        = Icons.Default.CameraAlt,
                            contentDescription = "Add a photo",
                            tint               = MaterialTheme.colorScheme.onErrorContainer,
                            modifier           = Modifier.size(48.dp)
                        )
                        Text(
                            "Photo Required",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Take a photo of ${pet.name} to unlock tasks, bond points, and streaks.",
                            fontSize  = 13.sp,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onErrorContainer
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.25f)
                        )
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "Complete tasks",
                                "Earn bond points",
                                "Increase streaks",
                                "Unlock achievements"
                            ).forEach { benefit ->
                                Text(
                                    "• $benefit",
                                    fontSize  = 13.sp,
                                    color     = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
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
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// EditPetDialog — unchanged from original
// ---------------------------------------------------------------------------
@Composable
private fun EditPetDialog(
    currentName: String,
    onDismiss  : () -> Unit,
    onConfirm  : (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Rename Pet", fontWeight = FontWeight.Bold) },
        text             = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Pet Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------------------------------------------------------------------
// BondTitleCard — IMPROVED: gradient strip on left instead of icon-only
// ---------------------------------------------------------------------------
@Composable
private fun BondTitleCard(title: String, unlockedAt: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.60f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient accent strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f)
                            )
                        )
                    )
            )
            Row(
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.tertiary,
                    modifier           = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "Bond Title",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        title,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "Unlocked at Level $unlockedAt",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// NextRewardCard — unchanged from original
// ---------------------------------------------------------------------------
@Composable
private fun NextRewardCard(level: Int, reward: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.60f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.secondary,
                modifier           = Modifier.size(22.dp)
            )
            Column {
                Text(
                    "Next Reward — Level $level",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Reach Level $level and $reward",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// VirtueIdentityCard — unchanged from original (already looks great)
// ---------------------------------------------------------------------------
@Composable
private fun VirtueIdentityCard(virtue: Virtue) {
    val virtueInfo = VirtueConfig[virtue]

    val virtueColor = when (virtue) {
        Virtue.COURAGE    -> Color(0xFFC62828)
        Virtue.DILIGENCE  -> Color(0xFF1565C0)
        Virtue.TEMPERANCE -> Color(0xFF2E7D32)
        Virtue.WISDOM     -> Color(0xFF6A1B9A)
        Virtue.COMPASSION -> Color(0xFFE65100)
    }
    val virtueBg = when (virtue) {
        Virtue.COURAGE    -> Color(0xFFFFEBEE)
        Virtue.DILIGENCE  -> Color(0xFFE3F2FD)
        Virtue.TEMPERANCE -> Color(0xFFE8F5E9)
        Virtue.WISDOM     -> Color(0xFFF3E5F5)
        Virtue.COMPASSION -> Color(0xFFFFF3E0)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = virtueBg),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = MaterialTheme.shapes.large
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Virtue accent strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(virtueColor)
            )
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape    = CircleShape,
                    color    = virtueColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter            = painterResource(id = virtueInfo.emblemRes),
                            contentDescription = virtue.name,
                            modifier           = Modifier.size(40.dp),
                            contentScale       = ContentScale.Fit
                        )
                    }
                }
                Column {
                    Text(
                        "Virtue Identity",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = virtueColor.copy(alpha = 0.75f)
                    )
                    Text(
                        virtue.name,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = virtueColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        virtueInfo.description,
                        fontSize = 13.sp,
                        color    = virtueColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PetMemoryTimeline — unchanged from original
// ---------------------------------------------------------------------------
@Composable
private fun PetMemoryTimeline(petName: String, memories: List<PetMemory>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$petName's Journey",
                fontSize   = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            memories.forEachIndexed { index, memory ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape    = MaterialTheme.shapes.small,
                        color    = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = memory.icon,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            memory.title,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            memory.subtitle,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < memories.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LevelUpDialog — unchanged from original
// ---------------------------------------------------------------------------
@Composable
private fun LevelUpDialog(event: LevelUpEvent, onDismiss: () -> Unit) {
    val isMilestone = event.newLevel % 5 == 0

    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(event) {
        phase = 0
        kotlinx.coroutines.delay(50)
        phase = 1
    }

    val bgAlpha by animateFloatAsState(
        targetValue   = if (phase == 1) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label         = "bg_alpha"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (phase == 1) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150),
        label         = "content_alpha"
    )
    val headerScale by animateFloatAsState(
        targetValue   = if (phase == 1) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "header_scale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier  = Modifier.fillMaxWidth().alpha(bgAlpha),
            shape     = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(8.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (isMilestone)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isMilestone) "🎉" else "⭐",
                    fontSize = 48.sp,
                    modifier = Modifier.scale(headerScale).alpha(contentAlpha)
                )
                Text(
                    if (isMilestone) "Milestone!" else "Level Up!",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isMilestone)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier   = Modifier.alpha(contentAlpha)
                )
                Text(
                    "${event.petName} reached Bond Level ${event.newLevel}!",
                    fontSize  = 15.sp,
                    textAlign = TextAlign.Center,
                    color     = if (isMilestone)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier  = Modifier.alpha(contentAlpha)
                )

                // milestoneReward is computed locally — LevelUpEvent only has petName/oldLevel/newLevel
                val milestoneReward = when (event.newLevel) {
                    5    -> "+1 daily task unlocked"
                    10   -> "Achievement tier unlocks!"
                    15   -> "Bond Badge earned"
                    20   -> "Gold border unlocked"
                    25   -> "Bonded Guardian title"
                    30   -> "Elite Protector title"
                    40   -> "Soul Bond Crest"
                    50   -> "Legendary Bond Status"
                    else -> null
                }
                if (milestoneReward != null) {
                    Surface(
                        shape    = MaterialTheme.shapes.medium,
                        color    = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        modifier = Modifier.alpha(contentAlpha)
                    ) {
                        Text(
                            milestoneReward,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center,
                            color      = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier   = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isMilestone)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (isMilestone) "Claim Reward" else "Awesome!",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                }
            }
        }
    }
}
