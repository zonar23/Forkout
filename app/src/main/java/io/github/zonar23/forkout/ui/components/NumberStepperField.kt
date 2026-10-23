package io.github.zonar23.forkout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

/**
 * Text field for a number with large − and + buttons on both sides.
 * The caller owns parsing; [onDecrease] and [onIncrease] adjust the value by its step.
 */
@Composable
fun NumberStepperField(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        StepButton(R.drawable.ic_remove, stringResource(R.string.stepper_decrease), onDecrease)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            readOnly = readOnly,
            isError = isError,
            textStyle = NumberTextStyles.medium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
        )
        StepButton(R.drawable.ic_add, stringResource(R.string.stepper_increase), onIncrease)
    }
}

@Composable
private fun StepButton(iconRes: Int, description: String, onClick: () -> Unit) {
    // An outline stays visible on screens and inside dialogs, which use different surfaces.
    OutlinedIconButton(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(56.dp),
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = description)
    }
}
