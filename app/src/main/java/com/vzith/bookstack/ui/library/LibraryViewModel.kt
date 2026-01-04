package com.vzith.bookstack.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vzith.bookstack.data.db.entity.BookEntity
import com.vzith.bookstack.data.db.entity.PageEntity
import com.vzith.bookstack.data.repository.BookRepository
import com.vzith.bookstack.data.repository.PageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * BookStack Android App - Library ViewModel (2026-01-05)
 */
data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val selectedBook: BookEntity? = null,
    val pages: List<PageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
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
}
