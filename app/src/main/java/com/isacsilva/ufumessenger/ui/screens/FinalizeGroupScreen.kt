package com.isacsilva.ufumessenger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isacsilva.ufumessenger.ui.viewmodel.FinalizeGroupViewModel
import com.isacsilva.ufumessenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (groupId: String) -> Unit,
    viewModel: FinalizeGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToChat.collect { groupId ->
            onGroupCreated(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nome do Grupo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createGroup() },
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Done, contentDescription = "Criar Grupo")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = uiState.groupName,
                    onValueChange = viewModel::onGroupNameChange,
                    label = { Text("Dê um nome ao seu grupo") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage != null
                )
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}