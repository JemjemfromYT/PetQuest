// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/AwardsScreen.kt
//
// REDESIGN: Awards screen
//
// Problem with old version:
//   Every achievement tile had its own random color (blue, pink, purple, teal…).
//   There was no visual logic. Looking at "First Steps" you saw 6 different
//   border colors for 6 cards in the same section — chaotic and amateurish.
//
// Fix:
//   Color belongs to the CATEGORY, not the achievement.
//   All tiles in "Streak" share the same orange. All in "Tasks" share the same blue.
//   Users learn the color system once and it becomes intuitive.
//   Locked tiles are always the same uniform grey — no color at all.
// ============================================================

package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

// ── Category color system ─────────────────────────────────────────────────────
// ONE color per category. All tiles in a category share it.
// This is predictable, learnable, and visually clean.
private data class CategoryDef(
    val label : String,
    val icon  : ImageVector,
    val color : Color,          // category accent color
    val titles: List<String>
)

private val CATEGORIES = listOf(
    CategoryDef(
        label  = "First Steps",
        icon   = Icons.Default.Pets,
        color  = Color(0xFF2E7D32),  // green — new beginnings
        titles = listOf(
            "First Pet", "First Verification", "Pet Lover",
            "Own 5 Pets", "Own 10 Pets",
            "Verify 3 Pets", "Verify 5 Pets", "Verify 10 Pets"
        )
    ),
    CategoryDef(
        label  = "Streak",
        icon   = Icons.Default.Whatshot,
        color  = Color(0xFFE65100),  // deep orange — fire, consistency
        titles = listOf(
            "3-Day Streak", "7-Day Streak", "14-Day Streak",
            "30-Day Streak", "60-Day Streak", "100-Day Streak"
        )
    ),
    CategoryDef(
        label  = "Tasks",
        icon   = Icons.Default.CheckCircle,
        color  = Color(0xFF1565C0),  // blue — productivity
        titles = listOf(
            "Complete 10 Tasks", "Complete 25 Tasks", "Complete 50 Tasks",
            "Complete 100 Tasks", "Complete 250 Tasks", "Complete 500 Tasks"
        )
    ),
    CategoryDef(
        label  = "Bond Points",
        icon   = Icons.Default.Star,
        color  = Color(0xFFF9A825),  // amber/gold — value, achievement
        titles = listOf(
            "Earn 100 Bond Points", "Earn 250 Bond Points", "Earn 500 Bond Points",
            "Earn 1000 Bond Points", "Earn 2500 Bond Points", "Earn 5000 Bond Points"
        )
    ),
    CategoryDef(
        label  = "Pet Bond Level",
        icon   = Icons.Default.EmojiEvents,
        color  = Color(0xFF6A1B9A),  // purple — deep mastery
        titles = listOf(
            "Bond Master", "Bond Veteran", "Level 10 Companion",
            "Virtue Master", "Dedicated Caregiver", "Elite Trainer"
        )
    ),
    CategoryDef(
        label  = "Trainer Level",
        icon   = Icons.Default.Stars,
        color  = Color(0xFFBF360C),  // burnt red — prestige
        titles = listOf(
            "Reach Level 10", "Reach Level 20", "Reach Level 30", "Reach Level 50"
        )
    ),
    CategoryDef(
        label  = "Species & Rarity",
        icon   = Icons.Default.Explore,
        color  = Color(0xFF00695C),  // teal — exploration, discovery
        titles = listOf(
            "Species Collector", "Animal Explorer", "Master Explorer",
            "Rarity Hunter", "Epic Tamer"
        )
    )
)

// Build a fast lookup: title → CategoryDef
private val TITLE_TO_CATEGORY: Map<String, CategoryDef> by lazy {
    buildMap {
        for (cat in CATEGORIES) {
            for (title in cat.titles) put(title, cat)
        }
    }
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

// ── Hint text (shown on locked tiles instead of the real title) ───────────────
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
    "Earn 1000 Bond Points" to "Earn 1,000 Bond Pts",
    "Earn 2500 Bond Points" to "Earn 2,500 Bond Pts",
    "Earn 5000 Bond Points" to "Earn 5,000 Bond Pts",
    "Bond Master"           to "Bond Level 5",
    "Bond Veteran"          to "Bond Level 10",
    "Level 10 Companion"    to "Bond Level 15",
    "Virtue Master"         to "Bond Level 20",
    "Dedicated Caregiver"   to "Bond Level 30",
    "Elite Trainer"         to "Bond Level 50",
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

// ── Grid item sealed type ─────────────────────────────────────────────────────
private sealed interface GridItem {
    data class Header(val cat: CategoryDef, val unlockedInCat: Int, val totalInCat: Int) : GridItem
    data class Tile(val achievement: AchievementEntity, val categoryColor: Color, val index: Int) : GridItem
}

// ── Main screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwardsScreen(
    viewModel        : PetQuestViewModel,
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
                val unlockedInCat = catItems.count { it.isUnlocked }
                result.add(GridItem.Header(cat, unlockedInCat, catItems.size))
                catItems.forEach { a ->
                    result.add(GridItem.Tile(a, cat.color, idx++))
                }
            }
        }
        // Any achievements not in a known category
        val known  = CATEGORIES.flatMap { it.titles }.toSet()
        val extras = achievements.filter { it.title !in known }
        if (extras.isNotEmpty()) {
            val fallbackCat = CategoryDef(
                label  = "Other",
                icon   = Icons.Default.Star,
                color  = Color(0xFFFB8C00),
                titles = extras.map { it.title }
            )
            val unlockedExtras = extras.count { it.isUnlocked }
            result.add(GridItem.Header(fallbackCat, unlockedExtras, extras.size))
            extras.forEach { a -> result.add(GridItem.Tile(a, fallbackCat.color, idx++)) }
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
            // ── Overall progress ──────────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )
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

            // ── Categories + tiles ────────────────────────────────────────────
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
                    is GridItem.Header -> CategoryHeader(item.cat, item.unlockedInCat, item.totalInCat)
                    is GridItem.Tile   -> AchievementTile(item.achievement, item.categoryColor, item.index)
                }
            }
        }
    }
}

