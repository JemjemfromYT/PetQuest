// ============================================================
// FILE PATH: app/src/main/java/com/example/petquest/ui/screens/PetVerificationScreen.kt
//
// HOW TO APPLY:
//   1. Open PetVerificationScreen.kt in Android Studio
//   2. Select ALL (Ctrl+A), Delete
//   3. Paste everything below this comment block, then Build
//
// ALSO REQUIRED:
//   - Use the updated app_build.gradle.kts.txt (same as before — no change needed)
//   - AndroidManifest.xml must have:
//       <uses-permission android:name="android.permission.CAMERA" />
//
// ROOT CAUSE OF surfaceProvider ERROR:
//   Previous versions used `Preview.Builder()` + `previewView.surfaceProvider`.
//   The `surfaceProvider` property on PreviewView can fail to resolve depending on
//   Kotlin/CameraX version alignment. This version switches to LifecycleCameraController
//   which wires the preview automatically through `previewView.controller = controller`
//   — completely avoiding `surfaceProvider` and the Preview use-case setup.
// ============================================================

package com.example.petquest.ui.screens

import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.petquest.data.model.PetType
import com.example.petquest.viewmodel.PetQuestViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

// ─────────────────────────────────────────────────────────────
// ML label sets — what ML Kit is expected to return per pet type.
// ─────────────────────────────────────────────────────────────
private val PET_TYPE_LABELS: Map<PetType, List<String>> = mapOf(
    PetType.DOG        to listOf("dog", "canine", "puppy", "poodle", "labrador", "bulldog", "terrier", "hound"),
    PetType.CAT        to listOf("cat", "kitten", "tabby", "siamese", "persian", "feline"),
    PetType.RABBIT     to listOf("rabbit", "hare", "bunny"),
    PetType.HAMSTER    to listOf("hamster", "gerbil"),
    PetType.BIRD       to listOf("bird", "parrot", "budgerigar", "cockatoo", "canary", "parakeet", "macaw"),
    PetType.FISH       to listOf("fish", "goldfish", "koi", "bony fish"),
    PetType.TURTLE     to listOf("turtle", "tortoise"),
    PetType.LIZARD     to listOf("lizard", "gecko", "iguana", "chameleon"),
    PetType.SNAKE      to listOf("snake", "python", "boa", "serpent"),
    PetType.HEDGEHOG   to listOf("hedgehog", "echidna"),
    PetType.CHICKEN    to listOf("chicken", "hen", "rooster", "poultry"),
    PetType.GUINEA_PIG to listOf("guinea pig", "cavy"),
    PetType.FERRET     to listOf("ferret", "mink"),
    PetType.HORSE      to listOf("horse", "pony", "mare", "stallion", "foal"),
    PetType.DUCK       to listOf("duck", "mallard"),
    PetType.FROG       to listOf("frog", "toad", "amphibian"),
    PetType.CRAB       to listOf("crab", "crustacean"),
    PetType.MONKEY     to listOf("monkey", "primate", "ape", "chimpanzee", "macaque"),
    PetType.FOX        to listOf("fox"),
    PetType.OWL        to listOf("owl", "raptor"),
    PetType.PENGUIN    to listOf("penguin"),
    PetType.PANDA      to listOf("panda", "giant panda"),
    PetType.GOAT       to listOf("goat"),
    PetType.PIG        to listOf("pig", "swine", "hog"),
    PetType.COW        to listOf("cattle", "cow", "bull", "bovine"),
    PetType.SHEEP      to listOf("sheep", "lamb"),
    PetType.DEER       to listOf("deer", "fawn", "reindeer"),
    PetType.BEAR       to listOf("bear", "grizzly"),
    PetType.WOLF       to listOf("wolf")
)

private val GENERIC_ANIMAL_LABELS = listOf(
    "animal", "mammal", "wildlife", "pet", "vertebrate", "fauna",
    "rodent", "reptile", "livestock", "bird", "canine", "feline"
)

