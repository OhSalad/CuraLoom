package com.pmu.mobileapp.ui.feature.add

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFeedScreen(onBack: () -> Unit, vm: AddFeedViewModel = viewModel()) {
    val context = LocalContext.current
    val state = vm.uiState

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.add_feed_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }
        )
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.add_feed_subtitle))
            OutlinedTextField(
                value = state.url,
                onValueChange = vm::onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feed_url_label)) },
                placeholder = { Text(stringResource(R.string.feed_url_hint)) },
                supportingText = { Text(stringResource(R.string.feed_url_multi_hint)) }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { vm.onCategorySelected(category) },
                        label = { Text(category) }
                    )
                }
                TextButton(onClick = vm::openCategoryDialog) { Text("+ ${stringResource(R.string.new_category)}") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (vm.validateFeed()) {
                        AddFeedResult.EMPTY_URL -> Toast.makeText(context, context.getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
                        AddFeedResult.INVALID_URL -> Toast.makeText(context, context.getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
                        AddFeedResult.VALIDATED -> Toast.makeText(context, context.getString(R.string.feed_validated), Toast.LENGTH_SHORT).show()
                        else -> {}
                    }
                }) {
                    Text(if (state.validated) stringResource(R.string.validated) else stringResource(R.string.validate))
                }
                Button(onClick = {
                    when (vm.addFeed()) {
                        AddFeedResult.EMPTY_URL -> Toast.makeText(context, context.getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
                        AddFeedResult.INVALID_URL -> Toast.makeText(context, context.getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
                        AddFeedResult.EXISTS -> Toast.makeText(context, context.getString(R.string.feed_exists), Toast.LENGTH_SHORT).show()
                        AddFeedResult.ADDED -> {
                            Toast.makeText(context, context.getString(R.string.feed_added), Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        else -> {}
                    }
                }) { Text(stringResource(R.string.add_to_library)) }
                Button(onClick = {
                    val result = vm.addFeedsFromCommaSeparatedInput()
                    if (result.added > 0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.feeds_added_summary, result.added, result.exists, result.invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        onBack()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.feeds_added_none_summary, result.exists, result.invalid),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }) { Text(stringResource(R.string.add_multiple)) }
            }
        }
    }

    if (state.showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = vm::closeCategoryDialog,
            title = { Text(stringResource(R.string.new_category_title)) },
            text = {
                OutlinedTextField(
                    value = state.newCategory,
                    onValueChange = vm::onNewCategoryChange,
                    label = { Text(stringResource(R.string.category_name_hint)) }
                )
            },
            confirmButton = { TextButton(onClick = vm::addCategory) { Text(stringResource(R.string.add)) } },
            dismissButton = { TextButton(onClick = vm::closeCategoryDialog) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
