package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: PetQuestViewModel) {
    val tasks by viewModel.todaysTasks.collectAsState()
    val pets by viewModel.allPets.collectAsState()
    val core = tasks.filter { it.type == TaskType.CORE }
    val optional = tasks.filter { it.type == TaskType.OPTIONAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Tasks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Add a pet to generate tasks!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val done = tasks.count { it.isCompleted }
                    LinearProgressIndicator(
                        progress = { if (tasks.isEmpty()) 0f else done / tasks.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$done / ${tasks.size} completed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (core.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("⚡ Core Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("+10 pts each", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(core) { task ->
                        val pet = pets.find { it.id == task.petId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(if (task.isCompleted) 0.dp else 2.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(petEmoji(pet?.type?.name ?: ""), fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface)
                                    Text(pet?.name ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { if (!task.isCompleted) viewModel.completeTask(task) },
                                    enabled = !task.isCompleted
                                )
                            }
                        }
                    }
                }

                if (optional.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("💡 Optional Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("+5 pts each", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(optional) { task ->
                        val pet = pets.find { it.id == task.petId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(if (task.isCompleted) 0.dp else 2.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(petEmoji(pet?.type?.name ?: ""), fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface)
                                    Text(pet?.name ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { if (!task.isCompleted) viewModel.completeTask(task) },
                                    enabled = !task.isCompleted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}