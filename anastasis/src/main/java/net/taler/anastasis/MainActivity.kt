package net.taler.anastasis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.models.CountryInfo
import net.taler.anastasis.models.UserAttributeSpec
import net.taler.anastasis.ui.backup.BackupContinentScreen
import net.taler.anastasis.ui.backup.BackupCountryScreen
import net.taler.anastasis.ui.backup.BackupUserAttributesScreen
import net.taler.anastasis.ui.home.HomeScreen
import net.taler.anastasis.ui.theme.AnastasisTheme

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
fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Routes.BackupContinent.route) {
            BackupContinentScreen(
                navController = navController,
                continents = listOf(
                    ContinentInfo("Europe"),
                    ContinentInfo("India"),
                    ContinentInfo("Asia"),
                    ContinentInfo("North America")
                ),
                onSelectContinent = {},
            )
        }
        composable(Routes.BackupCountry.route) {
            BackupCountryScreen(
                navController = navController,
                countries = listOf(
                    CountryInfo("ch", "Switzerland", "Europe"),
                    CountryInfo("de", "Germany", "Europe"),
                ),
                onSelectCountry = {},
            )
        }
        composable(Routes.BackupUserAttributes.route) {
            BackupUserAttributesScreen(
                navController = navController,
                userAttributes = listOf(
                    UserAttributeSpec(
                        type = "string",
                        name = "full_name",
                        label = "Full name",
                        widget = "anastasis_gtk_ia_full_name",
                        uuid = "9e8f463f-575f-42cb-85f3-759559997331",
                        validationLogic = null,
                        validationRegex = null,
                    ),
                    UserAttributeSpec(
                        type = "date",
                        name = "birthdate",
                        label = "Birthdate",
                        uuid = "83d655c7-bdb6-484d-904e-80c1058c8854",
                        widget = "anastasis_gtk_ia_birthdate",
                        validationLogic = null,
                        validationRegex = null,
                    ),
                ),
            )
        }
        composable(Routes.RecoveryCountry.route) {
            Text("This is the recover screen!")
        }
        composable(Routes.RestoreInit.route) {
            Text("This is the restore session screen!")
        }
    }
}
