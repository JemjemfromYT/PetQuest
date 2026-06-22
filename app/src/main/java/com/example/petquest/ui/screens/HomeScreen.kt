package com.example.petquest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.petquest.data.model.PetEntity
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PetQuestViewModel, navController: NavController) {
    val pets by viewModel.allPets.collectAsState()
    val tasks by viewModel.todaysTasks.collectAsState()
    val streak by viewModel.userStreak.collectAsState()
    val bondPoints by viewModel.totalBondPoints.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("PetQuest", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) },
        floatingActionButton = {
            FloatingActionButton({ navController.navigate("add_more_pet") }) {
                Icon(Icons.Default.Add, "Add Pet")
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(Modifier.weight(1f), "🔥", "$streak", "Streak")
                    StatCard(Modifier.weight(1f), "⭐", "$bondPoints", "Bond Pts")
                    StatCard(Modifier.weight(1f), "✅", "${tasks.count { it.isCompleted }}/${tasks.size}", "Tasks")
                }
            }
            item { Text("My Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            if (pets.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(24.dp).fillMaxWidth(), Alignment.CenterHorizontally) {
                            Text("🐾", fontSize = 48.sp); Spacer(Modifier.height(8.dp))
                            Text("No pets yet — tap + to add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pets) { pet -> PetSummaryCard(pet) { navController.navigate("pet_detail/${pet.id}") } }
                    }
                }
            }
            if (tasks.isNotEmpty()) {
                item { Text("Today's Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                items(tasks.take(4)) { task ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (task.isCompleted) "✅" else "⬜", fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(task.title, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, emoji: String, value: String, label: String) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PetSummaryCard(pet: PetEntity, onClick: () -> Unit) {
    Card(Modifier.width(140.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(12.dp), Alignment.CenterHorizontally) {
            Text(petEmoji(pet.type.name), fontSize = 40.sp); Spacer(Modifier.height(6.dp))
            Text(pet.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(pet.type.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Lv.${pet.bondLevel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (pet.isVerified) Text("✅ Verified", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

fun petEmoji(typeName: String) = when (typeName) {
    "DOG" -> "🐶"; "CAT" -> "🐱"; "RABBIT" -> "🐰"
    "BIRD" -> "🐦"; "CHICKEN" -> "🐔"; "TURTLE" -> "🐢"
    else -> "🐾"
}