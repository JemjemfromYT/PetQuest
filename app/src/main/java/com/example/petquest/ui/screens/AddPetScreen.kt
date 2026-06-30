package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.petquest.data.model.*
import java.util.Locale

private fun toTitleCase(input: String): String =
    input.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.lowercase(Locale.getDefault()).replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }
    }

private fun calculateVirtue(traits: Set<Trait>): Virtue {
    val tally = traits.groupingBy { it.virtue }.eachCount()
    return Virtue.entries.maxWithOrNull(
        compareBy { tally.getOrDefault(it, 0) }
    ) ?: Virtue.WISDOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(onBackClick: () -> Unit, onSavePet: (PetEntity) -> Unit) {
    var petName         by remember { mutableStateOf("") }
    var expandedType    by remember { mutableStateOf(false) }
    var selectedType    by remember { mutableStateOf(PetType.DOG) }
    var typeSearchQuery by remember { mutableStateOf("") }

    val selectedTraits  = remember { mutableStateListOf<Trait>() }
    val requiredTraits  = 5
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController   = LocalSoftwareKeyboardController.current

    val filteredTypes = remember(typeSearchQuery) {
        PetType.entries.filter {
            typeSearchQuery.isBlank() ||
                    it.name.replace("_", " ").contains(typeSearchQuery.trim(), ignoreCase = true)
        }
    }

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

            // Pet species emoji — live preview, updates as user selects type
            Text(petEmoji(selectedType.name), fontSize = 80.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                toTitleCase(selectedType.name),
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value         = petName,
                onValueChange = { petName = it },
                label         = { Text("Pet Name") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            Spacer(Modifier.height(20.dp))

            // ── Pet type dropdown ──────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = expandedType,
                onExpandedChange = {
                    expandedType = !expandedType
                    if (!expandedType) typeSearchQuery = ""
                }
            ) {
                LaunchedEffect(expandedType) {
                    if (expandedType) {
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
                OutlinedTextField(
                    value         = "${petEmoji(selectedType.name)} ${toTitleCase(selectedType.name)}",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Pet Type") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) },
                    modifier      = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded        = expandedType,
                    onDismissRequest = { expandedType = false; typeSearchQuery = "" }
                ) {
                    OutlinedTextField(
                        value         = typeSearchQuery,
                        onValueChange = { typeSearchQuery = it },
                        placeholder   = { Text("Search animals...") },
                        leadingIcon   = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        singleLine = true,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .focusRequester(searchFocusRequester)
                    )
                    if (filteredTypes.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No animals found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick  = {},
                            enabled  = false
                        )
                    } else {
                        filteredTypes.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(petEmoji(type.name), fontSize = 20.sp)
                                        Column {
                                            Text(
                                                toTitleCase(type.name),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                type.rarity.name,
                                                fontSize = 11.sp,
                                                color    = rarityColor(type.rarity)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedType    = type
                                    expandedType    = false
                                    typeSearchQuery = ""
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Trait picker ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Choose 5 Traits",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (selectedTraits.size == requiredTraits)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${selectedTraits.size} / $requiredTraits",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (selectedTraits.size == requiredTraits)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Pick exactly 5 traits that describe your pet's character.",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Your chosen traits determine your pet's Virtue, which shapes one special daily task.",
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            val allTraits = Trait.entries
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                modifier              = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                userScrollEnabled     = true
            ) {
                items(allTraits) { trait ->
                    val isSelected = trait in selectedTraits
                    val isDisabled = !isSelected && selectedTraits.size >= requiredTraits

                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            if (isSelected) {
                                selectedTraits.remove(trait)
                            } else if (!isDisabled) {
                                selectedTraits.add(trait)
                            }
                        },
                        label = {
                            Text(
                                toTitleCase(trait.name),
                                fontSize  = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines  = 2
                            )
                        },
                        enabled  = !isDisabled || isSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick  = {
                    val virtue = calculateVirtue(selectedTraits.toSet())
                    petToSave = PetEntity(
                        name   = petName.trim(),
                        type   = selectedType,
                        virtue = virtue
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled  = petName.isNotBlank() && selectedTraits.size == requiredTraits
            ) {
                Text("Add Pet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
private fun rarityColor(rarity: Rarity) = when (rarity) {
    Rarity.COMMON   -> MaterialTheme.colorScheme.onSurfaceVariant
    Rarity.UNCOMMON -> MaterialTheme.colorScheme.primary
    Rarity.RARE     -> MaterialTheme.colorScheme.tertiary
    Rarity.EPIC     -> MaterialTheme.colorScheme.error
}

@Composable
private fun CelebrationDialog(petName: String, petTypeName: String, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label         = "content_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Dialog(onDismissRequest = {}) {
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
                Text(
                    petEmoji(petTypeName),
                    fontSize = 88.sp,
                    modifier = Modifier.scale(iconScale)
                )

                Text(
                    "Welcome, $petName!",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.alpha(contentAlpha)
                )

                Text(
                    "Your new ${toTitleCase(petTypeName).lowercase()} companion is ready for adventures.",
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.alpha(contentAlpha)
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(contentAlpha)
                ) {
                    Text("Let's Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
