// ============================================================
// FILE: app/src/main/java/com/example/petquest/MainActivity.kt
// FULL REPLACEMENT
// Changes vs previous version:
//   - onResume  → SoundManager.resumeMusic()
//   - onPause   → SoundManager.pauseMusic()
//   - onDestroy → SoundManager.release()
//   - MainScreen: LaunchedEffect starts bg music when user hits main screen
// ============================================================

package com.example.petquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

                    val hasOnboarded by vm.hasOnboarded.collectAsState()

                    if (hasOnboarded == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    val startDest = remember { if (hasOnboarded == true) "main" else "welcome" }
                    val nav       = rememberNavController()

                    val pendingVerifyId by vm.pendingVerificationPetId.collectAsState()

                    NavHost(nav, startDestination = startDest) {
                        composable("welcome") {
                            WelcomeScreen { nav.navigate("benefits") }
                        }
                        composable("benefits") {
                            BenefitsScreen { nav.navigate("add_pet") }
                        }
                        composable("add_pet") {
                            LaunchedEffect(pendingVerifyId) {
                                val id = pendingVerifyId ?: return@LaunchedEffect
                                vm.clearPendingVerificationPetId()
                                nav.navigate("pet_verify/$id") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                            AddPetScreen(
                                onBackClick = { nav.navigateUp() },
                                onSavePet   = { pet -> vm.addPet(pet) }
                            )
                        }
                        composable("main") {
                            MainScreen(vm, nav)
                        }
                        composable("pet_detail/{petId}") { back ->
                            val id = back.arguments?.getString("petId")?.toIntOrNull()
                                ?: return@composable
                            PetDetailScreen(
                                petId         = id,
                                viewModel     = vm,
                                onBackClick   = { nav.navigateUp() },
                                onVerifyClick = { nav.navigate("pet_verify/$id") }
                            )
                        }
                        composable("pet_verify/{petId}") { back ->
                            val id = back.arguments?.getString("petId")?.toIntOrNull()
                                ?: return@composable
                            PetVerificationScreen(
                                petId     = id,
                                viewModel = vm,
                                onDone    = {
                                    vm.clearPendingVerificationPetId()
                                    nav.navigate("main") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("add_more_pet") {
                            LaunchedEffect(pendingVerifyId) {
                                val id = pendingVerifyId ?: return@LaunchedEffect
                                vm.clearPendingVerificationPetId()
                                nav.navigate("pet_verify/$id") {
                                    popUpTo("main") { inclusive = false }
                                }
                            }
                            AddPetScreen(
                                onBackClick = { nav.navigateUp() },
                                onSavePet   = { pet -> vm.addPet(pet) }
                            )
                        }
                        composable("admin") {
                            AdminScreen(
                                viewModel   = vm,
                                onBackClick = { nav.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Pause music when app goes to background, resume when it returns
    override fun onResume() {
        super.onResume()
        SoundManager.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

// ── Bottom nav items ──────────────────────────────────────────────────────────
private data class NavItem(
    val label         : String,
    val selectedIcon  : androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val NAV_ITEMS = listOf(
    NavItem("Home",       Icons.Filled.Home,        Icons.Outlined.Home),
    NavItem("Tasks",      Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    NavItem("Collection", Icons.Filled.Pets,        Icons.Outlined.Pets),
    NavItem("Awards",     Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    NavItem("Events",     Icons.Filled.Celebration, Icons.Outlined.Celebration),
    NavItem("Profile",    Icons.Filled.Person,      Icons.Outlined.Person)
)

@Composable
fun MainScreen(viewModel: PetQuestViewModel, outerNav: NavController) {
    var tab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    // Start background music the first time MainScreen is displayed
    LaunchedEffect(Unit) {
        SoundManager.startMusic(context)
    }

    // Level-up dialog
    var levelUpEvent by remember { mutableStateOf<LevelUpEvent?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.levelUpEvent.collect { event ->
            SoundManager.playLevelUp()        // 🔊 level-up sound
            levelUpEvent = event
        }
    }
    levelUpEvent?.let { event ->
        LevelUpDialog(event = event, onDismiss = { levelUpEvent = null })
    }

    // Tutorial state
    val hasSeenTutorial by viewModel.hasSeenTutorial.collectAsState()
    val showTutorial = !hasSeenTutorial

    Scaffold(
        bottomBar = {
            NavigationBar {
                NAV_ITEMS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick  = {
                            if (!showTutorial) {
                                SoundManager.playTap()    // 🔊 tap sound on nav
                                tab = index
                            }
                        },
                        icon = {
                            Icon(
                                imageVector        = if (tab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 10.sp
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(viewModel, outerNav)
                1 -> TasksScreen(
                    viewModel           = viewModel,
                    onVerifyPet         = { petId -> outerNav.navigate("pet_verify/$petId") },
                    onNavigateToProfile = { tab = 5 }
                )
                2 -> EncyclopediaScreen(viewModel)
                3 -> AwardsScreen(
                    viewModel         = viewModel,
                    onNavigateToTasks = { tab = 1 }
                )
                4 -> EventsScreen(viewModel = viewModel)
                5 -> ProfileScreen(
                    viewModel              = viewModel,
                    onAddPetClick          = { outerNav.navigate("add_more_pet") },
                    onAdminClick           = { outerNav.navigate("admin") },
                    onNavigateToCollection = { tab = 2 }
                )
            }

            if (showTutorial) {
                TutorialOverlay(
                    onDismiss      = { viewModel.markTutorialSeen() },
                    onTabHighlight = { highlightedTab -> tab = highlightedTab }
                )
            }
        }
    }
}
