package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.CategoryDeleteStrategy
import com.example.ui.theme.CategoryColorOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
  initialCategory: CategoryEntity? = null,
  onDismiss: () -> Unit,
  onSave: (name: String, description: String, iconKey: String, colorHex: Long) -> Unit,
) {
  var name by remember(initialCategory) { mutableStateOf(initialCategory?.name ?: "") }
  var description by remember(initialCategory) { mutableStateOf(initialCategory?.description ?: "") }
  var selectedIconKey by remember(initialCategory) { mutableStateOf(initialCategory?.iconKey ?: "folder") }
  var selectedColorHex by
    remember(initialCategory) { mutableLongStateOf(initialCategory?.colorHex ?: CategoryColorOptions.first().hex) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape = RoundedCornerShape(24.dp),
    title = { Text(if (initialCategory == null) "New Category" else "Edit Category") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Category name") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (optional)") },
          maxLines = 2,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("category_desc_input"),
        )

        Text("Color", style = MaterialTheme.typography.labelMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CategoryColorOptions.forEach { option ->
            val selected = selectedColorHex == option.hex
            Box(
              modifier =
                Modifier.size(36.dp)
                  .clip(CircleShape)
                  .background(Color(option.hex))
                  .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                  )
                  .clickable { selectedColorHex = option.hex },
              contentAlignment = Alignment.Center,
            ) {
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Selected ${option.name}",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }

        Text("Icon", style = MaterialTheme.typography.labelMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CategoryIconOptions.forEach { opt ->
            val selected = selectedIconKey == opt.key
            val tint = if (selected) Color(selectedColorHex) else MaterialTheme.colorScheme.onSurfaceVariant
            val bg =
              if (selected) Color(selectedColorHex).copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceContainerLow
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(bg)
                  .border(
                    width = if (selected) 1.5.dp else 0.dp,
                    color = if (selected) Color(selectedColorHex) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                  )
                  .clickable { selectedIconKey = opt.key },
              contentAlignment = Alignment.Center,
            ) {
              Icon(imageVector = opt.icon, contentDescription = opt.label, tint = tint)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(name.trim(), description.trim(), selectedIconKey, selectedColorHex)
            onDismiss()
          }
        },
        enabled = name.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("category_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun SafeDeleteCategoryDialog(
  category: CategoryEntity,
  associatedTaskCount: Int,
  otherCategories: List<CategoryEntity>,
  onDismiss: () -> Unit,
  onConfirmDelete: (strategy: CategoryDeleteStrategy, targetCategoryId: String?) -> Unit,
) {
  var selectedStrategy by remember { mutableStateOf(CategoryDeleteStrategy.MOVE_TO_INBOX) }
  var targetCategoryId by remember { mutableStateOf(otherCategories.firstOrNull()?.id) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete \"${category.name}\"?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text =
            if (associatedTaskCount == 0) {
              "This category has no tasks and can be safely removed."
            } else {
              "This category contains $associatedTaskCount task(s). Choose what should happen to them:"
            },
          style = MaterialTheme.typography.bodyMedium,
        )

        if (associatedTaskCount > 0) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedStrategy = CategoryDeleteStrategy.MOVE_TO_INBOX }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selectedStrategy == CategoryDeleteStrategy.MOVE_TO_INBOX,
              onClick = { selectedStrategy = CategoryDeleteStrategy.MOVE_TO_INBOX },
            )
            Text("Move tasks to Inbox", style = MaterialTheme.typography.bodyMedium)
          }

          if (otherCategories.isNotEmpty()) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { selectedStrategy = CategoryDeleteStrategy.MOVE_TO_CATEGORY }
                  .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = selectedStrategy == CategoryDeleteStrategy.MOVE_TO_CATEGORY,
                onClick = { selectedStrategy = CategoryDeleteStrategy.MOVE_TO_CATEGORY },
              )
              Text(
                "Move tasks to ${otherCategories.find { it.id == targetCategoryId }?.name ?: otherCategories.first().name}",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedStrategy = CategoryDeleteStrategy.DELETE_TASKS }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selectedStrategy == CategoryDeleteStrategy.DELETE_TASKS,
              onClick = { selectedStrategy = CategoryDeleteStrategy.DELETE_TASKS },
            )
            Text(
              "Delete category and all its tasks",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmDelete(
            if (associatedTaskCount == 0) CategoryDeleteStrategy.MOVE_TO_INBOX else selectedStrategy,
            targetCategoryId,
          )
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        modifier = Modifier.testTag("confirm_delete_category_button"),
      ) {
        Text("Delete")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun MoveCategoryDialog(
  categories: List<CategoryEntity>,
  currentCategoryId: String?,
  onDismiss: () -> Unit,
  onSelectCategory: (String?) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Move to Category") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                onSelectCategory(null)
                onDismiss()
              }
              .padding(vertical = 6.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(selected = currentCategoryId == null, onClick = null)
          Text("Inbox (No category)", modifier = Modifier.padding(start = 8.dp))
        }
        categories.forEach { cat ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  onSelectCategory(cat.id)
                  onDismiss()
                }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = currentCategoryId == cat.id, onClick = null)
            Icon(
              imageVector = categoryIconForKey(cat.iconKey),
              contentDescription = null,
              tint = Color(cat.colorHex),
              modifier = Modifier.padding(start = 8.dp).size(18.dp),
            )
            Text(cat.name, modifier = Modifier.padding(start = 8.dp))
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}
