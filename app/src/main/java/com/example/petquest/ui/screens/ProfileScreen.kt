package com.example.petquest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.model.PetEntity
import com.example.petquest.data.model.PetType
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PetQuestViewModel,
    onAddPetClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    val pets               by viewModel.allPets.collectAsState()
    val achievements       by viewModel.allAchievements.collectAsState()
    val userLevel          by viewModel.userLevel.collectAsState()
    val totalBondPoints    by viewModel.totalBondPoints.collectAsState()
    val streak             by viewModel.userStreak.collectAsState()
    val collectedSpecies   by viewModel.collectedSpecies.collectAsState()
    val collectionPercentage by viewModel.collectionPercentage.collectAsState()

    val unlockedCount  = achievements.count { it.isUnlocked }
    val speciesCount   = collectedSpecies.size
    val totalSpecies   = PetType.entries.size

    // Secret: tap the Level badge 5 times within 3 seconds to open Admin Mode
    var tapCount      by remember { mutableIntStateOf(0) }
    var lastTapMs     by remember { mutableLongStateOf(0L) }
    var showAdminHint by remember { mutableStateOf(false) }

    fun onLevelTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapMs > 3000L) tapCount = 0
        tapCount++
        lastTapMs = now
        if (tapCount >= 5) {
            tapCount = 0
            onAdminClick()
        } else if (tapCount >= 3) {
            showAdminHint = true
        }
    }

    LaunchedEffect(showAdminHint) {
        if (showAdminHint) {
            kotlinx.coroutines.delay(1500)
            showAdminHint = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAdminClick) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Admin",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Level card ─────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧑‍🤝‍🐾", fontSize = 56.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.clickable { onLevelTap() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Level $userLevel",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (showAdminHint) {
                                    Text(
                                        "🔧 Keep tapping...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (totalBondPoints % 100) / 100f },
                            modifier = Modifier.fillMaxWidth().height(10.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${totalBondPoints % 100} / 100 XP to Level ${userLevel + 1}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Stats row ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(Modifier.weight(1f), "🔥", "$streak",        "Streak")
                    StatCard(Modifier.weight(1f), "⭐", "$totalBondPoints","Bond Pts")
                    StatCard(Modifier.weight(1f), "🏆", "$unlockedCount", "Awards")
                }
            }

            // ── Collection progress card (V1.2) ────────────────────────────
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
                                "🐾 Species Collected",
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
                            progress = { if (totalSpecies == 0) 0f else speciesCount / totalSpecies.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$collectionPercentage% complete  •  Tap Collection tab to explore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Pet collection header ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🐾 My Pets (${pets.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(onClick = onAddPetClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Pet")
                    }
                }
            }

            if (pets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No pets yet! Tap + to add one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    val rows = pets.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowPets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowPets.forEach { pet ->
                                    PetCollectionCard(
                                        pet = pet,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowPets.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PetCollectionCard(pet: PetEntity, modifier: Modifier = Modifier) {
    val rarityColor = when (pet.type.rarity.name) {
        "COMMON"   -> MaterialTheme.colorScheme.tertiary
        "UNCOMMON" -> MaterialTheme.colorScheme.secondary
        "RARE"     -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(petEmoji(pet.type.name), fontSize = 38.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    pet.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (pet.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Text("✅", fontSize = 12.sp)
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = rarityColor.copy(alpha = 0.15f)
            ) {
                Text(
                    pet.type.rarity.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "Lv.${pet.bondLevel}  •  ${pet.bondPoints} pts",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { (pet.bondPoints % 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )

            Text(
                pet.personality.name,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
