package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(onBackClick: () -> Unit, onSavePet: (PetEntity) -> Unit) {
    var petName by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(PetType.DOG) }
    var expandedPersonality by remember { mutableStateOf(false) }
    var selectedPersonality by remember { mutableStateOf(Personality.PLAYFUL) }

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

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Pet Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))

            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = "${petEmoji(selectedType.name)} ${selectedType.name}  (${selectedType.rarity.name})",
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
                            text = { Text("${petEmoji(type.name)} ${type.name}  (${type.rarity.name})") },
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
                    onSavePet(
                        PetEntity(
                            name = petName.trim(),
                            type = selectedType,
                            personality = selectedPersonality
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = petName.isNotBlank()
            ) {
                Text("Save Pet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBackClick) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}