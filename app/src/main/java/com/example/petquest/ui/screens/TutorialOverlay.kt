// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/TutorialOverlay.kt
// FULL REPLACEMENT
//
// Layout overhaul:
//  - Bottom sheet card (like a game dialog), cat sits ON TOP of the card
//  - Cat is now 180×200dp — much bigger, properly centered
//  - Chibi proportions: massive head, tiny body peeking above card rim
//  - Mouth opens/closes while text types (talking animation)
//  - Typewriter text with simple blinking block cursor ▋
//  - Friendly sky-blue eyes, rosy cheeks, warm cream fur
//  - No floating sparkles (they looked cheap)
//  - Card uses full width with proper padding hierarchy
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
import androidx.compose.ui.geometry.CornerRadius
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val CatFur      = Color(0xFFFDE8C2)
private val CatFurShade = Color(0xFFDDB97A)
private val CatInnerEar = Color(0xFFFFCBD8)
private val CatEarOuter = Color(0xFFCB9A5A)
private val CatIris     = Color(0xFF4DB6FF)
private val CatPupil    = Color(0xFF1A3D6B)
private val CatBlush    = Color(0xFFFF9EB5)
private val CatNose     = Color(0xFFFF8FAB)
private val CatMouth    = Color(0xFF7A4E2D)
private val CatWhisker  = Color(0xFFC8A882)
private val CatBelly    = Color(0xFFFFF6E8)


// ── Tutorial steps ────────────────────────────────────────────────────────────
data class TutorialStep(
    val tabIndex : Int,
    val title    : String,
    val message  : String,
    val isLast   : Boolean = false
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(-1, "Hi! I'm Whisker 🐱",
        "I live in PetQuest and I'll be your guide! Let me show you around. Tap Got it to continue!"),
    TutorialStep(0,  "Home 🏠",
        "Your Home screen shows all your pets, today's streak, bond points, and how many tasks you've done today."),
    TutorialStep(1,  "Daily Tasks ✅",
        "Go to Tasks every day to take care of your pets. Completing tasks earns Bond Points and keeps your streak alive!"),
    TutorialStep(2,  "Collection 🐾",
        "There are 29 species to discover! Every new pet you add unlocks their slot in the collection."),
    TutorialStep(3,  "Awards 🏆",
        "Awards are achievements you earn by caring for your pets every day. There are 42 to unlock — start collecting!"),
    TutorialStep(4,  "Events 🌟",
        "Seasonal events bring special challenges and exclusive badges. They only come once a year, so don't miss them!"),
    TutorialStep(5,  "Your Profile 👤",
        "Profile shows your keeper level and your full pet history. The more you care for your pets, the higher you climb!"),
    TutorialStep(-1, "All done! 🎉",
        "That's everything! Take great care of your pet every single day. Even small moments build lifelong bonds. Good luck!",
        isLast = true)
)

// ── Main overlay ──────────────────────────────────────────────────────────────
@Composable
fun TutorialOverlay(
    onDismiss      : () -> Unit,
    onTabHighlight : (Int) -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = TUTORIAL_STEPS[stepIndex]

    var displayedMessage by remember { mutableStateOf("") }
    var isTalking        by remember { mutableStateOf(false) }
    var showCursor       by remember { mutableStateOf(true) }

    LaunchedEffect(stepIndex) {
        displayedMessage = ""
        isTalking        = true
        showCursor       = true
        for (ch in step.message) {
            displayedMessage += ch
            delay(26L)
        }
        isTalking = false
    }

    LaunchedEffect(isTalking) {
        while (isTalking) {
            showCursor = !showCursor
            delay(420L)
        }
        showCursor = false
    }

    LaunchedEffect(step.tabIndex) {
        if (step.tabIndex >= 0) onTabHighlight(step.tabIndex)
    }

    // Full-screen dark overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000).copy(alpha = 0.55f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* absorb */ }
    ) {
        // ── Bottom sheet + cat ─────────────────────────────────────────────────
        //   outer Box positioned at bottom, respects nav bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 72.dp)         // clear nav bar
        ) {
            // Card — starts 95dp below the outer Box top (cat overlaps above)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 95.dp)          // room for cat to overlap
                    .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(top = 90.dp)          // inner space for cat body peeking inside card
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step dots
                StepDots(total = TUTORIAL_STEPS.size, current = stepIndex)

                Spacer(Modifier.height(12.dp))

                // Title
                Text(
                    text       = step.title,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFFFF6B35),
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                // Typewriter message — fixed min height to prevent layout jump
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 70.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val cursorStr = if (showCursor && isTalking) "▋" else ""
                    Text(
                        text       = displayedMessage + cursorStr,
                        fontSize   = 14.sp,
                        color      = Color(0xFF4A4A4A),
                        textAlign  = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    if (!step.isLast) {
                        TextButton(onClick = onDismiss) {
                            Text("Skip", color = Color(0xFFBDBDBD), fontSize = 13.sp)
                        }
                    } else {
                        Spacer(Modifier.width(56.dp))
                    }

                    Button(
                        onClick  = { if (step.isLast) onDismiss() else stepIndex++ },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                        shape    = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(46.dp).defaultMinSize(minWidth = 130.dp)
                    ) {
                        Text(
                            text       = if (step.isLast) "Let's go! 🐾" else "Got it! →",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── Cat — sits on top of the card, centered ──────────────────────
            WhiskerCat(
                isTalking = isTalking,
                modifier  = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 180.dp, height = 200.dp)
            )
        }
    }
}

