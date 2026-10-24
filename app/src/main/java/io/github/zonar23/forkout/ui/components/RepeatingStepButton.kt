package io.github.zonar23.forkout.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val REPEAT_START_DELAY_MS = 400L
private const val REPEAT_FIRST_INTERVAL_MS = 150L
private const val REPEAT_MIN_INTERVAL_MS = 40L

/**
 * Round −/+ button that steps once on tap and keeps stepping, faster and faster,
 * while it is held down.
 */
@Composable
fun RepeatingStepButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val currentOnStep by rememberUpdatedState(onStep)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Set once holding has started repeating, so the click on release does not add a step.
    var repeated by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (!isPressed) return@LaunchedEffect
        repeated = false
        delay(REPEAT_START_DELAY_MS)
        repeated = true
        var interval = REPEAT_FIRST_INTERVAL_MS
        while (isActive) {
            currentOnStep()
            delay(interval)
            interval = (interval * 0.85).toLong().coerceAtLeast(REPEAT_MIN_INTERVAL_MS)
        }
    }

    OutlinedIconButton(
        onClick = { if (repeated) repeated = false else currentOnStep() },
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.size(size),
    ) {
        Icon(painterResource(iconRes), contentDescription = contentDescription)
    }
}
