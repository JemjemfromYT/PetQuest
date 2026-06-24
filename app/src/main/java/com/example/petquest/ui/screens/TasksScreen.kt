package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.data.model.TaskEntity
import com.example.petquest.data.model.TaskType
import com.example.petquest.viewmodel.PetQuestViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: PetQuestViewModel,
    onVerifyPet: (Int) -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val tasks  by viewModel.todaysTasks.collectAsState()
    val pets   by viewModel.allPets.collectAsState()
    val streak by viewModel.userStreak.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val safeTab = if (pets.isEmpty()) 0 else selectedTab.coerceIn(0, pets.lastIndex)

    // ── Streak change detection ───────────────────────────────────────────────
    var showStreakOverlay  by remember { mutableStateOf(false) }
    var overlayStreakValue by remember { mutableIntStateOf(streak) }
    var prevStreak        by remember { mutableIntStateOf(streak) }

    LaunchedEffect(streak) {
        if (streak > prevStreak && prevStreak >= 0) {
            overlayStreakValue = streak
            showStreakOverlay  = true
            delay(2800)
            showStreakOverlay  = false
        }
        prevStreak = streak
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            // ── Empty state: no pets ──────────────────────────────────────────
            if (pets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        imageRes    = R.drawable.empty_tasks,
                        title       = "No Tasks Yet",
                        description = "Add a pet and verify them to start receiving daily tasks.",
                        actionLabel = "Go to Profile",
                        onAction    = onNavigateToProfile
                    )
                }
                return@Scaffold
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // ── Pet tab row ───────────────────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex = safeTab,
                    edgePadding      = 16.dp,
                    containerColor   = MaterialTheme.colorScheme.primaryContainer
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
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Text(petEmoji(pet.type.name), fontSize = 24.sp)
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
                                        fontSize   = 12.sp,
                                        fontWeight = if (safeTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines   = 1
                                    )
                                    if (isLocked) {
                                        Text(
                                            "Locked",
                                            fontSize   = 10.sp,
                                            color      = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (petTasks.isNotEmpty()) {
                                        Text(
                                            if (allDone) "Done" else "$donePet/${petTasks.size}",
                                            fontSize = 10.sp,
                                            color    = if (allDone)
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

                // ── Tasks for selected pet ────────────────────────────────────
                val selectedPet = pets.getOrNull(safeTab)
                if (selectedPet == null) return@Column

                // ── Verification gate ─────────────────────────────────────────
                if (!selectedPet.isVerified) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
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
                                    painter           = painterResource(id = R.drawable.ic_locked),
                                    contentDescription = "Locked",
                                    modifier          = Modifier.size(56.dp),
                                    contentScale      = ContentScale.Fit
                                )
                                Text(
                                    "Verification Required",
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign  = TextAlign.Center
                                )
                                Text(
                                    "${selectedPet.name} must be verified before they can complete tasks and earn bond points.",
                                    fontSize  = 14.sp,
                                    color     = MaterialTheme.colorScheme.onErrorContainer,
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
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    return@Column
                }

                // ── Normal task list (verified pets only) ─────────────────────
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
                            imageRes    = R.drawable.empty_tasks,
                            title       = "All Done!",
                            description = "${selectedPet.name} has no tasks remaining for today.",
                            actionLabel = "Check Back Tomorrow",
                            onAction    = {}
                        )
                    }
                    return@Column
                }

                val taskProgress by animateFloatAsState(
                    targetValue   = doneCount / petTasks.size.toFloat(),
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label         = "task_progress"
                )

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Progress card ─────────────────────────────────────────
                    item(key = "progress") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${selectedPet.name}'s Progress",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 14.sp
                                    )
                                    Text(
                                        "$doneCount / ${petTasks.size} done",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 13.sp,
                                        color      = MaterialTheme.colorScheme.primary
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

                    // ── Daily reset countdown ─────────────────────────────────
                    item(key = "reset_timer") {
                        TaskResetTimer()
                    }

                    // ── Core tasks ────────────────────────────────────────────
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

                    // ── Optional tasks ────────────────────────────────────────
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

        // ── Streak celebration overlay — renders above everything ─────────────
        if (showStreakOverlay) {
            StreakCelebrationOverlay(streakCount = overlayStreakValue)
        }
    }
}

