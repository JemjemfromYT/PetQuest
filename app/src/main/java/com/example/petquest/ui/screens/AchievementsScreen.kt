package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.petquest.data.model.AchievementEntity
import com.example.petquest.viewmodel.PetQuestViewModel

// ── Progression colors (no rarity text) ──────────────────────────────────────
private val AchievementColorRed    = Color(0xFFE53935)
private val AchievementColorGreen  = Color(0xFF43A047)
private val AchievementColorOrange = Color(0xFFFB8C00)
private val AchievementColorPurple = Color(0xFF7B1FA2)
private val AchievementColorBlue   = Color(0xFF1E88E5)

private fun achievementColor(title: String): Color = when (title) {
    // Streak: Red → Green → Orange → Purple
    "3-Day Streak"          -> AchievementColorRed
    "7-Day Streak"          -> AchievementColorGreen
    "14-Day Streak"         -> AchievementColorGreen
    "30-Day Streak"         -> AchievementColorOrange
    "60-Day Streak"         -> AchievementColorOrange
    "100-Day Streak"        -> AchievementColorPurple
    // Tasks: Red → Green → Orange → Purple
    "Complete 10 Tasks"     -> AchievementColorRed
    "Complete 25 Tasks"     -> AchievementColorRed
    "Complete 50 Tasks"     -> AchievementColorGreen
    "Complete 100 Tasks"    -> AchievementColorOrange
    "Complete 250 Tasks"    -> AchievementColorPurple
    "Complete 500 Tasks"    -> AchievementColorPurple
    // Bond Points: Red → Green → Orange → Purple
    "Earn 100 Bond Points"  -> AchievementColorRed
    "Earn 250 Bond Points"  -> AchievementColorRed
    "Earn 500 Bond Points"  -> AchievementColorGreen
    "Earn 1000 Bond Points" -> AchievementColorOrange
    "Earn 2500 Bond Points" -> AchievementColorPurple
    "Earn 5000 Bond Points" -> AchievementColorPurple
    // Pet Bond Level: Red → Green → Orange → Purple
    "Bond Master"           -> AchievementColorRed
    "Bond Veteran"          -> AchievementColorRed
    "Level 10 Companion"    -> AchievementColorGreen
    "Virtue Master"         -> AchievementColorGreen
    "Dedicated Caregiver"   -> AchievementColorOrange
    "Elite Trainer"         -> AchievementColorPurple
    // Trainer Level: Red → Green → Orange → Purple
    "Reach Level 10"        -> AchievementColorRed
    "Reach Level 20"        -> AchievementColorGreen
    "Reach Level 30"        -> AchievementColorOrange
    "Reach Level 50"        -> AchievementColorPurple
    else                    -> AchievementColorBlue
}

// ── Category definition ───────────────────────────────────────────────────────
private data class AchievementCategory(val label: String, val titles: List<String>)

private val CATEGORIES = listOf(
    AchievementCategory("🐾  First Steps", listOf(
        "First Pet", "First Verification", "Pet Lover",
        "Own 5 Pets", "Own 10 Pets",
        "Verify 3 Pets", "Verify 5 Pets", "Verify 10 Pets"
    )),
    AchievementCategory("🔥  Streak", listOf(
        "3-Day Streak", "7-Day Streak", "14-Day Streak",
        "30-Day Streak", "60-Day Streak", "100-Day Streak"
    )),
    AchievementCategory("✅  Tasks", listOf(
        "Complete 10 Tasks", "Complete 25 Tasks", "Complete 50 Tasks",
        "Complete 100 Tasks", "Complete 250 Tasks", "Complete 500 Tasks"
    )),
    AchievementCategory("💎  Bond Points", listOf(
        "Earn 100 Bond Points", "Earn 250 Bond Points", "Earn 500 Bond Points",
        "Earn 1000 Bond Points", "Earn 2500 Bond Points", "Earn 5000 Bond Points"
    )),
    AchievementCategory("⭐  Pet Bond Level", listOf(
        "Bond Master", "Bond Veteran", "Level 10 Companion",
        "Virtue Master", "Dedicated Caregiver", "Elite Trainer"
    )),
    AchievementCategory("🏆  Trainer Level", listOf(
        "Reach Level 10", "Reach Level 20", "Reach Level 30", "Reach Level 50"
    )),
    AchievementCategory("🌍  Species & Rarity", listOf(
        "Species Collector", "Animal Explorer", "Master Explorer",
        "Rarity Hunter", "Epic Tamer"
    ))
)

