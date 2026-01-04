package com.vzith.bookstack.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vzith.bookstack.BookStackApplication
import com.vzith.bookstack.data.api.BookStackApiClient

/**
 * BookStack Android App - Settings Screen (2026-01-05)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val keystoreManager = BookStackApplication.instance.keystoreManager

    var serverUrl by remember { mutableStateOf(keystoreManager.getServerUrl() ?: "") }
    var syncServerUrl by remember { mutableStateOf(keystoreManager.getSyncServerUrl() ?: "ws://100.78.187.47:3032") }
    var tokenId by remember { mutableStateOf(keystoreManager.getTokenId() ?: "") }
    var tokenSecret by remember { mutableStateOf(keystoreManager.getTokenSecret() ?: "") }
    var showSecret by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Configuration Section
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("BookStack Server URL") },
                placeholder = { Text("http://192.168.1.100:8080") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = syncServerUrl,
                onValueChange = { syncServerUrl = it },
                label = { Text("Sync Server URL (Hocuspocus)") },
                placeholder = { Text("ws://192.168.1.100:3032") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // API Token Section
            Text(
                text = "API Token",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Generate a token in BookStack: Settings → API Tokens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = tokenId,
                onValueChange = { tokenId = it },
                label = { Text("Token ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tokenSecret,
                onValueChange = { tokenSecret = it },
                label = { Text("Token Secret") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showSecret)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Hide" else "Show"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    keystoreManager.saveServerUrl(serverUrl.trim())
                    keystoreManager.saveSyncServerUrl(syncServerUrl.trim())
                    keystoreManager.saveToken(tokenId.trim(), tokenSecret.trim())
                    BookStackApiClient.clear() // Force rebuild with new credentials
                    saveSuccess = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverUrl.isNotBlank() && tokenId.isNotBlank() && tokenSecret.isNotBlank()
            ) {
                Text("Save Configuration")
            }

            // Clear Button
            OutlinedButton(
                onClick = {
                    keystoreManager.clearAll()
                    BookStackApiClient.clear()
                    serverUrl = ""
                    syncServerUrl = "ws://100.78.187.47:3032"
                    tokenId = ""
                    tokenSecret = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All Data")
            }

            // Success message
            if (saveSuccess) {
                Text(
                    text = "✓ Configuration saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
