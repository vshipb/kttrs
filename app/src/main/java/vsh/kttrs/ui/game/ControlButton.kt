package vsh.kttrs.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ControlButton(
    onClick: () -> Unit,
    @DrawableRes drawableId: Int,
    contentDescription: String,
    repeatable: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        var job: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    if (repeatable) {
                        job = launch {
                            onClick()
                            delay(200) // DAS delay
                            while (true) {
                                onClick()
                                delay(50) // Repeat rate
                            }
                        }
                    } else {
                        onClick()
                    }
                }
                is PressInteraction.Release -> {
                    job?.cancel()
                }
                is PressInteraction.Cancel -> {
                    job?.cancel()
                }
            }
        }
    }

    OutlinedButton(
        onClick = { /* Handled by interaction source */ },
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, Color.White)
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = contentDescription,
            modifier = Modifier.size(48.dp)
        )
    }
}