// ── Category header ───────────────────────────────────────────────────────────
@Composable
private fun CategoryHeader(cat: CategoryDef, unlocked: Int, total: Int) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar in category color
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cat.color)
        )
        Spacer(Modifier.width(10.dp))

        // Category icon pill
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = cat.color.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector        = cat.icon,
                contentDescription = null,
                tint               = cat.color,
                modifier           = Modifier.padding(4.dp).size(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))

        Text(
            cat.label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f)
        )

        // Category progress count
        Text(
            "$unlocked/$total",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = if (unlocked == total) cat.color
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// Lightens a dark color for use on dark backgrounds in dark mode.
private fun lightenColor(c: Color): Color = Color(
    red   = 1f - (1f - c.red)   * 0.65f,
    green = 1f - (1f - c.green) * 0.65f,
    blue  = 1f - (1f - c.blue)  * 0.65f,
    alpha = c.alpha
)

// ── Single achievement tile ───────────────────────────────────────────────────
// DESIGN RULES:
//   Unlocked → category color accent strip (6dp) + light category tint bg + colored icon
//   Locked   → thin grey strip (3dp) + grey bg + grey lock icon — ALL locked tiles look identical
// NO random per-achievement borders. Color comes only from the category.
@Composable
private fun AchievementTile(
    achievement   : AchievementEntity,
    categoryColor : Color,
    tileIndex     : Int
) {
    val a    = achievement
    val icon = ICON_MAP[a.title] ?: Icons.Default.Star
    val hint = HINT_MAP[a.title] ?: "Keep playing"

    val isDark       = isSystemInDarkTheme()
    val displayColor = if (isDark) lightenColor(categoryColor) else categoryColor

    // Stagger fade-in so tiles don't all appear at once
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(a.title) {
        kotlinx.coroutines.delay(tileIndex * 25L)
        visible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label         = "tile_alpha_$tileIndex"
    )

    // Subtle pop animation when first unlocked
    var unlockStarted by remember { mutableStateOf(false) }
    LaunchedEffect(a.isUnlocked) {
        if (a.isUnlocked) {
            unlockStarted = false
            kotlinx.coroutines.delay(40)
            unlockStarted = true
        }
    }
    val unlockScale by animateFloatAsState(
        targetValue   = if (a.isUnlocked && unlockStarted) 1f else if (a.isUnlocked) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "tile_scale_$tileIndex"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .alpha(if (a.isUnlocked) cardAlpha else cardAlpha * 0.70f)
            .scale(if (a.isUnlocked) unlockScale else 1f),
        shape     = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (a.isUnlocked) 4.dp else 0.dp
        ),
        colors    = CardDefaults.cardColors(
            containerColor = if (a.isUnlocked)
                (if (isDark) categoryColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top accent strip — thick + category color when unlocked, thin grey when locked
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (a.isUnlocked) 5.dp else 2.dp)
                    .background(
                        if (a.isUnlocked)
                            Brush.horizontalGradient(
                                listOf(categoryColor, categoryColor.copy(alpha = 0.50f))
                            )
                        else
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                )
                            )
                    )
            )

            Spacer(Modifier.height(14.dp))

            // Icon area
            Surface(
                modifier = Modifier.size(56.dp),
                shape    = MaterialTheme.shapes.large,
                color    = if (a.isUnlocked)
                    (if (isDark) displayColor.copy(alpha = 0.22f) else categoryColor.copy(alpha = 0.20f))
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (a.isUnlocked) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = a.title,
                            tint               = displayColor,
                            modifier           = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title or hint
            Text(
                text       = if (a.isUnlocked) a.title else hint,
                fontWeight = if (a.isUnlocked) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 11.sp,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                color      = if (a.isUnlocked)
                    displayColor
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                modifier   = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(6.dp))

            // "✓ UNLOCKED" chip — shown only when unlocked
            if (a.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = displayColor.copy(alpha = 0.20f)
                ) {
                    Text(
                        "✓  UNLOCKED",
                        modifier      = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize      = 8.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = displayColor,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
    