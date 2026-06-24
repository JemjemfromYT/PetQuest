package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
// petEmoji — species identity visual used across all screens
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

// ---------------------------------------------------------------------------
// StatCard — supports an optional PNG icon via painterResource
// ---------------------------------------------------------------------------
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    iconRes: Int? = null,
    dimmed: Boolean = false
) {
    val containerColor = if (dimmed)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (dimmed)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Image(
                    painter            = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier           = Modifier.size(22.dp),
                    contentScale       = ContentScale.Fit
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = contentColor)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// HomeScreen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PetQuestViewModel, navController: NavController) {
    val pets             by viewModel.allPets.collectAsState()
    val tasks            by viewModel.todaysTasks.collectAsState()
    val streak           by viewModel.userStreak.collectAsState()
    val totalBondPoints  by viewModel.totalBondPoints.collectAsState()
    val userLevel        by viewModel.userLevel.collectAsState()
    val collectedSpecies by viewModel.collectedSpecies.collectAsState()

    val doneTasks    = tasks.count { it.isCompleted }
    val speciesCount = collectedSpecies.size
    val totalSpecies = PetType.entries.size

    val collectionProgress by animateFloatAsState(
        targetValue   = if (totalSpecies == 0) 0f else speciesCount / totalSpecies.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label         = "collection_progress"
    )

    val todayProgress by animateFloatAsState(
        targetValue   = if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Stat cards row ────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value    = "$streak",
                        label    = "Streak",
                        iconRes  = R.drawable.ic_streak,
                        dimmed   = doneTasks == 0
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value    = "$totalBondPoints",
                        label    = "Bond Pts",
                        iconRes  = R.drawable.ic_bondpoints
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value    = "Lv.$userLevel",
                        label    = "Level",
                        iconRes  = R.drawable.ic_level
                    )
                }
            }

            // ── Collection progress card ──────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Species Collection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "$speciesCount / $totalSpecies",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 14.sp,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { collectionProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${if (totalSpecies == 0) 0 else (speciesCount * 100) / totalSpecies}% of all species collected",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Today's progress card ─────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Progress", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { todayProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$doneTasks / ${tasks.size} tasks done",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Your Pets heading ─────────────────────────────────────────────
            item {
                Text("Your Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (pets.isEmpty()) {
                item {
                    EmptyStateCard(
                        imageRes    = R.drawable.empty_collection,
                        title       = "No Pets Yet",
                        description = "Add your first pet to start building bonds and earning rewards.",
                        actionLabel = "Add a Pet",
                        onAction    = { navController.navigate("add_more_pet") }
                    )
                }
            } else {
                itemsIndexed(pets) { index, pet ->
                    val virtueInfo = VirtueConfig[pet.virtue]
                    val isVerified = pet.isVerified

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(pet.id) {
                        kotlinx.coroutines.delay(index * 60L)
                        visible = true
                    }
                    val cardAlpha by animateFloatAsState(
                        targetValue   = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label         = "pet_alpha_$index"
                    )
                    val offsetY by animateDpAsState(
                        targetValue   = if (visible) 0.dp else 16.dp,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label         = "pet_offset_$index"
                    )

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue   = if (isPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label         = "card_scale_$index"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                            .alpha(if (isVerified) cardAlpha else cardAlpha * 0.55f)
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
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ── Photo or emoji — no lock icons as placeholders ─
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier         = Modifier
                                        .size(52.dp)
                                        .clip(MaterialTheme.shapes.small),
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
                                        Text(petEmoji(pet.type.name), fontSize = 38.sp)
                                    }
                                }
                                if (!isVerified) {
                                    Surface(
                                        shape    = MaterialTheme.shapes.extraSmall,
                                        color    = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint               = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier           = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Pet name + species + verification status
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pet.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Text(
                                    pet.type.name.replace("_", " "),
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isVerified) {
                                    Spacer(Modifier.height(3.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            "Verification Required",
                                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Virtue emblem badge
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape    = MaterialTheme.shapes.small,
                                color    = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Image(
                                    painter            = painterResource(id = virtueInfo.emblemRes),
                                    contentDescription = "${pet.virtue.name} emblem",
                                    modifier           = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale       = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // Bond level + verification badge
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Lv.${pet.bondLevel}",
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isVerified)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize   = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                if (isVerified) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Row(
                                            modifier              = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.Shield,
                                                contentDescription = "Verified",
                                                tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier           = Modifier.size(10.dp)
                                            )
                                            Text(
                                                "Verified",
                                                fontSize   = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector        = Icons.Default.Lock,
                                        contentDescription = "Unverified",
                                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier           = Modifier.size(14.dp)
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
// EmptyStateCard — reusable artwork + title + description + button structure
// ---------------------------------------------------------------------------
@Composable
fun EmptyStateCard(
    imageRes: Int,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
                modifier           = Modifier.size(96.dp),
                contentScale       = ContentScale.Fit
            )
            Text(
                title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
