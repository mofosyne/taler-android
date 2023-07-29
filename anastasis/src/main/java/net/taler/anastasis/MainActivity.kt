package net.taler.anastasis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import net.taler.anastasis.ui.backup.SelectAuthMethodsScreen
import net.taler.anastasis.ui.common.SelectContinentScreen
import net.taler.anastasis.ui.common.SelectCountryScreen
import net.taler.anastasis.ui.common.SelectUserAttributesScreen
import net.taler.anastasis.ui.home.HomeScreen
import net.taler.anastasis.ui.theme.AnastasisTheme
import net.taler.anastasis.viewmodels.ReducerViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val navRoute by viewModel.navRoute.collectAsState()
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
        Routes.RestoreInit.route -> {
            Text("This is the restore session screen!")
        }
    }
}