// ── WhiskerCat ───────────────────────────────────────────────────────────────
@Composable
private fun WhiskerCat(isTalking: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "wcat")

    val bobY by inf.animateFloat(
        initialValue  = 0f, targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "bob"
    )
    val blinkTimer by inf.animateFloat(
        initialValue  = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label         = "blink"
    )
    val eyesClosed = blinkTimer > 9.5f

    val mouthCycle by inf.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "mouth"
    )
    val mouthOpen = if (isTalking) mouthCycle else 0f

    val tailSwing by inf.animateFloat(
        initialValue  = -22f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "tail"
    )

    Canvas(
        modifier = modifier.graphicsLayer { translationY = bobY }
    ) {
        val w = size.width
        val h = size.height

        // ── Tail ──────────────────────────────────────────────────────────────
        drawCatTail(w, h, tailSwing)

        // ── Body (small peeking above card rim) ───────────────────────────────
        val bodyW = w * 0.50f
        val bodyH = h * 0.22f
        val bodyL = (w - bodyW) / 2f
        val bodyT = h * 0.76f
        drawRoundRect(
            color        = CatFur,
            topLeft      = Offset(bodyL, bodyT),
            size         = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.40f)
        )
        drawOval(
            color   = CatBelly,
            topLeft = Offset(bodyL + bodyW * 0.18f, bodyT + bodyH * 0.10f),
            size    = Size(bodyW * 0.64f, bodyH * 0.72f)
        )

        // ── Head ─────────────────────────────────────────────────────────────
        val hcx = w * 0.50f
        val hcy = h * 0.40f
        val hr  = w * 0.34f          // big chibi head radius

        // soft drop shadow
        drawCircle(Color(0x22000000), hr + 6f, Offset(hcx, hcy + 6f))
        // head
        drawCircle(CatFur, hr, Offset(hcx, hcy))

        // ── Ears ──────────────────────────────────────────────────────────────
        drawRoundEar(hcx - hr * 0.58f, hcy - hr * 0.82f, hr * 0.28f)
        drawRoundEar(hcx + hr * 0.58f, hcy - hr * 0.82f, hr * 0.28f)

        // ── Forehead stripes ─────────────────────────────────────────────────
        val stripeA = CatFurShade.copy(alpha = 0.55f)
        val sw      = 4f
        drawLine(stripeA, Offset(hcx - hr * 0.25f, hcy - hr * 0.60f), Offset(hcx - hr * 0.12f, hcy - hr * 0.82f), sw, StrokeCap.Round)
        drawLine(stripeA, Offset(hcx,              hcy - hr * 0.65f), Offset(hcx,              hcy - hr * 0.88f), sw, StrokeCap.Round)
        drawLine(stripeA, Offset(hcx + hr * 0.25f, hcy - hr * 0.60f), Offset(hcx + hr * 0.12f, hcy - hr * 0.82f), sw, StrokeCap.Round)

        // ── Blush ─────────────────────────────────────────────────────────────
        drawCircle(CatBlush.copy(alpha = 0.50f), hr * 0.24f, Offset(hcx - hr * 0.65f, hcy + hr * 0.28f))
        drawCircle(CatBlush.copy(alpha = 0.50f), hr * 0.24f, Offset(hcx + hr * 0.65f, hcy + hr * 0.28f))

        // ── Eyes ──────────────────────────────────────────────────────────────
        val eyeR  = hr * 0.24f
        val eyeLx = hcx - hr * 0.38f
        val eyeRx = hcx + hr * 0.38f
        val eyeY  = hcy - hr * 0.03f
        drawChibiEye(eyeLx, eyeY, eyeR, eyesClosed)
        drawChibiEye(eyeRx, eyeY, eyeR, eyesClosed)

        // ── Nose ──────────────────────────────────────────────────────────────
        val noseCy = hcy + hr * 0.30f
        drawCircle(CatNose, hr * 0.09f, Offset(hcx, noseCy))

        // ── Mouth ─────────────────────────────────────────────────────────────
        drawChibiMouth(hcx, noseCy + hr * 0.14f, hr, mouthOpen)

        // ── Whiskers ──────────────────────────────────────────────────────────
        drawCatWhiskers(hcx, noseCy, hr)
    }
}

