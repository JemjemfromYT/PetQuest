// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/TutorialOverlay.kt
// FULL REPLACEMENT — Whisker drawn as a real cat character with
// blinking eyes, tail wag, ear twitch, and breathing animation
// ============================================================

package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Colour palette for Whisker ────────────────────────────────────────────────
private val CatBody       = Color(0xFFF4A261)   // warm orange
private val CatBodyDark   = Color(0xFFE07B39)   // ear/shadow shading
private val CatInnerEar   = Color(0xFFFFB5C8)   // pink inner ear
private val CatEye        = Color(0xFF2B2B2B)   // eye iris
private val CatPupil      = Color(0xFF111111)   // pupil
private val CatEyeShine   = Color(0xFFFFFFFF)   // eye highlight
private val CatNose       = Color(0xFFFF8FAB)   // pink nose
private val CatMouth      = Color(0xFF333333)   // mouth lines
private val CatWhisker    = Color(0xFFFFFFFF)   // whiskers
private val CatBelly      = Color(0xFFFDE8D0)   // lighter belly patch
private val CatTail       = Color(0xFFF4A261)   // tail

// ---------------------------------------------------------------------------
// Tutorial step data
// ---------------------------------------------------------------------------
data class TutorialStep(
    val tabIndex : Int,
    val title    : String,
    val message  : String,
    val isLast   : Boolean = false
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        tabIndex = -1,
        title    = "Hey, I'm Whisker!",
        message  = "Welcome to PetQuest! I'll show you around real quick. It only takes a minute, I promise!"
    ),
    TutorialStep(
        tabIndex = 0,
        title    = "Home",
        message  = "This is your Home screen. You can see all your pets, your streak, bond points, and today's task progress at a glance."
    ),
    TutorialStep(
        tabIndex = 1,
        title    = "Daily Tasks",
        message  = "Every day, head to Tasks to take care of your pets. Completing tasks earns Bond Points and keeps your streak alive!"
    ),
    TutorialStep(
        tabIndex = 2,
        title    = "Collection",
        message  = "There are 29 pet species to discover. Each pet you own unlocks their entry. How many can you collect?"
    ),
    TutorialStep(
        tabIndex = 3,
        title    = "Awards",
        message  = "Awards are achievements you unlock by caring for your pets. Keep going and you'll unlock them all!"
    ),
    TutorialStep(
        tabIndex = 4,
        title    = "Events",
        message  = "Seasonal events give you bonus challenges and exclusive badges. Check here often so you don't miss out!"
    ),
    TutorialStep(
        tabIndex = 5,
        title    = "Your Profile",
        message  = "Profile tracks your keeper level, bond points, and all the pets you've raised. It's your pet-keeping legacy!"
    ),
    TutorialStep(
        tabIndex = -1,
        title    = "You're all set!",
        message  = "Take great care of your pets every single day. Small moments build lifelong bonds. Good luck, keeper!",
        isLast   = true
    )
)

// ---------------------------------------------------------------------------
// TutorialOverlay — full-screen overlay with Whisker
// ---------------------------------------------------------------------------
@Composable
fun TutorialOverlay(
    onDismiss      : () -> Unit,
    onTabHighlight : (Int) -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[stepIndex]

    LaunchedEffect(step.tabIndex) {
        if (step.tabIndex >= 0) onTabHighlight(step.tabIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* absorb touches */ }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepDots(total = TUTORIAL_STEPS.size, current = stepIndex)

            Spacer(Modifier.height(10.dp))

            SpeechBubble(
                title   = step.title,
                message = step.message,
                isLast  = step.isLast,
                onNext  = { if (step.isLast) onDismiss() else stepIndex++ },
                onSkip  = onDismiss
            )

            Spacer(Modifier.height(8.dp))

            // Whisker the cat — drawn entirely with Canvas
            WhiskerCat()
        }
    }
}

