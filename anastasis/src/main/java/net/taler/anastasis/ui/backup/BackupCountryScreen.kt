package net.taler.anastasis.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import net.taler.anastasis.R
import net.taler.anastasis.Routes
import net.taler.anastasis.models.CountryInfo
import net.taler.anastasis.ui.reusable.components.Picker
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing

@Composable
fun BackupCountryScreen(
    navController: NavController,
    countries: List<CountryInfo>,
    onSelectCountry: (country: CountryInfo) -> Unit,
) {
    WizardPage(
        title = stringResource(R.string.backup_country_title),
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate(Routes.Home.route)
            }) {
                Icon(Icons.Default.ArrowBack, "back")
            }
        },
        onPrevClicked = {
            navController.navigate(Routes.BackupContinent.route)
        },
        onNextClicked = {
            navController.navigate(Routes.BackupUserAttributes.route)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.Top,
        ) {
            Picker(
                label = stringResource(R.string.country),
                options = countries.map { it.name }.toSet(),
                onOptionChanged = { option ->
                    countries.find { it.name == option }?.let { country ->
                        onSelectCountry(country)
                    }
                },
            )
        }
    }
}

@Composable
@Preview
fun BackupCountryScreenPreview() {
    val navController = rememberNavController()
    BackupCountryScreen(
        navController = navController,
        countries = listOf(
            CountryInfo("ch", "Switzerland", "Europe"),
            CountryInfo("de", "Germany", "Europe"),
        ),
        onSelectCountry = {},
    )
}