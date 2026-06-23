package com.example.petquest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.petquest.data.model.PetType
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel

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

// StatCard — emoji param removed; value + label only
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
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
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = contentColor)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
            // ── Stat cards row ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(Modifier.weight(1f), "$streak",         "Streak",   dimmed = doneTasks == 0)
                    StatCard(Modifier.weight(1f), "$totalBondPoints","Bond Pts")
                    StatCard(Modifier.weight(1f), "Lv.$userLevel",   "Level")
                }
            }

            // ── Collection progress card ─────────────────────────────────────
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
                            progress = { if (totalSpecies == 0) 0f else speciesCount / totalSpecies.toFloat() },
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

            // ── Today's progress card ───────────────────────────────────────
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
                            progress = { if (tasks.isEmpty()) 0f else doneTasks / tasks.size.toFloat() },
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

            // ── Your Pets heading ───────────────────────────────────────────
            item {
                Text("Your Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (pets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🐾", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No pets yet — tap Profile to add one!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pets) { pet ->
                    val virtueInfo = VirtueConfig[pet.virtue]
                    val isVerified = pet.isVerified

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isVerified) 1f else 0.55f)
                            .clickable { navController.navigate("pet_detail/${pet.id}") },
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
                            // Pet species emoji with lock overlay for unverified
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Text(petEmoji(pet.type.name), fontSize = 38.sp)
                                if (!isVerified) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
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
                                            "Verification Required",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Virtue emblem only — no title beneath
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
                                    color = if (isVerified)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                if (isVerified) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "Verified",
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                "Verified",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Unverified",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
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
