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
import com.example.petquest.data.model.PetEntity
import com.example.petquest.data.model.TaskEntity
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel

// Flat list item types for the LazyColumn
private sealed class TaskListItem {
    data class PetHeader(val pet: PetEntity, val done: Int, val total: Int) : TaskListItem()
    data class CoreDivider(val petId: Int) : TaskListItem()
    data class OptionalDivider(val petId: Int) : TaskListItem()
    data class Task(val task: TaskEntity) : TaskListItem()
    data class PetSpacer(val petId: Int) : TaskListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: PetQuestViewModel) {
    val tasks by viewModel.todaysTasks.collectAsState()
    val pets by viewModel.allPets.collectAsState()
    val doneCount = tasks.count { it.isCompleted }

    // Build flat list grouped by pet → core → optional
    val listItems = remember(pets, tasks) {
        buildList {
            pets.forEach { pet ->
                val petTasks = tasks.filter { it.petId == pet.id }
                if (petTasks.isEmpty()) return@forEach
                val core = petTasks.filter { it.type == TaskType.CORE }
                val optional = petTasks.filter { it.type == TaskType.OPTIONAL }
                val done = petTasks.count { it.isCompleted }

                add(TaskListItem.PetHeader(pet, done, petTasks.size))
                if (core.isNotEmpty()) {
                    add(TaskListItem.CoreDivider(pet.id))
                    core.forEach { add(TaskListItem.Task(it)) }
                }
                if (optional.isNotEmpty()) {
                    add(TaskListItem.OptionalDivider(pet.id))
                    optional.forEach { add(TaskListItem.Task(it)) }
                }
                add(TaskListItem.PetSpacer(pet.id))
            }
        }
    }

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
                modifier = Modifier.fillMaxSize().padding(padding),
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Global progress card
                item(key = "progress") {
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
                                Text("Today's Progress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "$doneCount / ${tasks.size} done",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { doneCount / tasks.size.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(listItems, key = { item ->
                    when (item) {
                        is TaskListItem.PetHeader -> "header_${item.pet.id}"
                        is TaskListItem.CoreDivider -> "core_div_${item.petId}"
                        is TaskListItem.OptionalDivider -> "opt_div_${item.petId}"
                        is TaskListItem.Task -> "task_${item.task.id}"
                        is TaskListItem.PetSpacer -> "spacer_${item.petId}"
                    }
                }) { item ->
                    when (item) {
                        is TaskListItem.PetHeader -> PetSectionHeader(item.pet, item.done, item.total)
                        is TaskListItem.CoreDivider -> TypeDivider("⚡ Core Tasks", "+10 pts",
                            MaterialTheme.colorScheme.primary)
                        is TaskListItem.OptionalDivider -> TypeDivider("💡 Optional", "+5 pts",
                            MaterialTheme.colorScheme.secondary)
                        is TaskListItem.Task -> TaskRow(
                            task = item.task,
                            isCore = item.task.type == TaskType.CORE,
                            onCheck = { viewModel.completeTask(item.task) }
                        )
                        is TaskListItem.PetSpacer -> Spacer(Modifier.height(10.dp))
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PetSectionHeader(pet: PetEntity, done: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(petEmoji(pet.type.name), fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                pet.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (done == total)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ) {
                Text(
                    "$done / $total",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (done == total)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypeDivider(label: String, pts: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color)
        Spacer(Modifier.width(6.dp))
        Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.15f)) {
            Text(
                pts,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, isCore: Boolean, onCheck: () -> Unit) {
    val isDone = task.isCompleted
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { if (!isDone) onCheck() },
                enabled = !isDone
            )
            Text(
                text = task.title,
                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                fontSize = 14.sp,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color = if (isDone)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (!isDone) {
                Text(
                    if (isCore) "+10" else "+5",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCore)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
