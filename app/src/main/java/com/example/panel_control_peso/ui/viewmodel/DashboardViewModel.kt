package com.example.panel_control_peso.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.panel_control_peso.data.api.RetrofitClient
import com.example.panel_control_peso.data.model.*
import com.example.panel_control_peso.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val kpis: GlobalKpis = GlobalKpis(),
    val unforcedBlocks: List<UnforcedBlock> = emptyList(),
    val filteredBlocks: List<UnforcedBlock> = emptyList(),
    val selectedBlockSummary: BlockSummary? = null,
    val selectedWeightAnalytics: WeightAnalytics? = null,
    val selectedPhytosanitaryAnalytics: PhytosanitaryAnalytics? = null,
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val categoryFilter: String = "TODOS",
    val selectedBlockName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentServerUrl: String = RetrofitClient.BASE_URL
)

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun setServerUrl(newUrl: String) {
        RetrofitClient.setCustomBaseUrl(newUrl)
        _uiState.value = _uiState.value.copy(currentServerUrl = RetrofitClient.BASE_URL)
        loadDashboardData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val kpisResult = repository.getGlobalKpis()
            val blocksResult = repository.getUnforcedBlocks()

            val kpis = kpisResult.getOrDefault(GlobalKpis())
            val blocks = blocksResult.getOrDefault(emptyList())

            var errorMsg: String? = null
            if (kpisResult.isFailure && blocksResult.isFailure) {
                errorMsg = "Error al conectar con la API (${kpisResult.exceptionOrNull()?.message})"
            }

            _uiState.value = _uiState.value.copy(
                kpis = kpis,
                unforcedBlocks = blocks,
                isLoading = false,
                errorMessage = errorMsg
            )
            applyFilters()

            if (blocks.isNotEmpty() && _uiState.value.selectedBlockName.isEmpty()) {
                selectBlock(blocks.first().bloque)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onCategoryFilterChanged(category: String) {
        _uiState.value = _uiState.value.copy(categoryFilter = category)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val category = _uiState.value.categoryFilter

        val filtered = _uiState.value.unforcedBlocks.filter { block ->
            val matchesQuery = query.isEmpty() ||
                    block.bloque.lowercase().contains(query) ||
                    (block.descripcion?.lowercase()?.contains(query) == true)

            val matchesCategory = when (category) {
                "URGENTE" -> block.categoriaForzamiento == "URGENTE"
                "PROXIMO" -> block.categoriaForzamiento == "PROXIMO"
                "NORMAL" -> block.categoriaForzamiento == "NORMAL"
                else -> true
            }

            matchesQuery && matchesCategory
        }

        _uiState.value = _uiState.value.copy(filteredBlocks = filtered)
    }

    fun selectBlock(bloque: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedBlockName = bloque,
                isLoading = true
            )

            val summaryRes = repository.getBlockSummary(bloque)
            val weightRes = repository.getWeightAnalytics(bloque)
            val phytoRes = repository.getPhytosanitaryAnalytics(bloque)

            _uiState.value = _uiState.value.copy(
                selectedBlockSummary = summaryRes.getOrNull(),
                selectedWeightAnalytics = weightRes.getOrNull(),
                selectedPhytosanitaryAnalytics = phytoRes.getOrNull(),
                isLoading = false
            )
        }
    }
}
