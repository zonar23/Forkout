package io.github.zonar23.forkout.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.ui.components.ConfirmDeleteDialog
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.MenuAction
import io.github.zonar23.forkout.ui.components.OverflowMenu
import io.github.zonar23.forkout.ui.components.TextInputDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlanDetailScreen(
    onBack: () -> Unit,
    onEditPlan: (Long) -> Unit,
    onOpenDay: (Long) -> Unit,
    onOpenPlan: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val plan = uiState.plan

    LaunchedEffect(uiState.isGone) {
        if (uiState.isGone) onBack()
    }
    LaunchedEffect(uiState.copiedPlanId) {
        uiState.copiedPlanId?.let {
            viewModel.onCopiedPlanOpened()
            onOpenPlan(it)
        }
    }

    val copyPlanName = plan?.let { stringResource(R.string.copy_name_suffix, it.name) }
    val nextDayName = stringResource(R.string.plan_day_default_name, nextDayLabel(uiState.days.size))
    val resources = LocalResources.current

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = plan?.name.orEmpty(),
            onBack = onBack,
            actions = {
                if (plan != null) {
                    IconButton(onClick = { onEditPlan(plan.id) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.plan_detail_edit),
                        )
                    }
                    OverflowMenu(
                        actions = listOf(
                            MenuAction(stringResource(R.string.plan_detail_copy), R.drawable.ic_content_copy) {
                                copyPlanName?.let(viewModel::copyPlan)
                            },
                        ),
                    )
                }
            },
        )

        if (plan == null) return@Column

        Box(modifier = Modifier.fillMaxSize()) {
            DayList(
                days = uiState.days,
                header = {
                    PlanHeader(summary = plan.summary(), note = plan.note)
                },
                onOpenDay = onOpenDay,
                onReorder = viewModel::onReorder,
                onRename = viewModel::startRename,
                onCopy = { day -> viewModel.copyDay(day, resources.getString(R.string.copy_name_suffix, day.name)) },
                onDelete = viewModel::startDelete,
            )
            FloatingActionButton(
                onClick = { viewModel.addDay(nextDayName) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.plan_day_add),
                )
            }
        }
    }

    uiState.renaming?.let {
        TextInputDialog(
            title = stringResource(R.string.plan_day_rename_title),
            value = uiState.renameText,
            onValueChange = viewModel::onRenameTextChange,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRename,
        )
    }
    uiState.deleting?.let {
        ConfirmDeleteDialog(
            title = stringResource(R.string.plan_day_delete_dialog_title),
            text = stringResource(R.string.plan_day_delete_dialog_text),
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun PlanHeader(summary: String, note: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (note.isNotBlank()) {
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.plan_days_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/**
 * Days sortable by dragging the handle. The list keeps a local copy during the gesture and
 * persists the order when the drag stops.
 */
@Composable
private fun DayList(
    days: List<PlanDayItem>,
    header: @Composable () -> Unit,
    onOpenDay: (Long) -> Unit,
    onReorder: (List<PlanDayEntity>) -> Unit,
    onRename: (PlanDayEntity) -> Unit,
    onCopy: (PlanDayEntity) -> Unit,
    onDelete: (PlanDayEntity) -> Unit,
) {
    var items by remember(days) { mutableStateOf(days) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = items.indexOfFirst { it.day.id == from.key }
        val toIndex = items.indexOfFirst { it.day.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            items = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 88.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") { header() }
        if (items.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.plan_days_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(items, key = { it.day.id }) { item ->
            ReorderableItem(reorderState, key = item.day.id) { isDragging ->
                ListItem(
                    leadingContent = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(onDragStopped = { onReorder(items.map { it.day }) }),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_indicator),
                                contentDescription = stringResource(R.string.drag_to_reorder),
                            )
                        }
                    },
                    headlineContent = { Text(item.day.name) },
                    supportingContent = {
                        Text(pluralStringResource(R.plurals.plan_day_exercise_count, item.exerciseCount, item.exerciseCount))
                    },
                    trailingContent = {
                        OverflowMenu(
                            actions = listOf(
                                MenuAction(stringResource(R.string.plan_day_rename), R.drawable.ic_edit) { onRename(item.day) },
                                MenuAction(stringResource(R.string.plan_day_copy), R.drawable.ic_content_copy) { onCopy(item.day) },
                                MenuAction(stringResource(R.string.plan_day_delete), R.drawable.ic_delete, isDestructive = true) {
                                    onDelete(item.day)
                                },
                            ),
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                    ),
                    modifier = Modifier.clickable { onOpenDay(item.day.id) },
                )
            }
        }
    }
}
