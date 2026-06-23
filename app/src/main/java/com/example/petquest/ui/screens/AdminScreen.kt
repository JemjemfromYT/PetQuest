package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: PetQuestViewModel, onBackClick: () -> Unit) {
    val pets   by viewModel.allPets.collectAsState()
    val streak by viewModel.userStreak.collectAsState()

    var streakInput by remember(streak) { mutableStateOf(streak.toString()) }

    var snackMessage      by remember { mutableStateOf("") }
    var snackTrigger      by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackTrigger) {
        if (snackTrigger > 0 && snackMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackMessage)
        }
    }

    fun showSnack(msg: String) {
        snackMessage = msg
        snackTrigger++
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Warning banner ─────────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Admin / Debug Mode",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "For testing only. Changes are real and saved.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // ── Bond Points section ────────────────────────────────────────
            item { AdminSectionHeader("Bond Points") }

            if (pets.isEmpty()) {
                item {
                    Text(
                        "No pets yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(pets, key = { it.id }) { pet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Monogram replaces petEmoji() — first 2 letters of type
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            pet.type.name.take(2),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    pet.name,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Lv.${pet.bondLevel} • ${pet.bondPoints} pts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(10, 50, 100, 500).forEach { pts ->
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.adminAddBondPoints(pet.id, pts)
                                            showSnack("+$pts pts added to ${pet.name}")
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("+$pts", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Tasks section ──────────────────────────────────────────────
            item { AdminSectionHeader("Tasks") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.adminCompleteAllTasks()
                            showSnack("All tasks completed!")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Complete All", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.adminResetTasks()
                            showSnack("Tasks reset!")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Tasks", fontSize = 13.sp)
                    }
                }
            }

            // ── Achievements section ───────────────────────────────────────
            item { AdminSectionHeader("Achievements") }

            item {
                Button(
                    onClick = {
                        viewModel.adminUnlockAllAchievements()
                        showSnack("All achievements unlocked!")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Unlock All Achievements")
                }
            }

            // ── Streak section ─────────────────────────────────────────────
            item { AdminSectionHeader("Streak") }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = streakInput,
                        onValueChange = { streakInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val v = streakInput.toIntOrNull() ?: 0
                            viewModel.adminSetStreak(v)
                            showSnack("Streak set to $v days")
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Set")
                    }
                }
            }

            // ── Verification section ───────────────────────────────────────
            item { AdminSectionHeader("Verification") }

            item {
                Button(
                    onClick = {
                        viewModel.adminVerifyAllPets()
                        showSnack("All pets verified!")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify All Pets")
                }
            }

            // ── Close button ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Close Admin Mode", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AdminSectionHeader(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
    HorizontalDivider()
}
