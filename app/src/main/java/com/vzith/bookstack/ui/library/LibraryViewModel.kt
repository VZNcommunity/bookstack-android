package com.vzith.bookstack.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vzith.bookstack.data.api.SearchResult
import com.vzith.bookstack.data.db.entity.BookEntity
import com.vzith.bookstack.data.db.entity.PageEntity
import com.vzith.bookstack.data.repository.BookRepository
import com.vzith.bookstack.data.repository.PageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * BookStack Android App - Library ViewModel (2026-01-05)
 * Updated: 2026-01-11 - Added search functionality
 */
data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val selectedBook: BookEntity? = null,
    val pages: List<PageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Search state (2026-01-11)
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchActive: Boolean = false,
    // Create page dialog (2026-01-11)
    val showCreatePageDialog: Boolean = false,
    val createPageName: String = "",
    val isCreatingPage: Boolean = false,
    // Delete confirmation (2026-01-11)
    val pageToDelete: PageEntity? = null
)

class LibraryViewModel : ViewModel() {

    private val bookRepository = BookRepository()
    private val pageRepository = PageRepository()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Observe books from local DB
        viewModelScope.launch {
            bookRepository.getAllBooks().collect { books ->
                _uiState.update { it.copy(books = books) }
            }
        }

        // Initial refresh
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = bookRepository.refreshBooks()
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectBook(book: BookEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedBook = book, isLoading = true) }

            // Observe pages for this book
            pageRepository.getPagesByBook(book.id).collect { pages ->
                _uiState.update { it.copy(pages = pages) }
            }
        }

        // Refresh pages from API
        viewModelScope.launch {
            val result = pageRepository.refreshPages(book.id)
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedBook = null, pages = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Search functionality (2026-01-11)
    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Debounce search
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }

        val result = bookRepository.search(query)
        result.onSuccess { results ->
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message, isSearching = false) }
        }
    }

    fun activateSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun deactivateSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                isSearchActive = false,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    // Create page functionality (2026-01-11)
    fun showCreatePageDialog() {
        _uiState.update { it.copy(showCreatePageDialog = true, createPageName = "") }
    }

    fun hideCreatePageDialog() {
        _uiState.update { it.copy(showCreatePageDialog = false, createPageName = "") }
    }

    fun onCreatePageNameChange(name: String) {
        _uiState.update { it.copy(createPageName = name) }
    }

    fun createPage() {
        val state = _uiState.value
        val bookId = state.selectedBook?.id ?: return
        val name = state.createPageName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPage = true) }

            val result = pageRepository.createPage(
                bookId = bookId,
                chapterId = null,
                name = name,
                html = "<p></p>"
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showCreatePageDialog = false,
                        createPageName = "",
                        isCreatingPage = false
                    )
                }
                // Refresh pages list
                pageRepository.refreshPages(bookId)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = e.message, isCreatingPage = false)
                }
            }
        }
    }

    // Delete page functionality (2026-01-11)
    fun confirmDeletePage(page: PageEntity) {
        _uiState.update { it.copy(pageToDelete = page) }
    }

    fun cancelDeletePage() {
        _uiState.update { it.copy(pageToDelete = null) }
    }

    fun deletePage() {
        val page = _uiState.value.pageToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pageToDelete = null) }

            val result = pageRepository.deletePage(page.id)
            result.onSuccess {
                // Pages list will auto-update via Flow
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
