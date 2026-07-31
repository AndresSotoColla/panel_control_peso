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

enum class ViewMode { BLOQUE, GRUPO, LOTE }

data class GroupSummary(
    val name: String,
    val totalBloques: Int,
    val totalArea: Double,
    val totalPoblacion: Long,
    val blocks: List<UnforcedBlock>
)

data class LoteSummary(
    val name: String,
    val totalBloques: Int,
    val totalArea: Double,
    val totalPoblacion: Long,
    val blocks: List<UnforcedBlock>
)

data class DashboardUiState(
    val kpis: GlobalKpis = GlobalKpis(),
    val unforcedBlocks: List<UnforcedBlock> = emptyList(),
    val forcedBlocks: List<UnforcedBlock> = emptyList(),
    
    val filteredUnforcedBlocks: List<UnforcedBlock> = emptyList(),
    val filteredForcedBlocks: List<UnforcedBlock> = emptyList(),

    val groupedBySiembraUnforced: List<GroupSummary> = emptyList(),
    val groupedByLoteUnforced: List<LoteSummary> = emptyList(),
    val groupedBySiembraForced: List<GroupSummary> = emptyList(),
    val groupedByLoteForced: List<LoteSummary> = emptyList(),
    
    // Auto-complete coincidences / suggestions
    val bloqueSuggestions: List<String> = emptyList(),
    val grupoSiembraSuggestions: List<String> = emptyList(),
    val loteSuggestions: List<String> = emptyList(),

    val selectedBlockSummary: BlockSummary? = null,
    val selectedGroupSummary: GroupSummary? = null,
    val selectedLoteSummary: LoteSummary? = null,
    val selectedWeightAnalytics: WeightAnalytics? = null,
    val selectedPhytosanitaryAnalytics: PhytosanitaryAnalytics? = null,
    
    val selectedTab: Int = 0,
    val viewMode: ViewMode = ViewMode.BLOQUE,
    val searchQuery: String = "",
    val grupoSiembraFilter: String = "",
    val loteFilter: String = "",
    val soloUltimoMes: Boolean = false,
    val selectedBlockName: String = "",
    val selectedGroupName: String = "",
    val selectedLoteName: String = "",
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

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
        applyFilters()
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
                lote = _uiState.value.loteFilter,
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
            updateSuggestions()
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
                grupoSiembra = _uiState.value.grupoSiembraFilter,
                lote = _uiState.value.loteFilter
            )
            val forced = res.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                forcedBlocks = forced,
                isLoading = false
            )
            updateSuggestions()
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateSuggestions()
        applyFilters()
    }

    fun onGrupoSiembraFilterChanged(grupo: String) {
        _uiState.value = _uiState.value.copy(grupoSiembraFilter = grupo)
        updateSuggestions()
        applyFilters()
    }

    fun onLoteFilterChanged(lote: String) {
        _uiState.value = _uiState.value.copy(loteFilter = lote)
        updateSuggestions()
        applyFilters()
    }

    fun toggleSoloUltimoMes(activo: Boolean) {
        _uiState.value = _uiState.value.copy(soloUltimoMes = activo)
        loadDashboardData()
    }

    private fun updateSuggestions() {
        val allBlocks = (_uiState.value.unforcedBlocks + _uiState.value.forcedBlocks)
        
        val qBlock = _uiState.value.searchQuery.lowercase().trim()
        val qGrupo = _uiState.value.grupoSiembraFilter.lowercase().trim()
        val qLote = _uiState.value.loteFilter.lowercase().trim()

        val blockSugg = if (qBlock.isNotEmpty()) {
            allBlocks.map { it.bloque }.distinct().filter { it.lowercase().contains(qBlock) }.take(6)
        } else emptyList()

        val grupoSugg = if (qGrupo.isNotEmpty()) {
            allBlocks.mapNotNull { it.grupoSiembra }.distinct().filter { it.lowercase().contains(qGrupo) }.take(6)
        } else emptyList()

        val loteSugg = if (qLote.isNotEmpty()) {
            allBlocks.map { it.loteCalculado }.distinct().filter { it.lowercase().contains(qLote) }.take(6)
        } else emptyList()

        _uiState.value = _uiState.value.copy(
            bloqueSuggestions = blockSugg,
            grupoSiembraSuggestions = grupoSugg,
            loteSuggestions = loteSugg
        )
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val grupoFilter = _uiState.value.grupoSiembraFilter.lowercase().trim()
        val loteFilter = _uiState.value.loteFilter.lowercase().trim()

        val filteredUnforced = _uiState.value.unforcedBlocks.filter { block ->
            val matchesQuery = query.isEmpty() ||
                    block.bloque.lowercase().contains(query) ||
                    (block.descripcion?.lowercase()?.contains(query) == true)

            val matchesGrupo = grupoFilter.isEmpty() ||
                    (block.grupoSiembra?.lowercase()?.contains(grupoFilter) == true)

            val matchesLote = loteFilter.isEmpty() ||
                    block.loteCalculado.lowercase().contains(loteFilter)

            matchesQuery && matchesGrupo && matchesLote
        }.sortedBy { it.fechaSiembra ?: "9999-99-99" }

        val filteredForced = _uiState.value.forcedBlocks.filter { block ->
            val matchesQuery = query.isEmpty() ||
                    block.bloque.lowercase().contains(query) ||
                    (block.descripcion?.lowercase()?.contains(query) == true) ||
                    (block.grupoForza?.lowercase()?.contains(query) == true)

            val matchesGrupo = grupoFilter.isEmpty() ||
                    (block.grupoSiembra?.lowercase()?.contains(grupoFilter) == true)

            val matchesLote = loteFilter.isEmpty() ||
                    block.loteCalculado.lowercase().contains(loteFilter)

            matchesQuery && matchesGrupo && matchesLote
        }.sortedBy { it.fechaSiembra ?: "9999-99-99" }

        // Compute Grouping Summaries
        val groupedSiembraUnforced = filteredUnforced.groupBy { it.grupoSiembra ?: "Sin Grupo" }
            .map { (name, bList) ->
                GroupSummary(
                    name = name,
                    totalBloques = bList.size,
                    totalArea = bList.sumOf { it.area ?: 0.0 },
                    totalPoblacion = bList.sumOf { it.poblacion ?: 0L },
                    blocks = bList
                )
            }

        val groupedLoteUnforced = filteredUnforced.groupBy { it.loteCalculado }
            .map { (name, bList) ->
                LoteSummary(
                    name = "Lote $name",
                    totalBloques = bList.size,
                    totalArea = bList.sumOf { it.area ?: 0.0 },
                    totalPoblacion = bList.sumOf { it.poblacion ?: 0L },
                    blocks = bList
                )
            }

        val groupedSiembraForced = filteredForced.groupBy { it.grupoSiembra ?: "Sin Grupo" }
            .map { (name, bList) ->
                GroupSummary(
                    name = name,
                    totalBloques = bList.size,
                    totalArea = bList.sumOf { it.area ?: 0.0 },
                    totalPoblacion = bList.sumOf { it.poblacion ?: 0L },
                    blocks = bList
                )
            }

        val groupedLoteForced = filteredForced.groupBy { it.loteCalculado }
            .map { (name, bList) ->
                LoteSummary(
                    name = "Lote $name",
                    totalBloques = bList.size,
                    totalArea = bList.sumOf { it.area ?: 0.0 },
                    totalPoblacion = bList.sumOf { it.poblacion ?: 0L },
                    blocks = bList
                )
            }

        _uiState.value = _uiState.value.copy(
            filteredUnforcedBlocks = filteredUnforced,
            filteredForcedBlocks = filteredForced,
            groupedBySiembraUnforced = groupedSiembraUnforced,
            groupedByLoteUnforced = groupedLoteUnforced,
            groupedBySiembraForced = groupedSiembraForced,
            groupedByLoteForced = groupedLoteForced
        )
    }

    fun selectBlock(bloque: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedBlockName = bloque,
                selectedGroupName = "",
                selectedLoteName = "",
                selectedGroupSummary = null,
                selectedLoteSummary = null,
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

    fun selectGroup(groupSummary: GroupSummary) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedGroupName = groupSummary.name,
                selectedGroupSummary = groupSummary,
                selectedBlockName = "Grupo: ${groupSummary.name}",
                selectedLoteName = "",
                selectedLoteSummary = null,
                isLoading = true
            )

            val weightRes = repository.getGroupWeightAnalytics(groupSummary.name)
            val firstBlock = groupSummary.blocks.firstOrNull()?.bloque
            val phytoRes = if (firstBlock != null) repository.getPhytosanitaryAnalytics(firstBlock) else null

            _uiState.value = _uiState.value.copy(
                selectedWeightAnalytics = weightRes.getOrNull(),
                selectedPhytosanitaryAnalytics = phytoRes?.getOrNull(),
                isLoading = false
            )
        }
    }

    fun selectLote(loteSummary: LoteSummary) {
        viewModelScope.launch {
            val rawLote = loteSummary.name.removePrefix("Lote ").trim()
            _uiState.value = _uiState.value.copy(
                selectedLoteName = loteSummary.name,
                selectedLoteSummary = loteSummary,
                selectedBlockName = loteSummary.name,
                selectedGroupName = "",
                selectedGroupSummary = null,
                isLoading = true
            )

            val firstBlock = loteSummary.blocks.firstOrNull()?.bloque
            val weightRes = if (firstBlock != null) repository.getWeightAnalytics(firstBlock) else null
            val phytoRes = if (firstBlock != null) repository.getPhytosanitaryAnalytics(firstBlock) else null

            _uiState.value = _uiState.value.copy(
                selectedWeightAnalytics = weightRes?.getOrNull(),
                selectedPhytosanitaryAnalytics = phytoRes?.getOrNull(),
                isLoading = false
            )
        }
    }
}
