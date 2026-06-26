package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.data.model.PetType
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel

// ---------------------------------------------------------------------------
// petEmoji
// ---------------------------------------------------------------------------
fun petEmoji(typeName: String): String = when (typeName.uppercase()) {
    "DOG"        -> "🐶"
    "CAT"        -> "🐱"
    "RABBIT"     -> "🐰"
    "HAMSTER"    -> "🐹"
    "BIRD"       -> "🦜"
    "FISH"       -> "🐟"
    "TURTLE"     -> "🐢"
    "LIZARD"     -> "🦎"
    "SNAKE"      -> "🐍"
    "HEDGEHOG"   -> "🦔"
    "CHICKEN"    -> "🐔"
    "GUINEA_PIG" -> "🐭"
    "FERRET"     -> "🦦"
    "HORSE"      -> "🐴"
    "DUCK"       -> "🦆"
    "FROG"       -> "🐸"
    "CRAB"       -> "🦀"
    "MONKEY"     -> "🐒"
    "FOX"        -> "🦊"
    "OWL"        -> "🦉"
    "PENGUIN"    -> "🐧"
    "PANDA"      -> "🐼"
    "GOAT"       -> "🐐"
    "PIG"        -> "🐷"
    "COW"        -> "🐄"
    "SHEEP"      -> "🐑"
    "DEER"       -> "🦌"
    "BEAR"       -> "🐻"
    "WOLF"       -> "🐺"
    else         -> "🐾"
}

fun trainerTitle(level: Int): String = when {
    level >= 50 -> "Legendary Keeper"
    level >= 40 -> "Grand Master"
    level >= 30 -> "Master Trainer"
    level >= 20 -> "Devoted Trainer"
    level >= 10 -> "Caretaker"
    else        -> ""
}

