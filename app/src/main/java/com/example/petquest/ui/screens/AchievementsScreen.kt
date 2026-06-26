package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.data.model.AchievementEntity
import com.example.petquest.viewmodel.PetQuestViewModel

// ── Achievement color map ─────────────────────────────────────────────────────
// IMPROVED: each achievement has its own distinct color personality.
// "First Steps" tiles were all identical blue before — now each is unique.
// Tier progression: bronze (starter) → silver (mid) → gold (high) → plat (elite)
private fun achievementColor(title: String): Color = when (title) {

    // ── First Steps — each action has its own personality ──────────────────────
    "First Pet"          -> Color(0xFF1565C0)   // deep blue     — your first companion
    "First Verification" -> Color(0xFF0288D1)   // sky blue      — you snapped a photo
    "Pet Lover"          -> Color(0xFFE91E63)   // rose pink     — affection / love
    "Own 5 Pets"         -> Color(0xFF7B1FA2)   // purple        — growing family
    "Own 10 Pets"        -> Color(0xFF4A148C)   // deep purple   — full house
    "Verify 3 Pets"      -> Color(0xFF00695C)   // teal          — committed
    "Verify 5 Pets"      -> Color(0xFF2E7D32)   // green         — consistent
    "Verify 10 Pets"     -> Color(0xFF1B5E20)   // dark green    — master verifier

    // ── Streak ─────────────────────────────────────────────────────────────────
    "3-Day Streak"   -> Color(0xFFD84315)   // deep red-orange (starter warmth)
    "7-Day Streak"   -> Color(0xFF2E7D32)   // green           (a whole week!)
    "14-Day Streak"  -> Color(0xFF1565C0)   // blue            (two weeks)
    "30-Day Streak"  -> Color(0xFFFB8C00)   // amber           (a month!)
    "60-Day Streak"  -> Color(0xFFE65100)   // deep orange     (two months)
    "100-Day Streak" -> Color(0xFF7B1FA2)   // purple          (elite)

    // ── Tasks ──────────────────────────────────────────────────────────────────
    "Complete 10 Tasks"  -> Color(0xFFD84315)
    "Complete 25 Tasks"  -> Color(0xFFBF360C)
    "Complete 50 Tasks"  -> Color(0xFF2E7D32)
    "Complete 100 Tasks" -> Color(0xFFFB8C00)
    "Complete 250 Tasks" -> Color(0xFFE65100)
    "Complete 500 Tasks" -> Color(0xFF7B1FA2)

    // ── Bond Points ─────────────────────────────────────────────────────────────
    "Earn 100 Bond Points"  -> Color(0xFFD84315)
    "Earn 250 Bond Points"  -> Color(0xFFBF360C)
    "Earn 500 Bond Points"  -> Color(0xFF2E7D32)
    "Earn 1000 Bond Points" -> Color(0xFFFB8C00)
    "Earn 2500 Bond Points" -> Color(0xFFE65100)
    "Earn 5000 Bond Points" -> Color(0xFF7B1FA2)

    // ── Pet Bond Level ──────────────────────────────────────────────────────────
    "Bond Master"        -> Color(0xFFD84315)
    "Bond Veteran"       -> Color(0xFF2E7D32)
    "Level 10 Companion" -> Color(0xFF1565C0)
    "Virtue Master"      -> Color(0xFFFB8C00)
    "Dedicated Caregiver"-> Color(0xFFE65100)
    "Elite Trainer"      -> Color(0xFF7B1FA2)

    // ── Trainer Level ───────────────────────────────────────────────────────────
    "Reach Level 10" -> Color(0xFFD84315)
    "Reach Level 20" -> Color(0xFF2E7D32)
    "Reach Level 30" -> Color(0xFFFB8C00)
    "Reach Level 50" -> Color(0xFF7B1FA2)

    // ── Species & Rarity ────────────────────────────────────────────────────────
    "Species Collector" -> Color(0xFF00695C)
    "Animal Explorer"   -> Color(0xFF2E7D32)
    "Master Explorer"   -> Color(0xFFFB8C00)
    "Rarity Hunter"     -> Color(0xFFE65100)
    "Epic Tamer"        -> Color(0xFF7B1FA2)

    else -> Color(0xFFFB8C00)
}

