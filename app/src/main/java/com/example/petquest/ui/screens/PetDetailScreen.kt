package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.petquest.data.model.Personality
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

    // Collect level-up events while this screen is visible
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
            currentName        = pet.name,
            currentPersonality = pet.personality,
            onDismiss          = { showEditDialog = false },
            onConfirm          = { newName, newPersonality ->
                viewModel.editPet(pet.id, newName, newPersonality)
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
                if (pets.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    LaunchedEffect(Unit) { onBackClick() }
                }
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
            // Photo or emoji
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "🎯", pet.type.name,        "Type")
                StatCard(Modifier.weight(1f), "💫", pet.personality.name, "Vibe")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "🏅", "Lv.${pet.bondLevel}", "Bond Level")
                StatCard(Modifier.weight(1f), "⭐", "${pet.bondPoints}",   "Bond Pts")
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (pet.bondPoints % 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(
                "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Verification section
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

// ─── Edit Pet Dialog ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPetDialog(
    currentName:        String,
    currentPersonality: Personality,
    onDismiss:          () -> Unit,
    onConfirm:          (String, Personality) -> Unit
) {
    var name               by remember { mutableStateOf(currentName) }
    var selectedPersonality by remember { mutableStateOf(currentPersonality) }
    var expanded           by remember { mutableStateOf(false) }

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

                ExposedDropdownMenuBox(
                    expanded         = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value         = selectedPersonality.name,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Personality") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Personality.entries.forEach { p ->
                            DropdownMenuItem(
                                text    = { Text(p.name) },
                                onClick = { selectedPersonality = p; expanded = false }
                            )
                        }
                    }
                }

                Text(
                    "Note: pet type cannot be changed after creation.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(name, selectedPersonality) },
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
                modifier                = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(12.dp)
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
                    fontSize  = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.alpha(contentAlpha)
                )

                // Level progression display
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
