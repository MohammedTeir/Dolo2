package com.dolo.dolo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.db.LibraryItemEntity
import com.dolo.core.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySortOption(val displayName: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    TITLE_ASC("Name (A-Z)"),
    SIZE_DESC("Size (Largest)")
}

data class LibraryUiState(
    val items: List<LibraryItemEntity> = emptyList(),
    val isGridView: Boolean = false,
    val sortOption: LibrarySortOption = LibrarySortOption.DATE_DESC,
    val searchQuery: String = "",
    val selectedItemIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _isGridView = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(LibrarySortOption.DATE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeLibrary(),
        _isGridView,
        _sortOption,
        _searchQuery,
        _selectedItemIds
    ) { rawItems, isGrid, sort, query, selected ->
        val filtered = if (query.isBlank()) {
            rawItems
        } else {
            rawItems.filter { it.title.contains(query, ignoreCase = true) }
        }

        val sorted = when (sort) {
            LibrarySortOption.DATE_DESC -> filtered.sortedByDescending { it.downloadedAt }
            LibrarySortOption.DATE_ASC -> filtered.sortedBy { it.downloadedAt }
            LibrarySortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }

        LibraryUiState(
            items = sorted,
            isGridView = isGrid,
            sortOption = sort,
            searchQuery = query,
            selectedItemIds = selected,
            isMultiSelectMode = selected.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOption(option: LibrarySortOption) {
        _sortOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(id: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll(allItems: List<LibraryItemEntity>) {
        _selectedItemIds.value = allItems.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            libraryRepository.deleteItem(id, deleteFileOnDisk = true)
            _selectedItemIds.value = _selectedItemIds.value - id
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val toDelete = _selectedItemIds.value.toList()
            libraryRepository.deleteItems(toDelete, deleteFilesOnDisk = true)
            _selectedItemIds.value = emptySet()
        }
    }

    fun renameItem(id: String, newTitle: String) {
        viewModelScope.launch {
            libraryRepository.renameItem(id, newTitle)
        }
    }

    fun moveToVault(id: String) {
        viewModelScope.launch {
            libraryRepository.moveToVault(id)
            _selectedItemIds.value = _selectedItemIds.value - id
        }
    }

    fun moveSelectedToVault() {
        viewModelScope.launch {
            _selectedItemIds.value.forEach { id ->
                libraryRepository.moveToVault(id)
            }
            _selectedItemIds.value = emptySet()
        }
    }
}
