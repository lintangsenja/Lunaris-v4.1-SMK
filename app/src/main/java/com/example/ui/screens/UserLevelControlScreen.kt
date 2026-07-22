package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.InventoryViewModel

@Composable
fun UserLevelControlScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    RoleManagementScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
