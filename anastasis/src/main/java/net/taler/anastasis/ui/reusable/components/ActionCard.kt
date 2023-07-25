package net.taler.anastasis.ui.reusable.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    headline: String,
    subhead: String? = null,
    body: String? = null,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(headline, style = MaterialTheme.typography.titleLarge)
            }
            subhead?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
@Preview
fun ActionCardPreview() {
    ActionCard(
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(Icons.Default.KeyboardArrowUp, "arrowUp") },
        headline = "Headline",
        subhead = "Subhead",
        body = "Supporting text",
    ) {}
}