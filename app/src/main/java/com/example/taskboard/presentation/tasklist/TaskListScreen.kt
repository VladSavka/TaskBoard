@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.taskboard.presentation.tasklist

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.taskboard.R
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.ThemeMode
import com.example.taskboard.presentation.common.DueDateField
import com.example.taskboard.presentation.common.PrioritySelector
import com.example.taskboard.presentation.theme.ThemeMenu
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Thin task list pane: it renders [TaskListUiState] and forwards user intents. No
 * business logic here. A top app bar carries the sort and theme controls, a search
 * bar filters the list, and each task is a [Card]; the add-task FAB shows from
 * every settled state.
 */
@Composable
fun TaskListPane(
    state: TaskListUiState,
    themeSelected: ThemeMode,
    themeOptions: List<ThemeMode>,
    onThemeSelect: (ThemeMode) -> Unit,
    onSearch: (String) -> Unit,
    onSort: (TaskSort) -> Unit,
    onToggle: (String) -> Unit,
    onAdd: (String, String, Priority, LocalDate?) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUndoDelete: () -> Unit,
    events: Flow<TaskBoardEvent>,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    TaskBoardEventEffect(events, snackbarHostState, onUndoDelete)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    SortMenu(selected = state.sort, onSort = onSort)
                    ThemeMenu(selected = themeSelected, options = themeOptions, onSelect = onThemeSelect)
                },
            )
        },
        floatingActionButton = {
            if (state.isAddActionAvailable) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.cd_add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(query = state.query, onSearch = onSearch)
            TaskListContent(
                list = state.list,
                onToggle = onToggle,
                onOpen = onOpen,
                onDelete = onDelete,
                onRetry = onRetry,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onAdd = { title, notes, priority, dueDate ->
                onAdd(title, notes, priority, dueDate)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

/** Collects one-shot [events] and surfaces them as snackbars (a failed save; an undoable delete). */
@Composable
private fun TaskBoardEventEffect(
    events: Flow<TaskBoardEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoDelete: () -> Unit,
) {
    val saveFailedMessage = stringResource(R.string.save_failed)
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                // A brief error so a failed add/toggle/delete isn't silent.
                TaskBoardEvent.SaveFailed ->
                    snackbarHostState.showSnackbar(saveFailedMessage, duration = SnackbarDuration.Short)
                // Offer to undo a delete for as long as the snackbar shows.
                TaskBoardEvent.TaskDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndoDelete()
                }
            }
        }
    }
}

@Composable
private fun TaskListContent(
    list: ListState,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (list) {
        ListState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ListState.Empty -> ListMessage(stringResource(R.string.tasks_empty), modifier)
        is ListState.Error -> ErrorState(list.canRetry, onRetry, modifier)
        is ListState.Content ->
            if (list.items.isEmpty()) {
                // Top-aligned so the "no results" note sits just under the search bar,
                // not centred where the keyboard would cover it.
                ListMessage(stringResource(R.string.tasks_no_results), modifier, Alignment.TopCenter)
            } else {
                // Resolve the theme colour here (not inside the lazy item): reading it in
                // this composable makes the list re-run its items when the theme flips —
                // LazyColumn doesn't propagate a colour-scheme change to already-composed
                // keyed items on its own, so the cards would otherwise stay light.
                val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list.items, key = { it.task.id }) { item ->
                        TaskCard(
                            item = item,
                            subtitleColor = subtitleColor,
                            onToggle = { onToggle(item.task.id) },
                            onClick = { onOpen(item.task.id) },
                            onDelete = { onDelete(item.task.id) },
                        )
                    }
                }
            }
    }
}

@Composable
private fun SearchField(query: String, onSearch: (String) -> Unit) {
    SearchBar(
        query = query,
        onQueryChange = onSearch,
        onSearch = {},
        active = false,
        onActiveChange = {},
        placeholder = { Text(stringResource(R.string.search_label)) },
        leadingIcon = { Icon(Icons.Filled.Search, stringResource(R.string.cd_search)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        content = {},
    )
}

private val sortOptions = listOf(
    TaskSort.NONE to R.string.sort_default,
    TaskSort.PRIORITY to R.string.sort_priority,
    TaskSort.COMPLETION to R.string.sort_completion,
)

@Composable
private fun SortMenu(selected: TaskSort, onSort: (TaskSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Sort, stringResource(R.string.cd_sort))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Text(
            stringResource(R.string.sort_menu_title),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        sortOptions.forEach { (sort, labelRes) ->
            SortMenuItem(labelRes = labelRes, selected = selected == sort) {
                onSort(sort)
                expanded = false
            }
        }
    }
}

@Composable
private fun SortMenuItem(@StringRes labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(labelRes)) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}

@Composable
private fun TaskCard(
    item: TaskListItem,
    subtitleColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = item.task.done,
                onCheckedChange = { onToggle() },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Column(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                Text(item.task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.label_priority, item.task.priority.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                )
                item.dueLabel?.let { due ->
                    Text(
                        stringResource(R.string.label_due_date, dueText(due)),
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.Top)) {
                Icon(Icons.Filled.Close, stringResource(R.string.cd_delete))
            }
        }
    }
}

/** Renders a [DueLabel] to its localised text. */
@Composable
private fun dueText(label: DueLabel): String = when (label) {
    DueLabel.Today -> stringResource(R.string.due_today)
    DueLabel.Tomorrow -> stringResource(R.string.due_tomorrow)
    DueLabel.Yesterday -> stringResource(R.string.due_yesterday)
    is DueLabel.InDays -> stringResource(R.string.due_in_days, label.days)
    is DueLabel.DaysAgo -> stringResource(R.string.due_days_ago, label.days)
}

@Composable
private fun AddTaskDialog(onAdd: (String, String, Priority, LocalDate?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.add_title_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.add_notes_label)) },
                )
                PrioritySelector(selected = priority, onSelect = { priority = it })
                DueDateField(dueDate = dueDate, onDueDateChange = { dueDate = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, notes, priority, dueDate) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.dialog_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun ListMessage(
    message: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    Box(modifier, contentAlignment = alignment) {
        Text(message, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ErrorState(canRetry: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.tasks_error))
        if (canRetry) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}
