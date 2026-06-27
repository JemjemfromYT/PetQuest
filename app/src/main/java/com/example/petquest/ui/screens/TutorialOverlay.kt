// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/TutorialOverlay.kt
// NEW FILE — copy into your project
//
// This is a floating cat guide (Whisker) that appears the first
// time a new user lands on the main screen after creating their
// first pet. It walks through each tab with a speech bubble.
// ============================================================

package com.example.petquest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Tutorial step data
// ---------------------------------------------------------------------------
data class TutorialStep(
    val tabIndex : Int,     // which bottom nav tab to spotlight (-1 = none)
    val title    : String,
    val message  : String,
    val isLast   : Boolean = false
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        tabIndex = -1,
        title    = "Hey, I'm Whisker!",
        message  = "Welcome to PetQuest! I'll show you around real quick. It only takes a minute, I promise! 😸"
    ),
    TutorialStep(
        tabIndex = 0,
        title    = "Home",
        message  = "This is your Home screen. You can see all your pets, your streak, bond points, and today's task progress at a glance."
    ),
    TutorialStep(
        tabIndex = 1,
        title    = "Daily Tasks",
        message  = "Every day, head to Tasks to take care of your pets. Completing tasks earns Bond Points and keeps your streak alive! 🔥"
    ),
    TutorialStep(
        tabIndex = 2,
        title    = "Collection",
        message  = "There are 29 pet species to discover. Each pet you own unlocks their entry. How many can you collect? 🐾"
    ),
    TutorialStep(
        tabIndex = 3,
        title    = "Awards",
        message  = "Awards are achievements you unlock by caring for your pets. Keep going and you'll unlock them all — 42 total!"
    ),
    TutorialStep(
        tabIndex = 4,
        title    = "Events",
        message  = "Seasonal events give you bonus challenges and exclusive badges. Check here often so you don't miss out! 🌟"
    ),
    TutorialStep(
        tabIndex = 5,
        title    = "Your Profile",
        message  = "Profile tracks your keeper level, bond points, and all the pets you've raised. It's your pet-keeping legacy!"
    ),
    TutorialStep(
        tabIndex = -1,
        title    = "You're all set!",
        message  = "Take great care of your pets every single day. Small moments build lifelong bonds. Good luck, keeper! 💛",
        isLast   = true
    )
)

// ---------------------------------------------------------------------------
// TutorialOverlay — the full-screen overlay composable
// ---------------------------------------------------------------------------
@Composable
fun TutorialOverlay(
    onDismiss       : () -> Unit,
    onTabHighlight  : (Int) -> Unit  // tells MainScreen which tab to show
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[stepIndex]

    // Notify parent which tab to highlight
    LaunchedEffect(step.tabIndex) {
        if (step.tabIndex >= 0) onTabHighlight(step.tabIndex)
    }

    // Dim background — tap to skip
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication          = null,
                interactionSource   = remember { MutableInteractionSource() }
            ) { /* absorb touches */ }
    ) {

        // ── Whisker + speech bubble, pinned to bottom-center ────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator dots
            StepDots(
                total   = TUTORIAL_STEPS.size,
                current = stepIndex
            )

            Spacer(Modifier.height(12.dp))

            // Speech bubble
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn(tween(300)) + slideInVertically { it / 3 },
                exit    = fadeOut(tween(200)) + slideOutVertically { it / 3 }
            ) {
                SpeechBubble(
                    title        = step.title,
                    message      = step.message,
                    isLast       = step.isLast,
                    onNext       = {
                        if (step.isLast) {
                            onDismiss()
                        } else {
                            stepIndex++
                        }
                    },
                    onSkip       = onDismiss
                )
            }

            Spacer(Modifier.height(16.dp))

            // Animated cat
            BouncingCat()
        }
    }
}

// ---------------------------------------------------------------------------
// SpeechBubble — white card with tail pointing down toward the cat
// ---------------------------------------------------------------------------
@Composable
private fun SpeechBubble(
    title   : String,
    message : String,
    isLast  : Boolean,
    onNext  : () -> Unit,
    onSkip  : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = title,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFFF7043),
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = message,
            fontSize  = 14.sp,
            color     = Color(0xFF424242),
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            // Skip button (hidden on last step)
            if (!isLast) {
                TextButton(onClick = onSkip) {
                    Text(
                        "Skip",
                        color    = Color(0xFF9E9E9E),
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }

            Button(
                onClick  = onNext,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7043)
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    if (isLast) "Let's go! 🐾" else "Next →",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// BouncingCat — pixel-art style cat emoji that bobs up and down
// ---------------------------------------------------------------------------
@Composable
private fun BouncingCat() {
    val infiniteTransition = rememberInfiniteTransition(label = "cat_bounce")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = -14f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_y"
    )

    val tiltAngle by infiniteTransition.animateFloat(
        initialValue   = -4f,
        targetValue    = 4f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .graphicsLayer {
                translationY = bounceOffset
                rotationZ    = tiltAngle
            }
            .background(Color(0xFFFF7043), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Cat face drawn with text emoji — no drawable needed
        Text(
            text     = "😺",
            fontSize = 46.sp
        )
    }
}

// ---------------------------------------------------------------------------
// StepDots — small progress indicator
// ---------------------------------------------------------------------------
@Composable
private fun StepDots(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            Box(
                modifier = Modifier
                    .size(if (isActive) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFFFF7043)
                        else Color(0xFFFFCCBC)
                    )
            )
        }
    }
}