// ── Category definition ───────────────────────────────────────────────────────
private data class AchievementCategory(
    val label : String,
    val icon  : ImageVector,
    val titles: List<String>
)

private val CATEGORIES = listOf(
    AchievementCategory("First Steps", Icons.Default.Pets, listOf(
        "First Pet", "First Verification", "Pet Lover",
        "Own 5 Pets", "Own 10 Pets",
        "Verify 3 Pets", "Verify 5 Pets", "Verify 10 Pets"
    )),
    AchievementCategory("Streak", Icons.Default.Whatshot, listOf(
        "3-Day Streak", "7-Day Streak", "14-Day Streak",
        "30-Day Streak", "60-Day Streak", "100-Day Streak"
    )),
    AchievementCategory("Tasks", Icons.Default.CheckCircle, listOf(
        "Complete 10 Tasks", "Complete 25 Tasks", "Complete 50 Tasks",
        "Complete 100 Tasks", "Complete 250 Tasks", "Complete 500 Tasks"
    )),
    AchievementCategory("Bond Points", Icons.Default.Star, listOf(
        "Earn 100 Bond Points", "Earn 250 Bond Points", "Earn 500 Bond Points",
        "Earn 1000 Bond Points", "Earn 2500 Bond Points", "Earn 5000 Bond Points"
    )),
    AchievementCategory("Pet Bond Level", Icons.Default.EmojiEvents, listOf(
        "Bond Master", "Bond Veteran", "Level 10 Companion",
        "Virtue Master", "Dedicated Caregiver", "Elite Trainer"
    )),
    AchievementCategory("Trainer Level", Icons.Default.Stars, listOf(
        "Reach Level 10", "Reach Level 20", "Reach Level 30", "Reach Level 50"
    )),
    AchievementCategory("Species & Rarity", Icons.Default.Explore, listOf(
        "Species Collector", "Animal Explorer", "Master Explorer",
        "Rarity Hunter", "Epic Tamer"
    ))
)

// ── Grid item model ───────────────────────────────────────────────────────────
private sealed interface GridItem {
    data class Header(val label: String, val icon: ImageVector) : GridItem
    data class Tile(val achievement: AchievementEntity, val tileIndex: Int) : GridItem
}

// ── Icon map ──────────────────────────────────────────────────────────────────
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
    "Earn 1000 Bond Points" to Icons.Default.Star,
    "Earn 2500 Bond Points" to Icons.Default.Star,
    "Earn 5000 Bond Points" to Icons.Default.Star,
    "Earn 500 Bond Points"  to Icons.Default.Star,
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

// ── Hint text map ─────────────────────────────────────────────────────────────
private val HINT_MAP: Map<String, String> = mapOf(
    "First Pet"             to "Add your first pet",
    "First Verification"    to "Verify with a photo",
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
    "Earn 100 Bond Points"  to "Earn 100 Bond Pts",
    "Earn 250 Bond Points"  to "Earn 250 Bond Pts",
    "Earn 500 Bond Points"  to "Earn 500 Bond Pts",
    "Earn 1000 Bond Points" to "Earn 1000 Bond Pts",
    "Earn 2500 Bond Points" to "Earn 2500 Bond Pts",
    "Earn 5000 Bond Points" to "Earn 5000 Bond Pts",
    "Bond Master"           to "Reach Bond Level 5",
    "Bond Veteran"          to "Reach Bond Level 10",
    "Level 10 Companion"    to "Reach Bond Level 15",
    "Virtue Master"         to "Reach Bond Level 20",
    "Dedicated Caregiver"   to "Reach Bond Level 30",
    "Elite Trainer"         to "Reach Bond Level 50",
    "Reach Level 10"        to "Trainer Lv.10",
    "Reach Level 20"        to "Trainer Lv.20",
    "Reach Level 30"        to "Trainer Lv.30",
    "Reach Level 50"        to "Trainer Lv.50",
    "Species Collector"     to "Collect 5 species",
    "Animal Explorer"       to "Collect 10 species",
    "Master Explorer"       to "Collect 15 species",
    "Rarity Hunter"         to "Own every rarity",
    "Epic Tamer"            to "Own an Epic pet"
)

