/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
 *
 * GNU Taler is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * GNU Taler is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Taler; see the file COPYING.  If not, see <http://www.gnu.org/licenses/>
 */

package net.taler.anastasis.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import net.taler.anastasis.R
import net.taler.anastasis.Routes
import net.taler.anastasis.models.UserAttributeSpec
import net.taler.anastasis.ui.reusable.components.DatePickerField
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupUserAttributesScreen(
    navController: NavController,
    userAttributes: List<UserAttributeSpec>,
) {
    val values = remember { mutableStateMapOf<String, String>() }

    WizardPage(
        title = stringResource(R.string.backup_user_attributes_title),
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate(Routes.Home.route)
            }) {
                Icon(Icons.Default.ArrowBack, "back")
            }
        },
        onPrevClicked = {
            navController.navigate(Routes.BackupCountry.route)
        },
        onNextClicked = {},
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.Top,
        ) {
            items(items = userAttributes) { attr ->
                when (attr.type) {
                    "string" -> OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = values[attr.uuid] ?: "",
                        onValueChange = { values[attr.uuid] = it },
                        label = { Text(attr.label) },
                    )
                    "date" -> @Composable {
                        val cal = Calendar.getInstance()
                        var yy by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
                        var mm by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
                        var dd by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
                        DatePickerField(
                            modifier = Modifier.fillMaxWidth(),
                            label = attr.label,
                            yy = yy,
                            mm = mm,
                            dd = dd,
                            onDateSelected = { y, m, d ->
                                yy = y
                                mm = m
                                dd = d
                            },
                        )
                    }
                }
                Spacer(Modifier.height(LocalSpacing.current.small))
            }
        }
    }
}

@Preview
@Composable
fun BackupUserAttributesScreenPreview() {
    val navController = rememberNavController()
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