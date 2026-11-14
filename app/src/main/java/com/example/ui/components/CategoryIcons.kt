package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(val key: String, val label: String, val icon: ImageVector)

val CategoryIconOptions =
  listOf(
    CategoryIconOption("folder", "General", Icons.Rounded.Folder),
    CategoryIconOption("school", "College", Icons.Rounded.School),
    CategoryIconOption("person", "Personal", Icons.Rounded.Person),
    CategoryIconOption("code", "Projects", Icons.Rounded.Code),
    CategoryIconOption("book", "Learning", Icons.AutoMirrored.Rounded.MenuBook),
    CategoryIconOption("fitness", "Fitness", Icons.Rounded.FitnessCenter),
    CategoryIconOption("wallet", "Finance", Icons.Rounded.AccountBalanceWallet),
    CategoryIconOption("work", "Work", Icons.Rounded.Work),
    CategoryIconOption("palette", "Creative", Icons.Rounded.Palette),
    CategoryIconOption("home", "Home", Icons.Rounded.Home),
    CategoryIconOption("shopping", "Errands", Icons.Rounded.ShoppingBag),
    CategoryIconOption("star", "Goals", Icons.Rounded.Star),
  )

fun categoryIconForKey(key: String): ImageVector =
  CategoryIconOptions.find { it.key == key }?.icon ?: Icons.Rounded.Folder
