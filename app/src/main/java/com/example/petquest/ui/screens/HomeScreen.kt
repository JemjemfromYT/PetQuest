package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.petquest.R
import com.example.petquest.data.model.PetType
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel

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
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    contentScale = ContentScale.Fit
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

    // Animated collection progress
    val collectionProgress by animateFloatAsState(
        targetValue = if (totalSpecies == 0) 0f else speciesCount / totalSpecies.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "collection_progress"
    )

    // Animated today's progress
    val todayProgress by animateFloatAsState(
        targetValue = if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "today_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PetQuest", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Stat cards row ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "$streak",
                        label = "Streak",
                        iconRes = R.drawable.ic_streak,
                        dimmed = doneTasks == 0
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "$totalBondPoints",
                        label = "Bond Pts",
                        iconRes = R.drawable.ic_bondpoints
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "Lv.$userLevel",
                        label = "Level",
                        iconRes = R.drawable.ic_level
                    )
                }
            }

            // ── Collection progress card ──────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Species Collection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "$speciesCount / $totalSpecies",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Today's progress card ─────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        imageRes = R.drawable.empty_collection,
                        title = "No Pets Yet",
                        description = "Add your first pet to start building bonds and earning rewards.",
                        actionLabel = "Add a Pet",
                        onAction = { navController.navigate("profile") }
                    )
                }
            } else {
                // ── Animated pet cards ────────────────────────────────────────
                itemsIndexed(pets) { index, pet ->
                    val virtueInfo = VirtueConfig[pet.virtue]
                    val isVerified = pet.isVerified

                    // Staggered fade-in + slide-up
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(pet.id) {
                        kotlinx.coroutines.delay(index * 60L)
                        visible = true
                    }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "pet_alpha_$index"
                    )
                    val offsetY by animateDpAsState(
                        targetValue = if (visible) 0.dp else 16.dp,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "pet_offset_$index"
                    )

                    // Press scale feedback
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "card_scale_$index"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = offsetY)
                            .alpha(if (isVerified) alpha else alpha * 0.55f)
                            .scale(cardScale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { navController.navigate("pet_detail/${pet.id}") },
                        elevation = CardDefaults.cardElevation(if (isVerified) 3.dp else 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVerified)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ── Lock icon for unverified; empty space for verified ──
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isVerified) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_locked),
                                        contentDescription = "Locked",
                                        modifier = Modifier.size(32.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    // Verified check icon
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_verified),
                                        contentDescription = "Verified",
                                        modifier = Modifier.size(32.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Pet name + species
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pet.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    pet.type.name.replace("_", " "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!isVerified) {
                                    Spacer(Modifier.height(3.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            "Unverified",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Virtue emblem — visually dominant
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Image(
                                    painter = painterResource(id = virtueInfo.emblemRes),
                                    contentDescription = "${pet.virtue.name} emblem",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // Bond level
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Lv.${pet.bondLevel}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVerified)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    pet.virtue.name,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
