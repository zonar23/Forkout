package io.github.zonar23.forkout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

private const val MAX_LENGTH = 6

/**
 * Large on-screen number pad for entering a value with sweaty fingers. The first key
 * replaces the shown value, like on a calculator.
 */
@Composable
fun NumberPadDialog(
    title: String,
    initialValue: String,
    unit: String,
    allowDecimal: Boolean,
    isValid: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    var replaceOnNextKey by remember { mutableStateOf(true) }

    fun type(key: String) {
        val base = if (replaceOnNextKey) "" else text
        replaceOnNextKey = false
        if (key == "," && (!allowDecimal || base.contains(','))) return
        if (base.length >= MAX_LENGTH) return
        text = if (base == "0" && key != ",") key else base + key
    }

    fun backspace() {
        replaceOnNextKey = false
        text = text.dropLast(1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = text.ifEmpty { "0" },
                        style = NumberTextStyles.large,
                        color = if (replaceOnNextKey) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                    )
                }
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    KeyRow { row.forEach { key -> Key(label = key, onClick = { type(key) }) } }
                }
                KeyRow {
                    if (allowDecimal) {
                        Key(label = ",", onClick = { type(",") })
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Key(label = "0", onClick = { type("0") })
                    FilledTonalButton(
                        onClick = ::backspace,
                        colors = keyColors(),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_backspace),
                            contentDescription = stringResource(R.string.number_pad_delete),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = isValid(text)) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowScope.Key(
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        colors = keyColors(),
        modifier = Modifier
            .weight(1f)
            .height(64.dp),
    ) {
        Text(label, style = NumberTextStyles.medium)
    }
}

/** Dark keys; the default tonal color is the accent in this theme and the dialog surface is lighter. */
@Composable
private fun keyColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.background,
    contentColor = MaterialTheme.colorScheme.onSurface,
)
