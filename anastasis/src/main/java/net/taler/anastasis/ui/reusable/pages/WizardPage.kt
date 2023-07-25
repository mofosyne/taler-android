package net.taler.anastasis.ui.reusable.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardPage(
    modifier: Modifier = Modifier,
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    showNext: Boolean = true,
    showPrev: Boolean = true,
    onNextClicked: () -> Unit = {},
    onPrevClicked: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = navigationIcon,
            )
        },
    ) {
        Column(
            modifier = modifier.padding(it),
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                content()
            }
            Divider()
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showPrev) {
                    TextButton(
                        onClick = onPrevClicked,
                    ) {
                        Icon(
                            Icons.Default.NavigateBefore,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Previous")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (showNext) {
                    Button(
                        onClick = onNextClicked,
                    ) {
                        Text("Next")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Icon(
                            Icons.Default.NavigateNext,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun WizardPagePreview() {
    WizardPage(
        title = "Title",
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, null)
            }
        },
    ) {
        Box (
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("This is a wizard page")
        }
    }
}