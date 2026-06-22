package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val pet = pets.find { it.id == petId } ?: return

    val rarityColor = when (pet.type.rarity.name) {
        "COMMON" -> MaterialTheme.colorScheme.tertiary
        "UNCOMMON" -> MaterialTheme.colorScheme.secondary
        "RARE" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.size(200.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (pet.photoUri != null) {
                    AsyncImage(
                        model = pet.photoUri,
                        contentDescription = "Pet photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "🎯", pet.type.name, "Type")
                StatCard(Modifier.weight(1f), "💫", pet.personality.name, "Vibe")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "🏅", "Lv.${pet.bondLevel}", "Bond Level")
                StatCard(Modifier.weight(1f), "⭐", "${pet.bondPoints}", "Bond Pts")
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (pet.bondPoints % 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(
                "${pet.bondPoints % 100}/100 pts to Level ${pet.bondLevel + 1}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (pet.isVerified) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Pet Verified!", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onVerifyClick,
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