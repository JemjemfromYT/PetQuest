package com.example.petquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.petquest.ui.screens.*
import com.example.petquest.ui.theme.PetQuestTheme
import com.example.petquest.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val app = LocalContext.current.applicationContext as PetQuestApplication
                    val vm: PetQuestViewModel = viewModel(
                        factory = PetQuestViewModelFactory(
                            app.petRepository,
                            app.userPreferencesRepository
                        )
                    )

                    // null means DataStore hasn't loaded yet — wait before showing anything
                    val hasOnboarded by vm.hasOnboarded.collectAsState()

                    if (hasOnboarded == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    // startDestination is determined once, after real data is available
                    val startDest = if (hasOnboarded == true) "main" else "welcome"
                    val nav = rememberNavController()

                    NavHost(nav, startDestination = startDest) {
                        composable("welcome") {
                            WelcomeScreen { nav.navigate("benefits") }
                        }
                        composable("benefits") {
                            BenefitsScreen { nav.navigate("add_pet") }
                        }
                        composable("add_pet") {
                            AddPetScreen(
                                onBackClick = { nav.popBackStack() },
                                onSavePet   = { pet ->
                                    vm.addPet(pet)
                                    nav.navigate("main") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            MainScreen(vm, nav)
                        }
                        composable("pet_detail/{petId}") { back ->
                            val id = back.arguments?.getString("petId")?.toIntOrNull()
                                ?: return@composable
                            PetDetailScreen(
                                petId       = id,
                                viewModel   = vm,
                                onBackClick = { nav.popBackStack() },
                                onVerifyClick = { nav.navigate("pet_verify/$id") }
                            )
                        }
                        composable("pet_verify/{petId}") { back ->
                            val id = back.arguments?.getString("petId")?.toIntOrNull()
                                ?: return@composable
                            PetVerificationScreen(
                                petId     = id,
                                viewModel = vm,
                                onDone    = { nav.popBackStack() }
                            )
                        }
                        composable("add_more_pet") {
                            AddPetScreen(
                                onBackClick = { nav.popBackStack() },
                                onSavePet   = { pet ->
                                    vm.addPet(pet)
                                    nav.popBackStack()
                                }
                            )
                        }
                        composable("admin") {
                            AdminScreen(
                                viewModel   = vm,
                                onBackClick = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PetQuestViewModel, outerNav: NavController) {
    var tab by remember { mutableIntStateOf(0) }

    // ── Global level-up dialog shown from any tab ──────────────────────────
    var levelUpEvent by remember { mutableStateOf<LevelUpEvent?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.levelUpEvent.collect { event ->
            levelUpEvent = event
        }
    }
    levelUpEvent?.let { event ->
        LevelUpDialog(event = event, onDismiss = { levelUpEvent = null })
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    "Home"    to Icons.Default.Home,
                    "Tasks"   to Icons.Default.CheckCircle,
                    "Awards"  to Icons.Default.EmojiEvents,
                    "Profile" to Icons.Default.Person
                ).forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick  = { tab = i },
                        icon     = { Icon(icon, label) },
                        label    = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(viewModel, outerNav)
                1 -> TasksScreen(viewModel)
                2 -> AchievementsScreen(viewModel)
                3 -> ProfileScreen(
                    viewModel    = viewModel,
                    onAddPetClick = { outerNav.navigate("add_more_pet") },
                    onAdminClick  = { outerNav.navigate("admin") }
                )
            }
        }
    }
}
