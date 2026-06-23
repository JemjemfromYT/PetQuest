package com.example.petquest.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.petquest.R
import com.example.petquest.viewmodel.PetQuestViewModel
import java.io.File

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

    var photoUri          by remember { mutableStateOf<Uri?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

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
    ) { success ->
        if (success) photoUri = cameraUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraUri)
    }

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
            onDismiss   = onDone
        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Take or choose a photo of your pet to verify them and unlock all features.",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // What unlocks on verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
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
                                painter = painterResource(id = R.drawable.ic_verified),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Photo preview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Pet photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // PNG placeholder — replaces 📷 emoji
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_locked),
                                contentDescription = "No photo",
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                "No photo yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { launchCamera() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take Photo", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    photoUri?.let { uri ->
                        viewModel.verifyPet(petId, uri.toString())
                        showSuccessDialog = true
                    }
                },
                enabled = photoUri != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    if (photoUri != null) "Confirm Verification" else "Take a photo first",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Verification Success Dialog — replaces emoji decorations with PNG ─────────

@Composable
private fun VerificationSuccessDialog(
    petName: String,
    petTypeName: String,
    onDismiss: () -> Unit
) {
    var started by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "verify_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 250),
        label         = "verify_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = { /* prevent accidental dismiss */ }) {
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PNG artwork replaces emoji row and icon — painterResource allows
                // easy swap when verification_success.png is provided
                Image(
                    painter = painterResource(id = R.drawable.verification_success),
                    contentDescription = "Verified",
                    modifier = Modifier
                        .size(100.dp)
                        .scale(iconScale),
                    contentScale = ContentScale.Fit
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Now unlocked:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        listOf(
                            "Task completion",
                            "Bond point earning",
                            "Streak tracking",
                            "Achievement progress"
                        ).forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Text(
                                    feature,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(contentAlpha),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Let's Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
