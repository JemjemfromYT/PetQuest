package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.viewmodel.PetQuestViewModel

// ── Achievement visual rarity tier ───────────────────────────────────────────
private enum class AchievementTier { COMMON, UNCOMMON, RARE, EPIC }

@Composable
private fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.COMMON   -> MaterialTheme.colorScheme.tertiary
    AchievementTier.UNCOMMON -> MaterialTheme.colorScheme.secondary
    AchievementTier.RARE     -> MaterialTheme.colorScheme.primary
    AchievementTier.EPIC     -> MaterialTheme.colorScheme.error
}

private fun tierLabel(tier: AchievementTier): String = when (tier) {
    AchievementTier.COMMON   -> "Common"
    AchievementTier.UNCOMMON -> "Uncommon"
    AchievementTier.RARE     -> "Rare"
    AchievementTier.EPIC     -> "Epic"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: PetQuestViewModel,
    onNavigateToTasks: () -> Unit = {}
) {
    val achievements by viewModel.allAchievements.collectAsState()

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount    = achievements.size

    val iconMap: Map<String, ImageVector> = mapOf(
        "First Pet"             to Icons.Default.Pets,
        "First Verification"    to Icons.Default.CameraAlt,
        "Pet Lover"             to Icons.Default.Favorite,
        "Bond Master"           to Icons.Default.EmojiEvents,
        "7-Day Streak"          to Icons.Default.Whatshot,
        "3-Day Streak"          to Icons.Default.Whatshot,
        "30-Day Streak"         to Icons.Default.Whatshot,
        "Epic Tamer"            to Icons.Default.Stars,
        "Rarity Hunter"         to Icons.Default.Explore,
        "Species Collector"     to Icons.Default.Explore,
        "Animal Explorer"       to Icons.Default.Explore,
        "Complete 25 Tasks"     to Icons.Default.CheckCircle,
        "Complete 50 Tasks"     to Icons.Default.CheckCircle,
        "Complete 100 Tasks"    to Icons.Default.CheckCircle,
        "Complete 250 Tasks"    to Icons.Default.CheckCircle,
        "Earn 250 Bond Points"  to Icons.Default.Star,
        "Earn 500 Bond Points"  to Icons.Default.Star,
        "Earn 1000 Bond Points" to Icons.Default.Star,
        "Reach Level 10"        to Icons.Default.EmojiEvents,
        "Reach Level 20"        to Icons.Default.EmojiEvents,
        "Own 5 Pets"            to Icons.Default.Favorite,
        "Verify 3 Pets"         to Icons.Default.CameraAlt,
        "Verify 5 Pets"         to Icons.Default.CameraAlt
    )

    val tierMap: Map<String, AchievementTier> = mapOf(
        "First Pet"             to AchievementTier.COMMON,
        "3-Day Streak"          to AchievementTier.COMMON,
        "Complete 25 Tasks"     to AchievementTier.COMMON,
        "First Verification"    to AchievementTier.COMMON,
        "Earn 250 Bond Points"  to AchievementTier.COMMON,
        "Pet Lover"             to AchievementTier.UNCOMMON,
        "Bond Master"           to AchievementTier.UNCOMMON,
        "7-Day Streak"          to AchievementTier.UNCOMMON,
        "Own 5 Pets"            to AchievementTier.UNCOMMON,
        "Verify 3 Pets"         to AchievementTier.UNCOMMON,
        "Complete 50 Tasks"     to AchievementTier.UNCOMMON,
        "Earn 500 Bond Points"  to AchievementTier.UNCOMMON,
        "Rarity Hunter"         to AchievementTier.UNCOMMON,
        "30-Day Streak"         to AchievementTier.RARE,
        "Species Collector"     to AchievementTier.RARE,
        "Reach Level 10"        to AchievementTier.RARE,
        "Complete 100 Tasks"    to AchievementTier.RARE,
        "Earn 1000 Bond Points" to AchievementTier.RARE,
        "Verify 5 Pets"         to AchievementTier.RARE,
        "Epic Tamer"            to AchievementTier.EPIC,
        "Animal Explorer"       to AchievementTier.EPIC,
        "Reach Level 20"        to AchievementTier.EPIC,
        "Complete 250 Tasks"    to AchievementTier.EPIC
    )

    val hintMap: Map<String, String> = mapOf(
        "First Pet"             to "Add your first pet",
        "First Verification"    to "Verify a pet with a photo",
        "Pet Lover"             to "Own 3 or more pets",
        "Bond Master"           to "Reach Bond Level 5",
        "7-Day Streak"          to "7 days in a row",
        "3-Day Streak"          to "3 days in a row",
        "30-Day Streak"         to "30 days in a row",
        "Epic Tamer"            to "Reach Bond Level 20",
        "Rarity Hunter"         to "Collect Rare or Epic species",
        "Species Collector"     to "Collect 10 species",
        "Animal Explorer"       to "Collect 20 species",
        "Complete 25 Tasks"     to "Complete 25 tasks",
        "Complete 50 Tasks"     to "Complete 50 tasks",
        "Complete 100 Tasks"    to "Complete 100 tasks",
        "Complete 250 Tasks"    to "Complete 250 tasks",
        "Earn 250 Bond Points"  to "Earn 250 Bond Points",
        "Earn 500 Bond Points"  to "Earn 500 Bond Points",
        "Earn 1000 Bond Points" to "Earn 1000 Bond Points",
        "Reach Level 10"        to "Reach Bond Level 10",
        "Reach Level 20"        to "Reach Bond Level 20",
        "Own 5 Pets"            to "Have 5 pets",
        "Verify 3 Pets"         to "Verify 3 pets",
        "Verify 5 Pets"         to "Verify 5 pets"
    )

    val overallProgress by animateFloatAsState(
        targetValue   = if (totalCount == 0) 0f else unlockedCount / totalCount.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "achievements_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Awards", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (achievements.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    imageRes    = R.drawable.empty_achievements,
                    title       = "No Awards Yet",
                    description = "Complete tasks and build bonds to earn awards.",
                    actionLabel = "Start a Task",
                    onAction    = onNavigateToTasks
                )
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            modifier              = Modifier.fillMaxSize().padding(padding),
            contentPadding        = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            // ── Compact progress header ───────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.weight(1f).height(6.dp)
                    )
                    Text(
                        "$unlockedCount / $totalCount",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Achievement badge tiles ───────────────────────────────────────
            itemsIndexed(achievements) { index, a ->
                val tier  = tierMap[a.title] ?: AchievementTier.COMMON
                val icon  = iconMap[a.title] ?: Icons.Default.Star
                val hint  = hintMap[a.title] ?: "Keep playing"
                val tColor = tierColor(tier)

                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(a.title) {
                    kotlinx.coroutines.delay(index * 35L)
                    visible = true
                }
                val cardAlpha by animateFloatAsState(
                    targetValue   = if (visible) 1f else 0f,
                    animationSpec = tween(durationMillis = 220),
                    label         = "ach_alpha_$index"
                )

                var unlockAnimStarted by remember { mutableStateOf(false) }
                LaunchedEffect(a.isUnlocked) {
                    if (a.isUnlocked) {
                        unlockAnimStarted = false
                        kotlinx.coroutines.delay(40)
                        unlockAnimStarted = true
                    }
                }
                val unlockScale by animateFloatAsState(
                    targetValue   = if (a.isUnlocked && unlockAnimStarted) 1f else if (a.isUnlocked) 0.82f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "unlock_scale_$index"
                )

                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .alpha(if (a.isUnlocked) cardAlpha else cardAlpha * 0.55f)
                        .scale(if (a.isUnlocked) unlockScale else 1f),
                    shape     = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(if (a.isUnlocked) 5.dp else 0.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = if (a.isUnlocked)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── Tier accent strip ─────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    if (a.isUnlocked) tColor else tColor.copy(alpha = 0.2f)
                                )
                        )

                        Spacer(Modifier.height(14.dp))

                        // ── Icon badge ────────────────────────────────────────
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape    = MaterialTheme.shapes.large,
                            color    = if (a.isUnlocked)
                                tColor.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (a.isUnlocked) {
                                    Icon(
                                        imageVector        = icon,
                                        contentDescription = a.title,
                                        tint               = tColor,
                                        modifier           = Modifier.size(32.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector        = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier           = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // ── Title ─────────────────────────────────────────────
                        Text(
                            text       = if (a.isUnlocked) a.title else hint,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp,
                            textAlign  = TextAlign.Center,
                            maxLines   = 2,
                            color      = if (a.isUnlocked)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier   = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(Modifier.height(6.dp))

                        // ── Tier label badge ──────────────────────────────────
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (a.isUnlocked)
                                tColor.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ) {
                            Text(
                                text       = tierLabel(tier),
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = if (a.isUnlocked) tColor
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
