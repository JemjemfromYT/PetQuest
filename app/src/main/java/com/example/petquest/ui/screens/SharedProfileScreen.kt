package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.Virtue
import com.example.petquest.data.repository.FirebaseRepository
import com.example.petquest.data.repository.PetSummary
import com.example.petquest.data.repository.PublicProfile
import com.example.petquest.ui.VirtueConfig
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
        for (cat in ACHIEV_CATEGORIES) for (title in cat.titles) put(title, cat)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pet emoji fallback
// ─────────────────────────────────────────────────────────────────────────────

private fun speciesEmoji(speciesName: String): String = when (speciesName.uppercase()) {
    "DOG"      -> "🐶"; "CAT"      -> "🐱"; "RABBIT"   -> "🐰"; "HAMSTER"  -> "🐹"
    "BIRD"     -> "🐦"; "FISH"     -> "🐟"; "TURTLE"   -> "🐢"; "SNAKE"    -> "🐍"
    "LIZARD"   -> "🦎"; "PARROT"   -> "🦜"; "HEDGEHOG" -> "🦔"; "FOX"      -> "🦊"
    "WOLF"     -> "🐺"; "BEAR"     -> "🐻"; "PANDA"    -> "🐼"; "LION"     -> "🦁"
    "TIGER"    -> "🐯"; "MONKEY"   -> "🐒"; "HORSE"    -> "🐴"; "COW"      -> "🐄"
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
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Trainer Profile",
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Profile Not Found", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("This trainer may have removed PetQuest\nor the link has expired.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}

