// ============================================================
// FILE: app/src/main/java/com/example/petquest/ui/screens/TutorialOverlay.kt
// FULL REPLACEMENT
//
// Changes vs previous version:
//  - Chibi proportions: giant head, tiny body, no scary geometry
//  - Friendly blue eyes with big sparkle highlights (not dark/creepy)
//  - Rosy blush marks on cheeks
//  - Rounded ears (oval tips, not sharp triangles)
//  - Typewriter text effect — text types out letter by letter
//  - Mouth opens/closes in sync with typing (cat actually "speaks")
//  - Warm cream/peach fur palette (not harsh orange)
//  - Stripe markings on forehead
//  - Sparkle dots floating around cat
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette — warm, friendly, NOT scary ──────────────────────────────────────
private val Fur          = Color(0xFFFFF0D6)  // warm cream
private val FurShade     = Color(0xFFE8C99A)  // shading / stripes
private val FurDark      = Color(0xFFD4A86A)  // darker ear outline
private val InnerEar     = Color(0xFFFFCBD8)  // soft pink
private val EyeWhite     = Color(0xFFFFFFFF)
private val EyeIris      = Color(0xFF5DB8FF)  // friendly sky blue
private val EyePupil     = Color(0xFF1C3A5E)  // deep blue, not black
private val EyeShine1    = Color(0xFFFFFFFF)
private val EyeShine2    = Color(0xFFE0F4FF)
private val BlushColor   = Color(0xFFFFB3C1)  // rosy cheeks
private val NoseColor    = Color(0xFFFF8FAB)  // pink nose
private val MouthColor   = Color(0xFF7A4E2D)  // warm brown mouth
private val WhiskerColor = Color(0xFFBDA27A)  // warm whiskers
private val BellyColor   = Color(0xFFFFF8EC)  // belly patch

// ── Tutorial steps ────────────────────────────────────────────────────────────
data class TutorialStep(
    val tabIndex : Int,
    val title    : String,
    val message  : String,
    val isLast   : Boolean = false
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(-1, "Hey, I'm Whisker!",
        "Welcome to PetQuest! I live here and I'll show you around. Tap Next and I'll explain everything!"),
    TutorialStep(0, "Home",
        "This is your Home screen. Every day you'll see your pets here, along with your streak and bond points."),
    TutorialStep(1, "Daily Tasks",
        "Head to Tasks every day to care for your pets. Finish them to earn Bond Points and keep your streak going!"),
    TutorialStep(2, "Collection",
        "You can collect 29 different pet species. Each new pet you add unlocks their spot in the collection!"),
    TutorialStep(3, "Awards",
        "Awards are achievements earned by caring for your pets. There are 42 to unlock — start collecting them!"),
    TutorialStep(4, "Events",
        "Seasonal events bring special challenges and exclusive badges. Don't miss them — they only come once a year!"),
    TutorialStep(5, "Your Profile",
        "Your Profile shows your keeper level and your pet history. The more you care, the higher you rise!"),
    TutorialStep(-1, "All done!",
        "That's everything! Now go take great care of your pet every day. I'll be cheering for you!",
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

    // Typewriter state — resets each time stepIndex changes
    var displayedMessage by remember { mutableStateOf("") }
    var isTalking        by remember { mutableStateOf(true) }

    LaunchedEffect(stepIndex) {
        displayedMessage = ""
        isTalking        = true
        for (ch in step.message) {
            displayedMessage += ch
            delay(28L)
        }
        isTalking = false
    }

    LaunchedEffect(step.tabIndex) {
        if (step.tabIndex >= 0) onTabHighlight(step.tabIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* absorb */ }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp, start = 18.dp, end = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepDots(total = TUTORIAL_STEPS.size, current = stepIndex)
            Spacer(Modifier.height(10.dp))

            SpeechBubble(
                title           = step.title,
                displayedMessage= displayedMessage,
                fullMessage     = step.message,
                isLast          = step.isLast,
                isTalking       = isTalking,
                onNext          = { if (step.isLast) onDismiss() else stepIndex++ },
                onSkip          = onDismiss
            )

            Spacer(Modifier.height(6.dp))
            WhiskerCat(isTalking = isTalking)
        }
    }
}

