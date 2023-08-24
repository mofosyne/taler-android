package net.taler.anastasis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import net.taler.anastasis.ui.backup.BackupFinishedScreen
import net.taler.anastasis.ui.backup.EditSecretScreen
import net.taler.anastasis.ui.backup.ReviewPoliciesScreen
import net.taler.anastasis.ui.backup.SelectAuthMethodsScreen
import net.taler.anastasis.ui.common.LoadingScreen
import net.taler.anastasis.ui.common.SelectContinentScreen
import net.taler.anastasis.ui.common.SelectCountryScreen
import net.taler.anastasis.ui.common.SelectUserAttributesScreen
import net.taler.anastasis.ui.dialogs.ErrorDialog
import net.taler.anastasis.ui.home.HomeScreen
import net.taler.anastasis.ui.recovery.RecoveryFinishedScreen
import net.taler.anastasis.ui.recovery.SelectChallengeScreen
import net.taler.anastasis.ui.recovery.SelectSecretScreen
import net.taler.anastasis.ui.recovery.SolveChallengeScreen
import net.taler.anastasis.ui.theme.AnastasisTheme
import net.taler.anastasis.viewmodels.ReducerViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val viewModel: ReducerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.goBack()) finish()
            }
        })

        setContent {
            AnastasisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainNavHost()
                }
            }
        }
    }
}

@Composable
fun MainNavHost(
    viewModel: ReducerViewModel = hiltViewModel(),
) {
    val error by viewModel.reducerError.collectAsState()
    error?.let {
        ErrorDialog(error = it) {
            viewModel.cleanError()
        }
    }

    val navRoute by viewModel.navRoute.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    if (tasks.isForegroundLoading) {
        LoadingScreen()
    } else {
        when (navRoute) {
            Routes.Home.route -> {
                HomeScreen()
            }

            Routes.SelectContinent.route -> {
                SelectContinentScreen()
            }

            Routes.SelectCountry.route -> {
                SelectCountryScreen()
            }

            Routes.SelectUserAttributes.route -> {
                SelectUserAttributesScreen()
            }

            Routes.SelectAuthMethods.route -> {
                SelectAuthMethodsScreen()
            }

            Routes.ReviewPolicies.route -> {
                ReviewPoliciesScreen()
            }

            Routes.EditSecret.route -> {
                EditSecretScreen()
            }

            Routes.BackupFinished.route -> {
                BackupFinishedScreen()
            }

            Routes.SelectSecret.route -> {
                SelectSecretScreen()
            }

            Routes.SelectChallenge.route -> {
                SelectChallengeScreen()
            }

            Routes.SolveChallenge.route -> {
                SolveChallengeScreen()
            }

            Routes.RecoveryFinished.route -> {
                RecoveryFinishedScreen()
            }

            Routes.RestoreInit.route -> {
                Text("This is the restore session screen!")
            }
        }
    }
}