// ---------------------------------------------------------------------------
// WhiskerCat — fully drawn cat with animations
// ---------------------------------------------------------------------------
@Composable
private fun WhiskerCat() {
    val infinite = rememberInfiniteTransition(label = "whisker")

    // Breathing / floating bob
    val bobY by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = -10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Subtle body squeeze (like breathing)
    val breathe by infinite.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.03f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Blink — spends most time at 0 (open), quickly goes to 1 (closed) and back
    val blinkCycle by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink_cycle"
    )
    // Eyes closed only during the tiny window 9.6–10.0 of the 0–10 cycle
    val eyesClosed = blinkCycle > 9.6f

    // Tail wag angle in degrees
    val tailAngle by infinite.animateFloat(
        initialValue  = -20f,
        targetValue   = 20f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail"
    )

    // Ear twitch — very occasional
    val earTwitch by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ear_twitch"
    )
    val leftEarTilt  = if (earTwitch > 3.3f && earTwitch < 3.6f) -8f else 0f
    val rightEarTilt = if (earTwitch > 3.3f && earTwitch < 3.6f) 8f else 0f

    Canvas(
        modifier = Modifier
            .size(width = 120.dp, height = 140.dp)
            .graphicsLayer {
                translationY = bobY
            }
    ) {
        val w = size.width
        val h = size.height

        // ── Tail (drawn behind body) ──────────────────────────────────────────
        drawTail(w, h, tailAngle)

        // ── Body ──────────────────────────────────────────────────────────────
        val bodyLeft   = w * 0.20f
        val bodyTop    = h * 0.50f
        val bodyWidth  = w * 0.60f * breathe
        val bodyHeight = h * 0.42f
        drawOval(
            color    = CatBody,
            topLeft  = Offset(bodyLeft + (w * 0.60f - bodyWidth) / 2, bodyTop),
            size     = Size(bodyWidth, bodyHeight)
        )
        // Belly patch
        drawOval(
            color   = CatBelly,
            topLeft = Offset(w * 0.32f, bodyTop + h * 0.06f),
            size    = Size(w * 0.36f, h * 0.28f)
        )

        // ── Head ──────────────────────────────────────────────────────────────
        val headCx = w * 0.50f
        val headCy = h * 0.36f
        val headR  = w * 0.28f
        drawCircle(
            color  = CatBody,
            center = Offset(headCx, headCy),
            radius = headR
        )

        // ── Ears ─────────────────────────────────────────────────────────────
        drawEar(
            cx        = headCx - headR * 0.52f,
            tipX      = headCx - headR * 0.85f,
            tipY      = headCy - headR * 1.20f,
            baseY     = headCy - headR * 0.60f,
            bodyColor = CatBodyDark,
            innerColor= CatInnerEar,
            tiltDeg   = leftEarTilt
        )
        drawEar(
            cx        = headCx + headR * 0.52f,
            tipX      = headCx + headR * 0.85f,
            tipY      = headCy - headR * 1.20f,
            baseY     = headCy - headR * 0.60f,
            bodyColor = CatBodyDark,
            innerColor= CatInnerEar,
            tiltDeg   = rightEarTilt
        )

        // ── Eyes ──────────────────────────────────────────────────────────────
        val eyeLX = headCx - headR * 0.38f
        val eyeRX = headCx + headR * 0.38f
        val eyeY  = headCy - headR * 0.05f
        val eyeR  = headR * 0.20f

        drawEye(eyeLX, eyeY, eyeR, eyesClosed)
        drawEye(eyeRX, eyeY, eyeR, eyesClosed)

        // ── Nose ──────────────────────────────────────────────────────────────
        val noseCx = headCx
        val noseCy = headCy + headR * 0.26f
        drawNose(noseCx, noseCy, headR * 0.10f)

        // ── Mouth ─────────────────────────────────────────────────────────────
        drawMouth(noseCx, noseCy + headR * 0.12f, headR)

        // ── Whiskers ──────────────────────────────────────────────────────────
        drawWhiskers(noseCx, noseCy, headR)
    }
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

