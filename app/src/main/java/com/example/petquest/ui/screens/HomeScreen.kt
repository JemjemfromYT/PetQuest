package com.example.petquest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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

@Composable
fun StatCard(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PetQuestViewModel, navController: NavController) {
    val pets by viewModel.allPets.collectAsState()
    val tasks by viewModel.todaysTasks.collectAsState()
    val streak by viewModel.userStreak.collectAsState()
    val totalBondPoints by viewModel.totalBondPoints.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()

    val doneTasks = tasks.count { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PetQuest 🐾", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(Modifier.weight(1f), "🔥", "$streak", "Streak")
                    StatCard(Modifier.weight(1f), "⭐", "$totalBondPoints", "Bond Pts")
                    StatCard(Modifier.weight(1f), "🎖️", "Lv.$userLevel", "Level")
                }
            }

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

            item {
                Text("Your Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (pets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("pet_detail/${pet.id}") },
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(petEmoji(pet.type.name), fontSize = 40.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pet.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(
                                    "${pet.type.name.replace("_", " ")}  •  ${pet.personality.name}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Lv.${pet.bondLevel}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (pet.isVerified) Text("✅", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
