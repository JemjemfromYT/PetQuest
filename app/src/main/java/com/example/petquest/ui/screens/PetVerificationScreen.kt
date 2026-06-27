// ============================================================
// FILE PATH:  app/src/main/java/com/example/petquest/ui/screens/PetVerificationScreen.kt
//
// BUGS FIXED:
//   1. SCROLL BUG — The Column had no scroll modifier. On small phones the
//      "Confirm Verification" button was pushed off the bottom of the screen
//      with no way to reach it. Fixed by adding verticalScroll() to the Column
//      and removing Spacer(weight(1f)) which doesn't work in scrollable columns.
//
// HOW TO APPLY:
//   1. Open PetVerificationScreen.kt in Android Studio
//   2. Select ALL (Ctrl+A), Delete
//   3. Paste everything BELOW this comment block
// ============================================================

package com.example.petquest.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.viewmodel.PetQuestViewModel
import java.io.File

private data class OnboardingCard(val emoji: String, val title: String, val body: String)

private val ONBOARDING_CARDS = listOf(
    OnboardingCard(
        emoji = "📋",
        title = "Complete Daily Tasks",
        body  = "Each day you'll get a fresh set of tasks for your pet — Core tasks, Optional tasks, and a special Virtue task unique to their personality."
    ),
    OnboardingCard(
        emoji = "⭐",
        title = "Earn Bond Points & Level Up",
        body  = "Completing tasks earns Bond Points. Every 100 points raises your Bond Level. Higher levels unlock rewards — extra tasks, badges, and a gold border."
    ),
    OnboardingCard(
        emoji = "🔮",
        title = "Your Pet's Virtue",
        body  = "The traits you chose when creating your pet formed their Virtue. Each day your pet's Virtue shapes one special task — a reflection of who they are."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetVerificationScreen(
    petId: Int,
    viewModel: PetQuestViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val pets    by viewModel.allPets.collectAsState()
    val pet     = pets.find { it.id == petId }

    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    var photoUri          by remember { mutableStateOf<Uri?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showOnboarding    by remember { mutableStateOf(false) }

    val photoFile = remember {
        val dir = File(context.filesDir, "pet_photos").also { it.mkdirs() }
        File(dir, "pet_verify_${petId}_${System.currentTimeMillis()}.jpg")
    }

    val cameraUri: Uri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) photoUri = cameraUri }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(cameraUri) }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) cameraLauncher.launch(cameraUri)
        else permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    if (showSuccessDialog && pet != null) {
        VerificationSuccessDialog(
            petName     = pet.name,
            petTypeName = pet.type.name,
            onDismiss   = {
                showSuccessDialog = false
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        // FIX: verticalScroll so the Confirm button is always reachable on small phones
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Take or choose a photo of your pet to verify them and unlock all features.",
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Tasks", "Bond Pts", "Streaks").forEach { label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter            = painterResource(id = R.drawable.ic_verified),
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp),
                                contentScale       = ContentScale.Fit
                            )
                            Text(
                                label,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model              = photoUri,
                        contentDescription = "Pet photo",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter            = painterResource(id = R.drawable.ic_locked),
                                contentDescription = "No photo",
                                modifier           = Modifier.size(64.dp),
                                contentScale       = ContentScale.Fit
                            )
                            Text(
                                "No photo yet",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Button(
                onClick  = { launchCamera() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take Photo", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick  = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold)
            }

            // FIX: replaced Spacer(weight(1f)) with fixed spacer — weight doesn't
            // work inside a verticalScroll column
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    photoUri?.let { uri ->
                        viewModel.verifyPet(petId, uri.toString())
                        showSuccessDialog = true
                    }
                },
                enabled  = photoUri != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    if (photoUri != null) "Confirm Verification" else "Take a photo first",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnboardingOverlay(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val isLast = page == ONBOARDING_CARDS.lastIndex
    val card   = ONBOARDING_CARDS[page]

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(page) {
        visible = false
        kotlinx.coroutines.delay(80)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label         = "onboard_alpha"
    )
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.92f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "onboard_scale"
    )

    Dialog(
        onDismissRequest = {},
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier  = Modifier.fillMaxWidth(0.9f).alpha(alpha).scale(scale),
                shape     = MaterialTheme.shapes.extraLarge,
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
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
                                modifier = Modifier.size(
                                    width  = if (i == page) 20.dp else 8.dp,
                                    height = 8.dp
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (i == page)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
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
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            if (isLast) "Let's Start!" else "Next",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLast) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector        = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp)
                            )
                        }
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

@Composable
private fun VerificationSuccessDialog(
    petName: String,
    petTypeName: String,
    onDismiss: () -> Unit
) {
    var started by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label         = "verify_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 250),
        label         = "verify_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = {}) {
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.verification_success),
                    contentDescription = "Verified",
                    modifier           = Modifier.size(100.dp).scale(iconScale),
                    contentScale       = ContentScale.Fit
                )

                Text(
                    "Pet Verified!",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "$petName the ${petTypeName.replace("_", " ").lowercase()} is now fully active!",
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
                    shape    = MaterialTheme.shapes.medium,
                    color    = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier            = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Now unlocked:",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        listOf("Task completion", "Bond point earning", "Streak tracking", "Achievement progress").forEach { feature ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter            = painterResource(id = R.drawable.ic_verified),
                                    contentDescription = null,
                                    modifier           = Modifier.size(14.dp),
                                    contentScale       = ContentScale.Fit
                                )
                                Text(feature, fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp).alpha(contentAlpha),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Let's Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
