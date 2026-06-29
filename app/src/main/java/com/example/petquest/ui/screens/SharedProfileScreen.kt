// app/src/main/java/com/example/petquest/ui/screens/SharedProfileScreen.kt
// HOW TO APPLY: Open this file → Ctrl+A → Delete → Paste this entire file
// CHANGES:
//   1. Replaced FirebaseRepository with SupabaseRepository in the function signature
//   2. Added SpPetPhoto helper composable that handles BOTH:
//      - Real https:// URLs (Supabase Storage) → AsyncImage works perfectly
//      - Old data:image/jpeg;base64,... strings (from the old Firebase era) → decoded to Bitmap
//      THIS IS THE BUG FIX: AsyncImage cannot load data: URIs natively.
//      Both web page and Trainer Profile now show real photos.

package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import coil.compose.AsyncImage
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.Virtue
import com.example.petquest.data.repository.SupabaseRepository
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
    "New Year Celebrant",
    "Beloved Keeper",
    "Lucky Lantern Holder",
    "Easter Hatchling",
    "Sun Keeper",
    "Pumpkin Master",
    "Festive Keeper"
)

private val EVENT_BADGE_EMOJI = mapOf(
    "New Year Celebrant"   to "\uD83C\uDF8A",
    "Beloved Keeper"       to "\uD83D\uDC9D",
    "Lucky Lantern Holder" to "\uD83C\uDFEE",
    "Easter Hatchling"     to "\uD83D\uDC23",
    "Sun Keeper"           to "\u2600\uFE0F",
    "Pumpkin Master"       to "\uD83C\uDF83",
    "Festive Keeper"       to "\uD83C\uDF84"
)

