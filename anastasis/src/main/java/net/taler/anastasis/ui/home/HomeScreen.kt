package net.taler.anastasis.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import net.taler.anastasis.R
import net.taler.anastasis.Routes
import net.taler.anastasis.ui.reusable.components.ActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.home_title))
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Backup
            ActionCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                icon = { Icon(Icons.Outlined.Upload, null) },
                headline = stringResource(R.string.backup_secret),
                onClick = {
                    navController.navigate(Routes.BackupContinent.route)
                },
            )

            // Recovery
            ActionCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                icon = { Icon(Icons.Outlined.Download, null) },
                headline = stringResource(R.string.recover_secret),
                onClick = {
                    navController.navigate(Routes.RecoveryCountry.route)
                },
            )

            // Restore session
            ActionCard(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                icon = { Icon(Icons.Outlined.Restore, null) },
                headline = stringResource(R.string.restore_session),
                onClick = {
                    navController.navigate(Routes.RestoreInit.route)
                },
            )
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}