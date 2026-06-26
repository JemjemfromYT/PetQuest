// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/EventsScreen.kt
// COPY THIS ENTIRE FILE — replace the existing one in Android Studio
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import com.example.petquest.viewmodel.PetQuestViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

// ──────────────────────────────────────────────────────────────────────────────
// Simple date helper — works on API 24+ (no java.time needed)
// ──────────────────────────────────────────────────────────────────────────────

data class SimpleDate(val year: Int, val month: Int, val day: Int)

fun todaySimpleDate(): SimpleDate {
    val c = Calendar.getInstance()
    return SimpleDate(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,   // Calendar.MONTH is 0-based
        c.get(Calendar.DAY_OF_MONTH)
    )
}

private fun SimpleDate.toMillis(): Long {
    val c = Calendar.getInstance()
    c.set(year, month - 1, day, 0, 0, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun SimpleDate.isBeforeOrEqual(other: SimpleDate) = this.toMillis() <= other.toMillis()
private fun SimpleDate.isAfterOrEqual(other: SimpleDate)  = this.toMillis() >= other.toMillis()

private fun daysBetween(from: SimpleDate, to: SimpleDate): Long {
    val diff = to.toMillis() - from.toMillis()
    return diff / (24L * 60 * 60 * 1000)
}

// ──────────────────────────────────────────────────────────────────────────────
// Data Model
// ──────────────────────────────────────────────────────────────────────────────

data class SeasonalEvent(
    val id           : String,
    val name         : String,
    val emoji        : String,
    val description  : String,
    val startMonth   : Int,
    val startDay     : Int,
    val endMonth     : Int,
    val endDay       : Int,
    val gradientStart: Color,
    val gradientEnd  : Color,
    val accentColor  : Color,
    val badgeTitle   : String,       // Achievement title unlocked on claim
    val badgeEmoji   : String        // Emoji shown on the badge chip in Profile
)

// ──────────────────────────────────────────────────────────────────────────────
// All Seasonal Events — badgeTitle must be unique, shown on Profile
// ──────────────────────────────────────────────────────────────────────────────

val ALL_SEASONAL_EVENTS = listOf(
    SeasonalEvent(
        id = "new_year", name = "New Year Celebration", emoji = "🎊",
        description    = "Ring in the new year with your pets! Complete all today's tasks and claim your New Year bonus.",
        startMonth = 12, startDay = 26, endMonth = 1, endDay = 7,
        gradientStart  = Color(0xFFFFC107), gradientEnd = Color(0xFFFF6F00),
        accentColor    = Color(0xFFFFD54F),
        badgeTitle     = "New Year Celebrant",
        badgeEmoji     = "🎊"
    ),
    SeasonalEvent(
        id = "valentine", name = "Valentine's Day", emoji = "💝",
        description    = "Celebrate the love between you and your pets. Finish all tasks today and claim your Valentine bonus.",
        startMonth = 2, startDay = 1, endMonth = 2, endDay = 14,
        gradientStart  = Color(0xFFE91E63), gradientEnd = Color(0xFFFF80AB),
        accentColor    = Color(0xFFFF4081),
        badgeTitle     = "Beloved Keeper",
        badgeEmoji     = "💝"
    ),
    SeasonalEvent(
        id = "chinese_new_year", name = "Lunar New Year", emoji = "🏮",
        description    = "Celebrate the Lunar New Year with golden blessings. Complete all tasks and claim your Lucky bonus.",
        startMonth = 1, startDay = 25, endMonth = 2, endDay = 9,
        gradientStart  = Color(0xFFD32F2F), gradientEnd = Color(0xFFFF6F00),
        accentColor    = Color(0xFFFFD700),
        badgeTitle     = "Lucky Lantern Holder",
        badgeEmoji     = "🏮"
    ),
    SeasonalEvent(
        id = "easter", name = "Easter Egg Hunt", emoji = "🐣",
        description    = "Spring has arrived! Hunt for Easter eggs hidden in your daily tasks and claim your Spring bonus.",
        startMonth = 3, startDay = 30, endMonth = 4, endDay = 20,
        gradientStart  = Color(0xFF9C27B0), gradientEnd = Color(0xFF81C784),
        accentColor    = Color(0xFFCE93D8),
        badgeTitle     = "Easter Hatchling",
        badgeEmoji     = "🐣"
    ),
    SeasonalEvent(
        id = "summer", name = "Summer Splash", emoji = "☀️",
        description    = "The sunniest season is here! Keep your pets cool and happy. Finish all tasks daily to claim your Sun bonus.",
        startMonth = 6, startDay = 21, endMonth = 8, endDay = 31,
        gradientStart  = Color(0xFF0288D1), gradientEnd = Color(0xFFFDD835),
        accentColor    = Color(0xFF29B6F6),
        badgeTitle     = "Sun Keeper",
        badgeEmoji     = "☀️"
    ),
    SeasonalEvent(
        id = "halloween", name = "Halloween", emoji = "🎃",
        description    = "Spooky season is here! Trick or treat your way through tasks and claim your Halloween bonus.",
        startMonth = 10, startDay = 1, endMonth = 10, endDay = 31,
        gradientStart  = Color(0xFF6A1B9A), gradientEnd = Color(0xFFE65100),
        accentColor    = Color(0xFFFF6F00),
        badgeTitle     = "Pumpkin Master",
        badgeEmoji     = "🎃"
    ),
    SeasonalEvent(
        id = "christmas", name = "Christmas", emoji = "🎄",
        description    = "The most wonderful time of year! Share the holiday spirit, finish all tasks, and claim your Festive bonus.",
        startMonth = 12, startDay = 1, endMonth = 12, endDay = 25,
        gradientStart  = Color(0xFF1B5E20), gradientEnd = Color(0xFFD32F2F),
        accentColor    = Color(0xFF81C784),
        badgeTitle     = "Festive Keeper",
        badgeEmoji     = "🎄"
    )
)

// Used by ProfileScreen to identify which achievements are event badges
val EVENT_BADGE_TITLES: Set<String> = ALL_SEASONAL_EVENTS.map { it.badgeTitle }.toSet()

// ──────────────────────────────────────────────────────────────────────────────
// Date helpers (all Calendar-based, API 24+ compatible)
// ──────────────────────────────────────────────────────────────────────────────

private fun eventStart(event: SeasonalEvent, year: Int) =
    SimpleDate(year, event.startMonth, event.startDay)

private fun eventEnd(event: SeasonalEvent, year: Int): SimpleDate {
    val endYear = if (event.endMonth < event.startMonth) year + 1 else year
    return SimpleDate(endYear, event.endMonth, event.endDay)
}

fun isEventActive(event: SeasonalEvent, today: SimpleDate): Boolean {
    val start = eventStart(event, today.year)
    val end   = eventEnd(event, today.year)
    return if (end.toMillis() < start.toMillis()) {
        today.isAfterOrEqual(start) || today.isBeforeOrEqual(end)
    } else {
        today.isAfterOrEqual(start) && today.isBeforeOrEqual(end)
    }
}

fun daysUntilEvent(event: SeasonalEvent, today: SimpleDate): Long {
    var start = eventStart(event, today.year)
    if (start.toMillis() <= today.toMillis()) start = eventStart(event, today.year + 1)
    return daysBetween(today, start)
}

fun daysRemainingInEvent(event: SeasonalEvent, today: SimpleDate): Long {
    var end = eventEnd(event, today.year)
    if (end.toMillis() < today.toMillis()) end = eventEnd(event, today.year + 1)
    return daysBetween(today, end)
}

fun getActiveEvent(today: SimpleDate): SeasonalEvent? =
    ALL_SEASONAL_EVENTS.firstOrNull { isEventActive(it, today) }

fun getNextEvent(today: SimpleDate): SeasonalEvent? =
    ALL_SEASONAL_EVENTS
        .filter { !isEventActive(it, today) }
        .minByOrNull { daysUntilEvent(it, today) }

// ──────────────────────────────────────────────────────────────────────────────
// Main EventsScreen — receives viewModel so Claim button is real
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(viewModel: PetQuestViewModel) {
    val today       = remember { todaySimpleDate() }
    val activeEvent = remember { getActiveEvent(today) }
    val nextEvent   = remember { getNextEvent(today) }

    val todaysTasks        by viewModel.todaysTasks.collectAsState()
    val bonusClaimed       by viewModel.todayEventBonusClaimed.collectAsState()
    val allAchievements    by viewModel.allAchievements.collectAsState()

    val allTasksDone = todaysTasks
        .groupBy { it.petId }
        .any { (_, tasks) -> tasks.isNotEmpty() && tasks.all { it.isCompleted } }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    val headerStart = activeEvent?.gradientStart ?: Color(0xFFFF8C42)
    val headerEnd   = activeEvent?.gradientEnd   ?: Color(0xFFFFB77A)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(headerStart, headerEnd)))
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

            if (activeEvent != null) {
                item {
                    ActiveEventBanner(
                        event          = activeEvent,
                        today          = today,
                        allTasksDone   = allTasksDone,
                        bonusClaimed   = bonusClaimed,
                        onClaimBonus   = {
                            viewModel.claimEventBonus(
                                badgeTitle       = activeEvent.badgeTitle,
                                badgeDescription = "Badge earned during ${activeEvent.name}"
                            )
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message  = "${activeEvent.emoji} Bonus claimed! +20 bond pts per pet. Badge unlocked!",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        },
                        onTasksNotDone = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message  = "Finish all today's tasks first to claim the bonus!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }

                item {
                    val alreadyOwns = allAchievements.any {
                        it.title == activeEvent.badgeTitle && it.isUnlocked
                    }
                    EventBadgeCard(
                        event       = activeEvent,
                        badgeEarned = alreadyOwns
                    )
                }
            } else {
                item { NoActiveEventCard() }
            }

            if (nextEvent != null) {
                item { ComingSoonCard(event = nextEvent, today = today) }
            }

            item {
                Text(
                    "All Events",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
            }
            item { EventsCalendarRow(today = today) }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Active Event Banner
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ActiveEventBanner(
    event         : SeasonalEvent,
    today         : SimpleDate,
    allTasksDone  : Boolean,
    bonusClaimed  : Boolean,
    onClaimBonus  : () -> Unit,
    onTasksNotDone: () -> Unit
) {
    val daysLeft      = daysRemainingInEvent(event, today)
    val countdownText = when (daysLeft) {
        0L   -> "Last day!"
        1L   -> "1 day left!"
        else -> "$daysLeft days left"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val emojiScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(event.gradientStart, event.gradientEnd)))
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
                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
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
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(event.emoji, fontSize = 64.sp, modifier = Modifier.scale(emojiScale))

                Text(
                    event.name,
                    fontSize      = 26.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    textAlign     = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

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
                        modifier              = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
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

                // Status hint — show task progress requirement when not claimed
                if (!bonusClaimed && !allTasksDone) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Warning,
                                contentDescription = null,
                                tint               = Color.White.copy(alpha = 0.85f),
                                modifier           = Modifier.size(14.dp)
                            )
                            Text(
                                "Complete all today's tasks to unlock the claim",
                                fontSize   = 12.sp,
                                color      = Color.White.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Claim Daily Bonus button
                Button(
                    onClick = {
                        when {
                            bonusClaimed    -> { /* already claimed, button is disabled */ }
                            !allTasksDone   -> onTasksNotDone()
                            else            -> onClaimBonus()
                        }
                    },
                    enabled = !bonusClaimed,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor         = Color.White,
                        contentColor           = event.gradientStart,
                        disabledContainerColor = Color.White.copy(alpha = 0.40f),
                        disabledContentColor   = Color.White
                    ),
                    shape    = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        bonusClaimed  -> {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bonus Claimed ✓", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        !allTasksDone -> {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Finish Tasks to Claim", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        else          -> {
                            Icon(Icons.Default.CardGiftcard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Claim Daily Event Bonus", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Event Badge Card — shows the exclusive badge users can earn
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventBadgeCard(event: SeasonalEvent, badgeEarned: Boolean) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (badgeEarned)
                event.gradientStart.copy(alpha = 0.10f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Badge icon
            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = if (badgeEarned) event.accentColor.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (badgeEarned) event.badgeEmoji else "🔒",
                        fontSize = 28.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint               = if (badgeEarned) event.accentColor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        "Limited Badge",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (badgeEarned) event.accentColor
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    event.badgeTitle,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    if (badgeEarned)
                        "Earned! Displayed on your Profile."
                    else
                        "Complete all tasks → claim the daily bonus to earn this badge.",
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            if (badgeEarned) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = "Earned",
                    tint               = event.accentColor,
                    modifier           = Modifier.size(22.dp)
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
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🌙", fontSize = 48.sp)
            Text("No Active Event", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(
                "Check back soon — the next event is almost here!",
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Coming Soon Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ComingSoonCard(event: SeasonalEvent, today: SimpleDate) {
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
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = event.gradientStart.copy(alpha = 0.15f)
                ) {
                    Text(
                        "COMING SOON",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = event.gradientStart,
                        letterSpacing = 1.sp,
                        modifier      = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(event.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (daysAway == 1L) "Tomorrow!" else "In $daysAway days",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = event.gradientStart
                )
                Text(
                    "Badge: ${event.badgeEmoji} ${event.badgeTitle}",
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// All Events Calendar Row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EventsCalendarRow(today: SimpleDate) {
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
                border    = if (isActive) BorderStroke(2.dp, event.accentColor) else null
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                if (isActive)
                                    listOf(
                                        event.gradientStart.copy(alpha = 0.20f),
                                        event.gradientEnd.copy(alpha = 0.05f)
                                    )
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
                        lineHeight = 16.sp
                    )
                    if (isActive) {
                        Surface(shape = RoundedCornerShape(6.dp), color = event.accentColor) {
                            Text(
                                "ACTIVE",
                                fontSize      = 9.sp,
                                fontWeight    = FontWeight.ExtraBold,
                                color         = Color.White,
                                letterSpacing = 0.8.sp,
                                modifier      = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
