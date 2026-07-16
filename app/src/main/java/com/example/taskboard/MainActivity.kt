package com.example.taskboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskboard.presentation.taskdetail.TaskDetailPane
import com.example.taskboard.presentation.taskdetail.TaskDetailPlaceholder
import com.example.taskboard.presentation.taskdetail.TaskDetailViewModel
import com.example.taskboard.presentation.tasklist.TaskListPane
import com.example.taskboard.presentation.tasklist.TaskListViewModel
import com.example.taskboard.presentation.theme.TaskBoardTheme
import com.example.taskboard.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskBoardApp()
        }
    }
}

@Composable
fun TaskBoardApp(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val mode by themeViewModel.selectedMode.collectAsState()
    TaskBoardTheme(darkTheme = mode.isDark(isSystemInDarkTheme())) {
        TaskBoardScaffold(themeViewModel = themeViewModel)
    }
}

/**
 * Adaptive host wiring the task list and detail panes into a Material 3
 * [ListDetailPaneScaffold]: they sit side by side on wide screens and become
 * list→detail navigation on compact ones. Opening a task is navigation only — the
 * navigator holds the selected id (surviving recreation); the detail pane builds a
 * key-scoped ViewModel for it, and shows a placeholder when nothing is selected.
 * Saving reloads the list through the shared repository flow; the system back and the
 * detail toolbar's back return to the list. Navigation is suspending, so it runs on a
 * remembered [rememberCoroutineScope].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TaskBoardScaffold(
    themeViewModel: ThemeViewModel,
    listViewModel: TaskListViewModel = hiltViewModel(),
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val state by listViewModel.uiState.collectAsState()
    val themeMode by themeViewModel.selectedMode.collectAsState()

    val openTaskId = navigator.currentDestination
        ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
        ?.contentKey as? String

    // The task the detail pane renders. It follows [openTaskId] but is NOT cleared on
    // back: when we navigate away, openTaskId drops to null while the pane is still
    // animating out, so keeping the last id here shows that task through the animation
    // instead of flashing the "select a task" placeholder.
    var detailTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(openTaskId) { openTaskId?.let { detailTaskId = it } }

    BackHandler(navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                TaskListPane(
                    state = state,
                    themeSelected = themeMode,
                    themeOptions = themeViewModel.options,
                    onThemeSelect = themeViewModel::onSelectMode,
                    onSearch = listViewModel::onSearch,
                    onSort = listViewModel::onSort,
                    onToggle = listViewModel::onToggleCompletion,
                    onAdd = listViewModel::onAddTask,
                    onRetry = listViewModel::onRetry,
                    onOpen = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                    onDelete = listViewModel::onDelete,
                    onUndoDelete = listViewModel::onUndoDelete,
                    events = listViewModel.events,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val id = detailTaskId
                if (id == null) {
                    TaskDetailPlaceholder()
                } else {
                    // A fresh detail ViewModel per opened task (id via assisted injection),
                    // keyed so it is scoped to this selection — no manual reset on close.
                    val detailViewModel = hiltViewModel<TaskDetailViewModel, TaskDetailViewModel.Factory>(
                        key = id,
                        creationCallback = { factory -> factory.create(id) },
                    )
                    TaskDetailPane(
                        viewModel = detailViewModel,
                        onBack = { scope.launch { navigator.navigateBack() } },
                        onSaved = { scope.launch { navigator.navigateBack() } },
                    )
                }
            }
        },
    )
}