private fun DrawScope.drawTail(w: Float, h: Float, angleDeg: Float) {
    val angleRad = (angleDeg * PI / 180f).toFloat()
    val tailPath = Path().apply {
        val startX  = w * 0.72f
        val startY  = h * 0.75f
        val ctrl1X  = w * 0.90f + cos(angleRad) * w * 0.25f
        val ctrl1Y  = h * 0.55f + sin(angleRad) * h * 0.10f
        val ctrl2X  = w * 0.85f + cos(angleRad) * w * 0.15f
        val ctrl2Y  = h * 0.35f + sin(angleRad) * h * 0.08f
        val endX    = w * 0.80f + cos(angleRad) * w * 0.18f
        val endY    = h * 0.28f + sin(angleRad) * h * 0.06f
        moveTo(startX, startY)
        cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)
    }
    drawPath(
        path  = tailPath,
        color = CatTail,
        style = Stroke(width = 18f, cap = StrokeCap.Round)
    )
    // Slightly darker outline for depth
    drawPath(
        path  = tailPath,
        color = CatBodyDark,
        style = Stroke(width = 18f, cap = StrokeCap.Round)
    )
    drawPath(
        path  = tailPath,
        color = CatTail,
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawEar(
    cx: Float, tipX: Float, tipY: Float, baseY: Float,
    bodyColor: Color, innerColor: Color, tiltDeg: Float
) {
    val earPath = Path().apply {
        moveTo(cx - 14f, baseY)
        lineTo(tipX, tipY + tiltDeg * 2)
        lineTo(cx + 14f, baseY)
        close()
    }
    drawPath(earPath, bodyColor)

    val innerPath = Path().apply {
        moveTo(cx - 7f, baseY)
        lineTo(tipX, tipY + tiltDeg * 2 + 10f)
        lineTo(cx + 7f, baseY)
        close()
    }
    drawPath(innerPath, innerColor)
}

private fun DrawScope.drawEye(cx: Float, cy: Float, r: Float, closed: Boolean) {
    if (closed) {
        // Closed eye — a curved arc line
        drawArc(
            color      = CatEye,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter  = false,
            topLeft    = Offset(cx - r, cy - r * 0.3f),
            size       = Size(r * 2f, r),
            style      = Stroke(width = r * 0.5f, cap = StrokeCap.Round)
        )
    } else {
        // White of eye
        drawCircle(color = CatEyeShine, center = Offset(cx, cy), radius = r)
        // Iris
        drawCircle(color = CatEye, center = Offset(cx, cy), radius = r * 0.72f)
        // Pupil
        drawCircle(color = CatPupil, center = Offset(cx, cy), radius = r * 0.42f)
        // Shine
        drawCircle(color = CatEyeShine, center = Offset(cx - r * 0.22f, cy - r * 0.22f), radius = r * 0.18f)
        drawCircle(color = CatEyeShine, center = Offset(cx + r * 0.18f, cy + r * 0.18f), radius = r * 0.09f)
    }
}

private fun DrawScope.drawNose(cx: Float, cy: Float, r: Float) {
    val nosePath = Path().apply {
        moveTo(cx, cy - r)           // top centre
        lineTo(cx + r * 1.4f, cy + r)  // bottom right
        lineTo(cx - r * 1.4f, cy + r)  // bottom left
        close()
    }
    drawPath(nosePath, CatNose)
}

private fun DrawScope.drawMouth(cx: Float, topY: Float, headR: Float) {
    val seg = headR * 0.14f
    // Centre vertical drop
    drawLine(
        color       = CatMouth,
        start       = Offset(cx, topY),
        end         = Offset(cx, topY + seg),
        strokeWidth = 3f,
        cap         = StrokeCap.Round
    )
    // Left curve
    drawArc(
        color      = CatMouth,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter  = false,
        topLeft    = Offset(cx - seg * 2, topY),
        size       = Size(seg * 2, seg * 1.5f),
        style      = Stroke(width = 3f, cap = StrokeCap.Round)
    )
    // Right curve
    drawArc(
        color      = CatMouth,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter  = false,
        topLeft    = Offset(cx, topY),
        size       = Size(seg * 2, seg * 1.5f),
        style      = Stroke(width = 3f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawWhiskers(cx: Float, cy: Float, headR: Float) {
    val wLen  = headR * 1.2f
    val yBase = cy - headR * 0.02f
    val angles = listOf(-8f, 0f, 8f)
    angles.forEach { angleDeg ->
        val rad = (angleDeg * PI / 180f).toFloat()
        // Left whisker
        drawLine(
            color       = CatWhisker,
            start       = Offset(cx - headR * 0.15f, yBase),
            end         = Offset(cx - headR * 0.15f - wLen * cos(rad), yBase + wLen * sin(rad)),
            strokeWidth = 2.2f,
            cap         = StrokeCap.Round
        )
        // Right whisker
        drawLine(
            color       = CatWhisker,
            start       = Offset(cx + headR * 0.15f, yBase),
            end         = Offset(cx + headR * 0.15f + wLen * cos(rad), yBase + wLen * sin(rad)),
            strokeWidth = 2.2f,
            cap         = StrokeCap.Round
        )
    }
}

// ---------------------------------------------------------------------------
// SpeechBubble
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
            .shadow(10.dp, RoundedCornerShape(20.dp))
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
            text       = message,
            fontSize   = 14.sp,
            color      = Color(0xFF424242),
            textAlign  = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (!isLast) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                }
            } else {
                Spacer(Modifier.width(64.dp))
            }

            Button(
                onClick  = onNext,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text       = if (isLast) "Let's go!" else "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// StepDots
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
                    .background(if (isActive) Color(0xFFFF7043) else Color(0xFFFFCCBC))
            )
        }
    }
}
