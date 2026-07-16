package com.example.taskboard.presentation.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.taskboard.R
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.presentation.common.DueDateField
import com.example.taskboard.presentation.common.PrioritySelector
import java.time.LocalDate

/**
 * Thin task detail/edit pane. It renders the editable fields from
 * [TaskDetailViewModel.uiState] (seeded by the board host) and forwards edits/save;
 * no business logic lives here. It invokes [onSaved] once the ViewModel reports a
 * successful save and [onBack] when the toolbar back is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailPane(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.save_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskDetailEvent.Saved -> onSaved()
                TaskDetailEvent.SaveFailed -> snackbarHostState.showSnackbar(saveFailedMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = detail
        if (current == null) {
            TaskDetailPlaceholder(Modifier.padding(padding))
        } else {
            TaskDetailForm(
                detail = current,
                onEdit = viewModel::onEdit,
                onSave = viewModel::onSave,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** The empty detail pane shown when no task is selected. */
@Composable
fun TaskDetailPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.detail_placeholder))
    }
}

@Composable
private fun TaskDetailForm(
    detail: TaskDetail,
    onEdit: (String, String, Priority, LocalDate?) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = detail.title,
            onValueChange = { onEdit(it, detail.notes, detail.priority, detail.dueDate) },
            label = { Text(stringResource(R.string.add_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = detail.notes,
            onValueChange = { onEdit(detail.title, it, detail.priority, detail.dueDate) },
            label = { Text(stringResource(R.string.add_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        PrioritySelector(
            selected = detail.priority,
            onSelect = { onEdit(detail.title, detail.notes, it, detail.dueDate) },
        )
        DueDateField(
            dueDate = detail.dueDate,
            onDueDateChange = { onEdit(detail.title, detail.notes, detail.priority, it) },
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.detail_save))
        }
    }
}