// ---------------------------------------------------------------------------
// Task Reset Timer
// ---------------------------------------------------------------------------
@Composable
private fun TaskResetTimer() {
    fun millisUntilMidnight(): Long {
        val now      = Calendar.getInstance()
        val midnight = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    var millisLeft by remember { mutableLongStateOf(millisUntilMidnight()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            millisLeft = millisUntilMidnight()
        }
    }

    val hours   = (millisLeft / (1000L * 60L * 60L)).toInt()
    val minutes = ((millisLeft / (1000L * 60L)) % 60L).toInt()
    val label   = if (hours > 0) "Tasks reset in ${hours}h ${minutes}m" else "Tasks reset in ${minutes}m"

    Text(
        text     = label,
        fontSize = 12.sp,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

// ---------------------------------------------------------------------------
// Streak Celebration Overlay
// ---------------------------------------------------------------------------
@Composable
private fun StreakCelebrationOverlay(streakCount: Int) {
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(streakCount) {
        phase = 0
        delay(30)
        phase = 1
        delay(2000)
        phase = 2
    }

    val scrimAlpha by animateFloatAsState(
        targetValue   = when (phase) { 1 -> 0.72f; else -> 0f },
        animationSpec = tween(durationMillis = if (phase == 2) 500 else 300),
        label         = "scrim_alpha"
    )
    val flameScale by animateFloatAsState(
        targetValue   = when (phase) { 1 -> 1f; else -> 0.5f },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        else
            tween(durationMillis = 400),
        label = "flame_scale"
    )
    val numberScale by animateFloatAsState(
        targetValue   = when (phase) { 1 -> 1f; else -> 0.3f },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        else
            tween(durationMillis = 350),
        label = "number_scale"
    )
    val textAlpha by animateFloatAsState(
        targetValue   = when (phase) { 1 -> 1f; else -> 0f },
        animationSpec = tween(
            durationMillis = if (phase == 1) 400 else 350,
            delayMillis    = if (phase == 1) 200 else 0
        ),
        label = "text_alpha"
    )
    val sparkle1Offset by animateIntOffsetAsState(
        targetValue   = when (phase) { 1 -> IntOffset(-80, -90); else -> IntOffset(0, 0) },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        else tween(300),
        label = "sparkle1"
    )
    val sparkle2Offset by animateIntOffsetAsState(
        targetValue   = when (phase) { 1 -> IntOffset(90, -70); else -> IntOffset(0, 0) },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        else tween(300),
        label = "sparkle2"
    )
    val sparkle3Offset by animateIntOffsetAsState(
        targetValue   = when (phase) { 1 -> IntOffset(0, -130); else -> IntOffset(0, 0) },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        else tween(300),
        label = "sparkle3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(scrimAlpha)
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {}

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(textAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 22.sp, modifier = Modifier.offset { sparkle1Offset })
                Text("⭐", fontSize = 18.sp, modifier = Modifier.offset { sparkle2Offset })
                Text("💫", fontSize = 20.sp, modifier = Modifier.offset { sparkle3Offset })
            }

            Box(contentAlignment = Alignment.Center) {
                Text("🔥", fontSize = 96.sp, modifier = Modifier.scale(flameScale))
                Text(
                    "$streakCount",
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    modifier   = Modifier.scale(numberScale).offset(y = 14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Day $streakCount Streak!",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.alpha(textAlpha)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (streakCount == 1) "You're on a roll — keep it up!"
                else "Amazing — $streakCount days in a row!",
                fontSize  = 15.sp,
                color     = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .padding(horizontal = 40.dp)
                    .alpha(textAlpha)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// TaskTypeHeader
// ---------------------------------------------------------------------------
@Composable
private fun TaskTypeHeader(
    label: String,
    pts: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.15f)) {
            Text(
                pts,
                modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ---------------------------------------------------------------------------
// TaskRow
// ---------------------------------------------------------------------------
@Composable
private fun TaskRow(task: TaskEntity, isCore: Boolean, onCheck: () -> Unit) {
    val isDone = task.isCompleted

    var checkAnimStarted by remember { mutableStateOf(isDone) }
    LaunchedEffect(isDone) {
        if (isDone) {
            checkAnimStarted = false
            delay(30)
            checkAnimStarted = true
        }
    }
    val checkScale by animateFloatAsState(
        targetValue   = if (isDone && checkAnimStarted) 1f else if (isDone) 0.88f else 1f,
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
                checked         = isDone,
                onCheckedChange = { if (!isDone) onCheck() },
                enabled         = !isDone
            )
            Text(
                text           = task.title,
                fontWeight     = if (isDone) FontWeight.Normal else FontWeight.Medium,
                fontSize       = 14.sp,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color          = if (isDone)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier       = Modifier.weight(1f)
            )
            if (!isDone) {
                Text(
                    if (isCore) "+10" else "+5",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isCore)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
