package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.model.PetType
import com.example.petquest.data.repository.FirebaseRepository
import com.example.petquest.data.repository.PetSummary
import com.example.petquest.data.repository.PublicProfile
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Internal state machine
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface SharedProfileState {
    data object Loading : SharedProfileState
    data class  Success(val profile: PublicProfile) : SharedProfileState
    data object NotFound : SharedProfileState
    data object Error : SharedProfileState
}

// ─────────────────────────────────────────────────────────────────────────────
// Known event-badge titles — keep in sync with EventsScreen / ProfileScreen
// ─────────────────────────────────────────────────────────────────────────────

private val KNOWN_EVENT_BADGE_TITLES = setOf(
    "Spring Blossom Badge",
    "Summer Splash Badge",
    "Autumn Harvest Badge",
    "Winter Frost Badge",
    "New Year Sprint Badge",
    "Valentine Bond Badge"
)

// ─────────────────────────────────────────────────────────────────────────────
// Achievement category definitions (mirrors AwardsScreen color logic)
// ─────────────────────────────────────────────────────────────────────────────

private data class AchievCategoryDef(
    val label  : String,
    val icon   : ImageVector,
    val color  : Color,
    val titles : List<String>
)

private val ACHIEV_CATEGORIES = listOf(
    AchievCategoryDef(
        label  = "First Steps",
        icon   = Icons.Default.Pets,
        color  = Color(0xFF2E7D32),
        titles = listOf(
            "First Pet", "First Verification", "Pet Lover",
            "Own 5 Pets", "Own 10 Pets",
            "Verify 3 Pets", "Verify 5 Pets", "Verify 10 Pets"
        )
    ),
    AchievCategoryDef(
        label  = "Streak",
        icon   = Icons.Default.Whatshot,
        color  = Color(0xFFE65100),
        titles = listOf(
            "3-Day Streak", "7-Day Streak", "14-Day Streak",
            "30-Day Streak", "60-Day Streak", "100-Day Streak"
        )
    ),
    AchievCategoryDef(
        label  = "Tasks",
        icon   = Icons.Default.CheckCircle,
        color  = Color(0xFF1565C0),
        titles = listOf(
            "Complete 10 Tasks", "Complete 25 Tasks", "Complete 50 Tasks",
            "Complete 100 Tasks", "Complete 250 Tasks", "Complete 500 Tasks"
        )
    ),
    AchievCategoryDef(
        label  = "Bond Points",
        icon   = Icons.Default.Star,
        color  = Color(0xFFF9A825),
        titles = listOf(
            "Earn 100 Bond Points", "Earn 250 Bond Points", "Earn 500 Bond Points",
            "Earn 1000 Bond Points", "Earn 2500 Bond Points", "Earn 5000 Bond Points"
        )
    ),
    AchievCategoryDef(
        label  = "Pet Bond Level",
        icon   = Icons.Default.EmojiEvents,
        color  = Color(0xFF6A1B9A),
        titles = listOf(
            "Bond Master", "Bond Veteran", "Level 10 Companion",
            "Virtue Master", "Dedicated Caregiver", "Elite Trainer"
        )
    ),
    AchievCategoryDef(
        label  = "Trainer Level",
        icon   = Icons.Default.Stars,
        color  = Color(0xFFBF360C),
        titles = listOf(
            "Reach Level 10", "Reach Level 20", "Reach Level 30", "Reach Level 50"
        )
    ),
    AchievCategoryDef(
        label  = "Species & Rarity",
        icon   = Icons.Default.Explore,
        color  = Color(0xFF00695C),
        titles = listOf(
            "Species Collector", "Animal Explorer", "Master Explorer",
            "Rarity Hunter", "Epic Tamer"
        )
    )
)