private fun confidenceForType(petType: PetType, labels: List<Pair<String, Float>>): Float {
    val specific = PET_TYPE_LABELS[petType] ?: emptyList()
    var best = 0f
    for ((text, conf) in labels) {
        if (specific.any { text.contains(it) || it.contains(text) }) {
            best = maxOf(best, conf)
        }
    }
    val genericBoost = labels
        .filter { (text, _) -> GENERIC_ANIMAL_LABELS.any { text.contains(it) } }
        .maxOfOrNull { it.second }
        ?.times(0.20f) ?: 0f
    return minOf(1f, best + genericBoost)
}

// ─────────────────────────────────────────────────────────────
// @ExperimentalGetImage required to read YUV image from ImageProxy
// ─────────────────────────────────────────────────────────────
@ExperimentalGetImage
private fun analyzeFrame(
    imageProxy: ImageProxy,
    labeler   : ImageLabeler,
    onResult  : (List<Pair<String, Float>>) -> Unit
) {
    val media = imageProxy.image
    if (media != null) {
        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        labeler.process(input)
            .addOnSuccessListener { labels ->
                onResult(labels.map { it.text.lowercase() to it.confidence })
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

// ─────────────────────────────────────────────────────────────
// Onboarding cards
// ─────────────────────────────────────────────────────────────
private data class OnboardingCard(val emoji: String, val title: String, val body: String)

private val ONBOARDING_CARDS = listOf(
    OnboardingCard(
        emoji = "📋",
        title = "Complete Daily Tasks",
        body  = "Each day you'll get a fresh set of tasks — Core tasks, Optional tasks, and a special Virtue task unique to their personality."
    ),
    OnboardingCard(
        emoji = "⭐",
        title = "Earn Bond Points & Level Up",
        body  = "Completing tasks earns Bond Points. Every 100 points raises your Bond Level. Higher levels unlock rewards, badges, and a gold border."
    ),
    OnboardingCard(
        emoji = "🔮",
        title = "Your Pet's Virtue",
        body  = "The traits you chose formed their Virtue. Each day your pet's Virtue shapes one special task — a reflection of who they are."
    )
)

// ─────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetVerificationScreen(
    petId    : Int,
    viewModel: PetQuestViewModel,
    onDone   : () -> Unit
) {
    val context = LocalContext.current
    val pets    by viewModel.allPets.collectAsState()
    val pet     = pets.find { it.id == petId }
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var scanConfidence by remember { mutableFloatStateOf(0f) }
    var topLabels      by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var scanningActive by remember { mutableStateOf(true) }
    var showSuccess    by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }

    val animatedConfidence by animateFloatAsState(
        targetValue   = scanConfidence,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "confidence"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    val labeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.30f)
                .build()
        )
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            labeler.close()
            cameraExecutor.shutdown()
        }
    }

    if (showSuccess && pet != null) {
        VerificationSuccessDialog(
            petName     = pet.name,
            petTypeName = pet.type.name,
            confidence  = scanConfidence,
            onDismiss   = {
                showSuccess = false
                viewModel.verifyPet(petId, "ml_verified_${System.currentTimeMillis()}")
                if (!hasSeenOnboarding) {
                    viewModel.markOnboardingSeen()
                    showOnboarding = true
                } else {
                    onDone()
                }
            }
        )
    }

    if (showOnboarding) {
        OnboardingOverlay(onFinish = onDone)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Your Pet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasCameraPermission) {
                PermissionCard {
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            } else {
                val petTypeName = pet?.type?.name
                    ?.replace("_", " ")
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() }
                    ?: "your pet"

                StatusBanner(
                    confidence  = animatedConfidence,
                    petTypeName = petTypeName,
                    scanning    = scanningActive
                )

                // ── Camera preview ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    // KEY FIX: LifecycleCameraController wires the preview
                    // automatically via previewView.controller — no surfaceProvider needed.
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory  = { ctx ->
                            val previewView = PreviewView(ctx)

                            val controller = LifecycleCameraController(ctx).apply {
                                setEnabledUseCases(
                                    CameraController.IMAGE_ANALYSIS or
                                            CameraController.IMAGE_CAPTURE
                                )
                                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                setImageAnalysisAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!scanningActive) {
                                        imageProxy.close()
                                        return@setImageAnalysisAnalyzer
                                    }
                                    analyzeFrame(imageProxy, labeler) { parsed ->
                                        topLabels = parsed.take(5)
                                        pet?.let { p ->
                                            scanConfidence = confidenceForType(p.type, parsed)
                                        }
                                    }
                                }
                                // lifecycleOwner is obtained inside the factory via ctx cast
                                // which is always a ComponentActivity / FragmentActivity
                            }

                            // Bind lifecycle: ctx is the Activity which IS a LifecycleOwner
                            val lifecycleOwner = ctx as androidx.lifecycle.LifecycleOwner
                            controller.bindToLifecycle(lifecycleOwner)

                            previewView.controller = controller
                            previewView
                        }
                    )

                    ScanOverlay(
                        scanComplete = animatedConfidence >= 0.75f,
                        pulseAlpha   = if (animatedConfidence < 0.75f) pulseAlpha else 1f,
                        scanLineY    = scanLineOffset
                    )
                }

                if (topLabels.isNotEmpty()) {
                    DetectedLabelsRow(labels = topLabels)
                }

                Spacer(Modifier.height(12.dp))

                ConfidenceRing(
                    confidence  = animatedConfidence,
                    petTypeName = petTypeName
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick  = {
                        scanningActive = false
                        showSuccess    = true
                    },
                    enabled  = scanConfidence >= 0.75f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (scanConfidence >= 0.75f) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Confirm — ${(scanConfidence * 100).toInt()}% match",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Scanning for $petTypeName...",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Status banner
// ─────────────────────────────────────────────────────────────
@Composable
private fun StatusBanner(confidence: Float, petTypeName: String, scanning: Boolean) {
    val text = when {
        !scanning           -> "Verification complete"
        confidence >= 0.75f -> "$petTypeName detected!"
        confidence >= 0.40f -> "Animal detected — move closer..."
        else                -> "Point camera at your $petTypeName"
    }
    val containerColor = when {
        confidence >= 0.75f -> MaterialTheme.colorScheme.secondaryContainer
        confidence >= 0.40f -> MaterialTheme.colorScheme.primaryContainer
        else                -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Text(
            text       = text,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Scan overlay — corner brackets, sweep line, success ring
// ─────────────────────────────────────────────────────────────
@Composable
private fun ScanOverlay(
    scanComplete: Boolean,
    pulseAlpha  : Float,
    scanLineY   : Float
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF2E7D32)
    val bracketColor = if (scanComplete) successColor else primaryColor

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w          = size.width
        val h          = size.height
        val bracketLen = w * 0.12f
        val strokeW    = 4.dp.toPx()
        val inset      = 20.dp.toPx()

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.7f
            )
        )

        drawBracket(Offset(inset, inset),         bracketLen, strokeW, bracketColor.copy(alpha = pulseAlpha), false, false)
        drawBracket(Offset(w - inset, inset),     bracketLen, strokeW, bracketColor.copy(alpha = pulseAlpha), true,  false)
        drawBracket(Offset(inset, h - inset),     bracketLen, strokeW, bracketColor.copy(alpha = pulseAlpha), false, true)
        drawBracket(Offset(w - inset, h - inset), bracketLen, strokeW, bracketColor.copy(alpha = pulseAlpha), true,  true)

        if (!scanComplete) {
            val lineY = inset + (h - inset * 2) * scanLineY
            drawLine(
                brush       = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                start       = Offset(inset, lineY),
                end         = Offset(w - inset, lineY),
                strokeWidth = 2.5f.dp.toPx()
            )
        } else {
            val cx = w / 2f
            val cy = h / 2f
            val r  = 36.dp.toPx()
            drawCircle(color = successColor.copy(alpha = 0.25f), radius = r * 1.4f, center = Offset(cx, cy))
            drawCircle(color = successColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW))
        }
    }
}