// ── WhiskerCat — fully drawn chibi cat ───────────────────────────────────────
@Composable
private fun WhiskerCat(isTalking: Boolean) {
    val inf = rememberInfiniteTransition(label = "whisker_anim")

    // Gentle float
    val bobY by inf.animateFloat(
        initialValue  = 0f, targetValue = -9f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "bob"
    )

    // Breathing scale
    val breathe by inf.animateFloat(
        initialValue  = 1f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "breathe"
    )

    // Blink timer — 0→8, blink when > 7.7
    val blinkTimer by inf.animateFloat(
        initialValue  = 0f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label         = "blink"
    )
    val eyesClosed = blinkTimer > 7.7f

    // Mouth openness when talking — alternates 0→1→0 quickly
    val mouthCycle by inf.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(220, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "mouth"
    )
    val mouthOpenness = if (isTalking) mouthCycle else 0f

    // Tail wag
    val tailSwing by inf.animateFloat(
        initialValue  = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "tail"
    )

    // Sparkle rotation for floating dots
    val sparkleAngle by inf.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label         = "sparkle"
    )

    Canvas(
        modifier = Modifier
            .size(130.dp, 145.dp)
            .graphicsLayer { translationY = bobY }
    ) {
        val w = size.width
        val h = size.height

        // ── Sparkles around the cat ──────────────────────────────────────────
        drawSparkles(w, h, sparkleAngle)

        // ── Tail ─────────────────────────────────────────────────────────────
        drawChibiTail(w, h, tailSwing)

        // ── Body (small, below head) ──────────────────────────────────────────
        val bodyW = w * 0.44f * breathe
        val bodyH = h * 0.28f
        val bodyL = (w - bodyW) / 2f
        val bodyT = h * 0.68f
        drawRoundRect(
            color       = Fur,
            topLeft     = Offset(bodyL, bodyT),
            size        = Size(bodyW, bodyH),
            cornerRadius= androidx.compose.ui.geometry.CornerRadius(bodyW * 0.45f)
        )
        // belly patch
        drawOval(
            color   = BellyColor,
            topLeft = Offset(bodyL + bodyW * 0.18f, bodyT + bodyH * 0.10f),
            size    = Size(bodyW * 0.64f, bodyH * 0.72f)
        )

        // ── Head (large chibi) ────────────────────────────────────────────────
        val hcx = w * 0.50f
        val hcy = h * 0.38f
        val hr  = w * 0.32f

        // Head shadow
        drawCircle(FurShade, radius = hr + 2f, center = Offset(hcx, hcy + 4f))
        // Head
        drawCircle(Fur, radius = hr, center = Offset(hcx, hcy))

        // ── Forehead stripes ─────────────────────────────────────────────────
        drawForeheadStripes(hcx, hcy, hr)

        // ── Ears ─────────────────────────────────────────────────────────────
        drawRoundedEar(hcx - hr * 0.60f, hcy - hr * 0.82f, hr * 0.30f, FurDark, InnerEar)
        drawRoundedEar(hcx + hr * 0.60f, hcy - hr * 0.82f, hr * 0.30f, FurDark, InnerEar)

        // ── Blush marks ───────────────────────────────────────────────────────
        drawCircle(BlushColor.copy(alpha = 0.55f), radius = hr * 0.22f, center = Offset(hcx - hr * 0.62f, hcy + hr * 0.25f))
        drawCircle(BlushColor.copy(alpha = 0.55f), radius = hr * 0.22f, center = Offset(hcx + hr * 0.62f, hcy + hr * 0.25f))

        // ── Eyes ─────────────────────────────────────────────────────────────
        val eyeLx = hcx - hr * 0.36f
        val eyeRx = hcx + hr * 0.36f
        val eyeY  = hcy - hr * 0.04f
        val eyeR  = hr * 0.22f
        drawChibiEye(eyeLx, eyeY, eyeR, eyesClosed, leftEye = true)
        drawChibiEye(eyeRx, eyeY, eyeR, eyesClosed, leftEye = false)

        // ── Nose ─────────────────────────────────────────────────────────────
        val noseCy = hcy + hr * 0.28f
        drawCircle(NoseColor, radius = hr * 0.09f, center = Offset(hcx, noseCy))

        // ── Mouth ────────────────────────────────────────────────────────────
        drawChibiMouth(hcx, noseCy + hr * 0.13f, hr, mouthOpenness)

        // ── Whiskers ─────────────────────────────────────────────────────────
        drawChibiWhiskers(hcx, noseCy, hr)
    }
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

