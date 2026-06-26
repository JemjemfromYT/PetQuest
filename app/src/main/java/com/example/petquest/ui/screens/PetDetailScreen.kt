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
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: Int,
    viewModel: PetQuestViewModel,
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
            pet.bondLevel >= 50 -> "Eternal Companion"  to 50
            pet.bondLevel >= 40 -> "Soul Bonded"         to 40
            pet.bondLevel >= 30 -> "Elite Protector"     to 30
            pet.bondLevel >= 25 -> "Bonded Guardian"     to 25
            pet.bondLevel >= 20 -> "Master Companion"    to 20
            pet.bondLevel >= 15 -> "Lifelong Partner"    to 15
            pet.bondLevel >= 10 -> "Loyal Friend"        to 10
            pet.bondLevel >= 5  -> "Trusted Companion"   to 5
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
                add(PetMemory(Icons.Default.Pets,        "First Steps",      "${pet.name} joined your family"))
                if (pet.isVerified)
                    add(PetMemory(Icons.Default.CameraAlt,   "Verified",         "${pet.name} is fully active"))
                if (pet.bondLevel >= 5)
                    add(PetMemory(Icons.Default.Star,        "Level 5 Reached",  "+1 daily task unlocked"))
                if (pet.bondLevel >= 10)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 10 Reached", "Achievement tier unlocked"))
                if (pet.bondLevel >= 15)
                    add(PetMemory(Icons.Default.Favorite,    "Level 15 Reached", "Bond Badge earned"))
                if (pet.bondLevel >= 20)
                    add(PetMemory(Icons.Default.Stars,       "Level 20 Reached", "Gold border unlocked"))
                if (pet.bondLevel >= 25)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 25 Reached", "Bonded Guardian title earned"))
                if (pet.bondLevel >= 30)
                    add(PetMemory(Icons.Default.EmojiEvents, "Level 30 Reached", "Elite Protector title earned"))
                if (pet.bondLevel >= 40)
                    add(PetMemory(Icons.Default.Stars,       "Level 40 Reached", "Soul Bond Crest unlocked"))
                if (pet.bondLevel >= 50)
                    add(PetMemory(Icons.Default.Stars,       "Level 50 Reached", "Legendary Bond Status achieved"))
                if (streak >= 7)
                    add(PetMemory(Icons.Default.Whatshot,    "7-Day Streak",     "7 days of dedication"))
                if (streak >= 30)
                    add(PetMemory(Icons.Default.Whatshot,    "30-Day Streak",    "30 days of unwavering care"))
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

            // ── Pet photo card — rarity-tinted background when no photo ────────
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

                // Gold sparkle overlay for level 20+
                if (hasGoldBorder) {
                    Box(
                        modifier         = Modifier
                            .size(220.dp)
                            .alpha(0.4f),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text("✨", fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Rarity badge — bolder, bigger ──────────────────────────────────
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = rarityColor
            ) {
                Text(
                    " ${pet.type.rarity.name} ",
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
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

            // ── Stat row: Bond Level / Type / Bond Points — each with its own tint ──
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

            Spacer(Modifier.height(12.dp))

            // ── Bond progress bar — rarity-tinted, taller, rounded ─────────────
            LinearProgressIndicator(
                progress   = { bondProgress },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color      = rarityColor,
                trackColor = rarityColor.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Bond Title ────────────────────────────────────────────────────
            if (bondTitle != null) {
                Spacer(Modifier.height(10.dp))
                BondTitleCard(title = bondTitle.first, unlockedAt = bondTitle.second)
            }

            // ── Next Reward ───────────────────────────────────────────────────
            if (nextReward != null) {
                Spacer(Modifier.height(10.dp))
                NextRewardCard(level = nextReward.first, reward = nextReward.second)
            }

            Spacer(Modifier.height(20.dp))

            // ── Virtue identity card — redesigned horizontal layout ────────────
            Box(modifier = Modifier.alpha(virtueAlpha)) {
                VirtueIdentityCard(virtue = pet.virtue)
            }

            Spacer(Modifier.height(20.dp))

            // ── Pet Memories Timeline ──────────────────────────────────────────
            if (memories.isNotEmpty()) {
                PetMemoryTimeline(petName = pet.name, memories = memories)
                Spacer(Modifier.height(20.dp))
            }

            // ── Verification Status ────────────────────────────────────────────
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
                // ── Unverified — prompt to verify ──────────────────────────────
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
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)
                        )
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Complete tasks",
                                "Earn bond points",
                                "Increase streaks",
                                "Unlock achievements"
                            ).forEach { feature ->
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

// ─── Pet Memory Timeline ───────────────────────────────────────────────────────

@Composable
private fun PetMemoryTimeline(petName: String, memories: List<PetMemory>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$petName's Journey",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            memories.forEachIndexed { index, memory ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Timeline spine
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape    = MaterialTheme.shapes.extraSmall,
                            color    = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector        = memory.icon,
                                    contentDescription = memory.title,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (index < memories.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                HorizontalDivider(
                                    modifier  = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter),
                                    color     = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 2.dp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Memory text
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (index < memories.lastIndex) 16.dp else 0.dp)
                    ) {
                        Text(
                            memory.title,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            memory.subtitle,
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Stars,
                contentDescription = "Bond title",
                tint               = MaterialTheme.colorScheme.tertiary,
                modifier           = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
}

// ─── Next Reward Card ──────────────────────────────────────────────────────────

@Composable
private fun NextRewardCard(level: Int, reward: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = Color(0xFFFFF8E1)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = "Next reward",
                tint               = Color(0xFFE65100),
                modifier           = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Next Reward — Level $level",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFBF360C).copy(alpha = 0.75f)
                )
                Text(
                    "Reach Level $level and $reward",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF4E2000)
                )
            }
        }
    }
}

