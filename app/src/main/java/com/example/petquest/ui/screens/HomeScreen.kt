// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/HomeScreen.kt
// FULL REPLACEMENT — adds event-aware header tinting + EventBadge on pet cards
// ============================================================

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
import androidx.compose.material.icons.filled.Celebration
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
import java.time.LocalDate

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
        dimmed                  -> MaterialTheme.colorScheme.surfaceVariant
        accentContainer != null -> accentContainer
        else                    -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        dimmed                -> MaterialTheme.colorScheme.onSurfaceVariant
        accentContent != null -> accentContent
        else                  -> MaterialTheme.colorScheme.onSecondaryContainer
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
// EmptyStateCard — shared helper
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
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter            = painterResource(id = imageRes),
                contentDescription = title,
                modifier           = Modifier.size(80.dp),
                contentScale       = ContentScale.Fit
            )
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Text(
                description,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
            Button(
                onClick = onAction,
                shape   = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
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
// HomeStatCard — internal helper (used only in this file)
// ---------------------------------------------------------------------------
@Composable
private fun HomeStatCard(
    iconRes        : Int,
    value          : String,
    label          : String,
    containerColor : Color,
    valueColor     : Color,
    dimmed         : Boolean = false,
    modifier       : Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(
            containerColor = if (dimmed) containerColor.copy(alpha = 0.45f) else containerColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Image(
                painter            = painterResource(id = iconRes),
                contentDescription = label,
                modifier           = Modifier.size(18.dp),
                contentScale       = ContentScale.Fit,
                alpha              = if (dimmed) 0.4f else 1f
            )
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 17.sp,
                color      = if (dimmed) valueColor.copy(alpha = 0.40f) else valueColor
            )
            Text(
                label,
                fontSize = 10.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// EventBadge — tiny overlay shown on pet cards during an active event
// ---------------------------------------------------------------------------
@Composable
fun EventBadge(event: SeasonalEvent) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = event.gradientStart.copy(alpha = 0.90f),
        modifier = Modifier
    ) {
        Text(
            event.emoji,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
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

    // ── Check for active seasonal event ──────────────────────────────────────
    val today        = remember { LocalDate.now() }
    val activeEvent  = remember { getActiveEvent(today) }

    val doneTasks    = tasks.count { it.isCompleted }
    val streakActive = doneTasks > 0 || tasks.isEmpty()
    val allDone      = tasks.isNotEmpty() && doneTasks == tasks.size

    val todayProgress by animateFloatAsState(
        targetValue   = if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label         = "today_progress"
    )

    // ── Header gradient — switches to event colors when an event is active ───
    val headerStart = activeEvent?.gradientStart ?: Color(0xFFFF8C42)
    val headerEnd   = activeEvent?.gradientEnd   ?: Color(0xFFFFB77A)

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(headerStart, headerEnd))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "PetQuest",
                        fontSize      = 24.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    // Show event badge in header if an event is active
                    if (activeEvent != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(activeEvent.emoji, fontSize = 14.sp)
                                Text(
                                    activeEvent.name,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                        }
                    }
                }
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
            // ── Active Event Quick Banner (subtle, on Home) ────────────────────
            if (activeEvent != null) {
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            activeEvent.gradientStart.copy(alpha = 0.15f),
                                            activeEvent.gradientEnd.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(activeEvent.emoji, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${activeEvent.name} is live!",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "${daysRemainingInEvent(activeEvent, today)} days left — check Events tab",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector        = Icons.Default.Celebration,
                                contentDescription = null,
                                tint               = activeEvent.gradientStart,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Top statistics row ────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HomeStatCard(
                            iconRes        = R.drawable.ic_streak,
                            value          = "$streak",
                            label          = "Streak",
                            dimmed         = !streakActive,
                            containerColor = Color(0xFFFFF3E0),
                            valueColor     = Color(0xFFBF360C),
                            modifier       = Modifier.weight(1f)
                        )
                        HomeStatCard(
                            iconRes        = R.drawable.ic_bondpoints,
                            value          = "$totalBondPoints",
                            label          = "Bond Pts",
                            containerColor = Color(0xFFE8F5E9),
                            valueColor     = Color(0xFF1B5E20),
                            modifier       = Modifier.weight(1f)
                        )
                        HomeStatCard(
                            iconRes        = R.drawable.ic_level,
                            value          = "Lv.$userLevel",
                            label          = "Level",
                            containerColor = Color(0xFFF3E5F5),
                            valueColor     = Color(0xFF4A148C),
                            modifier       = Modifier.weight(1f)
                        )
                    }

                    // Today's tasks progress
                    if (tasks.isNotEmpty()) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            colors    = CardDefaults.cardColors(
                                containerColor = if (allDone)
                                    Color(0xFFE8F5E9)
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
                                                else if (activeEvent != null)
                                                    Brush.horizontalGradient(listOf(activeEvent.gradientStart, activeEvent.gradientEnd))
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
                            // Accent strip
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
                                if (!isVerified) {
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.CameraAlt,
                                            contentDescription = "Needs photo",
                                            tint               = Color.White,
                                            modifier           = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                // ── EventBadge overlay ────────────────────────
                                if (activeEvent != null && isVerified) {
                                    Box(
                                        modifier         = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                    ) {
                                        EventBadge(activeEvent)
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
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            pet.type.rarity.name.lowercase()
                                                .replaceFirstChar { it.uppercaseChar() },
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = accentColor,
                                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        "·",
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Lv.${pet.bondLevel}",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                // Bond progress bar
                                if (isVerified) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                accentColor.copy(alpha = 0.12f)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(bondProgress)
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            accentColor.copy(alpha = 0.70f),
                                                            accentColor
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                } else {
                                    Text(
                                        "Needs a photo",
                                        fontSize = 11.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Virtue label — bottom right
                                if (virtueInfo != null) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            pet.virtue.name
                                                .lowercase()
                                                .replaceFirstChar { it.uppercaseChar() },
                                            fontSize = 11.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Image(
                                            painter            = painterResource(virtueInfo.emblemRes),
                                            contentDescription = pet.virtue.name,
                                            modifier           = Modifier.size(14.dp),
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
