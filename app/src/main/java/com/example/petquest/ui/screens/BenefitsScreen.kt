package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BenefitsScreen(onContinue: () -> Unit) {
    val benefits: List<Pair<ImageVector, String>> = listOf(
        Icons.Default.Pets        to "Build daily habits with your pet",
        Icons.Default.CheckCircle to "Daily tasks to deepen your bond",
        Icons.Default.Whatshot    to "Keep your streak alive — every day counts",
        Icons.Default.EmojiEvents to "Unlock achievements as you grow",
        Icons.Default.CameraAlt   to "Snap a photo to confirm your pet is real"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "What you'll do together",
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.primary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Every day with your pet looks like this:",
            fontSize  = 15.sp,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        benefits.forEach { (icon, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(text, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Add My First Pet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
    }
}