@Composable
private fun SpErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("Couldn't Load Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Check your connection and try again.", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
// Main profile content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpProfileContent(profile: PublicProfile) {
    val totalSpecies = PetType.entries.size

    val xpInLevel      = profile.bondPoints % 100
    val xpProgress by animateFloatAsState(
        targetValue   = xpInLevel / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "xp"
    )
    val speciesProgress by animateFloatAsState(
        targetValue   = if (totalSpecies == 0) 0f else profile.speciesCount / totalSpecies.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "species"
    )

    val eventBadges   = profile.unlockedBadgeTitles.filter { it in KNOWN_EVENT_BADGE_TITLES }
    val regularBadges = profile.unlockedBadgeTitles.filter { it !in KNOWN_EVENT_BADGE_TITLES }

    val groupedAchievs: List<Pair<AchievCategoryDef, List<String>>> =
        ACHIEV_CATEGORIES.mapNotNull { cat ->
            val earned = cat.titles.filter { it in regularBadges }
            if (earned.isEmpty()) null else cat to earned
        }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "fade")
    val slide by animateFloatAsState(
        if (visible) 0f else 40f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "slide"
    )

    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha; translationY = slide },
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Avatar + name ─────────────────────────────────────────────────────
        item { SpTrainerHeader(profile) }

        // ── Level card ────────────────────────────────────────────────────────
        item { SpLevelCard(level = profile.level, xpInLevel = xpInLevel, xpProgress = xpProgress) }

        // ── Stats row ─────────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpStatCard(Modifier.weight(1f), "${profile.bondPoints}", "Total Bond Pts",
                    Color(0xFF1B5E20), Color(0xFFE8F5E9))
                SpStatCard(Modifier.weight(1f), "${profile.petCount}", "Active Pets",
                    Color(0xFF4A148C), Color(0xFFF3E5F5))
            }
        }

        // ── Day Streak ────────────────────────────────────────────────────────
        if (profile.streak > 0) {
            item { SpStreakBanner(profile.streak) }
        }

        // ── Event Badges ──────────────────────────────────────────────────────
        item { SpEventBadgesSection(eventBadges) }

        // ── Species Collected ─────────────────────────────────────────────────
        item {
            SpSpeciesCard(
                collected       = profile.speciesCount,
                total           = totalSpecies,
                speciesProgress = speciesProgress
            )
        }

        // ── My Pets (flippable) ───────────────────────────────────────────────
        if (profile.pets.isNotEmpty()) {
            item {
                Text("My Pets (${profile.petCount})", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            }
            item { SpPetsGrid(profile.pets) }
        }

        // ── Achievements ──────────────────────────────────────────────────────
        item { SpAchievementsSection(groupedAchievs) }

        // ── Footer ────────────────────────────────────────────────────────────
        item {
            Text("🐾 Get PetQuest to start your own journey!",
                fontSize  = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trainer header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpTrainerHeader(profile: PublicProfile) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFFFFB77A), Color(0xFFFF8C42)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, null, modifier = Modifier.size(36.dp), tint = Color.White)
            }
            Surface(shape = CircleShape, color = Color(0xFF6650A4), modifier = Modifier.size(26.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${profile.level}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
        Column {
            Text(profile.trainerName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("PetQuest Trainer", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Level card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpLevelCard(level: Int, xpInLevel: Int, xpProgress: Float) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp),
        shape = MaterialTheme.shapes.large) {
        Box(modifier = Modifier.fillMaxWidth().background(
            Brush.horizontalGradient(listOf(Color(0xFFFF8C42), Color(0xFFFFCFA8)))
        )) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Pets, null, modifier = Modifier.size(38.dp).padding(4.dp), tint = Color.White)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Level $level", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("KP to next level", fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.80f))
                        Text("$xpInLevel / 100", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
                        Box(modifier = Modifier.fillMaxWidth(xpProgress.coerceIn(0f, 1f))
                            .fillMaxHeight().background(Color.White))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpStatCard(modifier: Modifier, value: String, label: String, textColor: Color, bgColor: Color) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Streak banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpStreakBanner(streak: Int) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Whatshot, null, tint = Color(0xFFE64A19), modifier = Modifier.size(28.dp))
            Column {
                Text("$streak Day Streak", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFBF360C))
                Text("Consistent trainer — logging in every day!", fontSize = 11.sp,
                    color = Color(0xFFE64A19).copy(alpha = 0.75f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Event badges
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpEventBadgesSection(eventBadges: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor =
            if (eventBadges.isNotEmpty()) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint =
                    if (eventBadges.isNotEmpty()) Color(0xFFF9A825) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Text("Event Badges", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (eventBadges.isNotEmpty()) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurfaceVariant)
                if (eventBadges.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF9A825).copy(alpha = 0.20f)) {
                        Text("${eventBadges.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF9A825))
                    }
                }
            }
            if (eventBadges.isEmpty()) {
                Text("No event badges earned yet.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                eventBadges.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { title ->
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF9A825).copy(alpha = 0.12f)) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🏅", fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE65100), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Seasonal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
// Species collected
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpSpeciesCard(collected: Int, total: Int, speciesProgress: Float) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Species Collected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                    Text("$collected / $total", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(3.5.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))) {
                Box(modifier = Modifier.fillMaxWidth(speciesProgress.coerceIn(0f, 1f)).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A6B44), Color(0xFF70C99A)))))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pets grid — flippable cards using PetSummary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetsGrid(pets: List<PetSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pet ->
                    SpFlippablePetCard(pet = pet, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Flippable pet card — tappable, shows photo on front and virtue on back
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpFlippablePetCard(pet: PetSummary, modifier: Modifier = Modifier) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue   = if (flipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "flip_${pet.name}"
    )

    val rarityColor = when (pet.rarity.uppercase()) {
        "COMMON"   -> Color(0xFF9E9E9E)
        "UNCOMMON" -> Color(0xFF43A047)
        "RARE"     -> Color(0xFF1E88E5)
        "EPIC"     -> Color(0xFF8E24AA)
        else       -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY      = rotation
                cameraDistance = 14f * density
            }
            .clickable { flipped = !flipped }
    ) {
        // Back face (virtue gradient)
        Box(modifier = Modifier.graphicsLayer {
            rotationY = 180f
            alpha     = if (rotation > 90f) 1f else 0f
        }) {
            SpPetCardBack(pet = pet, rarityColor = rarityColor)
        }

        // Front face (photo or emoji)
        Box(modifier = Modifier.graphicsLayer {
            alpha = if (rotation <= 90f) 1f else 0f
        }) {
            SpPetCardFront(pet = pet, rarityColor = rarityColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Front face — shows real photo if available, emoji otherwise
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetCardFront(pet: PetSummary, rarityColor: Color) {
    Card(elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Rarity top stripe
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(
                Brush.horizontalGradient(listOf(rarityColor, rarityColor.copy(alpha = 0.45f)))))

            Column(modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {

                // Photo or emoji
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.small),
                    contentAlignment = Alignment.TopEnd) {
                    if (!pet.photoUri.isNullOrBlank()) {
                        // Real verified photo
                        AsyncImage(
                            model              = pet.photoUri,
                            contentDescription = "${pet.name} photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        // Emoji fallback
                        Box(modifier = Modifier.fillMaxSize()
                            .background(rarityColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center) {
                            Text(speciesEmoji(pet.species), fontSize = 40.sp)
                        }
                    }

                    if (pet.isVerified) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(3.dp).size(14.dp))
                    }
                }

                Text(pet.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = rarityColor.copy(alpha = 0.13f)) {
                        Text(pet.rarity.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = rarityColor)
                    }
                    Text("Lv.${pet.bondLevel}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Back face — virtue gradient + companion title, mirrors ProfileScreen's PetCardBack
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetCardBack(pet: PetSummary, rarityColor: Color) {
    val virtue = try { Virtue.valueOf(pet.virtue.uppercase()) } catch (_: Exception) { Virtue.WISDOM }
    val virtueInfo = VirtueConfig[virtue]

    val (gradStart, gradMid, gradEnd) = when (virtue) {
        Virtue.COURAGE    -> Triple(Color(0xFF7B0000), Color(0xFFBF360C), Color(0xFFE53935))
        Virtue.WISDOM     -> Triple(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF5C6BC0))
        Virtue.DILIGENCE  -> Triple(Color(0xFF4E2800), Color(0xFF8D4B00), Color(0xFFEF6C00))
        Virtue.TEMPERANCE -> Triple(Color(0xFF004D40), Color(0xFF00695C), Color(0xFF26A69A))
        Virtue.COMPASSION -> Triple(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFEC407A))
    }

    Card(elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        shape = MaterialTheme.shapes.medium) {
        Box(modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradStart, gradMid, gradEnd)))) {

            // Faint emblem background
            Image(painter = painterResource(virtueInfo.emblemRes), contentDescription = null,
                modifier = Modifier.size(140.dp).align(Alignment.Center).graphicsLayer { alpha = 0.13f },
                contentScale = ContentScale.Fit)

            // Small emblem badge top-left
            Box(modifier = Modifier.align(Alignment.TopStart).padding(9.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)).padding(7.dp)) {
                Image(painter = painterResource(virtueInfo.emblemRes), contentDescription = virtue.name,
                    modifier = Modifier.size(16.dp))
            }

            // Companion title
            val earnedTitle = when {
                pet.bondLevel >= 50 -> "Eternal Companion"
                pet.bondLevel >= 40 -> "Soul Bonded"
                pet.bondLevel >= 30 -> "Elite Protector"
                pet.bondLevel >= 25 -> "Bonded Guardian"
                pet.bondLevel >= 20 -> "Master Companion"
                pet.bondLevel >= 15 -> "Lifelong Partner"
                pet.bondLevel >= 10 -> "Loyal Friend"
                pet.bondLevel >= 5  -> "Trusted Companion"
                else                -> "New Companion"
            }

            val words = earnedTitle.split(" ")
            Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                if (words.size >= 2) {
                    Text(words.first(), fontSize = 13.sp, fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center, letterSpacing = 4.sp)
                    Text(words.drop(1).joinToString(" ").uppercase(), fontSize = 21.sp,
                        fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp, lineHeight = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(earnedTitle.uppercase(), fontSize = 21.sp, fontWeight = FontWeight.Black,
                        color = Color.White, textAlign = TextAlign.Center, letterSpacing = (-0.5).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Achievements section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpAchievementsSection(grouped: List<Pair<AchievCategoryDef, List<String>>>) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor =
            if (grouped.isNotEmpty()) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint =
                    if (grouped.isNotEmpty()) Color(0xFFF9A825) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                Text("Achievements", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (grouped.isNotEmpty()) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurfaceVariant)
                if (grouped.isNotEmpty()) {
                    val totalCount = grouped.sumOf { it.second.size }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF9A825).copy(alpha = 0.20f)) {
                        Text("$totalCount unlocked", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF9A825))
                    }
                }
            }

            if (grouped.isEmpty()) {
                Text("No achievements unlocked yet.\nComplete tasks and care for your pets to earn badges!",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
            } else {
                grouped.forEachIndexed { index, (category, titles) ->
                    SpAchievCategoryRow(category, titles)
                    if (index < grouped.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpAchievCategoryRow(category: AchievCategoryDef, titles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(14.dp))
            Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = category.color)
        }
        titles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { title ->
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                        color = category.color.copy(alpha = 0.10f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(category.icon, null, tint = category.color, modifier = Modifier.size(14.dp))
                            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = category.color,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