// ── Main screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: PetQuestViewModel,
    onNavigateToTasks: () -> Unit = {}
) {
    val achievements by viewModel.allAchievements.collectAsState()

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount    = achievements.size

    val achievementMap = remember(achievements) { achievements.associateBy { it.title } }

    val gridItems: List<GridItem> = remember(achievementMap) {
        var idx    = 0
        val result = mutableListOf<GridItem>()
        for (cat in CATEGORIES) {
            val catItems = cat.titles.mapNotNull { achievementMap[it] }
            if (catItems.isNotEmpty()) {
                result.add(GridItem.Header(cat.label, cat.icon))
                catItems.forEach { a -> result.add(GridItem.Tile(a, idx++)) }
            }
        }
        val known  = CATEGORIES.flatMap { it.titles }.toSet()
        val extras = achievements.filter { it.title !in known }
        if (extras.isNotEmpty()) {
            result.add(GridItem.Header("Other", Icons.Default.Star))
            extras.forEach { a -> result.add(GridItem.Tile(a, idx++)) }
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
                    "Awards",
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }
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
            // Overall progress card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$unlockedCount of $totalCount unlocked",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(overallProgress * 100).toInt()}%",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(overallProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFBF360C), Color(0xFFFF8C42))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Category headers + tiles
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
                    is GridItem.Header -> CategoryHeader(item.label, item.icon)
                    is GridItem.Tile   -> AchievementTile(item.achievement, item.tileIndex)
                }
            }
        }
    }
}

// ── Category section header ───────────────────────────────────────────────────
@Composable
private fun CategoryHeader(label: String, icon: ImageVector) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFF8C42), Color(0xFFFFB77A))
                    )
                )
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.padding(4.dp).size(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onSurface
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
        animationSpec = tween(durationMillis = 220),
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
            .alpha(if (a.isUnlocked) cardAlpha else cardAlpha * 0.75f)
            .scale(if (a.isUnlocked) unlockScale else 1f),
        shape     = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (a.isUnlocked) 5.dp else 0.dp
        ),
        colors    = CardDefaults.cardColors(
            containerColor = if (a.isUnlocked)
                tColor.copy(alpha = 0.07f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (a.isUnlocked) 6.dp else 3.dp)
                    .background(
                        if (a.isUnlocked)
                            Brush.horizontalGradient(listOf(tColor, tColor.copy(alpha = 0.55f)))
                        else
                            Brush.horizontalGradient(
                                listOf(tColor.copy(alpha = 0.20f), tColor.copy(alpha = 0.08f))
                            )
                    )
            )

            Spacer(Modifier.height(14.dp))

            // Icon badge
            Surface(
                modifier = Modifier.size(60.dp),
                shape    = MaterialTheme.shapes.large,
                color    = if (a.isUnlocked)
                    tColor.copy(alpha = 0.18f)
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
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title (accent color when unlocked, hint text when locked)
            Text(
                text       = if (a.isUnlocked) a.title else hint,
                fontWeight = if (a.isUnlocked) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 11.sp,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                color      = if (a.isUnlocked) tColor
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier   = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(6.dp))

            // "UNLOCKED" micro-badge — only on unlocked tiles
            if (a.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = tColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        "UNLOCKED",
                        modifier      = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = tColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
