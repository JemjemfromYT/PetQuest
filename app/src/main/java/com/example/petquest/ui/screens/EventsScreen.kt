// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/EventsScreen.kt
// COPY THIS ENTIRE FILE — create it fresh in Android Studio
// ============================================================

package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

// ──────────────────────────────────────────────────────────────────────────────
// Data Model
// ──────────────────────────────────────────────────────────────────────────────

data class SeasonalEvent(
    val id          : String,
    val name        : String,
    val emoji       : String,
    val description : String,
    val startMonth  : Int,
    val startDay    : Int,
    val endMonth    : Int,
    val endDay      : Int,
    val gradientStart : Color,
    val gradientEnd   : Color,
    val accentColor   : Color,
    val rewards     : List<EventReward>
)

data class EventReward(
    val task        : String,
    val reward      : String,
    val icon        : String
)

// ──────────────────────────────────────────────────────────────────────────────
// All Seasonal Events (hardcoded, no backend needed)
// ──────────────────────────────────────────────────────────────────────────────

val ALL_SEASONAL_EVENTS = listOf(
    SeasonalEvent(
        id            = "new_year",
        name          = "New Year Celebration",
        emoji         = "🎊",
        description   = "Ring in the new year with your pets! Gold confetti and sparkling bonds await.",
        startMonth    = 12, startDay = 26,
        endMonth      = 1,  endDay   = 7,
        gradientStart = Color(0xFFFFC107),
        gradientEnd   = Color(0xFFFF6F00),
        accentColor   = Color(0xFFFFD54F),
        rewards = listOf(
            EventReward("Complete all pet tasks 3 days in a row", "✨ Golden Confetti Badge", "🏅"),
            EventReward("Log in 5 days during the event",          "🎊 New Year Crest",        "🎊"),
            EventReward("Earn 500+ bond points today",             "⭐ Star Keeper Title",      "⭐")
        )
    ),
    SeasonalEvent(
        id            = "valentine",
        name          = "Valentine's Day",
        emoji         = "💝",
        description   = "Celebrate the love between you and your pets. Hearts, roses, and extra bond points!",
        startMonth    = 2, startDay = 1,
        endMonth      = 2, endDay   = 14,
        gradientStart = Color(0xFFE91E63),
        gradientEnd   = Color(0xFFFF80AB),
        accentColor   = Color(0xFFFF4081),
        rewards = listOf(
            EventReward("Complete your pet's virtue task today",   "💖 Heart Locket Badge",     "💖"),
            EventReward("Earn bond points 7 days in a row",        "🌹 Rose Gold Title",        "🌹"),
            EventReward("Verify a pet this week",                  "💝 Beloved Keeper Badge",   "💝")
        )
    ),
    SeasonalEvent(
        id            = "chinese_new_year",
        name          = "Lunar New Year",
        emoji         = "🏮",
        description   = "Celebrate the Lunar New Year with festive lanterns and golden blessings for your pets!",
        startMonth    = 1, startDay = 25,
        endMonth      = 2, endDay   = 9,
        gradientStart = Color(0xFFD32F2F),
        gradientEnd   = Color(0xFFFF6F00),
        accentColor   = Color(0xFFFFD700),
        rewards = listOf(
            EventReward("Complete all tasks on New Year's Day",  "🏮 Lucky Lantern Badge",    "🏮"),
            EventReward("Feed your pet 5 days in a row",         "🐉 Dragon Keeper Title",    "🐉"),
            EventReward("Earn 1000 total bond points",           "🥢 Fortune Bond Crest",     "🥢")
        )
    ),
    SeasonalEvent(
        id            = "easter",
        name          = "Easter Egg Hunt",
        emoji         = "🐣",
        description   = "Spring has arrived! Hunt for Easter eggs hidden in your daily tasks.",
        startMonth    = 3, startDay = 30,
        endMonth      = 4, endDay   = 20,
        gradientStart = Color(0xFF9C27B0),
        gradientEnd   = Color(0xFF81C784),
        accentColor   = Color(0xFFCE93D8),
        rewards = listOf(
            EventReward("Complete all optional tasks this week",  "🐣 Easter Hatchling Badge",  "🐣"),
            EventReward("Keep a 5-day streak during the event",  "🌸 Spring Spirit Title",      "🌸"),
            EventReward("Bond with 3 different pets today",       "🥚 Golden Egg Crest",         "🥚")
        )
    ),
    SeasonalEvent(
        id            = "summer",
        name          = "Summer Splash",
        emoji         = "☀️",
        description   = "The sunniest season is here! Keep your pets cool and happy all summer long.",
        startMonth    = 6, startDay = 21,
        endMonth      = 8, endDay   = 31,
        gradientStart = Color(0xFF0288D1),
        gradientEnd   = Color(0xFFFDD835),
        accentColor   = Color(0xFF29B6F6),
        rewards = listOf(
            EventReward("Complete all core tasks for 7 days",     "☀️ Sun Keeper Badge",        "☀️"),
            EventReward("Give your pet fresh water every day",    "💧 Cool Blue Title",          "💧"),
            EventReward("Reach a 14-day streak",                  "🌊 Wave Rider Crest",         "🌊")
        )
    ),
    SeasonalEvent(
        id            = "halloween",
        name          = "Halloween",
        emoji         = "🎃",
        description   = "Spooky season is here! Trick or treat your way through special Halloween tasks.",
        startMonth    = 10, startDay = 1,
        endMonth      = 10, endDay   = 31,
        gradientStart = Color(0xFF6A1B9A),
        gradientEnd   = Color(0xFFE65100),
        accentColor   = Color(0xFFFF6F00),
        rewards = listOf(
            EventReward("Complete tasks after 6 PM for 3 days",  "🎃 Pumpkin Master Badge",    "🎃"),
            EventReward("Earn bond points on Halloween day",      "👻 Spooky Keeper Title",      "👻"),
            EventReward("Keep a 7-day streak this month",         "🦇 Night Guardian Crest",     "🦇")
        )
    ),
    SeasonalEvent(
        id            = "christmas",
        name          = "Christmas",
        emoji         = "🎄",
        description   = "The most wonderful time of year! Share the holiday spirit with your beloved pets.",
        startMonth    = 12, startDay = 1,
        endMonth      = 12, endDay   = 25,
        gradientStart = Color(0xFF1B5E20),
        gradientEnd   = Color(0xFFD32F2F),
        accentColor   = Color(0xFF81C784),
        rewards = listOf(
            EventReward("Complete all tasks on Christmas Eve",   "🎄 Festive Keeper Badge",    "🎄"),
            EventReward("Keep a 12-day streak in December",      "🌟 Yuletide Star Title",      "🌟"),
            EventReward("Gift your pet a special task today",    "🎁 Secret Santa Crest",       "🎁")
        )
    )
)

