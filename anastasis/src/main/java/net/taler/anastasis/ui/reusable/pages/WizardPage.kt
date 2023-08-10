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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.anastasis.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardPage(
    modifier: Modifier = Modifier,
    title: String,
    enableNext: Boolean = true,
    enablePrev: Boolean = true,
    showNext: Boolean = true,
    showPrev: Boolean = true,
    onBackClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {},
    onPrevClicked: () -> Unit = {},
    isLoading: Boolean = false,
    content: @Composable (nestedScrollConnection: NestedScrollConnection) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(
            modifier = modifier.padding(it),
        ) {
            if(isLoading)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                content(scrollBehavior.nestedScrollConnection)
            }
            Divider()
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showPrev) {
                    TextButton(
                        enabled = enablePrev,
                        onClick = onPrevClicked,
                    ) {
                        Icon(
                            Icons.Default.NavigateBefore,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.previous))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (showNext) {
                    Button(
                        enabled = enableNext,
                        onClick = onNextClicked,
                    ) {
                        Text(stringResource(R.string.next))
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
        isLoading = true,
    ) {
        Box (
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("This is a wizard page")
        }
    }
}