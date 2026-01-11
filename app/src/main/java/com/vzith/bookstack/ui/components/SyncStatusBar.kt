package com.vzith.bookstack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vzith.bookstack.BookStackApplication
import kotlinx.coroutines.delay

/**
 * BookStack Android App - Sync Status Bar (2026-01-11)
 *
 * Displays network/sync status at the top of the screen.
 * - Online: Green bar with cloud icon (briefly shown then hidden)
 * - Offline: Red bar with cloud-off icon (always visible)
 * - Syncing: Blue bar with sync icon
 */
@Composable
fun SyncStatusBar(
    modifier: Modifier = Modifier
) {
    val networkMonitor = BookStackApplication.instance.networkMonitor
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())

    // Auto-hide online status after 3 seconds
    var showOnlineStatus by remember { mutableStateOf(true) }

    LaunchedEffect(isOnline) {
        if (isOnline) {
            showOnlineStatus = true
            delay(3000)
            showOnlineStatus = false
        } else {
            showOnlineStatus = true // Always show offline
        }
    }

    // Show bar if offline OR briefly after going online
    val showBar = !isOnline || (isOnline && showOnlineStatus)

    AnimatedVisibility(
        visible = showBar,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    if (isOnline) Color(0xFF4CAF50) // Green
                    else Color(0xFFF44336) // Red
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.Cloud else Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOnline) "Online" else "Offline - Changes will sync when connected",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Sync status indicator for use in editor screen.
 * More detailed, shows sync state.
 */
@Composable
fun SyncIndicator(
    syncState: SyncStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (syncState) {
                SyncStatus.SYNCED -> Icons.Default.Cloud
                SyncStatus.SYNCING -> Icons.Default.CloudSync
                SyncStatus.OFFLINE -> Icons.Default.CloudOff
            },
            contentDescription = null,
            tint = when (syncState) {
                SyncStatus.SYNCED -> Color(0xFF4CAF50)
                SyncStatus.SYNCING -> Color(0xFF2196F3)
                SyncStatus.OFFLINE -> Color(0xFFF44336)
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = when (syncState) {
                SyncStatus.SYNCED -> "Synced"
                SyncStatus.SYNCING -> "Syncing..."
                SyncStatus.OFFLINE -> "Offline"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE
}