// ── Grid item model ───────────────────────────────────────────────────────────
private sealed interface GridItem {
    data class Header(val label: String) : GridItem
    data class Tile(val achievement: AchievementEntity, val tileIndex: Int) : GridItem
}

// ── Icon + hint maps ──────────────────────────────────────────────────────────
private val ICON_MAP: Map<String, ImageVector> = mapOf(
    "First Pet"             to Icons.Default.Pets,
    "First Verification"    to Icons.Default.CameraAlt,
    "Pet Lover"             to Icons.Default.Favorite,
    "Own 5 Pets"            to Icons.Default.Favorite,
    "Own 10 Pets"           to Icons.Default.Favorite,
    "Verify 3 Pets"         to Icons.Default.CameraAlt,
    "Verify 5 Pets"         to Icons.Default.CameraAlt,
    "Verify 10 Pets"        to Icons.Default.CameraAlt,
    "3-Day Streak"          to Icons.Default.Whatshot,
    "7-Day Streak"          to Icons.Default.Whatshot,
    "14-Day Streak"         to Icons.Default.Whatshot,
    "30-Day Streak"         to Icons.Default.Whatshot,
    "60-Day Streak"         to Icons.Default.Whatshot,
    "100-Day Streak"        to Icons.Default.Whatshot,
    "Complete 10 Tasks"     to Icons.Default.CheckCircle,
    "Complete 25 Tasks"     to Icons.Default.CheckCircle,
    "Complete 50 Tasks"     to Icons.Default.CheckCircle,
    "Complete 100 Tasks"    to Icons.Default.CheckCircle,
    "Complete 250 Tasks"    to Icons.Default.CheckCircle,
    "Complete 500 Tasks"    to Icons.Default.CheckCircle,
    "Earn 100 Bond Points"  to Icons.Default.Star,
    "Earn 250 Bond Points"  to Icons.Default.Star,
    "Earn 500 Bond Points"  to Icons.Default.Star,
    "Earn 1000 Bond Points" to Icons.Default.Star,
    "Earn 2500 Bond Points" to Icons.Default.Star,
    "Earn 5000 Bond Points" to Icons.Default.Star,
    "Bond Master"           to Icons.Default.EmojiEvents,
    "Bond Veteran"          to Icons.Default.EmojiEvents,
    "Level 10 Companion"    to Icons.Default.EmojiEvents,
    "Virtue Master"         to Icons.Default.Stars,
    "Dedicated Caregiver"   to Icons.Default.Stars,
    "Elite Trainer"         to Icons.Default.Stars,
    "Reach Level 10"        to Icons.Default.EmojiEvents,
    "Reach Level 20"        to Icons.Default.EmojiEvents,
    "Reach Level 30"        to Icons.Default.EmojiEvents,
    "Reach Level 50"        to Icons.Default.Stars,
    "Species Collector"     to Icons.Default.Explore,
    "Animal Explorer"       to Icons.Default.Explore,
    "Master Explorer"       to Icons.Default.Explore,
    "Rarity Hunter"         to Icons.Default.Explore,
    "Epic Tamer"            to Icons.Default.Stars
)

