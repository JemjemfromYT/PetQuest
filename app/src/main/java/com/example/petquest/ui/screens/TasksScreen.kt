// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/TasksScreen.kt
// FULL REPLACEMENT
// Changes vs previous version:
//   - SoundManager.playTaskComplete() called when any task is checked
//   - SoundManager.playStreak() called when streak increases
//   - No other changes — all existing UI logic preserved exactly
// ============================================================

package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.SoundManager
import com.example.petquest.data.model.TaskEntity
import com.example.petquest.data.model.TaskType
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.PetQuestViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

// ---------------------------------------------------------------------------
// Virtue colour lookup
// ---------------------------------------------------------------------------
private fun virtueAccentColor(virtueName: String): Color = when (virtueName.uppercase()) {
    "COURAGE"    -> Color(0xFFC62828)
    "JUSTICE"    -> Color(0xFF1565C0)
    "TEMPERANCE" -> Color(0xFF2E7D32)
    "WISDOM"     -> Color(0xFF6A1B9A)
    "LOYALTY"    -> Color(0xFFE65100)
    else         -> Color(0xFF37474F)
}

private fun virtueAccentBg(virtueName: String, isDark: Boolean): Color {
    if (isDark) return when (virtueName.uppercase()) {
        "COURAGE"    -> Color(0xFF4A1515)
        "JUSTICE"    -> Color(0xFF0D2D4A)
        "TEMPERANCE" -> Color(0xFF0D2E1A)
        "WISDOM"     -> Color(0xFF2A1040)
        "LOYALTY"    -> Color(0xFF3D1F00)
        else         -> Color(0xFF1E1E1E)
    }
    return when (virtueName.uppercase()) {
        "COURAGE"    -> Color(0xFFFFEBEE)
        "JUSTICE"    -> Color(0xFFE3F2FD)
        "TEMPERANCE" -> Color(0xFFE8F5E9)
        "WISDOM"     -> Color(0xFFF3E5F5)
        "LOYALTY"    -> Color(0xFFFFF3E0)
        else         -> Color(0xFFECEFF1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel           : PetQuestViewModel,
    onVerifyPet         : (Int) -> Unit,
    onNavigateToProfile : () -> Unit = {}
) {
    val tasks  by viewModel.todaysTasks.collectAsState()
    val pets   by viewModel.allPets.collectAsState()
    val streak by viewModel.userStreak.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val safeTab = if (pets.isEmpty()) 0 else selectedTab.coerceIn(0, pets.lastIndex)

    var showStreakOverlay  by remember { mutableStateOf(false) }
    var overlayStreakValue by remember { mutableIntStateOf(streak) }
    var prevStreak        by remember { mutableIntStateOf(streak) }

    LaunchedEffect(streak) {
        if (streak > prevStreak && prevStreak >= 0) {
            overlayStreakValue = streak
            SoundManager.playStreak()          // 🔊 streak sound
            showStreakOverlay  = true
            delay(2800)
            showStreakOverlay  = false
        }
        prevStreak = streak
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF8C42), Color(0xFFFFB77A))
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Today's Tasks",
                        fontSize      = 24.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        ) { padding ->
            if (pets.isEmpty()) {
                Box(
                    modifier         = Modifier
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
                                        Box(
                                            modifier         = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (pet.photoUri != null) {
                                                AsyncImage(
                                                    model              = pet.photoUri,
                                                    contentDescription = pet.name,
                                                    modifier           = Modifier.fillMaxSize(),
                                                    contentScale       = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    pet.name.take(1).uppercase(),
                                                    fontSize   = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        if (isLocked) {
                                            Icon(
                                                imageVector        = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint               = MaterialTheme.colorScheme.error,
                                                modifier           = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        pet.name,
                                        fontSize   = 11.sp,
                                        fontWeight = if (safeTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines   = 1
                                    )
                                    when {
                                        isLocked           -> Text(
                                            "Locked",
                                            fontSize   = 10.sp,
                                            color      = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        petTasks.isEmpty() -> {}
                                        allDone            -> Text(
                                            "✓ Done",
                                            fontSize   = 10.sp,
                                            color      = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        else               -> Text(
                                            "$donePet/${petTasks.size}",
                                            fontSize = 10.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                val selectedPet = pets.getOrNull(safeTab)
                if (selectedPet == null) return@Column

                // ── Verification gate ─────────────────────────────────────────
                if (!selectedPet.isVerified) {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "${selectedPet.name} needs a photo",
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign  = TextAlign.Center
                                )
                                Text(
                                    "Snap a quick photo of ${selectedPet.name} to unlock tasks and bond points.",
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

                // ── Task list (verified pets only) ────────────────────────────
                val petTasks  = tasks.filter { it.petId == selectedPet.id }
                val core      = petTasks.filter { it.type == TaskType.CORE }
                val virtue    = petTasks.filter { it.type == TaskType.VIRTUE }
                val optional  = petTasks.filter { it.type == TaskType.OPTIONAL }
                val doneCount = petTasks.count { it.isCompleted }
                val allDone   = petTasks.isNotEmpty() && doneCount == petTasks.size

                val isDark      = isSystemInDarkTheme()
                val virtueColor = virtueAccentColor(selectedPet.virtue.name)
                val virtueBg    = virtueAccentBg(selectedPet.virtue.name, isDark)

                if (petTasks.isEmpty()) {
                    Box(
                        modifier         = Modifier
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

                val virtueInfo = VirtueConfig[selectedPet.virtue]

                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "progress") {
                        ProgressCard(
                            progress = taskProgress,
                            done     = doneCount,
                            total    = petTasks.size,
                            petName  = selectedPet.name,
                            allDone  = allDone
                        )
                    }

                    item(key = "reset_timer") {
                        TaskResetTimer()
                    }

                    if (core.isNotEmpty()) {
                        item(key = "core_header") {
                            Spacer(Modifier.height(6.dp))
                            SectionHeader(
                                label = "Core Tasks",
                                pts   = "+10 pts each",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(core, key = { "c_${it.id}" }) { task ->
                            TaskRow(
                                task        = task,
                                accentColor = MaterialTheme.colorScheme.primary
                            ) {
                                viewModel.completeTask(task)
                                SoundManager.playTaskComplete()   // 🔊
                            }
                        }
                    }

                    if (virtue.isNotEmpty()) {
                        item(key = "virtue_header") {
                            Spacer(Modifier.height(6.dp))
                            VirtueSectionHeader(
                                virtueName = selectedPet.virtue.name,
                                emblemRes  = virtueInfo.emblemRes,
                                color      = virtueColor,
                                bgColor    = virtueBg
                            )
                        }
                        items(virtue, key = { "v_${it.id}" }) { task ->
                            TaskRow(
                                task        = task,
                                accentColor = virtueColor,
                                cardBg      = virtueBg.copy(alpha = 0.5f),
                                emblemRes   = virtueInfo.emblemRes
                            ) {
                                viewModel.completeTask(task)
                                SoundManager.playTaskComplete()   // 🔊
                            }
                        }
                    }

                    if (optional.isNotEmpty()) {
                        item(key = "opt_header") {
                            Spacer(Modifier.height(6.dp))
                            SectionHeader(
                                label = "Optional Tasks",
                                pts   = "+5 pts each",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        items(optional, key = { "o_${it.id}" }) { task ->
                            TaskRow(
                                task        = task,
                                accentColor = MaterialTheme.colorScheme.secondary
                            ) {
                                viewModel.completeTask(task)
                                SoundManager.playTaskComplete()   // 🔊
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        if (showStreakOverlay) {
            StreakCelebrationOverlay(streakCount = overlayStreakValue)
        }
    }
}

// ---------------------------------------------------------------------------
// ProgressCard
// ---------------------------------------------------------------------------
@Composable
private fun ProgressCard(
    progress : Float,
    done     : Int,
    total    : Int,
    petName  : String,
    allDone  : Boolean
) {
    val isDarkProgress = isSystemInDarkTheme()
    val doneContainer  = if (isDarkProgress) Color(0xFF0D2E1A) else Color(0xFFE8F5E9)
    val doneText       = if (isDarkProgress) Color(0xFF81C784) else Color(0xFF1B5E20)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = if (allDone) doneContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    if (allDone) "All done for $petName! 🎉" else "$petName's Progress",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (allDone) doneText
                    else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (allDone) doneText.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                ) {
                    Text(
                        "$done / $total done",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (allDone) doneText
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            if (allDone)
                                Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF81C784)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFBF360C), Color(0xFFFF8C42)))
                        )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TaskResetTimer
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
    val label   = if (hours > 0) "Resets in ${hours}h ${minutes}m" else "Resets in ${minutes}m"

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                label,
                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SectionHeader
// ---------------------------------------------------------------------------
@Composable
private fun SectionHeader(label: String, pts: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f))))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 15.sp,
            color      = color,
            modifier   = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                pts,
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ---------------------------------------------------------------------------
// VirtueSectionHeader
// ---------------------------------------------------------------------------
@Composable
private fun VirtueSectionHeader(
    virtueName : String,
    emblemRes  : Int,
    color      : Color,
    bgColor    : Color
) {
    Surface(
        shape    = MaterialTheme.shapes.small,
        color    = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f))))
            )
            Spacer(Modifier.width(10.dp))
            Image(
                painter            = painterResource(id = emblemRes),
                contentDescription = "$virtueName emblem",
                modifier           = Modifier.size(24.dp),
                contentScale       = ContentScale.Fit
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${virtueName.lowercase().replaceFirstChar { it.uppercase() }} Task",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 15.sp,
                color      = color,
                modifier   = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    "+10 pts each",
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TaskRow
// ---------------------------------------------------------------------------
@Composable
private fun TaskRow(
    task        : TaskEntity,
    accentColor : Color,
    cardBg      : Color  = Color.Unspecified,
    emblemRes   : Int?   = null,
    onCheck     : () -> Unit
) {
    val isDone = task.isCompleted

    val cardAlpha by animateFloatAsState(
        targetValue   = if (isDone) 0.6f else 1f,
        animationSpec = tween(durationMillis = 300),
        label         = "task_alpha_${task.id}"
    )

    val resolvedBg = when {
        isDone                      -> MaterialTheme.colorScheme.surfaceVariant
        cardBg != Color.Unspecified -> cardBg
        else                        -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = resolvedBg),
        elevation = CardDefaults.cardElevation(if (isDone) 0.dp else 2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (isDone) accentColor.copy(alpha = 0.25f) else accentColor)
            )

            Checkbox(
                checked         = isDone,
                onCheckedChange = { if (!isDone) onCheck() },
                enabled         = !isDone,
                colors          = CheckboxDefaults.colors(
                    checkedColor           = accentColor,
                    checkmarkColor         = Color.White,
                    uncheckedColor         = accentColor.copy(alpha = 0.55f),
                    disabledCheckedColor   = accentColor.copy(alpha = 0.38f),
                    disabledUncheckedColor = accentColor.copy(alpha = 0.18f)
                )
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
                modifier       = Modifier
                    .weight(1f)
                    .padding(end = 4.dp, top = 14.dp, bottom = 14.dp)
            )

            if (!isDone && emblemRes != null) {
                Spacer(Modifier.width(6.dp))
                Image(
                    painter            = painterResource(id = emblemRes),
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp).padding(end = 10.dp),
                    contentScale       = ContentScale.Fit
                )
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StreakCelebrationOverlay
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
        else tween(400),
        label = "flame_scale"
    )
    val numberScale by animateFloatAsState(
        targetValue   = when (phase) { 1 -> 1f; else -> 0.3f },
        animationSpec = if (phase == 1)
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        else tween(350),
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
        modifier         = Modifier
            .fillMaxSize()
            .alpha(scrimAlpha)
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {}

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier         = Modifier.size(120.dp).alpha(textAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 80.sp, modifier = Modifier.scale(flameScale))
            }
            Text(
                "$streakCount",
                fontSize   = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                modifier   = Modifier.scale(numberScale).alpha(textAlpha)
            )
            Text(
                "Day Streak!",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                modifier   = Modifier.alpha(textAlpha)
            )
        }

        Box(
            modifier         = Modifier.fillMaxSize().alpha(textAlpha),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 28.sp, modifier = Modifier.offset { sparkle1Offset })
            Text("⭐", fontSize = 22.sp, modifier = Modifier.offset { sparkle2Offset })
            Text("✨", fontSize = 20.sp, modifier = Modifier.offset { sparkle3Offset })
        }
    }
}