private fun DrawScope.drawSparkles(w: Float, h: Float, angle: Float) {
    val sparklePositions = listOf(
        Pair(0.12f, 0.18f), Pair(0.88f, 0.22f),
        Pair(0.06f, 0.55f), Pair(0.94f, 0.50f),
        Pair(0.20f, 0.82f), Pair(0.80f, 0.78f)
    )
    sparklePositions.forEachIndexed { i, (px, py) ->
        val phaseOffset  = i * 60f
        val animAngle    = (angle + phaseOffset) % 360f
        val pulseFactor  = (sin(animAngle * PI / 180f) * 0.4f + 0.8f).toFloat()
        val size         = w * 0.025f * pulseFactor
        val alpha        = (0.4f + pulseFactor * 0.3f).coerceIn(0f, 1f)
        drawCircle(
            color  = Color(0xFFFFE08A).copy(alpha = alpha),
            radius = size,
            center = Offset(w * px, h * py)
        )
        // 4-point star lines
        val cx = w * px; val cy = h * py
        val starSize = size * 1.8f
        drawLine(Color(0xFFFFE08A).copy(alpha = alpha * 0.7f), Offset(cx, cy - starSize), Offset(cx, cy + starSize), 1.8f)
        drawLine(Color(0xFFFFE08A).copy(alpha = alpha * 0.7f), Offset(cx - starSize, cy), Offset(cx + starSize, cy), 1.8f)
    }
}

