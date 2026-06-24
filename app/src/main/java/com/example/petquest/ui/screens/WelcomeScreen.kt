package com.example.petquest.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petquest.R

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue   = if (started) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 350),
        label         = "content_alpha"
    )

    val buttonAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 700),
        label         = "button_alpha"
    )

    LaunchedEffect(Unit) { started = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter            = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "PetQuest",
            modifier           = Modifier
                .size(120.dp)
                .scale(logoScale),
            contentScale       = ContentScale.Fit
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "PetQuest",
            fontSize   = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.alpha(contentAlpha)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Show up for your pet every day — even small things matter.",
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier  = Modifier.alpha(contentAlpha)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(buttonAlpha)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
