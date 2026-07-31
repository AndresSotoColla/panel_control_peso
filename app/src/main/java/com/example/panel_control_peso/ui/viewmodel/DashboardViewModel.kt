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

    // INDEPENDENT FILTERS FOR "SIN FORZAR" TAB
    val unforcedSearchQuery: String = "",
    val unforcedGrupoSiembraFilter: String = "",
    val unforcedLoteFilter: String = "",
    val unforcedViewMode: ViewMode = ViewMode.BLOQUE,
    val unforcedSoloUltimoMes: Boolean = false,
    
    // INDEPENDENT FILTERS FOR "FORZADOS" TAB
    val forcedSearchQuery: String = "",
    val forcedGrupoSiembraFilter: String = "",
    val forcedLoteFilter: String = "",
    val forcedViewMode: ViewMode = ViewMode.BLOQUE,

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

    fun setUnforcedViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(unforcedViewMode = mode)
        applyUnforcedFilters()
    }

    fun setForcedViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(forcedViewMode = mode)
        applyForcedFilters()
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
                search = _uiState.value.unforcedSearchQuery,
                grupoSiembra = _uiState.value.unforcedGrupoSiembraFilter,
                lote = _uiState.value.unforcedLoteFilter,
                ultimoMes = _uiState.value.unforcedSoloUltimoMes
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
            applyUnforcedFilters()

            if (blocks.isNotEmpty() && _uiState.value.selectedBlockName.isEmpty()) {
                selectBlock(blocks.first().bloque)
            }
        }
    }

    fun loadForcedBlocks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getForcedBlocks(
                search = _uiState.value.forcedSearchQuery,
                grupoSiembra = _uiState.value.forcedGrupoSiembraFilter,
                lote = _uiState.value.forcedLoteFilter
            )
            val forced = res.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                forcedBlocks = forced,
                isLoading = false
            )
            updateSuggestions()
            applyForcedFilters()
        }
    }

    // INDEPENDENT UNFORCED FILTERS
    fun onUnforcedSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(unforcedSearchQuery = query)
        updateSuggestions()
        applyUnforcedFilters()
    }

    fun onUnforcedGrupoSiembraFilterChanged(grupo: String) {
        _uiState.value = _uiState.value.copy(unforcedGrupoSiembraFilter = grupo)
        updateSuggestions()
        applyUnforcedFilters()
    }

    fun onUnforcedLoteFilterChanged(lote: String) {
        _uiState.value = _uiState.value.copy(unforcedLoteFilter = lote)
        updateSuggestions()
        applyUnforcedFilters()
    }

    fun toggleUnforcedSoloUltimoMes(activo: Boolean) {
        _uiState.value = _uiState.value.copy(unforcedSoloUltimoMes = activo)
        loadDashboardData()
    }

    // INDEPENDENT FORCED FILTERS
    fun onForcedSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(forcedSearchQuery = query)
        updateSuggestions()
        applyForcedFilters()
    }

    fun onForcedGrupoSiembraFilterChanged(grupo: String) {
        _uiState.value = _uiState.value.copy(forcedGrupoSiembraFilter = grupo)
        updateSuggestions()
        applyForcedFilters()
    }

    fun onForcedLoteFilterChanged(lote: String) {
        _uiState.value = _uiState.value.copy(forcedLoteFilter = lote)
        updateSuggestions()
        applyForcedFilters()
    }

    private fun updateSuggestions() {
        val allBlocks = (_uiState.value.unforcedBlocks + _uiState.value.forcedBlocks)
        
        val qBlock = if (_uiState.value.selectedTab == 4) _uiState.value.forcedSearchQuery.lowercase().trim() else _uiState.value.unforcedSearchQuery.lowercase().trim()
        val qGrupo = if (_uiState.value.selectedTab == 4) _uiState.value.forcedGrupoSiembraFilter.lowercase().trim() else _uiState.value.unforcedGrupoSiembraFilter.lowercase().trim()
        val qLote = if (_uiState.value.selectedTab == 4) _uiState.value.forcedLoteFilter.lowercase().trim() else _uiState.value.unforcedLoteFilter.lowercase().trim()

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

    private fun applyUnforcedFilters() {
        val query = _uiState.value.unforcedSearchQuery.lowercase().trim()
        val grupoFilter = _uiState.value.unforcedGrupoSiembraFilter.lowercase().trim()
        val loteFilter = _uiState.value.unforcedLoteFilter.lowercase().trim()

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

        _uiState.value = _uiState.value.copy(
            filteredUnforcedBlocks = filteredUnforced,
            groupedBySiembraUnforced = groupedSiembraUnforced,
            groupedByLoteUnforced = groupedLoteUnforced
        )
    }

    private fun applyForcedFilters() {
        val query = _uiState.value.forcedSearchQuery.lowercase().trim()
        val grupoFilter = _uiState.value.forcedGrupoSiembraFilter.lowercase().trim()
        val loteFilter = _uiState.value.forcedLoteFilter.lowercase().trim()

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
            filteredForcedBlocks = filteredForced,
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

            val weightRes = repository.getLoteWeightAnalytics(rawLote)
            val firstBlock = loteSummary.blocks.firstOrNull()?.bloque
            val phytoRes = if (firstBlock != null) repository.getPhytosanitaryAnalytics(firstBlock) else null

            _uiState.value = _uiState.value.copy(
                selectedWeightAnalytics = weightRes.getOrNull(),
                selectedPhytosanitaryAnalytics = phytoRes?.getOrNull(),
                isLoading = false
            )
        }
    }
}
