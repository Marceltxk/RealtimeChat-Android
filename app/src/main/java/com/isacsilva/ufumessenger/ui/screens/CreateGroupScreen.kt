package com.isacsilva.ufumessenger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import com.isacsilva.ufumessenger.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.ui.viewmodel.CreateGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onNextClick: (selectedContactIds: List<String>) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedCount = uiState.selectedContactIds.size
    val totalCount = uiState.potentialMembers.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Novo Grupo")
                        if (selectedCount > 0) {
                            Text(
                                "$selectedCount de $totalCount selecionados",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            // O botão só aparece se pelo menos um contacto for selecionado
            if (selectedCount > 0) {
                FloatingActionButton(onClick = { onNextClick(uiState.selectedContactIds.toList()) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avançar")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(uiState.potentialMembers) { contact ->
                    ContactSelectItem(
                        contact = contact,
                        isSelected = uiState.selectedContactIds.contains(contact.uid),
                        onCheckedChange = { isChecked ->
                            viewModel.onContactSelected(contact.uid, isChecked)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ContactSelectItem(
    contact: User,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Substituir por uma imagem de perfil
        Text(
            text = contact.username ?: "Utilizador",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(16.dp))
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onCheckedChange(it) }
        )
    }
}