private val TITLE_TO_ACHIEV_CATEGORY: Map<String, AchievCategoryDef> by lazy {
    buildMap {
        for (cat in ACHIEV_CATEGORIES) {
            for (title in cat.titles) put(title, cat)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pet emoji helper
// ─────────────────────────────────────────────────────────────────────────────

private fun speciesEmoji(speciesName: String): String = when (speciesName.uppercase()) {
    "DOG"      -> "🐶"
    "CAT"      -> "🐱"
    "RABBIT"   -> "🐰"
    "HAMSTER"  -> "🐹"
    "BIRD"     -> "🐦"
    "FISH"     -> "🐟"
    "TURTLE"   -> "🐢"
    "SNAKE"    -> "🐍"
    "LIZARD"   -> "🦎"
    "PARROT"   -> "🦜"
    "HEDGEHOG" -> "🦔"
    "FOX"      -> "🦊"
    "WOLF"     -> "🐺"
    "BEAR"     -> "🐻"
    "PANDA"    -> "🐼"
    "LION"     -> "🦁"
    "TIGER"    -> "🐯"
    "MONKEY"   -> "🐒"
    "HORSE"    -> "🐴"
    "COW"      -> "🐄"
    else       -> "🐾"
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedProfileScreen(
    uid                : String,
    firebaseRepository : FirebaseRepository,
    onBackClick        : () -> Unit
) {
    var state by remember { mutableStateOf<SharedProfileState>(SharedProfileState.Loading) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = SharedProfileState.Loading
            try {
                val profile = firebaseRepository.fetchProfile(uid)
                state = if (profile != null) SharedProfileState.Success(profile)
                else SharedProfileState.NotFound
            } catch (e: Exception) {
                state = SharedProfileState.Error
            }
        }
    }

    LaunchedEffect(uid) { load() }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF8C42), Color(0xFFFFB77A))
                        )
                    )
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        "Trainer Profile",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is SharedProfileState.Loading  -> SpLoadingState()
                is SharedProfileState.NotFound -> SpNotFoundState(onBackClick)
                is SharedProfileState.Error    -> SpErrorState(onRetry = { load() })
                is SharedProfileState.Success  -> SpProfileContent(s.profile)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / Error / NotFound states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF8C42))
            Spacer(Modifier.height(16.dp))
            Text("Loading trainer profile…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpNotFoundState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Profile Not Found", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "This trainer may have removed PetQuest\nor the link has expired.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}

@Composable
private fun SpErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("Couldn't Load Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Check your connection and try again.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main profile content — matches the look of ProfileScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpProfileContent(profile: PublicProfile) {
    val totalSpecies = PetType.entries.size

    val xpInLevel   = profile.bondPoints % 100
    val xpProgress by animateFloatAsState(
        targetValue    = xpInLevel / 100f,
        animationSpec  = tween(900, easing = FastOutSlowInEasing),
        label          = "xp"
    )
    val speciesProgress by animateFloatAsState(
        targetValue    = if (totalSpecies == 0) 0f
        else profile.speciesCount / totalSpecies.toFloat(),
        animationSpec  = tween(800, easing = FastOutSlowInEasing),
        label          = "species"
    )

    // Separate event badges from regular achievements
    val eventBadges     = profile.unlockedBadgeTitles.filter { it in KNOWN_EVENT_BADGE_TITLES }
    val regularBadges   = profile.unlockedBadgeTitles.filter { it !in KNOWN_EVENT_BADGE_TITLES }

    // Group regular achievements by category (preserve category order)
    val groupedAchievs: List<Pair<AchievCategoryDef, List<String>>> = ACHIEV_CATEGORIES.mapNotNull { cat ->
        val earned = cat.titles.filter { it in regularBadges }
        if (earned.isEmpty()) null else cat to earned
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "fade")
    val slide by animateFloatAsState(
        targetValue   = if (visible) 0f else 40f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "slide"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha; translationY = slide },
        contentPadding     = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Avatar + name ─────────────────────────────────────────────────────
        item {
            SpTrainerHeader(profile = profile)
        }

        // ── Level card ────────────────────────────────────────────────────────
        item {
            SpLevelCard(
                level     = profile.level,
                xpInLevel = xpInLevel,
                xpProgress = xpProgress
            )
        }

        // ── Stats row: Bond Points + Active Pets ──────────────────────────────
        item {
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement  = Arrangement.spacedBy(12.dp)
            ) {
                SpStatCard(
                    modifier  = Modifier.weight(1f),
                    value     = "${profile.bondPoints}",
                    label     = "Total Bond Pts",
                    textColor = Color(0xFF1B5E20),
                    bgColor   = Color(0xFFE8F5E9)
                )
                SpStatCard(
                    modifier  = Modifier.weight(1f),
                    value     = "${profile.petCount}",
                    label     = "Active Pets",
                    textColor = Color(0xFF4A148C),
                    bgColor   = Color(0xFFF3E5F5)
                )
            }
        }

        // ── Day Streak ────────────────────────────────────────────────────────
        if (profile.streak > 0) {
            item {
                SpStreakBanner(streak = profile.streak)
            }
        }

        // ── Event Badges ──────────────────────────────────────────────────────
        item {
            SpEventBadgesSection(eventBadges = eventBadges)
        }

        // ── Species Collected ─────────────────────────────────────────────────
        item {
            SpSpeciesCard(
                collected       = profile.speciesCount,
                total           = totalSpecies,
                speciesProgress = speciesProgress
            )
        }

        // ── My Pets ───────────────────────────────────────────────────────────
        if (profile.pets.isNotEmpty()) {
            item {
                Text(
                    "My Pets (${profile.petCount})",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 4.dp)
                )
            }
            item {
                SpPetsGrid(pets = profile.pets)
            }
        }

        // ── Achievements ──────────────────────────────────────────────────────
        item {
            SpAchievementsSection(grouped = groupedAchievs)
        }

        // ── Footer ────────────────────────────────────────────────────────────
        item {
            Text(
                "🐾 Get PetQuest to start your own journey!",
                fontSize   = 13.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trainer header (avatar + name)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpTrainerHeader(profile: PublicProfile) {
    Row(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar circle
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFB77A), Color(0xFFFF8C42))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, null,
                    modifier = Modifier.size(36.dp),
                    tint     = Color.White)
            }
            // Level badge
            Surface(
                shape  = CircleShape,
                color  = Color(0xFF6650A4),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${profile.level}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
            }
        }

        Column {
            Text(
                profile.trainerName,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                "PetQuest Trainer",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Level card — orange gradient, XP progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpLevelCard(level: Int, xpInLevel: Int, xpProgress: Float) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        shape     = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF8C42), Color(0xFFFFCFA8))
                    )
                )
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon box
                Card(
                    shape  = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier           = Modifier.size(38.dp).padding(4.dp),
                            tint               = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Level $level",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "KP to next level",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.80f)
                        )
                        Text(
                            "$xpInLevel / 100",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic colored stat card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpStatCard(
    modifier  : Modifier,
    value     : String,
    label     : String,
    textColor : Color,
    bgColor   : Color
) {
    Card(
        modifier  = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Day streak banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpStreakBanner(streak: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Whatshot,
                contentDescription = null,
                tint               = Color(0xFFE64A19),
                modifier           = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "$streak Day Streak",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFFBF360C)
                )
                Text(
                    "Consistent trainer — logging in every day!",
                    fontSize = 11.sp,
                    color    = Color(0xFFE64A19).copy(alpha = 0.75f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Event badges section — mirrors ProfileScreen's EventBadgesSection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpEventBadgesSection(eventBadges: List<String>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (eventBadges.isNotEmpty()) Color(0xFFFFFDE7)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint               = if (eventBadges.isNotEmpty()) Color(0xFFF9A825)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    "Event Badges",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (eventBadges.isNotEmpty()) Color(0xFF5D4037)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (eventBadges.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF9A825).copy(alpha = 0.20f)
                    ) {
                        Text(
                            "${eventBadges.size}",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFF9A825)
                        )
                    }
                }
            }

            if (eventBadges.isEmpty()) {
                Text(
                    "No event badges earned yet.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            } else {
                eventBadges.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { title ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(12.dp),
                                color    = Color(0xFFF9A825).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier              = Modifier.padding(10.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏅", fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            title,
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = Color(0xFFE65100),
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Seasonal",
                                            fontSize = 9.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Species collected card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpSpeciesCard(collected: Int, total: Int, speciesProgress: Float) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Species Collected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                ) {
                    Text(
                        "$collected / $total",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(speciesProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1A6B44), Color(0xFF70C99A))
                            )
                        )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pets grid — 2-column grid of compact pet cards from PetSummary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetsGrid(pets: List<PetSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pets.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { pet ->
                    SpPetCard(pet = pet, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpPetCard(pet: PetSummary, modifier: Modifier = Modifier) {
    val rarityColor = when (pet.rarity.uppercase()) {
        "COMMON"   -> Color(0xFF9E9E9E)
        "UNCOMMON" -> Color(0xFF43A047)
        "RARE"     -> Color(0xFF1E88E5)
        "EPIC"     -> Color(0xFF8E24AA)
        else       -> Color(0xFF9E9E9E)
    }

    Card(
        modifier  = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rarity top stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(rarityColor, rarityColor.copy(alpha = 0.45f))
                        )
                    )
            )

            Column(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pet emoji
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(rarityColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(speciesEmoji(pet.species), fontSize = 40.sp)
                    if (pet.isVerified) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(14.dp)
                        )
                    }
                }

                Text(
                    pet.name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.fillMaxWidth()
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = rarityColor.copy(alpha = 0.13f)
                    ) {
                        Text(
                            pet.rarity.lowercase().replaceFirstChar { it.uppercase() },
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = rarityColor
                        )
                    }
                    Text(
                        "Lv.${pet.bondLevel}",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Achievements section — grouped by category, only unlocked
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpAchievementsSection(grouped: List<Pair<AchievCategoryDef, List<String>>>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (grouped.isNotEmpty()) Color(0xFFFFF8E1)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint               = if (grouped.isNotEmpty()) Color(0xFFF9A825)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    "Achievements",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (grouped.isNotEmpty()) Color(0xFF5D4037)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (grouped.isNotEmpty()) {
                    val totalCount = grouped.sumOf { it.second.size }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF9A825).copy(alpha = 0.20f)
                    ) {
                        Text(
                            "$totalCount unlocked",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFFF9A825)
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                Text(
                    "No achievements unlocked yet.\nComplete tasks and care for your pets to earn badges!",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            } else {
                grouped.forEach { (category, titles) ->
                    SpAchievCategoryRow(category = category, titles = titles)
                    if (category != grouped.last().first) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpAchievCategoryRow(
    category : AchievCategoryDef,
    titles   : List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Category header
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint               = category.color,
                modifier           = Modifier.size(14.dp)
            )
            Text(
                category.label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = category.color
            )
        }

        // Achievement chips — wrap in rows of 2
        titles.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { title ->
                    SpAchievChip(
                        title    = title,
                        category = category,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpAchievChip(
    title    : String,
    category : AchievCategoryDef,
    modifier : Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = category.color.copy(alpha = 0.10f)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint               = category.color,
                modifier           = Modifier.size(14.dp)
            )
            Text(
                title,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                color      = category.color,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}
