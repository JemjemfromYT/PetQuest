package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R
import com.example.petquest.data.model.PetType
import com.example.petquest.data.model.Rarity
import com.example.petquest.viewmodel.PetQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(viewModel: PetQuestViewModel) {
    val collectedSpecies     by viewModel.collectedSpecies.collectAsState()
    val collectionPercentage by viewModel.collectionPercentage.collectAsState()
    val rarityStats          by viewModel.rarityCollectionStats.collectAsState()

    val totalSpecies   = PetType.entries.size
    val collectedCount = collectedSpecies.size

    var selectedRarity by remember { mutableStateOf<Rarity?>(null) }

    val rarityTabs: List<Rarity?> = listOf(null) + Rarity.entries
    val tabLabels = listOf("All") + Rarity.entries.map { rarity ->
        rarity.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    val filteredTypes = remember(selectedRarity) {
        if (selectedRarity == null) PetType.entries
        else PetType.entries.filter { it.rarity == selectedRarity }
    }

    // Animated collection progress
    val collectionProgress by animateFloatAsState(
        targetValue = if (totalSpecies == 0) 0f else collectedCount / totalSpecies.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "encyclopedia_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encyclopedia", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Collection Progress Header ───────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Collection Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "$collectedCount / $totalSpecies",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { collectionProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$collectionPercentage% of all species collected",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Milestone chips — text only, no emojis
                    val milestones = listOf(25, 50, 75, 100)
                    val reached = milestones.filter { collectionPercentage >= it }
                    if (reached.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reached.forEach { pct ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "$pct%",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Rarity Stats Row ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Rarity.entries.forEach { rarity ->
                    val stat       = rarityStats[rarity] ?: (0 to 0)
                    val rarityColor = encyclopediaRarityColor(rarity)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape    = MaterialTheme.shapes.small,
                        color    = rarityColor.copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${stat.first}/${stat.second}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = rarityColor
                            )
                            Text(
                                rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 9.sp,
                                color = rarityColor
                            )
                        }
                    }
                }
            }

            // ── Rarity Filter Tabs ───────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = rarityTabs.indexOf(selectedRarity),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                rarityTabs.forEachIndexed { index, rarity ->
                    Tab(
                        selected = selectedRarity == rarity,
                        onClick  = { selectedRarity = rarity },
                        text = {
                            Text(
                                tabLabels[index],
                                fontWeight = if (selectedRarity == rarity) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Species Grid ─────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTypes, key = { it.name }) { petType ->
                    val isCollected = petType in collectedSpecies
                    SpeciesCard(petType = petType, isCollected = isCollected)
                }
            }
        }
    }
}

@Composable
private fun SpeciesCard(petType: PetType, isCollected: Boolean) {
    val rarityColor = encyclopediaRarityColor(petType.rarity)

    // Fade in on first appearance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(petType.name) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "species_alpha_${petType.name}"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        elevation = CardDefaults.cardElevation(if (isCollected) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCollected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // PNG-ready: replace with per-species drawable when artwork available.
                    // Uncollected species show a lock placeholder instead of ❓ emoji.
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isCollected)
                            rarityColor.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCollected) {
                                // First two letters of species name as monogram
                                Text(
                                    petType.name.take(2),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rarityColor
                                )
                            } else {
                                // Locked appearance — not emoji
                                Text(
                                    "?",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }

                if (isCollected) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape    = MaterialTheme.shapes.extraSmall,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isCollected) petType.name.replace("_", " ") else "???",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCollected)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (isCollected) rarityColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = petType.rarity.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCollected) rarityColor
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun encyclopediaRarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON   -> MaterialTheme.colorScheme.tertiary
    Rarity.UNCOMMON -> MaterialTheme.colorScheme.secondary
    Rarity.RARE     -> MaterialTheme.colorScheme.primary
    Rarity.EPIC     -> MaterialTheme.colorScheme.error
}
