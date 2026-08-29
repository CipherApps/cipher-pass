package dev.cipher.pass

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.cipher.pass.ui.screens.*
import dev.cipher.pass.ui.theme.CipherTheme
import dev.cipher.pass.ui.viewmodel.SettingsViewModel
import android.view.WindowManager

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            val useDynamicColors by settingsViewModel.useDynamicColors.collectAsState(initial = true)
            CipherTheme(
                dynamicColor = useDynamicColors
            ) {
                CipherPass(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun CipherPass(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isAppLockEnabled by settingsViewModel.isAppLockEnabled.collectAsState(initial = null)
    val appPin by settingsViewModel.appPin.collectAsState(initial = null)

    if (isAppLockEnabled == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (isAppLockEnabled == true && !appPin.isNullOrEmpty()) {
        "lock"
    } else {
        "vault"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("lock") {
                LockScreen(
                    onUnlock = {
                        navController.navigate("vault") {
                            popUpTo("lock") { inclusive = true }
                        }
                    }
                )
            }

            composable("vault") {
                VaultScreen(
                    onEntryClick = { entryId ->
                        navController.navigate("entry/$entryId")
                    },
                    onAddEntry = {
                        navController.navigate("entry/new")
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenSecurityReport = {
                        navController.navigate("security_report")
                    }
                )
            }

            composable("security_report") {
                SecurityReportScreen(
                    onBack = { navController.popBackStack() },
                    onEntryClick = { entryId ->
                        navController.navigate("entry/$entryId")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "entry/{entryId}",
                arguments = listOf(
                    navArgument("entryId") { type = NavType.StringType }
                )
            ) {
                EntryDetailScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    onGeneratePassword = { navController.navigate("generator") }
                )
            }

            composable("generator") {
                PasswordGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onUsePassword = { generatedPassword ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("generated_password", generatedPassword)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}