// ──────────────────────────────────────────────────────────────────────────────
// Date Helpers (works for events that span year boundary like New Year Dec26–Jan7)
// ──────────────────────────────────────────────────────────────────────────────

fun isEventActive(event: SeasonalEvent, today: LocalDate): Boolean {
    val start = eventStartDate(event, today.year)
    val end   = eventEndDate(event, today.year)
    // Handle year-spanning events (e.g. Dec26–Jan7)
    return if (end.isBefore(start)) {
        today >= start || today <= end
    } else {
        today >= start && today <= end
    }
}

fun eventStartDate(event: SeasonalEvent, year: Int): LocalDate =
    LocalDate.of(year, event.startMonth, event.startDay)

fun eventEndDate(event: SeasonalEvent, year: Int): LocalDate {
    val endYear = if (event.endMonth < event.startMonth) year + 1 else year
    return LocalDate.of(endYear, event.endMonth, event.endDay)
}

fun daysUntilEvent(event: SeasonalEvent, today: LocalDate): Long {
    var start = eventStartDate(event, today.year)
    if (start.isBefore(today)) start = eventStartDate(event, today.year + 1)
    return ChronoUnit.DAYS.between(today, start)
}

fun daysRemainingInEvent(event: SeasonalEvent, today: LocalDate): Long {
    var end = eventEndDate(event, today.year)
    if (end.isBefore(today)) end = eventEndDate(event, today.year + 1)
    return ChronoUnit.DAYS.between(today, end)
}

fun getActiveEvent(today: LocalDate): SeasonalEvent? =
    ALL_SEASONAL_EVENTS.firstOrNull { isEventActive(it, today) }

fun getNextEvent(today: LocalDate): SeasonalEvent? =
    ALL_SEASONAL_EVENTS
        .filter { !isEventActive(it, today) }
        .minByOrNull { daysUntilEvent(it, today) }

// Get the header gradient for whatever event is active right now
fun getActiveEventGradient(today: LocalDate): Pair<Color, Color>? {
    val event = getActiveEvent(today) ?: return null
    return event.gradientStart to event.gradientEnd
}

