package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.petquest.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(onBackClick: () -> Unit, onSavePet: (PetEntity) -> Unit) {
    var petName             by remember { mutableStateOf("") }
    var expandedType        by remember { mutableStateOf(false) }
    var selectedType        by remember { mutableStateOf(PetType.DOG) }
    var expandedPersonality by remember { mutableStateOf(false) }
    var selectedPersonality by remember { mutableStateOf(Personality.PLAYFUL) }

    // Hold the pet to save until celebration is dismissed
    var petToSave by remember { mutableStateOf<PetEntity?>(null) }

    if (petToSave != null) {
        CelebrationDialog(
            petName     = petToSave!!.name,
            petTypeName = petToSave!!.type.name,
            onDismiss   = { onSavePet(petToSave!!); petToSave = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Pet", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Live preview of selected pet
            Text(petEmoji(selectedType.name), fontSize = 80.sp)
            Spacer(Modifier.height(4.dp))
            Text(selectedType.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Pet Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))

            // Pet Type — emoji + name only, no rarity shown
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = "${petEmoji(selectedType.name)} ${selectedType.name}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pet Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    PetType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text("${petEmoji(type.name)} ${type.name}") },
                            onClick = { selectedType = type; expandedType = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            ExposedDropdownMenuBox(
                expanded = expandedPersonality,
                onExpandedChange = { expandedPersonality = !expandedPersonality }
            ) {
                OutlinedTextField(
                    value = selectedPersonality.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Personality") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedPersonality) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedPersonality,
                    onDismissRequest = { expandedPersonality = false }
                ) {
                    Personality.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = { selectedPersonality = p; expandedPersonality = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    petToSave = PetEntity(
                        name        = petName.trim(),
                        type        = selectedType,
                        personality = selectedPersonality
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = petName.isNotBlank()
            ) {
                Text("Add Pet 🐾", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBackClick) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CelebrationDialog(petName: String, petTypeName: String, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val emojiScale by animateFloatAsState(
        targetValue  = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "emoji_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue  = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "content_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = { /* block accidental close */ }) {
        Card(
            shape  = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎉  ✨  🎊  ✨  🎉", fontSize = 20.sp)

                Text(
                    petEmoji(petTypeName),
                    fontSize = 88.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                Text(
                    "Welcome, $petName!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(contentAlpha)
                )

                Text(
                    "Your new ${petTypeName.lowercase()} companion is ready for adventures! 🚀",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(contentAlpha)
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Let's Go! 🐾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
