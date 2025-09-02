
package vsh.kttrs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ControlMode {
    Buttons,
    Swipes,
    Both
}

@Composable
fun SettingsScreen(
    currentControlMode: ControlMode,
    onControlModeChange: (ControlMode) -> Unit,
    showGhostPiece: Boolean,
    onShowGhostPieceChange: (Boolean) -> Unit
) {
    val selectedOption = remember { mutableStateOf(currentControlMode) }
    val ghostPieceChecked = remember { mutableStateOf(showGhostPiece) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Control Mode", style = MaterialTheme.typography.headlineSmall)
        ControlMode.entries.forEach { controlMode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (controlMode == selectedOption.value),
                    onClick = {
                        selectedOption.value = controlMode
                        onControlModeChange(controlMode)
                    }
                )
                Text(
                    text = controlMode.name,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show Ghost Piece",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = ghostPieceChecked.value,
                onCheckedChange = {
                    ghostPieceChecked.value = it
                    onShowGhostPieceChange(it)
                }
            )
        }
    }
}
