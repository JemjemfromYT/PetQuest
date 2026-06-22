package com.example.petquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BenefitsScreen(onContinueClick: () -> Unit) {
    val benefits = listOf(
        "📵" to "Reduce screen time",
        "🐕" to "Encourage real-life pet interaction",
        "❤️" to "Build stronger pet-owner relationships",
        "📅" to "Create healthy daily routines",
        "🏆" to "Reward your pet care with achievements"
    )
    Column(Modifier.fillMaxSize().padding(24.dp), Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Text("What does PetQuest help with?", fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(benefits) { (emoji, text) ->
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onContinueClick, Modifier.fillMaxWidth().height(56.dp)) {
            Text("Set Up My Pet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}