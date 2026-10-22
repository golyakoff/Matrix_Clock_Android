package net.agolyakov.tetrisclockble.ui.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Building blocks shared by the cards of both tabs

@Composable
fun HeaderCard(
    friendlyName: String,
    macAddress: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Matrix Clock",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = friendlyName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = macAddress,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun CardTitle(
    imageVector: ImageVector,
    text: String,
    showSwitch: Boolean = false,
    isOn: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    showBottomSpacing: Boolean = true
) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {
        Row (
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                text = text
            )
        }

        if(showSwitch)
        {
            Switch(
                checked = isOn,
                onCheckedChange = onCheckedChange
            )
        }
    }

    if (showBottomSpacing) {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CardButtonWide(
    imageVector: ImageVector,
    text: String,
    onClickAction: () -> Unit,
    isPrimary: Boolean = false
) {
    val containerColor =
        if (isPrimary)  MaterialTheme.colorScheme.primary
        else  MaterialTheme.colorScheme.surfaceVariant

    val contentColor =
        if (isPrimary)  MaterialTheme.colorScheme.onPrimary
        else  MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClickAction,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
