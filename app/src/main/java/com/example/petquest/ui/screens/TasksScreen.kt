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
import com.example.petquest.data.model.TaskEntity
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: PetQuestViewModel) {
    val tasks by viewModel.todaysTasks.collectAsState()
    val pets  by viewModel.allPets.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Clamp selected tab if pets change
    val safeTab = if (pets.isEmpty()) 0 else selectedTab.coerceIn(0, pets.lastIndex)

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
        if (pets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No tasks yet!\nAdd a pet to get started.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Pet tab row ──────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = safeTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                pets.forEachIndexed { index, pet ->
                    val petTasks  = tasks.filter { it.petId == pet.id }
                    val donePet   = petTasks.count { it.isCompleted }
                    val allDone   = petTasks.isNotEmpty() && donePet == petTasks.size

                    Tab(
                        selected = safeTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    petEmoji(pet.type.name),
                                    fontSize = 22.sp
                                )
                                Text(
                                    pet.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (safeTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                if (petTasks.isNotEmpty()) {
                                    Text(
                                        if (allDone) "✅ Done" else "$donePet/${petTasks.size}",
                                        fontSize = 10.sp,
                                        color = if (allDone)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ── Tasks for selected pet ───────────────────────────────────────
            val selectedPet = pets.getOrNull(safeTab)

            if (selectedPet == null) return@Column

            val petTasks  = tasks.filter { it.petId == selectedPet.id }
            val core      = petTasks.filter { it.type == TaskType.CORE }
            val optional  = petTasks.filter { it.type == TaskType.OPTIONAL }
            val doneCount = petTasks.count { it.isCompleted }

            if (petTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks for ${selectedPet.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Progress card
                item(key = "progress") {
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
                                    "${selectedPet.name}'s Progress",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "$doneCount / ${petTasks.size} done",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { doneCount / petTasks.size.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                }

                // Core tasks
                if (core.isNotEmpty()) {
                    item(key = "core_header") {
                        TaskTypeHeader(
                            emoji = "⚡",
                            label = "Core Tasks",
                            pts = "+10 pts",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(core, key = { "c_${it.id}" }) { task ->
                        TaskRow(task, isCore = true) { viewModel.completeTask(task) }
                    }
                }

                // Optional tasks
                if (optional.isNotEmpty()) {
                    item(key = "opt_header") {
                        Spacer(Modifier.height(4.dp))
                        TaskTypeHeader(
                            emoji = "💡",
                            label = "Optional Tasks",
                            pts = "+5 pts",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(optional, key = { "o_${it.id}" }) { task ->
                        TaskRow(task, isCore = false) { viewModel.completeTask(task) }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun TaskTypeHeader(
    emoji: String,
    label: String,
    pts: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.15f)) {
            Text(
                pts,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                fontSize = 11.sp,
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
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
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