// ─────────────────────────────────────────────────────────────────────────────
// Achievement category definitions
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
// THE KEY FIX: SpPetPhoto — handles both real URLs and base64 data URIs
//
// WHY THIS WAS BROKEN:
//   AsyncImage (Coil) loads https:// and file:// URIs perfectly.
//   But "data:image/jpeg;base64,..." is NOT supported by Coil — it just
//   shows nothing, which is exactly why you saw blank spaces.
//
// THE FIX:
//   If the photoUri starts with "data:", we manually decode the base64
//   into a Bitmap and draw it with Image().
//   If it's a real https:// URL (new Supabase photos), AsyncImage works fine.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetPhoto(photoUri: String, name: String, modifier: Modifier = Modifier) {
    if (photoUri.startsWith("data:image")) {
        // Decode base64 data URI into a Bitmap — cached with remember so it
        // only decodes once per unique URI, not on every recomposition.
        val bitmap = remember(photoUri) {
            try {
                val base64Part = photoUri.substringAfter("base64,")
                val bytes      = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap             = bitmap,
                contentDescription = "$name photo",
                modifier           = modifier,
                contentScale       = ContentScale.Crop
            )
        }
    } else {
        // Real https:// URL (Supabase Storage) — AsyncImage handles this perfectly
        AsyncImage(
            model              = photoUri,
            contentDescription = "$name photo",
            modifier           = modifier,
            contentScale       = ContentScale.Crop
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable — now accepts SupabaseRepository instead of FirebaseRepository
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedProfileScreen(
    uid                  : String,
    supabaseRepository   : SupabaseRepository,
    onBackClick          : () -> Unit
) {
    var state by remember { mutableStateOf<SharedProfileState>(SharedProfileState.Loading) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = SharedProfileState.Loading
            try {
                val profile = supabaseRepository.fetchProfile(uid)
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

    val xpInLevel = profile.bondPoints % 100
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
        item { SpTrainerHeader(profile) }
        item { SpLevelCard(level = profile.level, xpInLevel = xpInLevel, xpProgress = xpProgress) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpStatCard(Modifier.weight(1f), "${profile.bondPoints}", "Total Bond Pts",
                    Color(0xFF1B5E20), Color(0xFFE8F5E9))
                SpStatCard(Modifier.weight(1f), "${profile.petCount}", "Active Pets",
                    Color(0xFF4A148C), Color(0xFFF3E5F5))
            }
        }
        if (profile.streak > 0) {
            item { SpStreakBanner(profile.streak) }
        }
        item { SpEventBadgesSection(eventBadges) }
        item {
            SpSpeciesCard(
                collected       = profile.speciesCount,
                total           = totalSpecies,
                speciesProgress = speciesProgress
            )
        }
        if (profile.pets.isNotEmpty()) {
            item {
                Text("My Pets (${profile.petCount})", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            }
            item { SpPetsGrid(profile.pets) }
        }
        item { SpAchievementsSection(groupedAchievs) }
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
                                    Text(EVENT_BADGE_EMOJI[title] ?: "🏅", fontSize = 20.sp)
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
// Pets grid
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
// Flippable pet card
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
        Box(modifier = Modifier.graphicsLayer {
            rotationY = 180f
            alpha     = if (rotation > 90f) 1f else 0f
        }) {
            SpPetCardBack(pet = pet, rarityColor = rarityColor)
        }
        Box(modifier = Modifier.graphicsLayer {
            alpha = if (rotation <= 90f) 1f else 0f
        }) {
            SpPetCardFront(pet = pet, rarityColor = rarityColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Front face — FIXED: now uses SpPetPhoto which handles data: URIs and real URLs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetCardFront(pet: PetSummary, rarityColor: Color) {

    val tier = when {
        pet.bondLevel >= 50 -> 5
        pet.bondLevel >= 30 -> 4
        pet.bondLevel >= 20 -> 3
        pet.bondLevel >= 10 -> 2
        pet.bondLevel >= 5  -> 1
        else                -> 0
    }

    val borderColors: List<Color> = when (tier) {
        1 -> listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFB0BEC5))
        2 -> listOf(Color(0xFF1565C0), Color(0xFF64B5F6), Color(0xFF1565C0))
        3 -> listOf(Color(0xFFFFD700), Color(0xFFFFF8DC), Color(0xFFFFB300), Color(0xFFFFD700))
        4 -> listOf(Color(0xFFE040FB), Color(0xFFFFD700), Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFE040FB))
        5 -> listOf(Color(0xFFFF1744), Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF1744))
        else -> emptyList()
    }

    val borderWidth = when (tier) {
        0 -> 0.dp; 1 -> 1.5.dp; 2 -> 2.dp; 3 -> 2.5.dp; 4 -> 3.dp; else -> 3.5.dp
    }

    val lvColor: Color = when (tier) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> Color(0xFFB0BEC5)
        2 -> Color(0xFF42A5F5)
        3 -> Color(0xFFFFD700)
        4 -> Color(0xFFE040FB)
        else -> Color(0xFFFF6D00)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sp_pulse_${pet.name}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = if (tier >= 3) 0.65f else 1f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (tier >= 4) 800 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sp_pa"
    )

    val borderBrush: Brush? = when {
        tier == 0 -> null
        tier >= 3  -> Brush.linearGradient(borderColors.map { it.copy(alpha = pulseAlpha) })
        else       -> Brush.linearGradient(borderColors)
    }

    val stripeColors = if (borderColors.isNotEmpty())
        listOf(borderColors.first(), rarityColor, borderColors.last())
    else
        listOf(rarityColor, rarityColor.copy(alpha = 0.45f))

    Card(
        elevation = CardDefaults.cardElevation((4 + tier).dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((5 + tier).dp)
                    .background(Brush.horizontalGradient(stripeColors))
            )

            Column(
                modifier            = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(
                            if (borderBrush != null)
                                Modifier.border(borderWidth, borderBrush, MaterialTheme.shapes.small)
                            else Modifier
                        )
                        .clip(MaterialTheme.shapes.small),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (!pet.photoUri.isNullOrBlank()) {
                        SpPetPhoto(photoUri = pet.photoUri, name = pet.name, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(rarityColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(speciesEmoji(pet.species), fontSize = 40.sp)
                        }
                    }

                    Column(
                        modifier            = Modifier.padding(3.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (pet.isVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(14.dp)
                            )
                        }
                        if (tier >= 1) {
                            val tierVecIcon = if (tier >= 5) Icons.Default.EmojiEvents
                            else if (tier >= 4) Icons.Default.AutoAwesome
                            else Icons.Default.Star
                            Icon(
                                imageVector        = tierVecIcon,
                                contentDescription = null,
                                tint               = lvColor.copy(alpha = if (tier >= 3) pulseAlpha else 1f),
                                modifier           = Modifier.size(12.dp)
                            )
                        }
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
                        color      = lvColor
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Back face
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpPetCardBack(pet: PetSummary, rarityColor: Color) {
    val virtue     = try { Virtue.valueOf(pet.virtue.uppercase()) } catch (_: Exception) { Virtue.WISDOM }
    val virtueInfo = VirtueConfig[virtue]

    val (gradStart, gradMid, gradEnd) = when (virtue) {
        Virtue.COURAGE    -> Triple(Color(0xFF7B0000), Color(0xFFBF360C), Color(0xFFE53935))
        Virtue.WISDOM     -> Triple(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF5C6BC0))
        Virtue.DILIGENCE  -> Triple(Color(0xFF4E2800), Color(0xFF8D4B00), Color(0xFFEF6C00))
        Virtue.TEMPERANCE -> Triple(Color(0xFF004D40), Color(0xFF00695C), Color(0xFF26A69A))
        Virtue.COMPASSION -> Triple(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFEC407A))
    }

    val tier = when {
        pet.bondLevel >= 50 -> 5
        pet.bondLevel >= 30 -> 4
        pet.bondLevel >= 20 -> 3
        pet.bondLevel >= 10 -> 2
        pet.bondLevel >= 5  -> 1
        else                -> 0
    }

    val haloColor = when (tier) {
        0, 1 -> Color.White
        2    -> Color(0xFF90CAF9)
        3    -> Color(0xFFFFE082)
        4    -> Color(0xFFCE93D8)
        else -> Color(0xFFFF8A65)
    }

    val emblemAlpha = when {
        pet.bondLevel >= 50 -> 0.55f
        pet.bondLevel >= 30 -> 0.42f
        pet.bondLevel >= 20 -> 0.32f
        pet.bondLevel >= 10 -> 0.22f
        pet.bondLevel >= 5  -> 0.17f
        else                -> 0.13f
    }

    val emblemSize = when {
        pet.bondLevel >= 50 -> 160.dp
        pet.bondLevel >= 30 -> 150.dp
        pet.bondLevel >= 20 -> 145.dp
        else                -> 140.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sp_back_${pet.name}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = if (tier >= 2) 0.4f else 1f,
        targetValue   = if (tier >= 2) 0.9f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sp_bp"
    )

    val cornerDotAlpha = if (tier >= 3) pulseAlpha * 0.7f else 0f

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

    val tierBgColor = when (tier) {
        1 -> Color(0xFFB0BEC5); 2 -> Color(0xFF1565C0); 3 -> Color(0xFFFFB300)
        4 -> Color(0xFF7B1FA2); else -> Color(0xFFBF360C)
    }

    Card(
        elevation = CardDefaults.cardElevation((4 + tier).dp),
        modifier  = Modifier.fillMaxWidth().fillMaxHeight(),
        shape     = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(gradStart, gradMid, gradEnd)))
        ) {
            // Outer aura (tier 2+)
            if (tier >= 2) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 32.dp)
                        .align(Alignment.Center)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(haloColor.copy(alpha = pulseAlpha * 0.18f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Second outer ring (tier 4+)
            if (tier >= 4) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 60.dp)
                        .align(Alignment.Center)
                        .border(
                            1.dp,
                            Brush.sweepGradient(listOf(
                                haloColor.copy(alpha = pulseAlpha * 0.5f),
                                Color.Transparent,
                                haloColor.copy(alpha = pulseAlpha * 0.5f)
                            )),
                            CircleShape
                        )
                )
            }

            // Inner ring (tier 3+)
            if (tier >= 3) {
                Box(
                    modifier = Modifier
                        .size(emblemSize + 10.dp)
                        .align(Alignment.Center)
                        .border(
                            1.5.dp,
                            Brush.sweepGradient(listOf(
                                haloColor.copy(alpha = pulseAlpha * 0.7f),
                                Color.Transparent,
                                haloColor.copy(alpha = pulseAlpha * 0.7f)
                            )),
                            CircleShape
                        )
                )
            }

            // Central emblem
            Image(
                painter            = painterResource(virtueInfo.emblemRes),
                contentDescription = null,
                modifier           = Modifier
                    .size(emblemSize)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = emblemAlpha },
                contentScale       = ContentScale.Fit
            )

            // Corner sparkles — Icon, no emoji (tier 3+)
            if (tier >= 3) {
                val dotIcon   = if (tier >= 5) Icons.Default.EmojiEvents else Icons.Default.Star
                val dotSize   = if (tier >= 4) 10.dp else 8.dp
                val dotAlpha  = cornerDotAlpha
                Icon(dotIcon, null, tint = haloColor.copy(alpha = dotAlpha),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(dotSize))
                Icon(dotIcon, null, tint = haloColor.copy(alpha = dotAlpha),
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).size(dotSize))
                if (tier >= 4) {
                    Icon(dotIcon, null, tint = haloColor.copy(alpha = dotAlpha * 0.6f),
                        modifier = Modifier.align(Alignment.TopStart).padding(28.dp).size(dotSize))
                    Icon(dotIcon, null, tint = haloColor.copy(alpha = dotAlpha * 0.6f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp).size(dotSize))
                }
            }

            // Virtue icon pill — top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .padding(7.dp)
            ) {
                Image(
                    painter            = painterResource(virtueInfo.emblemRes),
                    contentDescription = virtue.name,
                    modifier           = Modifier.size(16.dp)
                )
            }

            // Level shown on front card — back stays clean, aura rings show progress

            // Title — bottom gradient overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val words = earnedTitle.split(" ")
                if (words.size >= 2) {
                    Text(words.first(), fontSize = 10.sp, fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.70f), textAlign = TextAlign.Center, letterSpacing = 3.sp)
                    Text(words.drop(1).joinToString(" ").uppercase(), fontSize = 18.sp,
                        fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp, lineHeight = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(earnedTitle.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black,
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