// ──────────────────────────────────────────────────────────────────────────────
// Main EventsScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen() {
    val today        = remember { LocalDate.now() }
    val activeEvent  = remember { getActiveEvent(today) }
    val nextEvent    = remember { getNextEvent(today) }

    val snackbarHostState = remember { SnackbarHostState() }
    var claimedBonus by remember { mutableStateOf(false) }

    // Determine header gradient: event colors if active, else default orange
    val headerStart = activeEvent?.gradientStart ?: Color(0xFFFF8C42)
    val headerEnd   = activeEvent?.gradientEnd   ?: Color(0xFFFFB77A)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(headerStart, headerEnd))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text          = if (activeEvent != null) "${activeEvent.emoji} Events" else "🎪 Events",
                    fontSize      = 24.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding      = PaddingValues(vertical = 16.dp)
        ) {

            // ── Active Event Banner ────────────────────────────────────────────
            if (activeEvent != null) {
                item {
                    ActiveEventBanner(
                        event          = activeEvent,
                        today          = today,
                        claimedBonus   = claimedBonus,
                        onClaimBonus   = {
                            claimedBonus = true
                        },
                        snackbarHost   = snackbarHostState
                    )
                }

                // ── Limited Rewards ───────────────────────────────────────────
                item {
                    EventRewardsCard(event = activeEvent)
                }

                // ── Event Theme Hint ──────────────────────────────────────────
                item {
                    EventThemeCard(event = activeEvent)
                }
            } else {
                // No active event
                item {
                    NoActiveEventCard()
                }
            }

            // ── Coming Up Next ─────────────────────────────────────────────────
            if (nextEvent != null) {
                item {
                    ComingSoonCard(event = nextEvent, today = today)
                }
            }

            // ── All Events Calendar ────────────────────────────────────────────
            item {
                Text(
                    "All Events",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                EventsCalendarRow(today = today)
            }

            // Bottom breathing room
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Active Event Banner
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ActiveEventBanner(
    event        : SeasonalEvent,
    today        : LocalDate,
    claimedBonus : Boolean,
    onClaimBonus : () -> Unit,
    snackbarHost : SnackbarHostState
) {
    val daysLeft     = daysRemainingInEvent(event, today)
    val countdownText = when (daysLeft) {
        0L   -> "Last day!"
        1L   -> "1 day left!"
        else -> "$daysLeft days left"
    }

    // Pulse animation on the emoji
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val emojiScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )

    val scope = rememberCoroutineScope()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(event.gradientStart, event.gradientEnd)
                    )
                )
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF69F0AE))
                        )
                        Text(
                            "ACTIVE NOW",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Big emoji
                Text(
                    event.emoji,
                    fontSize = 64.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                // Event name
                Text(
                    event.name,
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    textAlign     = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                // Description
                Text(
                    event.description,
                    fontSize   = 14.sp,
                    color      = Color.White.copy(alpha = 0.90f),
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Countdown chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.20f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Timer,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(16.dp)
                        )
                        Text(
                            countdownText,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Claim Daily Bonus button
                Button(
                    onClick = {
                        if (!claimedBonus) {
                            onClaimBonus()
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                snackbarHost.showSnackbar(
                                    message = "${event.emoji} Daily event bonus claimed! Come back tomorrow.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    enabled = !claimedBonus,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = event.gradientStart,
                        disabledContainerColor = Color.White.copy(alpha = 0.40f),
                        disabledContentColor   = Color.White
                    ),
                    shape   = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (claimedBonus) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Bonus Claimed",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 15.sp
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Claim Daily Event Bonus",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Limited Rewards Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventRewardsCard(event: SeasonalEvent) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = event.gradientStart.copy(alpha = 0.07f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = event.accentColor,
                    modifier           = Modifier.size(20.dp)
                )
                Text(
                    "Limited-Time Rewards",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            }

            HorizontalDivider(color = event.accentColor.copy(alpha = 0.20f))

            event.rewards.forEach { reward ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    // Icon bubble
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = event.accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(reward.icon, fontSize = 20.sp)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            reward.task,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 18.sp
                        )
                        Text(
                            "Reward: ${reward.reward}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = event.gradientStart
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Event Theme Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventThemeCard(event: SeasonalEvent) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color swatch row
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                listOf(event.gradientStart, event.accentColor, event.gradientEnd).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c)
                            .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Event Theme",
                    fontSize   = 11.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${event.name} palette is active throughout the app",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// No Active Event Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun NoActiveEventCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🌙", fontSize = 48.sp)
            Text(
                "No Active Event",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )
            Text(
                "Check back soon — the next event is almost here!",
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Coming Soon Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ComingSoonCard(event: SeasonalEvent, today: LocalDate) {
    val daysAway = daysUntilEvent(event, today)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            event.gradientStart.copy(alpha = 0.12f),
                            event.gradientEnd.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Event emoji in a colored circle
            Surface(
                shape    = RoundedCornerShape(18.dp),
                color    = event.gradientStart.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(event.emoji, fontSize = 30.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = event.gradientStart.copy(alpha = 0.15f)
                ) {
                    Text(
                        "COMING SOON",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = event.gradientStart,
                        letterSpacing = 1.sp,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    event.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    if (daysAway == 1L) "Tomorrow!" else "In $daysAway days",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = event.gradientStart
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// All Events Calendar Row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventsCalendarRow(today: LocalDate) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding        = PaddingValues(vertical = 4.dp)
    ) {
        items(ALL_SEASONAL_EVENTS) { event ->
            val isActive = isEventActive(event, today)
            val daysAway = if (!isActive) daysUntilEvent(event, today) else 0L

            Card(
                modifier  = Modifier.width(140.dp),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(if (isActive) 4.dp else 1.dp),
                border    = if (isActive)
                    BorderStroke(2.dp, event.accentColor)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                if (isActive)
                                    listOf(event.gradientStart.copy(alpha = 0.20f), event.gradientEnd.copy(alpha = 0.05f))
                                else
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        )
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(event.emoji, fontSize = 32.sp)
                    Text(
                        event.name,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 16.sp
                    )
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = event.accentColor
                        ) {
                            Text(
                                "ACTIVE",
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White,
                                letterSpacing = 0.8.sp,
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Text(
                            if (daysAway == 1L) "Tomorrow" else "In $daysAway days",
                            fontSize  = 11.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
