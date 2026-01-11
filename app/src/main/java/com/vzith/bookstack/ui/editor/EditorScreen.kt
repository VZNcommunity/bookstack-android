package com.vzith.bookstack.ui.editor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.vzith.bookstack.data.api.RevisionSummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * BookStack Android App - Editor Screen (2026-01-05)
 * Updated: 2026-01-11 - Added revision history and export
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pageId: Int,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pageId) {
        viewModel.loadPage(pageId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.page?.name ?: "Loading...",
                            maxLines = 1
                        )
                        // Sync status indicator
                        Text(
                            text = when (uiState.syncState) {
                                is SyncState.Connected -> "● Connected"
                                is SyncState.Synced -> "● Synced"
                                is SyncState.Connecting -> "○ Connecting..."
                                is SyncState.Disconnected -> "○ Offline"
                                is SyncState.Error -> "⚠ Error"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (uiState.syncState) {
                                is SyncState.Synced -> Color(0xFF4CAF50)
                                is SyncState.Connected -> Color(0xFF2196F3)
                                is SyncState.Error -> Color(0xFFF44336)
                                else -> Color.Gray
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasUnsavedChanges) {
                        Text(
                            text = "●",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                    // More menu (2026-01-11)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("History") },
                                leadingIcon = { Icon(Icons.Default.History, null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showRevisionHistory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showExportMenu()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.page == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Formatting Toolbar (2026-01-11: Enhanced)
                EditorToolbar(
                    richTextState = viewModel.richTextState,
                    onInsertLink = { showLinkDialog = true },
                    onInsertImage = { showImageDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                // Rich Text Editor
                RichTextEditor(
                    state = viewModel.richTextState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Revision History Sheet (2026-01-11)
    if (uiState.showRevisionHistory) {
        RevisionHistorySheet(
            revisions = uiState.revisions,
            isLoading = uiState.isLoadingRevisions,
            selectedRevisionHtml = uiState.selectedRevisionHtml,
            onPreview = { viewModel.previewRevision(it) },
            onRestore = { viewModel.restoreRevision(it) },
            onDismiss = { viewModel.hideRevisionHistory() }
        )
    }

    // Export Dialog (2026-01-11)
    if (uiState.showExportMenu) {
        ExportDialog(
            onExport = { format -> viewModel.exportPage(format) },
            onDismiss = { viewModel.hideExportMenu() }
        )
    }

    // Handle export result - share via intent (2026-01-11)
    LaunchedEffect(uiState.exportedContent) {
        uiState.exportedContent?.let { result ->
            when (result) {
                is ExportResult.Text -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = result.format
                        putExtra(Intent.EXTRA_TEXT, result.content)
                        putExtra(Intent.EXTRA_SUBJECT, result.filename)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${result.filename}"))
                }
                is ExportResult.Binary -> {
                    // Save to cache and share
                    val file = File(context.cacheDir, result.filename)
                    file.writeBytes(result.content)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = result.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${result.filename}"))
                }
            }
            viewModel.clearExportedContent()
        }
    }

    // Exporting overlay
    if (uiState.isExporting) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Link Dialog (2026-01-11)
    if (showLinkDialog) {
        InsertLinkDialog(
            onInsert = { url, text ->
                viewModel.richTextState.addLink(text = text, url = url)
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    // Image Dialog (2026-01-11)
    if (showImageDialog) {
        InsertImageDialog(
            onInsert = { url, alt ->
                // Insert image as HTML img tag
                val currentHtml = viewModel.richTextState.toHtml()
                val imgTag = "<img src=\"$url\" alt=\"$alt\" />"
                viewModel.richTextState.setHtml(currentHtml + imgTag)
                showImageDialog = false
            },
            onDismiss = { showImageDialog = false }
        )
    }
}

@Composable
private fun EditorToolbar(
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState,
    onInsertLink: () -> Unit = {},
    onInsertImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Undo/Redo (2026-01-11)
        IconButton(
            onClick = { /* richTextState.undo() - if supported */ },
            enabled = false // RichTextEditor may not support undo yet
        ) {
            Icon(
                Icons.Default.Undo,
                contentDescription = "Undo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        IconButton(
            onClick = { /* richTextState.redo() - if supported */ },
            enabled = false
        ) {
            Icon(
                Icons.Default.Redo,
                contentDescription = "Redo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )

        // Bold
        IconButton(
            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        ) {
            Icon(
                Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = if (richTextState.currentSpanStyle.fontWeight == FontWeight.Bold)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Italic
        IconButton(
            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        ) {
            Icon(
                Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = if (richTextState.currentSpanStyle.fontStyle == FontStyle.Italic)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Underline
        IconButton(
            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
        ) {
            Icon(
                Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )

        // Heading 1
        IconButton(
            onClick = {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
            }
        ) {
            Text(
                "H1",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Heading 2
        IconButton(
            onClick = {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
        ) {
            Text(
                "H2",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Heading 3
        IconButton(
            onClick = {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
        ) {
            Text(
                "H3",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )

        // Unordered List
        IconButton(
            onClick = { richTextState.toggleUnorderedList() }
        ) {
            Icon(
                Icons.Default.FormatListBulleted,
                contentDescription = "Bullet List",
                tint = if (richTextState.isUnorderedList)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Ordered List
        IconButton(
            onClick = { richTextState.toggleOrderedList() }
        ) {
            Icon(
                Icons.Default.FormatListNumbered,
                contentDescription = "Numbered List",
                tint = if (richTextState.isOrderedList)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )

        // Code (2026-01-11)
        IconButton(
            onClick = { richTextState.toggleCodeSpan() }
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = "Code",
                tint = if (richTextState.isCodeSpan)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quote (2026-01-11)
        IconButton(
            onClick = { /* Quote not directly supported, use paragraph style */ }
        ) {
            Icon(
                Icons.Default.FormatQuote,
                contentDescription = "Quote",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(
            modifier = Modifier
                .height(24.dp)
                .align(Alignment.CenterVertically)
        )

        // Link (2026-01-11)
        IconButton(onClick = onInsertLink) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Insert Link",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Image (2026-01-11)
        IconButton(onClick = onInsertImage) {
            Icon(
                Icons.Default.Image,
                contentDescription = "Insert Image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Horizontal Rule (2026-01-11)
        IconButton(
            onClick = { /* Add horizontal rule */ }
        ) {
            Icon(
                Icons.Default.HorizontalRule,
                contentDescription = "Horizontal Rule",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Revision History Sheet (2026-01-11)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevisionHistorySheet(
    revisions: List<RevisionSummary>,
    isLoading: Boolean,
    selectedRevisionHtml: String?,
    onPreview: (Int) -> Unit,
    onRestore: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Revision History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading && revisions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (revisions.isEmpty()) {
                Text(
                    text = "No revision history available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(revisions) { revision ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPreview(revision.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Revision ${revision.revision_number}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = try {
                                            dateFormat.format(
                                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                                    .parse(revision.created_at) ?: Date()
                                            )
                                        } catch (e: Exception) {
                                            revision.created_at
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    revision.created_by?.let { user ->
                                        Text(
                                            text = "by ${user.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    revision.summary?.let { summary ->
                                        if (summary.isNotBlank()) {
                                            Text(
                                                text = summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { onRestore(revision.id) }) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }

            // Preview area
            selectedRevisionHtml?.let { html ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    Text(
                        text = html.replace(Regex("<[^>]*>"), "").take(500),
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Export Dialog (2026-01-11)
@Composable
private fun ExportDialog(
    onExport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Page") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium
                )
                listOf(
                    "html" to "HTML",
                    "markdown" to "Markdown",
                    "plaintext" to "Plain Text",
                    "pdf" to "PDF"
                ).forEach { (format, label) ->
                    OutlinedButton(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = when (format) {
                                "html" -> Icons.Default.Code
                                "markdown" -> Icons.Default.Description
                                "pdf" -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.TextFields
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Insert Link Dialog (2026-01-11)
@Composable
private fun InsertLinkDialog(
    onInsert: (url: String, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Link") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Link Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onInsert(url, text.ifBlank { url }) },
                enabled = url.isNotBlank()
            ) {
                Text("Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Insert Image Dialog (2026-01-11)
@Composable
private fun InsertImageDialog(
    onInsert: (url: String, altText: String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var altText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Image") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://example.com/image.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = altText,
                    onValueChange = { altText = it },
                    label = { Text("Alt Text (optional)") },
                    placeholder = { Text("Description of image") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tip: Paste a URL to an image hosted online",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onInsert(url, altText) },
                enabled = url.isNotBlank()
            ) {
                Text("Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
