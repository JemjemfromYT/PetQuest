package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.petquest.data.model.Virtue
import com.example.petquest.ui.VirtueConfig
import com.example.petquest.viewmodel.LevelUpEvent
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: Int,
    viewModel: PetQuestViewModel,
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val pets by viewModel.allPets.collectAsState()
    val pet = pets.find { it.id == petId }

    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var levelUpEvent     by remember { mutableStateOf<LevelUpEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.levelUpEvent.collect { event ->
            if (event.petName == pet?.name) {
                levelUpEvent = event
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showEditDialog && pet != null) {
        EditPetDialog(
            currentName = pet.name,
            onDismiss   = { showEditDialog = false },
            onConfirm   = { newName ->
                viewModel.editPet(pet.id, newName)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && pet != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon             = { Text("🗑️", fontSize = 32.sp) },
            title            = { Text("Delete ${pet.name}?", fontWeight = FontWeight.Bold) },
            text             = {
                Text(
                    "This will permanently remove ${pet.name} and all their tasks. This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton    = {
                Button(
                    onClick = {
                        viewModel.deletePet(pet.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton    = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    levelUpEvent?.let { event ->
        LevelUpDialog(event = event, onDismiss = { levelUpEvent = null })
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Pet Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pet != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit pet")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete pet",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (pet == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val rarityColor = when (pet.type.rarity.name) {
            "COMMON"   -> MaterialTheme.colorScheme.tertiary
            "UNCOMMON" -> MaterialTheme.colorScheme.secondary
            "RARE"     -> MaterialTheme.colorScheme.primary
            else       -> MaterialTheme.colorScheme.error
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Photo or emoji ────────────────────────────────────────────────
            Card(
                modifier  = Modifier.size(200.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (pet.photoUri != null) {
                    AsyncImage(
                        model              = pet.photoUri,
                        contentDescription = "Pet photo",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(petEmoji(pet.type.name), fontSize = 80.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Badge(containerColor = rarityColor) {
                Text(
                    " ${pet.type.rarity.name} ",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Stat cards: Type | Bond Level | Bond Points ───────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(Modifier.weight(1f), "🎯", pet.type.name.replace("_", " "), "Type")
                StatCard(Modifier.weight(1f), "🏅", "Lv.${pet.bondLevel}",           "Bond Level")
                StatCard(Modifier.weight(1f), "⭐", "${pet.bondPoints}",              "Bond Pts")
            }

            Spacer(Modifier.height(12.dp))

            // ── Bond progress bar ─────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { (pet.bondPoints % 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(
                "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // ── Virtue Identity Card ──────────────────────────────────────────
            VirtueIdentityCard(virtue = pet.virtue)

            Spacer(Modifier.height(20.dp))

            // ── Verification section ──────────────────────────────────────────
            if (pet.isVerified) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Pet Verified!", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick  = onVerifyClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Verify My Pet (Take Photo)", fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Virtue Identity Card ─────────────────────────────────────────────────────

@Composable
fun VirtueIdentityCard(virtue: Virtue, modifier: Modifier = Modifier) {
    val info = VirtueConfig[virtue]

    Card(
        modifier  = modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Large emblem
            Image(
                painter            = painterResource(id = info.emblemRes),
                contentDescription = "${virtue.name} emblem",
                modifier           = Modifier.size(88.dp),
                contentScale       = ContentScale.Fit
            )

            Spacer(Modifier.height(4.dp))

            // Virtue name (e.g. WISDOM)
            Text(
                text       = virtue.name,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary
            )

            // Virtue title (e.g. "The Sage")
            Text(
                text       = info.title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Virtue description
            Text(
                text      = info.description,
                fontSize  = 13.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Edit Pet Dialog (name only — virtue is permanent) ───────────────────────

@Composable
private fun EditPetDialog(
    currentName: String,
    onDismiss:   () -> Unit,
    onConfirm:   (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon             = { Text("✏️", fontSize = 28.sp) },
        title            = { Text("Edit Pet", fontWeight = FontWeight.Bold) },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Pet Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text(
                    "Note: pet type and virtue cannot be changed after creation.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(name) },
                enabled  = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Level-Up Celebration Dialog ─────────────────────────────────────────────

@Composable
fun LevelUpDialog(event: LevelUpEvent, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val emojiScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "level_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label         = "level_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = { /* block accidental close */ }) {
        Card(
            shape     = MaterialTheme.shapes.extraLarge,
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎉  ✨  🎊  ✨  🎉", fontSize = 20.sp)

                Text(
                    "🏅",
                    fontSize = 88.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                Text(
                    "Level Up!",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "${event.petName} reached Bond Level ${event.newLevel}!",
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Row(
                    modifier              = Modifier.alpha(contentAlpha),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "Lv.${event.oldLevel}",
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("  →  ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Lv.${event.newLevel}",
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    "Keep completing tasks to reach the next level! 🚀",
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.alpha(contentAlpha)
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Awesome! 🐾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
