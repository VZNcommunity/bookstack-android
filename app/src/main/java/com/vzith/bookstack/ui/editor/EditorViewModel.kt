package com.vzith.bookstack.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedrejeb.richeditor.model.RichTextState
import com.vzith.bookstack.data.api.RevisionSummary
import com.vzith.bookstack.data.db.entity.PageEntity
import com.vzith.bookstack.data.repository.PageRepository
import com.vzith.bookstack.data.sync.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * BookStack Android App - Editor ViewModel (2026-01-05)
 * Updated: Integrated with CRDT SyncManager
 */
sealed class SyncState {
    object Disconnected : SyncState()
    object Connecting : SyncState()
    object Connected : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}

data class EditorUiState(
    val page: PageEntity? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val syncState: SyncState = SyncState.Disconnected,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    // Revision history (2026-01-11)
    val showRevisionHistory: Boolean = false,
    val revisions: List<RevisionSummary> = emptyList(),
    val isLoadingRevisions: Boolean = false,
    val selectedRevisionHtml: String? = null,
    // Export (2026-01-11)
    val showExportMenu: Boolean = false,
    val isExporting: Boolean = false,
    val exportedContent: ExportResult? = null
)

// Export result (2026-01-11)
sealed class ExportResult {
    data class Text(val content: String, val format: String, val filename: String) : ExportResult()
    data class Binary(val content: ByteArray, val mimeType: String, val filename: String) : ExportResult()
}

class EditorViewModel : ViewModel() {

    private val pageRepository = PageRepository()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val richTextState = RichTextState()

    private var autoSaveJob: Job? = null
    private var syncJob: Job? = null
    private var currentPageId: Int = 0
    private var syncManager: SyncManager? = null
    private var isRemoteUpdate = false // Flag to prevent echo

    fun loadPage(pageId: Int) {
        currentPageId = pageId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Try local first
            val localPage = pageRepository.getPage(pageId)
            if (localPage != null) {
                _uiState.update { it.copy(page = localPage) }
                richTextState.setHtml(localPage.html)
            }

            // Fetch from API
            val result = pageRepository.fetchPage(pageId)
            result.onSuccess { page ->
                _uiState.update { it.copy(page = page, isLoading = false) }
                if (localPage == null || !localPage.locallyModified) {
                    richTextState.setHtml(page.html)
                }

                // Initialize CRDT sync
                initializeSync(page.html)
            }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }

                // Try to sync anyway with local content
                if (localPage != null) {
                    initializeSync(localPage.html)
                }
            }
        }
    }

    private fun initializeSync(initialHtml: String) {
        // Clean up previous sync manager
        syncManager?.destroy()

        // Create new sync manager
        syncManager = SyncManager(
            pageId = currentPageId,
            onContentChange = { html ->
                // Update editor with remote changes
                isRemoteUpdate = true
                richTextState.setHtml(html)
                isRemoteUpdate = false
            }
        )

        // Initialize and connect
        viewModelScope.launch {
            syncManager?.initialize(initialHtml)
        }

        // Observe sync state
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            syncManager?.syncState?.collect { state ->
                val uiSyncState = when (state) {
                    SyncManager.SyncState.DISCONNECTED -> SyncState.Disconnected
                    SyncManager.SyncState.CONNECTING -> SyncState.Connecting
                    SyncManager.SyncState.CONNECTED -> SyncState.Connected
                    SyncManager.SyncState.SYNCED -> SyncState.Synced
                    SyncManager.SyncState.ERROR -> SyncState.Error("Sync error")
                }
                _uiState.update { it.copy(syncState = uiSyncState) }
            }
        }
    }

    fun onTextChange() {
        // Don't trigger sync for remote updates
        if (isRemoteUpdate) return

        _uiState.update { it.copy(hasUnsavedChanges = true) }
        scheduleAutoSave()
        scheduleSyncUpdate()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // Debounce 2 seconds
            saveToLocal()
        }
    }

    private fun scheduleSyncUpdate() {
        viewModelScope.launch {
            delay(500) // Debounce sync updates
            val html = richTextState.toHtml()
            syncManager?.onEditorChange(html)
        }
    }

    private suspend fun saveToLocal() {
        val html = richTextState.toHtml()
        pageRepository.updatePageHtml(currentPageId, html)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val html = richTextState.toHtml()
            val result = pageRepository.savePage(currentPageId, html)

            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun reconnect() {
        syncManager?.connect()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Revision history functions (2026-01-11)

    fun showRevisionHistory() {
        _uiState.update { it.copy(showRevisionHistory = true, isLoadingRevisions = true) }
        viewModelScope.launch {
            val result = pageRepository.getRevisions(currentPageId)
            result.onSuccess { revisions ->
                _uiState.update { it.copy(revisions = revisions, isLoadingRevisions = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoadingRevisions = false) }
            }
        }
    }

    fun hideRevisionHistory() {
        _uiState.update {
            it.copy(
                showRevisionHistory = false,
                selectedRevisionHtml = null
            )
        }
    }

    fun previewRevision(revisionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRevisions = true) }
            val result = pageRepository.getRevision(currentPageId, revisionId)
            result.onSuccess { revision ->
                _uiState.update {
                    it.copy(selectedRevisionHtml = revision.html, isLoadingRevisions = false)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoadingRevisions = false) }
            }
        }
    }

    fun restoreRevision(revisionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRevisions = true) }
            val result = pageRepository.getRevision(currentPageId, revisionId)
            result.onSuccess { revision ->
                // Apply revision content to editor
                richTextState.setHtml(revision.html)
                _uiState.update {
                    it.copy(
                        showRevisionHistory = false,
                        selectedRevisionHtml = null,
                        isLoadingRevisions = false,
                        hasUnsavedChanges = true
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoadingRevisions = false) }
            }
        }
    }

    // Export functions (2026-01-11)

    fun showExportMenu() {
        _uiState.update { it.copy(showExportMenu = true) }
    }

    fun hideExportMenu() {
        _uiState.update { it.copy(showExportMenu = false) }
    }

    fun exportPage(format: String) {
        val pageName = _uiState.value.page?.name ?: "page"
        val safeFileName = pageName.replace(Regex("[^a-zA-Z0-9.-]"), "_")

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, showExportMenu = false) }

            val result = when (format) {
                "html" -> pageRepository.exportHtml(currentPageId).map { content ->
                    ExportResult.Text(content, "text/html", "$safeFileName.html")
                }
                "markdown" -> pageRepository.exportMarkdown(currentPageId).map { content ->
                    ExportResult.Text(content, "text/markdown", "$safeFileName.md")
                }
                "plaintext" -> pageRepository.exportPlaintext(currentPageId).map { content ->
                    ExportResult.Text(content, "text/plain", "$safeFileName.txt")
                }
                "pdf" -> pageRepository.exportPdf(currentPageId).map { content ->
                    ExportResult.Binary(content, "application/pdf", "$safeFileName.pdf")
                }
                else -> Result.failure(Exception("Unknown format"))
            }

            result.onSuccess { exportResult ->
                _uiState.update { it.copy(isExporting = false, exportedContent = exportResult) }
            }.onFailure { e ->
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    fun clearExportedContent() {
        _uiState.update { it.copy(exportedContent = null) }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        syncJob?.cancel()
        syncManager?.destroy()
    }
}
