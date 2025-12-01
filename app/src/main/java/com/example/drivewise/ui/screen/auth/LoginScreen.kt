package com.example.drivewise.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivewise.domain.model.Role
import com.example.drivewise.ui.components.RoleToggle
import com.example.drivewise.ui.viewmodel.AuthEvent
import com.example.drivewise.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onAdminLoggedIn: () -> Unit,
    onClientLoggedIn: () -> Unit,
    onRoleMismatch: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()

    LaunchedEffect(event) {
        when (event) {
            is AuthEvent.LoggedIn -> {
                viewModel.clearEvent()
                when ((event as AuthEvent.LoggedIn).user.getRoleEnum()) {
                    Role.ADMIN -> onAdminLoggedIn()
                    Role.CLIENT -> onClientLoggedIn()
                }
            }
            AuthEvent.RoleMismatch -> {
                viewModel.clearEvent()
                onRoleMismatch()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(12.dp))

        RoleToggle(selected = state.selectedRole, onRoleSelected = viewModel::onRoleSelected)

        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = viewModel::login, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (state.isLoading) "Signing in..." else "Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text(text = "Go to Register")
        }
    }
}