private val HINT_MAP: Map<String, String> = mapOf(
    "First Pet"             to "Add your first pet",
    "First Verification"    to "Verify a pet with a photo",
    "Pet Lover"             to "Own 3 or more pets",
    "Own 5 Pets"            to "Have 5 pets",
    "Own 10 Pets"           to "Have 10 pets",
    "Verify 3 Pets"         to "Verify 3 pets",
    "Verify 5 Pets"         to "Verify 5 pets",
    "Verify 10 Pets"        to "Verify 10 pets",
    "3-Day Streak"          to "3 days in a row",
    "7-Day Streak"          to "7 days in a row",
    "14-Day Streak"         to "14 days in a row",
    "30-Day Streak"         to "30 days in a row",
    "60-Day Streak"         to "60 days in a row",
    "100-Day Streak"        to "100 days in a row",
    "Complete 10 Tasks"     to "Complete 10 tasks",
    "Complete 25 Tasks"     to "Complete 25 tasks",
    "Complete 50 Tasks"     to "Complete 50 tasks",
    "Complete 100 Tasks"    to "Complete 100 tasks",
    "Complete 250 Tasks"    to "Complete 250 tasks",
    "Complete 500 Tasks"    to "Complete 500 tasks",
    "Earn 100 Bond Points"  to "Earn 100 Bond Points",
    "Earn 250 Bond Points"  to "Earn 250 Bond Points",
    "Earn 500 Bond Points"  to "Earn 500 Bond Points",
    "Earn 1000 Bond Points" to "Earn 1000 Bond Points",
    "Earn 2500 Bond Points" to "Earn 2500 Bond Points",
    "Earn 5000 Bond Points" to "Earn 5000 Bond Points",
    "Bond Master"           to "Reach Bond Level 5",
    "Bond Veteran"          to "Reach Bond Level 10",
    "Level 10 Companion"    to "Reach Bond Level 15",
    "Virtue Master"         to "Reach Bond Level 20",
    "Dedicated Caregiver"   to "Reach Bond Level 30",
    "Elite Trainer"         to "Reach Bond Level 50",
    "Reach Level 10"        to "Reach Trainer Level 10",
    "Reach Level 20"        to "Reach Trainer Level 20",
    "Reach Level 30"        to "Reach Trainer Level 30",
    "Reach Level 50"        to "Reach Trainer Level 50",
    "Species Collector"     to "Collect 5 different species",
    "Animal Explorer"       to "Collect 10 different species",
    "Master Explorer"       to "Collect 15 different species",
    "Rarity Hunter"         to "Own a pet of every rarity",
    "Epic Tamer"            to "Own an Epic rarity pet"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: PetQuestViewModel,
    onNavigateToTasks: () -> Unit = {}
) {
    val achievements by viewModel.allAchievements.collectAsState()

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount    = achievements.size

    // Build ordered grid items from categories
    val achievementMap = remember(achievements) { achievements.associateBy { it.title } }
    val gridItems: List<GridItem> = remember(achievementMap) {
        var tileIdx = 0
        val result  = mutableListOf<GridItem>()
        for (cat in CATEGORIES) {
            val catItems = cat.titles.mapNotNull { achievementMap[it] }
            if (catItems.isNotEmpty()) {
                result.add(GridItem.Header(cat.label))
                catItems.forEach { a -> result.add(GridItem.Tile(a, tileIdx++)) }
            }
        }
        // Uncategorized achievements go at the end
        val knownTitles = CATEGORIES.flatMap { it.titles }.toSet()
        val extras = achievements.filter { it.title !in knownTitles }
        if (extras.isNotEmpty()) {
            result.add(GridItem.Header("Other"))
            extras.forEach { a -> result.add(GridItem.Tile(a, tileIdx++)) }
        }
        result
    }

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
            // ── Overall progress bar ──────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
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

            // ── Category headers + achievement tiles ──────────────────────────
            items(
                count = gridItems.size,
                span  = { i ->
                    when (gridItems[i]) {
                        is GridItem.Header -> GridItemSpan(maxLineSpan)
                        is GridItem.Tile   -> GridItemSpan(1)
                    }
                }
            ) { i ->
                when (val item = gridItems[i]) {
                    is GridItem.Header -> CategoryHeader(item.label)
                    is GridItem.Tile   -> AchievementTile(
                        achievement = item.achievement,
                        tileIndex   = item.tileIndex
                    )
                }
            }
        }
    }
}

// ── Category section header ───────────────────────────────────────────────────
@Composable
private fun CategoryHeader(label: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(
            modifier  = Modifier.padding(top = 4.dp),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// ── Single achievement tile ───────────────────────────────────────────────────
@Composable
private fun AchievementTile(achievement: AchievementEntity, tileIndex: Int) {
    val a      = achievement
    val icon   = ICON_MAP[a.title]  ?: Icons.Default.Star
    val hint   = HINT_MAP[a.title]  ?: "Keep playing"
    val tColor = achievementColor(a.title)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(a.title) {
        kotlinx.coroutines.delay(tileIndex * 30L)
        visible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label         = "ach_alpha_$tileIndex"
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
        label = "unlock_scale_$tileIndex"
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
            // Color accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(if (a.isUnlocked) tColor else tColor.copy(alpha = 0.2f))
            )

            Spacer(Modifier.height(14.dp))

            // Icon badge
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

            // Title / hint
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

            Spacer(Modifier.height(12.dp))
        }
    }
}
