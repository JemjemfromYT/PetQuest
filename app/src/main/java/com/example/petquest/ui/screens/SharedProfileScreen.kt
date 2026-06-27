package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.data.repository.FirebaseRepository
import com.example.petquest.data.repository.PublicProfile
import kotlinx.coroutines.launch

private sealed interface ProfileState {
    data object Loading  : ProfileState
    data class  Success(val profile: PublicProfile) : ProfileState
    data object NotFound : ProfileState
    data object Error    : ProfileState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedProfileScreen(
    uid                : String,
    firebaseRepository : FirebaseRepository,
    onBackClick        : () -> Unit
) {
    var state by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = ProfileState.Loading
            try {
                val profile = firebaseRepository.fetchProfile(uid)
                state = if (profile != null) ProfileState.Success(profile) else ProfileState.NotFound
            } catch (e: Exception) {
                state = ProfileState.Error
            }
        }
    }

    LaunchedEffect(uid) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trainer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val s = state) {
                is ProfileState.Loading  -> LoadingState()
                is ProfileState.NotFound -> NotFoundState(onBackClick)
                is ProfileState.Error    -> ErrorState(onRetry = { load() })
                is ProfileState.Success  -> ProfileCard(s.profile)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading trainer profile…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotFoundState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Profile Not Found", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("This trainer may have uninstalled PetQuest\nor the link has expired.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Go Back") }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("Couldn't Load Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Check your internet connection and try again.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun ProfileCard(profile: PublicProfile) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "fade")
    val scale by animateFloatAsState(if (visible) 1f else 0.92f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .scale(scale)
            .graphicsLayer(alpha = alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFFFFB77A), Color(0xFFFF8C42)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, null, modifier = Modifier.size(48.dp), tint = Color.White)
            }
            Surface(shape = CircleShape, color = Color(0xFF6650A4), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${profile.level}", fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        Text(profile.trainerName, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("PetQuest Trainer", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), Icons.Default.Star,
                    "${profile.level}", "Level", Color(0xFFF9A825), Color(0xFFFFF8E1))
                StatCard(Modifier.weight(1f), Icons.Default.Whatshot,
                    "${profile.streak}", "Day Streak", Color(0xFFE64A19), Color(0xFFFBE9E7))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), Icons.Default.Favorite,
                    "${profile.bondPoints}", "Bond Points", Color(0xFFAD1457), Color(0xFFFCE4EC))
                StatCard(Modifier.weight(1f), Icons.Default.Pets,
                    "${profile.petCount}", "Pets", Color(0xFF2E7D32), Color(0xFFE8F5E9))
            }
            StatCard(Modifier.fillMaxWidth(), Icons.Default.Explore,
                "${profile.speciesCount} species", "Discovered", Color(0xFF1565C0), Color(0xFFE3F2FD))
        }

        Text("🐾 Get PetQuest to start your own journey!",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, value: String,
                     label: String, iconTint: Color, bgColor: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, label, tint = iconTint, modifier = Modifier.size(24.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = iconTint)
            Text(label, fontSize = 11.sp, color = iconTint.copy(alpha = 0.75f))
        }
    }
}