private fun DrawScope.drawChibiTail(w: Float, h: Float, angleDeg: Float) {
    val rad = (angleDeg * PI / 180.0).toFloat()
    val tailPath = Path().apply {
        val sx = w * 0.68f; val sy = h * 0.78f
        val c1x = w * 0.90f + cos(rad) * w * 0.18f
        val c1y = h * 0.60f + sin(rad) * h * 0.08f
        val c2x = w * 0.82f + cos(rad) * w * 0.10f
        val c2y = h * 0.40f
        val ex  = w * 0.78f + cos(rad) * w * 0.12f
        val ey  = h * 0.30f
        moveTo(sx, sy)
        cubicTo(c1x, c1y, c2x, c2y, ex, ey)
    }
    drawPath(tailPath, FurShade, style = Stroke(20f, cap = StrokeCap.Round))
    drawPath(tailPath, Fur,      style = Stroke(14f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRoundedEar(cx: Float, cy: Float, r: Float, outerColor: Color, innerColor: Color) {
    drawOval(outerColor, topLeft = Offset(cx - r, cy - r * 1.2f), size = Size(r * 2f, r * 2.2f))
    drawOval(innerColor, topLeft = Offset(cx - r * 0.55f, cy - r * 0.75f), size = Size(r * 1.1f, r * 1.3f))
}

private fun DrawScope.drawForeheadStripes(hcx: Float, hcy: Float, hr: Float) {
    val stripeColor = FurShade.copy(alpha = 0.60f)
    val sw = 3.5f
    // 3 short curved stripes on forehead
    drawLine(stripeColor, Offset(hcx - hr * 0.22f, hcy - hr * 0.58f), Offset(hcx - hr * 0.10f, hcy - hr * 0.78f), sw, StrokeCap.Round)
    drawLine(stripeColor, Offset(hcx,             hcy - hr * 0.62f), Offset(hcx,             hcy - hr * 0.85f), sw, StrokeCap.Round)
    drawLine(stripeColor, Offset(hcx + hr * 0.22f, hcy - hr * 0.58f), Offset(hcx + hr * 0.10f, hcy - hr * 0.78f), sw, StrokeCap.Round)
}

private fun DrawScope.drawChibiEye(cx: Float, cy: Float, r: Float, closed: Boolean, leftEye: Boolean) {
    if (closed) {
        // Cute closed eye — upward arc (^)
        drawArc(
            color      = Color(0xFF5D3A1A),
            startAngle = 200f,
            sweepAngle = -160f,
            useCenter  = false,
            topLeft    = Offset(cx - r, cy - r * 0.5f),
            size       = Size(r * 2f, r * 1.2f),
            style      = Stroke(r * 0.45f, cap = StrokeCap.Round)
        )
        return
    }
    // White sclera
    drawCircle(EyeWhite, r, Offset(cx, cy))
    // Iris
    drawCircle(EyeIris, r * 0.72f, Offset(cx, cy))
    // Pupil
    drawCircle(EyePupil, r * 0.40f, Offset(cx, cy))
    // Primary shine (large top-left)
    drawCircle(EyeShine1, r * 0.22f, Offset(cx - r * 0.22f, cy - r * 0.20f))
    // Secondary shine (small bottom-right)
    drawCircle(EyeShine2, r * 0.10f, Offset(cx + r * 0.16f, cy + r * 0.16f))
    // Extra cute sparkle
    val starAngle = if (leftEye) 45f else 135f
    val starRad   = (starAngle * PI / 180.0).toFloat()
    val scx = cx + cos(starRad) * r * 0.50f
    val scy = cy - sin(starRad) * r * 0.50f
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.08f, Offset(scx, scy))
}

private fun DrawScope.drawChibiMouth(cx: Float, topY: Float, hr: Float, openness: Float) {
    val seg = hr * 0.16f
    if (openness < 0.1f) {
        // Gentle smile — two short arcs curving upward at edges
        drawArc(
            color = MouthColor,
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx - seg * 1.6f, topY - seg * 0.5f),
            size    = Size(seg * 1.6f, seg * 1.2f),
            style   = Stroke(3.5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = MouthColor,
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx, topY - seg * 0.5f),
            size    = Size(seg * 1.6f, seg * 1.2f),
            style   = Stroke(3.5f, cap = StrokeCap.Round)
        )
    } else {
        // Talking: small open oval that grows with openness
        val ovalW = seg * 1.4f
        val ovalH = seg * 0.6f + openness * seg * 0.9f
        drawOval(
            color   = MouthColor,
            topLeft = Offset(cx - ovalW / 2f, topY),
            size    = Size(ovalW, ovalH)
        )
        // Tongue hint when mouth is very open
        if (openness > 0.7f) {
            drawOval(
                color   = Color(0xFFFF8FAB).copy(alpha = (openness - 0.7f) * 2f),
                topLeft = Offset(cx - ovalW * 0.30f, topY + ovalH * 0.45f),
                size    = Size(ovalW * 0.60f, ovalH * 0.55f)
            )
        }
    }
}

private fun DrawScope.drawChibiWhiskers(noseCx: Float, noseCy: Float, hr: Float) {
    val len = hr * 1.05f
    data class W(val startX: Float, val startY: Float, val angle: Float, val right: Boolean)
    val whiskers = listOf(
        W(noseCx - hr * 0.08f, noseCy - hr * 0.06f, -10f, false),
        W(noseCx - hr * 0.08f, noseCy,               0f,  false),
        W(noseCx - hr * 0.08f, noseCy + hr * 0.06f, 10f,  false),
        W(noseCx + hr * 0.08f, noseCy - hr * 0.06f, -10f, true),
        W(noseCx + hr * 0.08f, noseCy,               0f,  true),
        W(noseCx + hr * 0.08f, noseCy + hr * 0.06f, 10f,  true),
    )
    whiskers.forEach { w ->
        val rad  = (w.angle * PI / 180.0).toFloat()
        val dirX = if (w.right) 1f else -1f
        drawLine(
            color       = WhiskerColor,
            start       = Offset(w.startX, w.startY),
            end         = Offset(w.startX + dirX * len * cos(rad), w.startY + len * sin(rad)),
            strokeWidth = 2.0f,
            cap         = StrokeCap.Round
        )
    }
}

// ── SpeechBubble — shows typewriter text ──────────────────────────────────────
@Composable
private fun SpeechBubble(
    title            : String,
    displayedMessage : String,
    fullMessage      : String,
    isLast           : Boolean,
    isTalking        : Boolean,
    onNext           : () -> Unit,
    onSkip           : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title with small paw icon
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("🐾 ", fontSize = 14.sp)
            Text(
                text       = title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color(0xFFFF7043)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Typewriter message — fixed height to avoid layout jumping
        Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp)) {
            Text(
                text       = displayedMessage,
                fontSize   = 13.5f.sp,
                color      = Color(0xFF424242),
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
                modifier   = Modifier.align(Alignment.TopCenter)
            )
            // Blinking cursor while typing
            if (isTalking) {
                val cursorAlpha by rememberInfiniteTransition(label = "cursor")
                    .animateFloat(
                        initialValue  = 1f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label         = "cursor_blink"
                    )
                Text(
                    text     = "|",
                    fontSize = 13.5f.sp,
                    color    = Color(0xFFFF7043).copy(alpha = cursorAlpha),
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(start = (displayedMessage.length * 7.8f).coerceAtMost(200f).dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (!isLast) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color(0xFFBDBDBD), fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }

            Button(
                onClick  = onNext,
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(
                    text       = if (isLast) "Thanks, Whisker!" else "Got it, next!",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = Color.White
                )
            }
        }
    }
}

// ── StepDots ─────────────────────────────────────────────────────────────────
@Composable
private fun StepDots(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 11.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) Color(0xFFFF7043) else Color(0xFFFFCCBC))
            )
        }
    }
}
