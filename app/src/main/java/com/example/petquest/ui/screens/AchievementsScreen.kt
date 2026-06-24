package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: PetQuestViewModel,
    onNavigateToTasks: () -> Unit = {}
) {
    val achievements         by viewModel.allAchievements.collectAsState()
    val collectionPercentage by viewModel.collectionPercentage.collectAsState()
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount    = achievements.size

    val emojiMap = mapOf(
        // ── Original ──────────────────────────────────────────────────────────
        "First Pet"          to "🐾",
        "First Verification" to "📷",
        "Pet Lover"          to "❤️",
        "Bond Master"        to "👑",
        "7-Day Streak"       to "🔥",
        "Epic Tamer"         to "🐉",
        "Rarity Hunter"      to "🎯",
        "Species Collector"  to "🗂️",
        "Animal Explorer"    to "🌍",
        // ── Streaks ───────────────────────────────────────────────────────────
        "3-Day Streak"       to "🔥",
        "30-Day Streak"      to "🔥",
        // ── Tasks ─────────────────────────────────────────────────────────────
        "Complete 25 Tasks"  to "✅",
        "Complete 50 Tasks"  to "✅",
        "Complete 100 Tasks" to "✅",
        "Complete 250 Tasks" to "✅",
        // ── Bond Points ───────────────────────────────────────────────────────
        "Earn 250 Bond Points"  to "⭐",
        "Earn 500 Bond Points"  to "⭐",
        "Earn 1000 Bond Points" to "⭐",
        // ── Pet Levels ────────────────────────────────────────────────────────
        "Reach Level 10" to "👑",
        "Reach Level 20" to "👑",
        // ── Pet Collection ────────────────────────────────────────────────────
        "Own 5 Pets"    to "❤️",
        "Verify 3 Pets" to "📷",
        "Verify 5 Pets" to "📷"
    )

    val overallProgress by animateFloatAsState(
        targetValue   = if (totalCount == 0) 0f else unlockedCount / totalCount.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "achievements_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (achievements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    imageRes    = R.drawable.empty_achievements,
                    title       = "No Achievements Yet",
                    description = "Add pets, complete tasks, and build bonds to earn achievements.",
                    actionLabel = "Start Your First Task",
                    onAction    = onNavigateToTasks
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Summary header ────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "$unlockedCount / $totalCount Unlocked",
                                fontSize   = 15.sp,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${if (totalCount == 0) 0 else (unlockedCount * 100) / totalCount}%",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Species Collected",
                                fontSize   = 13.sp,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${collectedSpecies.size}/29  ($collectionPercentage%)",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Achievement cards with staggered unlock animation ─────────────
            itemsIndexed(achievements) { index, a ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(a.title) {
                    kotlinx.coroutines.delay(index * 50L)
                    visible = true
                }

                val cardAlpha by animateFloatAsState(
                    targetValue   = if (visible) 1f else 0f,
                    animationSpec = tween(durationMillis = 250),
                    label         = "ach_alpha_$index"
                )

                var unlockAnimStarted by remember { mutableStateOf(false) }
                LaunchedEffect(a.isUnlocked) {
                    if (a.isUnlocked) {
                        unlockAnimStarted = false
                        kotlinx.coroutines.delay(50)
                        unlockAnimStarted = true
                    }
                }
                val unlockScale by animateFloatAsState(
                    targetValue   = if (a.isUnlocked && unlockAnimStarted) 1f else if (a.isUnlocked) 0.85f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "unlock_scale_$index"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardAlpha)
                        .scale(if (a.isUnlocked) unlockScale else 1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (a.isUnlocked)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(if (a.isUnlocked) 4.dp else 0.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier         = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (a.isUnlocked) {
                                Text(
                                    text     = emojiMap[a.title] ?: "⭐",
                                    fontSize = 36.sp
                                )
                            } else {
                                Icon(
                                    imageVector        = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                a.title,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp,
                                color      = if (a.isUnlocked)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (a.isUnlocked) a.description else "???",
                                fontSize = 13.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (a.isUnlocked) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Done",
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
