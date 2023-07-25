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
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.ui.reusable.components.Picker
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing

@Composable
fun BackupContinentScreen(
    navController: NavController,
    continents: List<ContinentInfo>,
    onSelectContinent: (continent: ContinentInfo) -> Unit,
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
        showPrev = false,
        onNextClicked = {
            navController.navigate(Routes.BackupCountry.route)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.Top,
        ) {
            Picker(
                label = stringResource(R.string.continent),
                options = continents.map { it.name }.toSet(),
                onOptionChanged = { option ->
                    continents.find { it.name == option }?.let { continent ->
                        onSelectContinent(continent)
                    }
                },
            )
        }
    }
}

@Composable
@Preview
fun BackupContinentScreenPreview() {
    val navController = rememberNavController()
    BackupContinentScreen(
        navController = navController,
        continents = listOf(
            ContinentInfo("Europe"),
            ContinentInfo("India"),
            ContinentInfo("Asia"),
            ContinentInfo("North America")),
        onSelectContinent = {},
    )
}