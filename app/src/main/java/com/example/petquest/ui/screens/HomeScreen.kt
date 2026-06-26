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
import androidx.compose.material.icons.filled.Shield
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
// petEmoji — species identity placeholder when no photo exists
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
// StatCard — used by ProfileScreen
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

    val doneTasks    = tasks.count { it.isCompleted }
    val streakActive = doneTasks > 0 || tasks.isEmpty()

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding      = PaddingValues(vertical = 16.dp)
        ) {
            // ── Top statistics row ────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeStatCard(
                            iconRes  = R.drawable.ic_streak,
                            value    = "$streak",
                            label    = "Streak",
                            dimmed   = !streakActive,
                            modifier = Modifier.weight(1f)
                        )
                        HomeStatCard(
                            iconRes  = R.drawable.ic_bondpoints,
                            value    = "$totalBondPoints",
                            label    = "Bond Points",
                            modifier = Modifier.weight(1f)
                        )
                        HomeStatCard(
                            iconRes  = R.drawable.ic_level,
                            value    = "Lv.$userLevel",
                            label    = "Level",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (tasks.isNotEmpty()) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress   = { todayProgress },
                                modifier   = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
                    val virtueInfo    = VirtueConfig[pet.virtue]
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
                    val isPressed         by interactionSource.collectIsPressedAsState()
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
                        elevation = CardDefaults.cardElevation(if (isVerified) 3.dp else 0.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = if (isVerified)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rarity accent strip
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(88.dp)
                                    .background(accentColor)
                            )

                            // Pet photo
                            Box(
                                modifier         = Modifier
                                    .padding(12.dp)
                                    .size(64.dp)
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
                                        Text(petEmoji(pet.type.name), fontSize = 36.sp)
                                    }
                                }
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
                                            modifier           = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Info column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 10.dp, end = 12.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Name + verified shield
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        pet.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 16.sp,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis,
                                        modifier   = Modifier.weight(1f, fill = false)
                                    )
                                    if (isVerified) {
                                        Icon(
                                            imageVector        = Icons.Default.Shield,
                                            contentDescription = "Verified",
                                            tint               = MaterialTheme.colorScheme.tertiary,
                                            modifier           = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                // Rarity · Level
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        pet.type.rarity.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = accentColor.copy(alpha = 0.75f)
                                    )
                                    Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    trackColor = accentColor.copy(alpha = 0.12f)
                                )

                                // Virtue emblem + unverified note
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (!isVerified) {
                                        Text(
                                            "Needs a photo",
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Spacer(Modifier.width(1.dp))
                                    }
                                    Image(
                                        painter            = painterResource(id = virtueInfo.emblemRes),
                                        contentDescription = null,
                                        modifier           = Modifier.size(20.dp),
                                        contentScale       = ContentScale.Fit
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
// HomeStatCard — large centered stat card (no icon)
// ---------------------------------------------------------------------------
@Composable
private fun HomeStatCard(
    iconRes  : Int,
    value    : String,
    label    : String,
    dimmed   : Boolean = false,
    modifier : Modifier = Modifier
) {
    val containerColor = if (dimmed)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.secondaryContainer
    val valueColor = if (dimmed)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSecondaryContainer
    val labelColor = if (dimmed)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter            = painterResource(id = iconRes),
                contentDescription = label,
                modifier           = Modifier
                    .size(20.dp)
                    .alpha(if (dimmed) 0.4f else 1f),
                contentScale       = ContentScale.Fit
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp,
                color      = valueColor,
                maxLines   = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text       = label,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                color      = labelColor,
                maxLines   = 1
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
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
