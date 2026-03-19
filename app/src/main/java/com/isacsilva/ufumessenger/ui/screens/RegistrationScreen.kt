package com.isacsilva.ufumessenger.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isacsilva.ufumessenger.R
import com.isacsilva.ufumessenger.ui.viewmodel.RegistrationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    uiState: RegistrationUiState,
    onSendCodeClick: (username: String, phoneNumber: String, activity: Activity) -> Unit,
    onRegisterClick: (code: String, username: String) -> Unit,
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    onErrorMessageShown: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    if (uiState.errorMessage != null) {
        val currentErrorMessage = uiState.errorMessage
        LaunchedEffect(currentErrorMessage, snackbarHostState) {
            snackbarHostState.showSnackbar(
                message = currentErrorMessage,
                duration = SnackbarDuration.Short
            )
            onErrorMessageShown()
        }
    }

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            onRegistrationSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create a New Account", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.usernameError != null,
                supportingText = { uiState.usernameError?.let { Text(it) } },
                enabled = !uiState.isCodeSent
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isCodeSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (e.g. +16505551234)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.phoneNumberError != null,
                    supportingText = { uiState.phoneNumberError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onSendCodeClick(username, phoneNumber, context as Activity) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = username.isNotBlank() && phoneNumber.isNotBlank()
                    ) {
                        Text("Send Verification Code")
                    }
                }
            } else {
                // Etapa 2: Inserir código de verificação
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Verification Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.codeError != null,
                    supportingText = { uiState.codeError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onRegisterClick(code, username) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = code.isNotBlank()
                    ) {
                        Text("Verify & Register")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    RegistrationScreen(
        uiState = RegistrationUiState(),
        onSendCodeClick = { _, _, _ -> },
        onRegisterClick = { _, _ -> },
        onNavigateBack = {},
        onRegistrationSuccess = {},
        onErrorMessageShown = {}
    )
}