// ── Canvas drawing functions ─────────────────────────────────────────────────

private fun DrawScope.drawCatTail(w: Float, h: Float, deg: Float) {
    val rad = (deg * PI / 180.0).toFloat()
    val path = Path().apply {
        moveTo(w * 0.70f, h * 0.82f)
        cubicTo(
            w * 0.92f + cos(rad) * w * 0.18f, h * 0.62f + sin(rad) * h * 0.06f,
            w * 0.88f + cos(rad) * w * 0.12f, h * 0.40f,
            w * 0.82f + cos(rad) * w * 0.14f, h * 0.28f
        )
    }
    drawPath(path, CatFurShade, style = Stroke(22f, cap = StrokeCap.Round))
    drawPath(path, CatFur,      style = Stroke(14f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRoundEar(cx: Float, cy: Float, r: Float) {
    drawOval(CatEarOuter, topLeft = Offset(cx - r, cy - r * 1.25f), size = Size(r * 2f, r * 2.2f))
    drawOval(CatFur,      topLeft = Offset(cx - r * 0.70f, cy - r * 0.80f), size = Size(r * 1.4f, r * 1.5f))
    drawOval(CatInnerEar, topLeft = Offset(cx - r * 0.50f, cy - r * 0.60f), size = Size(r * 1.0f, r * 1.1f))
}

private fun DrawScope.drawChibiEye(cx: Float, cy: Float, r: Float, closed: Boolean) {
    if (closed) {
        drawArc(
            color = CatMouth, startAngle = 200f, sweepAngle = -160f, useCenter = false,
            topLeft = Offset(cx - r, cy - r * 0.45f),
            size    = Size(r * 2f, r * 1.1f),
            style   = Stroke(r * 0.45f, cap = StrokeCap.Round)
        )
        return
    }
    drawCircle(Color.White, r, Offset(cx, cy))
    drawCircle(CatIris, r * 0.74f, Offset(cx, cy))
    drawCircle(CatPupil, r * 0.42f, Offset(cx, cy))
    // main shine (top-left)
    drawCircle(Color.White, r * 0.25f, Offset(cx - r * 0.24f, cy - r * 0.22f))
    // small secondary shine
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.11f, Offset(cx + r * 0.18f, cy + r * 0.18f))
}

private fun DrawScope.drawChibiMouth(cx: Float, topY: Float, hr: Float, openness: Float) {
    val s = hr * 0.17f
    if (openness < 0.08f) {
        // Resting smile: "w" shape
        drawArc(CatMouth, 30f, 120f, false, Offset(cx - s * 1.7f, topY - s * 0.4f), Size(s * 1.6f, s * 1.1f), style = Stroke(3.5f, cap = StrokeCap.Round))
        drawArc(CatMouth, 30f, 120f, false, Offset(cx + s * 0.1f, topY - s * 0.4f), Size(s * 1.6f, s * 1.1f), style = Stroke(3.5f, cap = StrokeCap.Round))
    } else {
        // Talking: growing open oval
        val ow = s * 1.5f
        val oh = s * 0.5f + openness * s * 1.1f
        drawOval(CatMouth, topLeft = Offset(cx - ow / 2f, topY), size = Size(ow, oh))
        if (openness > 0.65f) {
            drawOval(
                CatNose.copy(alpha = (openness - 0.65f) * 2.5f),
                topLeft = Offset(cx - ow * 0.28f, topY + oh * 0.46f),
                size    = Size(ow * 0.56f, oh * 0.50f)
            )
        }
    }
}

private fun DrawScope.drawCatWhiskers(noseCx: Float, noseCy: Float, hr: Float) {
    val len = hr * 1.0f
    listOf(-8f, 0f, 8f).forEach { deg ->
        val rad = (deg * PI / 180.0).toFloat()
        drawLine(CatWhisker, Offset(noseCx - hr * 0.08f, noseCy + sin(rad) * -len), Offset(noseCx - hr * 0.08f - len * cos(rad), noseCy - sin(rad) * len * 0.15f), 2.2f, StrokeCap.Round)
        drawLine(CatWhisker, Offset(noseCx + hr * 0.08f, noseCy + sin(rad) * -len), Offset(noseCx + hr * 0.08f + len * cos(rad), noseCy - sin(rad) * len * 0.15f), 2.2f, StrokeCap.Round)
    }
}

// ── StepDots ─────────────────────────────────────────────────────────────────
@Composable
private fun StepDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(if (i == current) 12.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Color(0xFFFF6B35) else Color(0xFFFFCCBC))
            )
        }
    }
}
