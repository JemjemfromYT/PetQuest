package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.data.model.TaskEntity
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: PetQuestViewModel,
    onVerifyPet: (Int) -> Unit
) {
    val tasks by viewModel.todaysTasks.collectAsState()
    val pets  by viewModel.allPets.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

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
        // ── Empty state: no pets ──────────────────────────────────────────────
        if (pets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    imageRes = R.drawable.empty_tasks,
                    title = "No Tasks Yet",
                    description = "Add a pet and verify them to start receiving daily tasks.",
                    actionLabel = "Go to Profile",
                    onAction = {}
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Pet tab row ───────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = safeTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                pets.forEachIndexed { index, pet ->
                    val petTasks = tasks.filter { it.petId == pet.id }
                    val donePet  = petTasks.count { it.isCompleted }
                    val allDone  = petTasks.isNotEmpty() && donePet == petTasks.size
                    val isLocked = !pet.isVerified

                    Tab(
                        selected = safeTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Virtue emblem as tab identity
                                val virtueInfo = com.example.petquest.ui.VirtueConfig[pet.virtue]
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Image(
                                        painter = painterResource(id = virtueInfo.emblemRes),
                                        contentDescription = "${pet.virtue.name} emblem",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (isLocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    pet.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (safeTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                if (isLocked) {
                                    Text(
                                        "Locked",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (petTasks.isNotEmpty()) {
                                    Text(
                                        if (allDone) "Done" else "$donePet/${petTasks.size}",
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

            // ── Tasks for selected pet ────────────────────────────────────────
            val selectedPet = pets.getOrNull(safeTab)
            if (selectedPet == null) return@Column

            // ── Verification gate ─────────────────────────────────────────────
            if (!selectedPet.isVerified) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_locked),
                                contentDescription = "Locked",
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )

                            Text(
                                "Verification Required",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                "${selectedPet.name} must be verified before they can complete tasks and earn bond points.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick  = { onVerifyPet(selectedPet.id) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Verify ${selectedPet.name}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                return@Column
            }

            // ── Normal task list (verified pets only) ─────────────────────────
            val petTasks  = tasks.filter { it.petId == selectedPet.id }
            val core      = petTasks.filter { it.type == TaskType.CORE }
            val optional  = petTasks.filter { it.type == TaskType.OPTIONAL }
            val doneCount = petTasks.count { it.isCompleted }

            if (petTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        imageRes = R.drawable.empty_tasks,
                        title = "All Done!",
                        description = "${selectedPet.name} has no tasks remaining for today.",
                        actionLabel = "Check Back Tomorrow",
                        onAction = {}
                    )
                }
                return@Column
            }

            // Animated progress
            val taskProgress by animateFloatAsState(
                targetValue = doneCount / petTasks.size.toFloat(),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "task_progress"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                progress = { taskProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                        }
                    }
                }

                if (core.isNotEmpty()) {
                    item(key = "core_header") {
                        TaskTypeHeader(
                            label = "Core Tasks",
                            pts   = "+10 pts",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(core, key = { "c_${it.id}" }) { task ->
                        TaskRow(task, isCore = true) { viewModel.completeTask(task) }
                    }
                }

                if (optional.isNotEmpty()) {
                    item(key = "opt_header") {
                        Spacer(Modifier.height(4.dp))
                        TaskTypeHeader(
                            label = "Optional Tasks",
                            pts   = "+5 pts",
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
    label: String,
    pts: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
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

    // Completion scale pop
    var checkAnimStarted by remember { mutableStateOf(isDone) }
    LaunchedEffect(isDone) {
        if (isDone) {
            checkAnimStarted = false
            kotlinx.coroutines.delay(30)
            checkAnimStarted = true
        }
    }
    val checkScale by animateFloatAsState(
        targetValue = if (isDone && checkAnimStarted) 1f else if (isDone) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "check_scale_${task.id}"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(checkScale),
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
