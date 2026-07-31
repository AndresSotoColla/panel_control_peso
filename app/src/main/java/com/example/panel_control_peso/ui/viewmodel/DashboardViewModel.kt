package com.example.panel_control_peso.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val forcedBlocks: List<UnforcedBlock> = emptyList(),
    val filteredUnforcedBlocks: List<UnforcedBlock> = emptyList(),
    val filteredForcedBlocks: List<UnforcedBlock> = emptyList(),
    val selectedBlockSummary: BlockSummary? = null,
    val selectedWeightAnalytics: WeightAnalytics? = null,
    val selectedPhytosanitaryAnalytics: PhytosanitaryAnalytics? = null,
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val grupoSiembraFilter: String = "",
    val soloUltimoMes: Boolean = false,
    val selectedBlockName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDirectDbMode: Boolean = false,
    val currentServerUrl: String = RetrofitClient.BASE_URL
)

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = DashboardRepository(context = application.applicationContext)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        repository.useDirectDbMode = _uiState.value.isDirectDbMode
        loadDashboardData()
    }

    fun toggleConnectionMode(useDirectDb: Boolean) {
        repository.useDirectDbMode = useDirectDb
        _uiState.value = _uiState.value.copy(isDirectDbMode = useDirectDb)
        loadDashboardData()
    }

    fun setServerUrl(newUrl: String) {
        RetrofitClient.setCustomBaseUrl(newUrl)
        repository.useDirectDbMode = false
        _uiState.value = _uiState.value.copy(
            currentServerUrl = RetrofitClient.BASE_URL,
            isDirectDbMode = false
        )
        loadDashboardData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        if (tabIndex == 4 && _uiState.value.forcedBlocks.isEmpty()) {
            loadForcedBlocks()
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val kpisResult = repository.getGlobalKpis()
            val blocksResult = repository.getUnforcedBlocks(
                search = _uiState.value.searchQuery,
                grupoSiembra = _uiState.value.grupoSiembraFilter,
                ultimoMes = _uiState.value.soloUltimoMes
            )

            val kpis = kpisResult.getOrDefault(GlobalKpis())
            val blocks = blocksResult.getOrDefault(emptyList())

            var errorMsg: String? = null
            if (kpisResult.isFailure && blocksResult.isFailure) {
                val err = kpisResult.exceptionOrNull()?.message ?: blocksResult.exceptionOrNull()?.message
                errorMsg = "Error de conexión ($err)"
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

    fun loadForcedBlocks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getForcedBlocks(
                search = _uiState.value.searchQuery,
                grupoSiembra = _uiState.value.grupoSiembraFilter
            )
            val forced = res.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                forcedBlocks = forced,
                isLoading = false
            )
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onGrupoSiembraFilterChanged(grupo: String) {
        _uiState.value = _uiState.value.copy(grupoSiembraFilter = grupo)
        applyFilters()
    }

    fun toggleSoloUltimoMes(activo: Boolean) {
        _uiState.value = _uiState.value.copy(soloUltimoMes = activo)
        loadDashboardData()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val grupoFilter = _uiState.value.grupoSiembraFilter.lowercase().trim()

        val filteredUnforced = _uiState.value.unforcedBlocks.filter { block ->
            val matchesQuery = query.isEmpty() ||
                    block.bloque.lowercase().contains(query) ||
                    (block.descripcion?.lowercase()?.contains(query) == true)

            val matchesGrupo = grupoFilter.isEmpty() ||
                    (block.grupoSiembra?.lowercase()?.contains(grupoFilter) == true)

            matchesQuery && matchesGrupo
        }

        val filteredForced = _uiState.value.forcedBlocks.filter { block ->
            val matchesQuery = query.isEmpty() ||
                    block.bloque.lowercase().contains(query) ||
                    (block.descripcion?.lowercase()?.contains(query) == true) ||
                    (block.grupoForza?.lowercase()?.contains(query) == true)

            val matchesGrupo = grupoFilter.isEmpty() ||
                    (block.grupoSiembra?.lowercase()?.contains(grupoFilter) == true)

            matchesQuery && matchesGrupo
        }

        _uiState.value = _uiState.value.copy(
            filteredUnforcedBlocks = filteredUnforced,
            filteredForcedBlocks = filteredForced
        )
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
