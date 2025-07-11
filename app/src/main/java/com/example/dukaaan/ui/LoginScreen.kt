package com.example.dukaaan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dukaaan.viewmodel.AuthViewModel
import com.example.dukaaan.viewmodel.LoginType
import com.example.dukaaan.viewmodel.AuthResult

@Composable
fun LoginScreen(authViewModel: AuthViewModel = viewModel(), onLoginSuccess: (String, LoginType, String, String) -> Unit) {
    val loginType by authViewModel.loginType.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val shopCode by authViewModel.shopCode.collectAsState()
    val staffId by authViewModel.staffId.collectAsState()
    val authResult by authViewModel.authResult.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(
                onClick = { authViewModel.setLoginType(LoginType.Owner) },
                enabled = loginType != LoginType.Owner,
                modifier = Modifier.weight(1f)
            ) { Text("Shop Owner") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { authViewModel.setLoginType(LoginType.Staff) },
                enabled = loginType != LoginType.Staff,
                modifier = Modifier.weight(1f)
            ) { Text("Staff") }
        }
        Spacer(modifier = Modifier.height(32.dp))
        when (loginType) {
            is LoginType.Owner -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { authViewModel.setEmail(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { authViewModel.setPassword(it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is LoginType.Staff -> {
                OutlinedTextField(
                    value = shopCode,
                    onValueChange = { authViewModel.setShopCode(it) },
                    label = { Text("Shop Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = staffId,
                    onValueChange = { authViewModel.setStaffId(it) },
                    label = { Text("Staff ID") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (loginType is LoginType.Owner) authViewModel.loginOwner() else authViewModel.loginStaff()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Login") }
        Spacer(modifier = Modifier.height(16.dp))
        when (authResult) {
            is AuthResult.Loading -> CircularProgressIndicator()
            is AuthResult.Error -> Text((authResult as AuthResult.Error).message, color = MaterialTheme.colors.error)
            is AuthResult.Success -> {
                LaunchedEffect(authResult) {
                    if (loginType is LoginType.Owner) {
                        onLoginSuccess((authResult as AuthResult.Success).userId, loginType, "", "")
                    } else if (loginType is LoginType.Staff) {
                        val result = authResult as AuthResult.Success
                        onLoginSuccess(result.userId, loginType, result.shopId ?: "", result.staffName ?: "")
                    }
                    authViewModel.resetAuthResult()
                }
            }
            else -> {}
        }
    }
} 