// ─── Virtue Identity Card — redesigned horizontal layout ──────────────────────

@Composable
fun VirtueIdentityCard(virtue: Virtue, modifier: Modifier = Modifier) {
    val info = VirtueConfig[virtue]

    // Virtue-specific accent colour — each virtue has a distinct identity
    val virtueColor = when (virtue.name.uppercase()) {
        "COURAGE"    -> Color(0xFFC62828)
        "JUSTICE"    -> Color(0xFF1565C0)
        "TEMPERANCE" -> Color(0xFF2E7D32)
        "WISDOM"     -> Color(0xFF6A1B9A)
        "LOYALTY"    -> Color(0xFFE65100)
        else         -> Color(0xFF37474F)
    }
    val virtueBg = when (virtue.name.uppercase()) {
        "COURAGE"    -> Color(0xFFFFEBEE)
        "JUSTICE"    -> Color(0xFFE3F2FD)
        "TEMPERANCE" -> Color(0xFFE8F5E9)
        "WISDOM"     -> Color(0xFFF3E5F5)
        "LOYALTY"    -> Color(0xFFFFF3E0)
        else         -> Color(0xFFECEFF1)
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = virtueBg),
        elevation = CardDefaults.cardElevation(4.dp),
        shape     = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left virtue-coloured accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(virtueColor)
            )

            // Circular emblem container
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(virtueColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter            = painterResource(id = info.emblemRes),
                    contentDescription = "${virtue.name} emblem",
                    modifier           = Modifier.size(52.dp),
                    contentScale       = ContentScale.Fit
                )
            }

            // Text column
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Virtue Identity",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = virtueColor.copy(alpha = 0.65f),
                    letterSpacing = 0.8.sp
                )
                Text(
                    text       = virtue.name,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = virtueColor
                )
                Text(
                    text  = info.description,
                    fontSize = 13.sp,
                    color    = virtueColor.copy(alpha = 0.75f)
                )
            }
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

// ─── Level-Up / Milestone Celebration Dialog ───────────────────────────────────

@Composable
fun LevelUpDialog(event: LevelUpEvent, onDismiss: () -> Unit) {
    val milestoneReward: String? = when (event.newLevel) {
        5  -> "an extra daily task is added to your routine"
        10 -> "a new achievement tier opens up — 5 new challenges"
        15 -> "a Bond Badge appears on ${event.petName}'s profile"
        20 -> "a gold border unlocks — ${event.petName} shines"
        25 -> "the Bonded Guardian title is earned"
        30 -> "the Elite Protector title is earned"
        40 -> "the Soul Bond Crest is unlocked"
        50 -> "Legendary Bond Status is achieved"
        else -> null
    }
    val isMilestone = milestoneReward != null

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
                containerColor = if (isMilestone)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
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
                    modifier           = Modifier.size(if (isMilestone) 100.dp else 88.dp).scale(iconScale),
                    contentScale       = ContentScale.Fit
                )

                Text(
                    if (isMilestone) "Milestone Reached!" else "Level Up!",
                    fontSize   = if (isMilestone) 26.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = if (isMilestone)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "${event.petName} reached Bond Level ${event.newLevel}!",
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isMilestone)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                // Level arrow
                Row(
                    modifier              = Modifier.alpha(contentAlpha),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = (if (isMilestone)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Lv.${event.oldLevel}  →  Lv.${event.newLevel}",
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isMilestone)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Milestone reward banner
                if (milestoneReward != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
                        shape    = MaterialTheme.shapes.medium,
                        color    = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    modifier           = Modifier.size(16.dp)
                                )
                                Text(
                                    "Milestone Reward",
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                milestoneReward,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign  = TextAlign.Center,
                                color      = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha),
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
