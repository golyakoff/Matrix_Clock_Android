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
        // Takes what the switch leaves: a long title wraps instead of pushing the switch off the card.
        Row (
            modifier = Modifier.weight(1f, fill = false),
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
            Spacer(Modifier.width(8.dp))
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
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