// ---------------------------------------------------------------------------
// StatCard — used by ProfileScreen (unchanged)
// ---------------------------------------------------------------------------
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    iconRes: Int? = null,
    dimmed: Boolean = false,
    subValue: String? = null
) {
    val containerColor = if (dimmed)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (dimmed)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier            = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter            = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier           = Modifier.size(18.dp),
                    contentScale       = ContentScale.Fit
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// Rarity color helper
// ---------------------------------------------------------------------------
@Composable
private fun rarityAccentColor(rarityName: String): Color = when (rarityName) {
    "COMMON"   -> MaterialTheme.colorScheme.tertiary
    "UNCOMMON" -> MaterialTheme.colorScheme.secondary
    "RARE"     -> MaterialTheme.colorScheme.primary
    else       -> MaterialTheme.colorScheme.error
}

// ---------------------------------------------------------------------------
// HomeScreen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PetQuestViewModel, navController: NavController) {
    val pets            by viewModel.allPets.collectAsState()
    val tasks           by viewModel.todaysTasks.collectAsState()
    val streak          by viewModel.userStreak.collectAsState()
    val totalBondPoints by viewModel.totalBondPoints.collectAsState()
    val userLevel       by viewModel.userLevel.collectAsState()

    val doneTasks = tasks.count { it.isCompleted }

    val todayProgress by animateFloatAsState(
        targetValue   = if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label         = "today_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("PetQuest", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding      = PaddingValues(vertical = 12.dp)
        ) {
            // ── Stat chips + daily progress ───────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Streak chip — goes gray when no tasks done today
                        CompactStatChip(
                            iconRes = R.drawable.ic_streak,
                            value   = "$streak",
                            dimmed  = doneTasks == 0 && tasks.isNotEmpty()
                        )
                        Spacer(Modifier.width(8.dp))
                        // Bond points chip
                        CompactStatChip(
                            iconRes = R.drawable.ic_bondpoints,
                            value   = "$totalBondPoints"
                        )
                        Spacer(Modifier.width(8.dp))
                        // Level chip
                        CompactStatChip(
                            iconRes = R.drawable.ic_level,
                            value   = "Lv.$userLevel"
                        )
                        Spacer(Modifier.weight(1f))
                        // Daily task count
                        if (tasks.isNotEmpty()) {
                            Text(
                                "$doneTasks / ${tasks.size}",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (doneTasks == tasks.size)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Thin daily progress bar
                    if (tasks.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress   = { todayProgress },
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color      = if (doneTasks == 0)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            else
                                MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (pets.isEmpty()) {
                item {
                    EmptyStateCard(
                        imageRes    = R.drawable.empty_collection,
                        title       = "No Pets Yet",
                        description = "Add your first pet to start building bonds.",
                        actionLabel = "Add a Pet",
                        onAction    = { navController.navigate("add_more_pet") }
                    )
                }
            } else {
                // ── Pet cards ─────────────────────────────────────────────────
                itemsIndexed(pets) { index, pet ->
                    val isVerified    = pet.isVerified
                    val hasGoldBorder = pet.bondLevel >= 20
                    val accentColor   = rarityAccentColor(pet.type.rarity.name)

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(pet.id) {
                        kotlinx.coroutines.delay(index * 50L)
                        visible = true
                    }
                    val cardAlpha by animateFloatAsState(
                        targetValue   = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 280),
                        label         = "pet_alpha_$index"
                    )
                    val offsetY by animateDpAsState(
                        targetValue   = if (visible) 0.dp else 12.dp,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        label         = "pet_offset_$index"
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue   = if (isPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label         = "card_scale_$index"
                    )
                    val bondProgress by animateFloatAsState(
                        targetValue   = (pet.bondPoints % 100) / 100f,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label         = "bond_progress_$index"
                    )

                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                            .alpha(if (isVerified) cardAlpha else cardAlpha * 0.6f)
                            .scale(cardScale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
                            ) { navController.navigate("pet_detail/${pet.id}") },
                        elevation = CardDefaults.cardElevation(if (isVerified) 4.dp else 0.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = if (isVerified)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Rarity accent strip
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(96.dp)
                                    .background(accentColor)
                            )

                            // Pet photo
                            Box(
                                modifier         = Modifier
                                    .padding(12.dp)
                                    .size(72.dp)
                                    .then(
                                        if (hasGoldBorder)
                                            Modifier.border(
                                                width = 2.dp,
                                                color = Color(0xFFFFD700),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                        else Modifier
                                    )
                                    .clip(MaterialTheme.shapes.medium),
                                contentAlignment = Alignment.Center
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
                                            .background(
                                                accentColor.copy(alpha = 0.08f),
                                                MaterialTheme.shapes.medium
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(petEmoji(pet.type.name), fontSize = 42.sp)
                                    }
                                }
                                // Lock overlay for unverified pets
                                if (!isVerified) {
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Lock,
                                            contentDescription = "Unverified",
                                            tint               = Color.White,
                                            modifier           = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // Info column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 12.dp, end = 12.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Name
                                Text(
                                    pet.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 17.sp,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )

                                // Rarity label + level
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            pet.type.rarity.name,
                                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontSize   = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = accentColor
                                        )
                                    }
                                    Text(
                                        "Lv.${pet.bondLevel}",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                // Bond XP bar
                                LinearProgressIndicator(
                                    progress   = { bondProgress },
                                    modifier   = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color      = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.15f)
                                )

                                // Unverified note
                                if (!isVerified) {
                                    Text(
                                        "Needs a photo",
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// CompactStatChip — original pill style with drawable icon
// ---------------------------------------------------------------------------
@Composable
private fun CompactStatChip(iconRes: Int, value: String, dimmed: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (dimmed)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter            = painterResource(id = iconRes),
                contentDescription = null,
                modifier           = Modifier.size(14.dp),
                contentScale       = ContentScale.Fit
            )
            Text(
                value,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = if (dimmed)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ---------------------------------------------------------------------------
// EmptyStateCard
// ---------------------------------------------------------------------------
@Composable
fun EmptyStateCard(
    imageRes    : Int,
    title       : String,
    description : String,
    actionLabel : String,
    onAction    : () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter            = painterResource(id = imageRes),
                contentDescription = null,
                modifier           = Modifier.size(96.dp),
                contentScale       = ContentScale.Fit
            )
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(
                description,
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick  = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