private fun DrawScope.drawBracket(
    anchor     : Offset,
    len        : Float,
    strokeWidth: Float,
    color      : Color,
    flipH      : Boolean,
    flipV      : Boolean
) {
    val dx = if (flipH) -len else len
    val dy = if (flipV) -len else len
    drawLine(color = color, start = anchor, end = Offset(anchor.x + dx, anchor.y), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = anchor, end = Offset(anchor.x, anchor.y + dy), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

// ─────────────────────────────────────────────────────────────
// Confidence ring
// ─────────────────────────────────────────────────────────────
@Composable
private fun ConfidenceRing(confidence: Float, petTypeName: String) {
    val ringColor = when {
        confidence >= 0.75f -> Color(0xFF2E7D32)
        confidence >= 0.40f -> MaterialTheme.colorScheme.primary
        else                -> MaterialTheme.colorScheme.surfaceVariant
    }
    val pct = (confidence * 100).toInt()

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke  = 6.dp.toPx()
                val pad     = stroke / 2f
                val arcSize = GeomSize(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(pad, pad), size = arcSize
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f, sweepAngle = 360f * confidence, useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(pad, pad), size = arcSize
                )
            }
            Text(text = "$pct%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ringColor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = if (confidence >= 0.75f) "$petTypeName verified!" else "Match confidence",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = ringColor
            )
            Text(
                text  = when {
                    confidence >= 0.75f -> "Ready to confirm — tap the button above"
                    confidence >= 0.40f -> "Getting closer — keep camera steady"
                    else                -> "Hold your phone up to your $petTypeName"
                },
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Detected labels chips
// ─────────────────────────────────────────────────────────────
@Composable
private fun DetectedLabelsRow(labels: List<Pair<String, Float>>) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        labels.take(4).forEach { (label, conf) ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text     = "${label.replaceFirstChar { it.uppercase() }} ${(conf * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Permission request card
// ─────────────────────────────────────────────────────────────
@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📷", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Camera Access Needed", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "PetQuest uses your camera to verify your pet using on-device AI. No photos are uploaded — everything stays on your phone.",
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Allow Camera", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Verification success dialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun VerificationSuccessDialog(
    petName    : String,
    petTypeName: String,
    confidence : Float,
    onDismiss  : () -> Unit
) {
    var started by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "verify_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(500, delayMillis = 250),
        label         = "verify_alpha"
    )
    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = {}) {
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.size(100.dp).scale(iconScale)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0xFF2E7D32).copy(alpha = 0.2f))
                        drawCircle(color = Color(0xFF2E7D32), style = Stroke(width = 4.dp.toPx()))
                    }
                    Text("✓", fontSize = 48.sp, color = Color(0xFF2E7D32))
                }

                Text(
                    "${(confidence * 100).toInt()}% Match Confirmed",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF2E7D32),
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "Pet Verified!",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "$petName the ${petTypeName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} is now fully active!",
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
                    shape    = MaterialTheme.shapes.medium,
                    color    = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Now unlocked:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        listOf("Task completion", "Bond point earning", "Streak tracking", "Achievement progress").forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("✓", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                Text(feature, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp).alpha(contentAlpha),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text("Let's Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Onboarding overlay
// ─────────────────────────────────────────────────────────────
@Composable
private fun OnboardingOverlay(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val isLast = page == ONBOARDING_CARDS.lastIndex
    val card   = ONBOARDING_CARDS[page]

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(page) {
        visible = false
        delay(80)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label         = "onboard_alpha"
    )
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.92f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "onboard_scale"
    )

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier  = Modifier.fillMaxWidth(0.9f).alpha(alpha).scale(scale),
                shape     = MaterialTheme.shapes.extraLarge,
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ONBOARDING_CARDS.indices.forEach { i ->
                            Surface(
                                modifier = Modifier.size(width = if (i == page) 20.dp else 8.dp, height = 8.dp),
                                shape    = MaterialTheme.shapes.extraSmall,
                                color    = if (i == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            ) {}
                        }
                    }

                    Text(card.emoji, fontSize = 64.sp)

                    Text(
                        card.title,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        card.body,
                        fontSize   = 14.sp,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick  = { if (isLast) onFinish() else page++ },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (isLast) "Let's Start!" else "Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (!isLast) { Spacer(Modifier.width(6.dp)); Text("→", fontSize = 16.sp) }
                    }

                    if (!isLast) {
                        TextButton(onClick = onFinish) {
                            Text("Skip", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
