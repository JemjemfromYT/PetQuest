package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: PetQuestViewModel) {
    val tasks by viewModel.todaysTasks.collectAsState()
    val pets  by viewModel.allPets.collectAsState()
    val core     = tasks.filter { it.type == TaskType.CORE }
    val optional = tasks.filter { it.type == TaskType.OPTIONAL }
    val doneCount = tasks.count { it.isCompleted }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No tasks yet!\nAdd a pet to get started.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Progress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "$doneCount / ${tasks.size} done",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (tasks.isEmpty()) 0f else doneCount / tasks.size.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (core.isNotEmpty()) {
                    item {
                        SectionHeader(
                            emoji = "⚡", label = "Core Tasks", pts = "+10 pts",
                            color = MaterialTheme.colorScheme.primary,
                            onColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    items(core, key = { it.id }) { task ->
                        val pet = pets.find { it.id == task.petId }
                        TaskCard(
                            title = task.title,
                            petLabel = "${petEmoji(pet?.type?.name ?: "")} ${pet?.name ?: ""}",
                            isDone = task.isCompleted,
                            onCheck = { viewModel.completeTask(task) }
                        )
                    }
                }

                if (optional.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(
                            emoji = "💡", label = "Optional Tasks", pts = "+5 pts",
                            color = MaterialTheme.colorScheme.secondary,
                            onColor = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    items(optional, key = { it.id }) { task ->
                        val pet = pets.find { it.id == task.petId }
                        TaskCard(
                            title = task.title,
                            petLabel = "${petEmoji(pet?.type?.name ?: "")} ${pet?.name ?: ""}",
                            isDone = task.isCompleted,
                            onCheck = { viewModel.completeTask(task) }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    emoji: String,
    label: String,
    pts: String,
    color: androidx.compose.ui.graphics.Color,
    onColor: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Surface(shape = MaterialTheme.shapes.small, color = color) {
            Text(
                pts,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                color = onColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    petLabel: String,
    isDone: Boolean,
    onCheck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { if (!isDone) onCheck() },
                enabled = !isDone
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = if (isDone)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = petLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}