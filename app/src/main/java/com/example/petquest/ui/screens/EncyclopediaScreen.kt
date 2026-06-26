package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val collectionProgress by animateFloatAsState(
        targetValue   = if (totalSpecies == 0) 0f else collectedCount / totalSpecies.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "encyclopedia_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Compact progress header ──────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { collectionProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
                Text(
                    "$collectedCount / $totalSpecies",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.primary
                )
            }

            // ── Rarity stats row ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Rarity.entries.forEach { rarity ->
                    val stat       = rarityStats[rarity] ?: (0 to 0)
                    val rarityColor = encyclopediaRarityColor(rarity)
                    val isComplete = stat.first == stat.second && stat.second > 0
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape    = MaterialTheme.shapes.small,
                        color    = rarityColor.copy(alpha = if (isComplete) 0.25f else 0.12f)
                    ) {
                        Column(
                            modifier            = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${stat.first}/${stat.second}",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color      = rarityColor
                            )
                            Text(
                                rarity.name.take(1) + rarity.name.drop(1).lowercase(),
                                fontSize = 9.sp,
                                color    = rarityColor
                            )
                        }
                    }
                }
            }

            // ── Rarity filter tabs — color-coded ─────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = rarityTabs.indexOf(selectedRarity),
                edgePadding      = 16.dp,
                containerColor   = MaterialTheme.colorScheme.surface
            ) {
                rarityTabs.forEachIndexed { index, rarity ->
                    val tabColor = if (rarity == null)
                        MaterialTheme.colorScheme.primary
                    else
                        encyclopediaRarityColor(rarity)
                    Tab(
                        selected     = selectedRarity == rarity,
                        onClick      = { selectedRarity = rarity },
                        selectedContentColor   = tabColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                tabLabels[index],
                                fontWeight = if (selectedRarity == rarity) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize   = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Species grid ─────────────────────────────────────────────────
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxSize()
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(petType.name) { visible = true }

    var prevCollected by remember { mutableStateOf(isCollected) }
    var collectPulse  by remember { mutableStateOf(false) }
    LaunchedEffect(isCollected) {
        if (isCollected && !prevCollected) {
            collectPulse = true
            kotlinx.coroutines.delay(600)
            collectPulse = false
        }
        prevCollected = isCollected
    }
    val pulseScale by animateFloatAsState(
        targetValue   = if (collectPulse) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "collect_pulse_${petType.name}"
    )

    val cardAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label         = "species_alpha_${petType.name}"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .then(
                if (isCollected)
                    Modifier
                        .border(width = 1.5.dp, color = rarityColor.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                        .scale(pulseScale)
                else
                    Modifier
            ),
        shape     = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(if (isCollected) 4.dp else 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isCollected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Rarity accent strip at top ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        if (isCollected) rarityColor else rarityColor.copy(alpha = 0.25f)
                    )
            )

            Spacer(Modifier.height(10.dp))

            // ── Species visual ────────────────────────────────────────────────
            Box(
                modifier         = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCollected) {
                    // Full emoji
                    Text(petEmoji(petType.name), fontSize = 44.sp)
                } else {
                    // Ghost silhouette + question mark
                    Text(
                        petEmoji(petType.name),
                        fontSize = 44.sp,
                        modifier = Modifier.alpha(0.07f)
                    )
                    Text(
                        "?",
                        fontSize   = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = rarityColor.copy(alpha = 0.65f)
                    )
                }
                // Collected checkmark badge
                if (isCollected) {
                    Box(
                        modifier          = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment  = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = "Collected",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Name ──────────────────────────────────────────────────────────
            Text(
                text       = if (isCollected) petType.name.replace("_", " ") else "???",
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
                color      = if (isCollected)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )

            Spacer(Modifier.height(4.dp))

            // ── Rarity badge ──────────────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (isCollected)
                    rarityColor.copy(alpha = 0.18f)
                else
                    rarityColor.copy(alpha = 0.08f)
            ) {
                Text(
                    text       = petType.rarity.name,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isCollected) rarityColor else rarityColor.copy(alpha = 0.35f)
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun encyclopediaRarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON   -> Color(0xFF9E9E9E)
    Rarity.UNCOMMON -> MaterialTheme.colorScheme.secondary
    Rarity.RARE     -> MaterialTheme.colorScheme.primary
    Rarity.EPIC     -> MaterialTheme.colorScheme.error
}
