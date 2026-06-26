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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.petquest.R
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
// StatCard — shared helper used by PetDetailScreen too
// ---------------------------------------------------------------------------
@Composable
fun StatCard(
    value           : String,
    label           : String,
    modifier        : Modifier = Modifier,
    iconRes         : Int?     = null,
    dimmed          : Boolean  = false,
    accentContainer : Color?   = null,
    accentContent   : Color?   = null
) {
    val containerColor = when {
        dimmed               -> MaterialTheme.colorScheme.surfaceVariant
        accentContainer != null -> accentContainer
        else                 -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        dimmed           -> MaterialTheme.colorScheme.onSurfaceVariant
        accentContent != null -> accentContent
        else             -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
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
fun rarityAccentColor(rarityName: String): Color = when (rarityName) {
    "COMMON"   -> Color(0xFF9E9E9E)
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
    val allDone      = tasks.isNotEmpty() && doneTasks == tasks.size

    val todayProgress by animateFloatAsState(
        targetValue   = if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label         = "today_progress"
    )

    Scaffold(
        topBar = {
            // IMPROVED: gradient header instead of flat solid color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF8C42),  // warm orange-amber start
                                Color(0xFFFFB77A)   // lighter peach end
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    "PetQuest",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }
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
                        // STREAK — warm orange
                        HomeStatCard(
                            iconRes        = R.drawable.ic_streak,
                            value          = "$streak",
                            label          = "Streak",
                            dimmed         = !streakActive,
                            containerColor = Color(0xFFFFF3E0),
                            valueColor     = Color(0xFFBF360C),
                            modifier       = Modifier.weight(1f)
                        )
                        // BOND POINTS — deep green
                        HomeStatCard(
                            iconRes        = R.drawable.ic_bondpoints,
                            value          = "$totalBondPoints",
                            label          = "Bond Pts",
                            containerColor = Color(0xFFE8F5E9),
                            valueColor     = Color(0xFF1B5E20),
                            modifier       = Modifier.weight(1f)
                        )
                        // LEVEL — deep violet
                        HomeStatCard(
                            iconRes        = R.drawable.ic_level,
                            value          = "Lv.$userLevel",
                            label          = "Level",
                            containerColor = Color(0xFFF3E5F5),
                            valueColor     = Color(0xFF4A148C),
                            modifier       = Modifier.weight(1f)
                        )
                    }

                    // Today's tasks progress — premium card
                    if (tasks.isNotEmpty()) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            colors    = CardDefaults.cardColors(
                                containerColor = if (allDone)
                                    Color(0xFFE8F5E9)  // green tint when all done
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(0.dp),
                            shape     = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (allDone) "All done today! 🎉" else "Today's Tasks",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (allDone) Color(0xFF1B5E20)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (allDone) Color(0xFF1B5E20).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            "$doneTasks / ${tasks.size}",
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = if (allDone) Color(0xFF1B5E20)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // IMPROVED: thicker progress bar (8dp) with rounded gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(todayProgress)
                                            .fillMaxHeight()
                                            .background(
                                                if (allDone)
                                                    Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF81C784)))
                                                else
                                                    Brush.horizontalGradient(listOf(Color(0xFFBF360C), Color(0xFFFF8C42)))
                                            )
                                    )
                                }
                            }
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
                    // IMPROVED: bond progress now shows in 0..1 within current level
                    val bondProgress by animateFloatAsState(
                        targetValue   = (pet.bondPoints % 100) / 100f,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                        label         = "bond_progress_$index"
                    )

                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                            .alpha(if (isVerified) cardAlpha else cardAlpha * 0.65f)
                            .scale(cardScale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
                            ) { navController.navigate("pet_detail/${pet.id}") },
                        // IMPROVED: verified pets get real elevation (shadow depth)
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isVerified) 4.dp else 0.dp
                        ),
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
                            // IMPROVED: accent strip is taller (matches full card height)
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(96.dp)
                                    .background(
                                        if (isVerified) accentColor
                                        else accentColor.copy(alpha = 0.35f)
                                    )
                            )

                            // Pet photo
                            Box(
                                modifier         = Modifier
                                    .padding(12.dp)
                                    .size(68.dp)
                                    .then(
                                        if (hasGoldBorder)
                                            Modifier.border(
                                                width = 2.5.dp,
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
                                                accentColor.copy(alpha = 0.10f),
                                                MaterialTheme.shapes.medium
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(petEmoji(pet.type.name), fontSize = 36.sp)
                                    }
                                }
                                // IMPROVED: lock overlay is darker and icon is bigger
                                if (!isVerified) {
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.CameraAlt,
                                                contentDescription = "Needs photo",
                                                tint               = Color.White,
                                                modifier           = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Info column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 10.dp, end = 12.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Name + verified checkmark
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
                                            imageVector        = Icons.Default.CheckCircle,
                                            contentDescription = "Verified",
                                            tint               = Color(0xFF43A047),
                                            modifier           = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // Rarity chip + level
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accentColor.copy(alpha = 0.13f)
                                    ) {
                                        Text(
                                            pet.type.rarity.name.lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = accentColor
                                        )
                                    }
                                    Text("·", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "Lv.${pet.bondLevel}",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(Modifier.height(2.dp))

                                // IMPROVED: bond XP bar is now 8dp (was 5dp) — much more visible
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.12f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(bondProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        accentColor,
                                                        accentColor.copy(alpha = 0.70f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // IMPROVED: virtue row — now shows virtue name next to emblem
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
                                    // IMPROVED: virtue emblem is now 28dp (was 20dp) + name label
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            pet.virtue.name.lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            fontSize   = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                        )
                                        Image(
                                            painter            = painterResource(id = virtueInfo.emblemRes),
                                            contentDescription = pet.virtue.name,
                                            modifier           = Modifier.size(28.dp),
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
}

// ---------------------------------------------------------------------------
// HomeStatCard — each card has its own personality colour
// ---------------------------------------------------------------------------
@Composable
private fun HomeStatCard(
    iconRes        : Int,
    value          : String,
    label          : String,
    dimmed         : Boolean = false,
    containerColor : Color   = MaterialTheme.colorScheme.secondaryContainer,
    valueColor     : Color   = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier       : Modifier = Modifier
) {
    val resolvedContainer = if (dimmed) MaterialTheme.colorScheme.surfaceVariant else containerColor
    val resolvedValue     = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else valueColor
    val resolvedLabel     = if (dimmed)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = resolvedContainer),
        // IMPROVED: stat cards now have subtle elevation so they lift off the background
        elevation = CardDefaults.cardElevation(defaultElevation = if (dimmed) 0.dp else 2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter            = painterResource(id = iconRes),
                contentDescription = label,
                modifier           = Modifier
                    .size(24.dp)
                    .alpha(if (dimmed) 0.35f else 1f),
                contentScale       = ContentScale.Fit
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp,
                color      = resolvedValue,
                maxLines   = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text       = label,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                color      = resolvedLabel,
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
                textAlign = TextAlign.Center
            )
            Button(onClick = onAction) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
