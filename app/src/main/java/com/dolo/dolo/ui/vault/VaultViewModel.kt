package com.dolo.dolo.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dolo.core.db.LibraryItemEntity
import com.dolo.core.repository.LibraryRepository
import com.dolo.core.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isInitialized: Boolean = false,
    val isAuthenticated: Boolean = false,
    val items: List<LibraryItemEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    val uiState: StateFlow<VaultUiState> = libraryRepository.observeVault()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        ).let { itemsFlow ->
            MutableStateFlow(VaultUiState()).apply {
                viewModelScope.launch {
                    itemsFlow.collect { items ->
                        value = value.copy(
                            items = items,
                            isInitialized = vaultRepository.isInitialized(),
                            isAuthenticated = _isAuthenticated.value
                        )
                    }
                }
            }
        }

    fun setVaultPassword(password: String) {
        vaultRepository.setPassword(password)
        _isAuthenticated.value = true
    }

    fun authenticate(password: String): Boolean {
        return if (vaultRepository.verifyPassword(password)) {
            _isAuthenticated.value = true
            true
        } else {
            false
        }
    }

    fun authenticateBiometric() {
        _isAuthenticated.value = true
    }

    fun lockVault() {
        _isAuthenticated.value = false
    }

    fun removeFromVault(id: String) {
        viewModelScope.launch {
            libraryRepository.removeFromVault(id)
        }
    }

    fun isInitialized() = vaultRepository.isInitialized()
    fun isBiometricEnabled() = vaultRepository.isBiometricEnabled()
}
