package com.isacsilva.ufumessenger.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isacsilva.ufumessenger.R
import com.isacsilva.ufumessenger.ui.viewmodel.LoginUiState


@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onSendCodeClick: (phoneNumber: String, activity: Activity) -> Unit,
    onSignInClick: (code: String) -> Unit,
    onSignUpClick: () -> Unit,
    onErrorMessageShown: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onErrorMessageShown()
        }
    }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bem-Vindo ao\nUfu Messenger",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Faça login ou Crie sua conta",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (!uiState.isCodeSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Telefone (ex: +16505551234)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !uiState.isLoading,
                    isError = uiState.phoneNumberError != null,
                    supportingText = { uiState.phoneNumberError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSendCodeClick(phoneNumber, context as Activity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading && phoneNumber.isNotBlank()
                ) {
                    Text("Enviar código", fontSize = 16.sp)
                }
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Código de verificação") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !uiState.isLoading,
                    isError = uiState.codeError != null,
                    supportingText = { uiState.codeError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSignInClick(code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading && code.isNotBlank()
                ) {
                    Text("Sign In", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Criar Conta", fontSize = 16.sp)
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        uiState = LoginUiState(isLoading = false),
        onSendCodeClick = { _, _ -> },
        onSignInClick = {},
        onSignUpClick = {},
        onErrorMessageShown = {},
        onLoginSuccess = {}
    )
}