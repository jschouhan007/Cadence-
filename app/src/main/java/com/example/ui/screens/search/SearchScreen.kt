package com.example.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
  viewModel: SearchViewModel,
  onNavigateBack: () -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onOpenCategoryDetail: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("search_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = { Text("Search") },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize()) {
        OutlinedTextField(
          value = uiState.query,
          onValueChange = viewModel::setQuery,
          placeholder = { Text("Search tasks, notes, categories, or tags") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.query.isNotEmpty()) {
              IconButton(onClick = { viewModel.setQuery("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { viewModel.commitSearch() }),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("search_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.includeCompleted,
            onClick = { viewModel.setIncludeCompleted(!uiState.includeCompleted) },
            label = { Text("Include completed") },
          )
        }

        if (uiState.query.isBlank()) {
          if (uiState.recentSearches.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = viewModel::clearRecentSearches) { Text("Clear") }
              }
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.recentSearches.forEach { term ->
                  AssistChip(
                    onClick = { viewModel.setQuery(term) },
                    label = { Text(term) },
                    leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
                  )
                }
              }
            }
          } else {
            EmptyStateView(
              icon = Icons.Rounded.Search,
              title = "Search your workspace",
              message = "Find tasks by title, description, notes, category, or #tags.",
            )
          }
        } else if (uiState.results.isEmpty()) {
          EmptyStateView(
            icon = Icons.Rounded.SearchOff,
            title = "No matches for \"${uiState.query}\"",
            message = "Try a shorter keyword or include completed tasks.",
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(uiState.results, key = { it.task.id }) { item ->
              TaskRowCard(
                item = item,
                onClick = {
                  viewModel.commitSearch()
                  onOpenTaskDetail(item.task.id)
                },
                onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                onCategoryClick = onOpenCategoryDetail,
              )
            }
          }
        }
      }
    }
  }
}
