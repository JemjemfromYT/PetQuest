package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: PetQuestViewModel) {
    val achievements         by viewModel.allAchievements.collectAsState()
    val collectionPercentage by viewModel.collectionPercentage.collectAsState()
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()

    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount    = achievements.size

    val emojiMap = mapOf(
        "First Pet"          to "🐾",
        "First Verification" to "📷",
        "Pet Lover"          to "❤️",
        "Bond Master"        to "👑",
        "7-Day Streak"       to "🔥",
        "Epic Tamer"         to "🐉",
        "Rarity Hunter"      to "🎯",
        "Species Collector"  to "🗂️",
        "Animal Explorer"    to "🌍"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Summary header ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$unlockedCount / $totalCount Unlocked",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${if (totalCount == 0) 0 else (unlockedCount * 100) / totalCount}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalCount == 0) 0f else unlockedCount / totalCount.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Species Collected",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${collectedSpecies.size}/29  ($collectionPercentage%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Achievement cards ───────────────────────────────────────────
            items(achievements) { a ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (a.isUnlocked)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(if (a.isUnlocked) 4.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Achievement icon: emoji for unlocked, Lock icon for locked
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (a.isUnlocked) {
                                Text(
                                    text = emojiMap[a.title] ?: "⭐",
                                    fontSize = 36.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                a.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
