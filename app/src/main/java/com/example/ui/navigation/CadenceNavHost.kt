package com.example.ui.navigation

import android.media.RingtoneManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.AppContainer
import com.example.data.local.entity.TaskPriority
import com.example.domain.reminder.InAppAlertHub
import com.example.domain.time.DateTimeUtils
import com.example.ui.CadenceViewModelFactory
import com.example.ui.components.QuickAddTaskSheet
import com.example.ui.screens.category.CategoryDetailScreen
import com.example.ui.screens.category.CategoryDetailViewModel
import com.example.ui.screens.focus.FocusScreen
import com.example.ui.screens.focus.FocusViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.reminders.ReminderCentreScreen
import com.example.ui.screens.reminders.ReminderCentreViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.taskdetail.TaskDetailScreen
import com.example.ui.screens.taskdetail.TaskDetailViewModel
import com.example.ui.theme.MotionDuration
import kotlinx.coroutines.launch

private data class TopLevelNavItem(
  val route: Any,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private data class QuickAddConfig(
  val initialCategoryId: String? = null,
  val initialDueAt: Long? = null,
)

@Composable
fun CadenceAppShell(
  container: AppContainer,
  hasCompletedOnboarding: Boolean,
  pendingOpenTaskId: String? = null,
  onClearPendingOpenTaskId: () -> Unit = {},
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val activeAlerts by InAppAlertHub.activeAlerts.collectAsStateWithLifecycle()
  val currentAlert = activeAlerts.firstOrNull()

  LaunchedEffect(currentAlert?.reminderId, currentAlert?.triggerAt) {
    if (currentAlert != null) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
      }
    }
  }

  LaunchedEffect(pendingOpenTaskId, hasCompletedOnboarding) {
    if (!pendingOpenTaskId.isNullOrBlank() && hasCompletedOnboarding) {
      navController.navigate(TaskDetailRoute(taskId = pendingOpenTaskId)) {
        launchSingleTop = true
      }
      onClearPendingOpenTaskId()
    }
  }

  val activeCategories by
    container.categoryRepository.observeActiveCategories().collectAsStateWithLifecycle(initialValue = emptyList())

  var quickAddConfig by remember { mutableStateOf<QuickAddConfig?>(null) }

  val topLevelItems = remember {
    listOf(
      TopLevelNavItem(
        route = TodayRoute,
        label = "Today",
        selectedIcon = Icons.Rounded.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny,
        testTag = "nav_today",
      ),
      TopLevelNavItem(
        route = TasksRoute,
        label = "Tasks",
        selectedIcon = Icons.Rounded.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        testTag = "nav_tasks",
      ),
      TopLevelNavItem(
        route = CalendarRoute,
        label = "Calendar",
        selectedIcon = Icons.Rounded.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_calendar",
      ),
      TopLevelNavItem(
        route = ProgressRoute,
        label = "Progress",
        selectedIcon = Icons.Rounded.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_progress",
      ),
    )
  }

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination
  val isTopLevelDestination =
    currentDest?.hasRoute<TodayRoute>() == true ||
      currentDest?.hasRoute<TasksRoute>() == true ||
      currentDest?.hasRoute<CalendarRoute>() == true ||
      currentDest?.hasRoute<ProgressRoute>() == true

  val navigateTopLevel: (Any) -> Unit = { target ->
    navController.navigate(target) {
      popUpTo<TodayRoute> { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail && isTopLevelDestination) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
          topLevelItems.forEach { item ->
            val selected = currentDest?.hasRoute(item.route::class) == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTopLevel(item.route) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.label,
                )
              },
              label = {
                Text(
                  item.label,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
              },
              modifier = Modifier.testTag(item.testTag),
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail && isTopLevelDestination) {
            androidx.compose.foundation.layout.Column {
              HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 0.dp,
              ) {
                topLevelItems.forEach { item ->
                  val selected = currentDest?.hasRoute(item.route::class) == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateTopLevel(item.route) },
                    colors =
                      NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
                    icon = {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    },
                    label = {
                      Text(
                        item.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                      )
                    },
                    modifier = Modifier.testTag(item.testTag),
                  )
                }
              }
            }
          }
        },
        floatingActionButton = {
          if (isTopLevelDestination) {
            FloatingActionButton(
              onClick = {
                val defaultDue =
                  if (currentDest?.hasRoute<TodayRoute>() == true) {
                    DateTimeUtils.endOfDay(System.currentTimeMillis())
                  } else null
                quickAddConfig = QuickAddConfig(initialDueAt = defaultDue)
              },
              shape = RoundedCornerShape(18.dp),
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
              modifier = Modifier.testTag("fab_add_task"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "Add task")
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          NavHost(
            navController = navController,
            startDestination = if (hasCompletedOnboarding) MainGraphRoute else OnboardingRoute,
            enterTransition = { fadeIn(animationSpec = tween(MotionDuration.Standard)) },
            exitTransition = { fadeOut(animationSpec = tween(MotionDuration.Fast)) },
          ) {
            composable<OnboardingRoute> {
              val vm: OnboardingViewModel =
                viewModel(
                  factory =
                    CadenceViewModelFactory {
                      OnboardingViewModel(
                        categoryRepository = container.categoryRepository,
                        preferencesRepository = container.preferencesRepository,
                      )
                    }
                )
              OnboardingScreen(
                viewModel = vm,
                onFinished = {
                  navController.navigate(MainGraphRoute) {
                    popUpTo<OnboardingRoute> { inclusive = true }
                  }
                },
              )
            }

            mainNavigationGraph(
              container = container,
              snackbarHostState = snackbarHostState,
              onOpenQuickAdd = { categoryId, dueAt ->
                quickAddConfig = QuickAddConfig(initialCategoryId = categoryId, initialDueAt = dueAt)
              },
              onOpenTaskDetail = { taskId -> navController.navigate(TaskDetailRoute(taskId = taskId)) },
              onOpenCategoryDetail = { catId -> navController.navigate(CategoryDetailRoute(catId)) },
              onOpenSearch = { navController.navigate(SearchRoute) },
              onOpenFocus = { taskId -> navController.navigate(FocusModeRoute(taskId)) },
              onOpenReminders = { navController.navigate(ReminderCentreRoute) },
              onOpenSettings = { navController.navigate(SettingsRoute) },
              onNavigateToCalendarTab = { navigateTopLevel(CalendarRoute) },
            )

            composable<CategoryDetailRoute> { entry ->
              val route = entry.toRoute<CategoryDetailRoute>()
              val vm: CategoryDetailViewModel =
                viewModel(
                  key = "cat_${route.categoryId}",
                  factory =
                    CadenceViewModelFactory {
                      CategoryDetailViewModel(
                        categoryId = route.categoryId,
                        categoryRepository = container.categoryRepository,
                        taskRepository = container.taskRepository,
                      )
                    },
                )
              CategoryDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenQuickAddForCategory = { catId ->
                  quickAddConfig = QuickAddConfig(initialCategoryId = catId)
                },
                onOpenTaskDetail = { taskId -> navController.navigate(TaskDetailRoute(taskId = taskId)) },
                onStartFocus = { taskId -> navController.navigate(FocusModeRoute(taskId)) },
              )
            }

            composable<TaskDetailRoute> { entry ->
              val route = entry.toRoute<TaskDetailRoute>()
              val initialPriority =
                TaskPriority.entries.find { it.name == route.initialPriorityName } ?: TaskPriority.MEDIUM
              val vm: TaskDetailViewModel =
                viewModel(
                  key = "task_${route.taskId ?: "new_${route.initialTitle}_${route.initialCategoryId}_${route.initialReminderAt}"}",
                  factory =
                    CadenceViewModelFactory {
                      TaskDetailViewModel(
                        initialTaskId = route.taskId,
                        initialTitle = route.initialTitle,
                        initialCategoryId = route.initialCategoryId,
                        initialPriority = initialPriority,
                        initialDueAt = route.initialDueAt,
                        initialIsAllDay = route.initialIsAllDay,
                        initialReminderAt = route.initialReminderAt,
                        taskRepository = container.taskRepository,
                        categoryRepository = container.categoryRepository,
                      )
                    },
                )
              TaskDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onStartFocus = { taskId -> navController.navigate(FocusModeRoute(taskId)) },
                onOpenDuplicatedTask = { newId ->
                  navController.navigate(TaskDetailRoute(taskId = newId)) {
                    popUpTo(route) { inclusive = true }
                  }
                },
              )
            }

            composable<SearchRoute> {
              val vm: SearchViewModel =
                viewModel(
                  factory =
                    CadenceViewModelFactory {
                      SearchViewModel(
                        taskRepository = container.taskRepository,
                        preferencesRepository = container.preferencesRepository,
                      )
                    }
                )
              SearchScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenTaskDetail = { taskId -> navController.navigate(TaskDetailRoute(taskId = taskId)) },
                onOpenCategoryDetail = { catId -> navController.navigate(CategoryDetailRoute(catId)) },
              )
            }

            composable<ReminderCentreRoute> {
              val vm: ReminderCentreViewModel =
                viewModel(
                  factory =
                    CadenceViewModelFactory {
                      ReminderCentreViewModel(taskRepository = container.taskRepository)
                    }
                )
              ReminderCentreScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenTaskDetail = { taskId -> navController.navigate(TaskDetailRoute(taskId = taskId)) },
              )
            }

            composable<FocusModeRoute> { entry ->
              val route = entry.toRoute<FocusModeRoute>()
              val vm: FocusViewModel =
                viewModel(
                  key = "focus_${route.taskId ?: "none"}",
                  factory =
                    CadenceViewModelFactory {
                      FocusViewModel(
                        initialTaskId = route.taskId,
                        focusRepository = container.focusRepository,
                        taskRepository = container.taskRepository,
                      )
                    },
                )
              FocusScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
              )
            }

            composable<SettingsRoute> {
              val vm: SettingsViewModel =
                viewModel(
                  factory =
                    CadenceViewModelFactory {
                      SettingsViewModel(
                        preferencesRepository = container.preferencesRepository,
                        categoryRepository = container.categoryRepository,
                        taskRepository = container.taskRepository,
                        focusRepository = container.focusRepository,
                        cloudSyncGateway = container.cloudSyncGateway,
                      )
                    }
                )
              SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }
  }

  quickAddConfig?.let { cfg ->
    QuickAddTaskSheet(
      categories = activeCategories,
      initialCategoryId = cfg.initialCategoryId,
      initialDueAt = cfg.initialDueAt,
      onDismiss = { quickAddConfig = null },
      onSaveQuickTask = { title, categoryId, priority, dueAt, isAllDay, reminderAt ->
        scope.launch {
          container.taskRepository.createQuickTask(
            title = title,
            categoryId = categoryId,
            priority = priority,
            dueAt = dueAt,
            isAllDay = isAllDay,
            reminderAt = reminderAt,
          )
        }
      },
      onOpenFullEditor = { title, categoryId, priority, dueAt, isAllDay, reminderAt ->
        quickAddConfig = null
        navController.navigate(
          TaskDetailRoute(
            taskId = null,
            initialTitle = title,
            initialCategoryId = categoryId,
            initialPriorityName = priority.name,
            initialDueAt = dueAt,
            initialIsAllDay = isAllDay,
            initialReminderAt = reminderAt,
          )
        )
      },
    )
  }

  currentAlert?.let { alert ->
    AlertDialog(
      onDismissRequest = { InAppAlertHub.dismiss(alert.reminderId) },
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.testTag("in_app_alert_dialog"),
      icon = {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                  )
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
          )
        }
      },
      title = {
        Text(
          text = "Task Alert",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = alert.taskTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Alarm,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Scheduled for ${DateTimeUtils.formatDateTimeShort(alert.triggerAt)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                NotificationManagerCompat.from(context).cancel(alert.notificationId)
                InAppAlertHub.dismiss(alert.reminderId)
                scope.launch {
                  val rem = container.database.reminderDao().getReminderById(alert.reminderId)
                  if (rem != null) {
                    container.taskRepository.snoozeReminder(rem, 10)
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("in_app_alert_snooze"),
            ) {
              Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Snooze 10m", maxLines = 1)
            }
            OutlinedButton(
              onClick = {
                InAppAlertHub.dismiss(alert.reminderId)
                navController.navigate(TaskDetailRoute(taskId = alert.taskId)) {
                  launchSingleTop = true
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("in_app_alert_open"),
            ) {
              Text("Open", maxLines = 1)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            NotificationManagerCompat.from(context).cancel(alert.notificationId)
            InAppAlertHub.dismiss(alert.reminderId)
            scope.launch {
              container.taskRepository.toggleTaskCompletion(alert.taskId)
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.testTag("in_app_alert_done"),
        ) {
          Text("Mark Done", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { InAppAlertHub.dismiss(alert.reminderId) },
          modifier = Modifier.testTag("in_app_alert_dismiss"),
        ) {
          Text("Dismiss")
        }
      },
    )
  }
}
