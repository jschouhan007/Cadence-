package com.example.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.AppContainer
import com.example.domain.time.DateTimeUtils
import com.example.ui.CadenceViewModelFactory
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.calendar.CalendarViewModel
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.progress.ProgressViewModel
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.screens.today.TodayScreen
import com.example.ui.screens.today.TodayViewModel

fun NavGraphBuilder.mainNavigationGraph(
  container: AppContainer,
  snackbarHostState: SnackbarHostState,
  onOpenQuickAdd: (initialCategoryId: String?, initialDueAt: Long?) -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onOpenCategoryDetail: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenFocus: (String?) -> Unit,
  onOpenReminders: () -> Unit,
  onOpenSettings: () -> Unit,
  onNavigateToCalendarTab: () -> Unit,
) {
  navigation<MainGraphRoute>(startDestination = TodayRoute) {
    composable<TodayRoute> {
      val vm: TodayViewModel =
        viewModel(
          factory =
            CadenceViewModelFactory {
              TodayViewModel(
                taskRepository = container.taskRepository,
                categoryRepository = container.categoryRepository,
                preferencesRepository = container.preferencesRepository,
                focusRepository = container.focusRepository,
              )
            }
        )
      TodayScreen(
        viewModel = vm,
        snackbarHostState = snackbarHostState,
        onOpenQuickAdd = {
          onOpenQuickAdd(null, DateTimeUtils.endOfDay(System.currentTimeMillis()))
        },
        onOpenTaskDetail = onOpenTaskDetail,
        onOpenCategoryDetail = onOpenCategoryDetail,
        onOpenSearch = onOpenSearch,
        onOpenFocus = onOpenFocus,
        onOpenReminders = onOpenReminders,
        onOpenSettings = onOpenSettings,
        onOpenCalendarTab = onNavigateToCalendarTab,
      )
    }

    composable<TasksRoute> {
      val vm: TasksViewModel =
        viewModel(
          factory =
            CadenceViewModelFactory {
              TasksViewModel(
                taskRepository = container.taskRepository,
                categoryRepository = container.categoryRepository,
              )
            }
        )
      TasksScreen(
        viewModel = vm,
        snackbarHostState = snackbarHostState,
        onOpenQuickAdd = { onOpenQuickAdd(null, null) },
        onOpenTaskDetail = onOpenTaskDetail,
        onOpenCategoryDetail = onOpenCategoryDetail,
        onOpenSearch = onOpenSearch,
        onOpenFocus = onOpenFocus,
        onOpenReminders = onOpenReminders,
        onOpenSettings = onOpenSettings,
      )
    }

    composable<CalendarRoute> {
      val vm: CalendarViewModel =
        viewModel(
          factory =
            CadenceViewModelFactory {
              CalendarViewModel(
                taskRepository = container.taskRepository,
                categoryRepository = container.categoryRepository,
              )
            }
        )
      CalendarScreen(
        viewModel = vm,
        onOpenQuickAddForDate = { dayMillis -> onOpenQuickAdd(null, dayMillis) },
        onOpenTaskDetail = onOpenTaskDetail,
        onOpenCategoryDetail = onOpenCategoryDetail,
        onOpenSearch = onOpenSearch,
        onOpenFocus = onOpenFocus,
        onOpenReminders = onOpenReminders,
        onOpenSettings = onOpenSettings,
      )
    }

    composable<ProgressRoute> {
      val vm: ProgressViewModel =
        viewModel(
          factory =
            CadenceViewModelFactory {
              ProgressViewModel(
                taskRepository = container.taskRepository,
                categoryRepository = container.categoryRepository,
                focusRepository = container.focusRepository,
                preferencesRepository = container.preferencesRepository,
              )
            }
        )
      ProgressScreen(
        viewModel = vm,
        onOpenSearch = onOpenSearch,
        onOpenFocus = onOpenFocus,
        onOpenReminders = onOpenReminders,
        onOpenSettings = onOpenSettings,
      )
    }
  }
}
