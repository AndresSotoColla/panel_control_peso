package com.example.panel_control_peso.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panel_control_peso.data.model.*
import com.example.panel_control_peso.ui.components.GrowthCurveChart
import com.example.panel_control_peso.ui.viewmodel.*
import java.util.Locale

// PALETA OFICIAL SUAVE: Naranja, Negro, Gris y Beige con Letra Negra
val AgroSoftBeigeBg = Color(0xFFF8F6F0)
val AgroCardWhite = Color(0xFFFFFFFF)
val AgroCardBeige = Color(0xFFF3EFE6)
val AgroBorderSoft = Color(0xFFE2DCD0)
val AgroDarkCharcoal = Color(0xFF18181B)
val AgroOrangeAccent = Color(0xFFEA580C)
val AgroOrangeLight = Color(0xFFFFEDD5)
val AgroOrangeDark = Color(0xFFC2410C)
val AgroGrayText = Color(0xFF52525B)
val AgroGrayLight = Color(0xFFA1A1AA)
val AgroBlueSoft = Color(0xFF2563EB)
val AgroDangerSoft = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Control de Peso & Forzamiento",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroDarkCharcoal)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AgroDarkCharcoal) {
                NavigationBarItem(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Agriculture, contentDescription = null) },
                    label = { Text("Sin Forzar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroOrangeAccent,
                        selectedTextColor = AgroOrangeAccent,
                        unselectedIconColor = AgroGrayLight,
                        unselectedTextColor = AgroGrayLight,
                        indicatorColor = AgroDarkCharcoal
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Forzados", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroOrangeAccent,
                        selectedTextColor = AgroOrangeAccent,
                        unselectedIconColor = AgroGrayLight,
                        unselectedTextColor = AgroGrayLight,
                        indicatorColor = AgroDarkCharcoal
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                    label = { Text("Peso", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroOrangeAccent,
                        selectedTextColor = AgroOrangeAccent,
                        unselectedIconColor = AgroGrayLight,
                        unselectedTextColor = AgroGrayLight,
                        indicatorColor = AgroDarkCharcoal
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    label = { Text("Fitosanitario", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroOrangeAccent,
                        selectedTextColor = AgroOrangeAccent,
                        unselectedIconColor = AgroGrayLight,
                        unselectedTextColor = AgroGrayLight,
                        indicatorColor = AgroDarkCharcoal
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Ficha", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroOrangeAccent,
                        selectedTextColor = AgroOrangeAccent,
                        unselectedIconColor = AgroGrayLight,
                        unselectedTextColor = AgroGrayLight,
                        indicatorColor = AgroDarkCharcoal
                    )
                )
            }
        },
        containerColor = AgroSoftBeigeBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 2x2 Header Grid (No emoticons, no scroll)
                KpiGridHeaderSection(kpis = state.kpis)

                if (state.errorMessage != null) {
                    ErrorBanner(
                        message = state.errorMessage!!,
                        onRetry = { viewModel.loadDashboardData() }
                    )
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AgroOrangeAccent,
                        trackColor = AgroBorderSoft
                    )
                }

                // Main Content Body based on selected Tab
                Box(modifier = Modifier.weight(1f)) {
                    when (state.selectedTab) {
                        0 -> SinForzarTabContent(state = state, viewModel = viewModel)
                        4 -> ForzadosTabContent(state = state, viewModel = viewModel)
                        1 -> PesoTabContent(state = state, viewModel = viewModel)
                        2 -> FitosanitarioTabContent(state = state, viewModel = viewModel)
                        3 -> DetalleBloqueTabContent(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun KpiGridHeaderSection(kpis: GlobalKpis) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CleanKpiCard(
                title = "Sin Forzar",
                value = "${kpis.totalBloquesSinForzar} bloques",
                modifier = Modifier.weight(1f),
                accentColor = AgroOrangeAccent
            )
            CleanKpiCard(
                title = "Área Sin Forzar",
                value = "${format1Dec(kpis.areaTotalSinForzar / 10000)} ha",
                modifier = Modifier.weight(1f),
                accentColor = AgroDarkCharcoal
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CleanKpiCard(
                title = "Población Total",
                value = "${kpis.poblacionTotalSinForzar} plantas",
                modifier = Modifier.weight(1f),
                accentColor = AgroDarkCharcoal
            )
            CleanKpiCard(
                title = "Inducción Reciente",
                value = "${kpis.induccionUltimoMes} bloques (últimos 2 meses)",
                modifier = Modifier.weight(1f),
                accentColor = AgroOrangeAccent
            )
        }
    }
}

@Composable
fun CleanKpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AgroGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroDangerSoft.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroDangerSoft.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = AgroDangerSoft)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Aviso de Conexión", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AgroDarkCharcoal)
                Text(message, style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
            }
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AgroDarkCharcoal)
            }
        }
    }
}

@Composable
fun ViewModeSelector(
    selectedMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedMode == ViewMode.BLOQUE,
            onClick = { onModeSelected(ViewMode.BLOQUE) },
            label = { Text("Ver por Bloque", fontSize = 11.sp, color = if (selectedMode == ViewMode.BLOQUE) Color.White else AgroDarkCharcoal) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AgroOrangeAccent, containerColor = AgroCardWhite),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedMode == ViewMode.BLOQUE, borderColor = AgroBorderSoft),
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == ViewMode.GRUPO,
            onClick = { onModeSelected(ViewMode.GRUPO) },
            label = { Text("Ver por Grupo", fontSize = 11.sp, color = if (selectedMode == ViewMode.GRUPO) Color.White else AgroDarkCharcoal) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AgroDarkCharcoal, containerColor = AgroCardWhite),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedMode == ViewMode.GRUPO, borderColor = AgroBorderSoft),
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == ViewMode.LOTE,
            onClick = { onModeSelected(ViewMode.LOTE) },
            label = { Text("Ver por Lote", fontSize = 11.sp, color = if (selectedMode == ViewMode.LOTE) Color.White else AgroDarkCharcoal) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AgroGrayText, containerColor = AgroCardWhite),
            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedMode == ViewMode.LOTE, borderColor = AgroBorderSoft),
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinForzarTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        
        // Independent View Mode Selector for Sin Forzar
        ViewModeSelector(
            selectedMode = state.unforcedViewMode,
            onModeSelected = { viewModel.setUnforcedViewMode(it) }
        )

        // Independent Dual Inputs with Live Auto-Suggestions for Sin Forzar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AutoSuggestSearchField(
                value = state.unforcedSearchQuery,
                onValueChange = { viewModel.onUnforcedSearchQueryChanged(it) },
                placeholder = "Buscar Bloque...",
                suggestions = state.bloqueSuggestions,
                onSuggestionSelected = { viewModel.onUnforcedSearchQueryChanged(it) },
                modifier = Modifier.weight(1f)
            )

            AutoSuggestSearchField(
                value = if (state.unforcedViewMode == ViewMode.LOTE) state.unforcedLoteFilter else state.unforcedGrupoSiembraFilter,
                onValueChange = {
                    if (state.unforcedViewMode == ViewMode.LOTE) viewModel.onUnforcedLoteFilterChanged(it)
                    else viewModel.onUnforcedGrupoSiembraFilterChanged(it)
                },
                placeholder = if (state.unforcedViewMode == ViewMode.LOTE) "Filtrar Lote..." else "Grupo Siembra...",
                suggestions = if (state.unforcedViewMode == ViewMode.LOTE) state.loteSuggestions else state.grupoSiembraSuggestions,
                onSuggestionSelected = {
                    if (state.unforcedViewMode == ViewMode.LOTE) viewModel.onUnforcedLoteFilterChanged(it)
                    else viewModel.onUnforcedGrupoSiembraFilterChanged(it)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Filter Chip for Inducción Últimos 2 Meses
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                selected = state.unforcedSoloUltimoMes,
                onClick = { viewModel.toggleUnforcedSoloUltimoMes(!state.unforcedSoloUltimoMes) },
                label = { Text("Inducción Últimos 2 Meses", fontSize = 10.sp, color = if (state.unforcedSoloUltimoMes) Color.White else AgroDarkCharcoal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AgroOrangeAccent, containerColor = AgroCardWhite),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.unforcedSoloUltimoMes, borderColor = AgroBorderSoft)
            )
            Text(
                "Orden: Siembra más antigua",
                style = MaterialTheme.typography.labelSmall,
                color = AgroGrayText
            )
        }

        // Dynamic Content Body based on unforcedViewMode
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.unforcedViewMode) {
                ViewMode.BLOQUE -> {
                    items(state.filteredUnforcedBlocks) { block ->
                        CleanBlockCardItem(
                            block = block,
                            isSelected = block.bloque == state.selectedBlockName,
                            onClick = {
                                viewModel.selectBlock(block.bloque)
                                viewModel.selectTab(3)
                            }
                        )
                    }
                }
                ViewMode.GRUPO -> {
                    items(state.groupedBySiembraUnforced) { group ->
                        GroupSummaryCardItem(
                            group = group,
                            isSelected = group.name == state.selectedGroupName,
                            onClick = {
                                viewModel.selectGroup(group)
                                viewModel.selectTab(1)
                            }
                        )
                    }
                }
                ViewMode.LOTE -> {
                    items(state.groupedByLoteUnforced) { lote ->
                        LoteSummaryCardItem(
                            lote = lote,
                            isSelected = lote.name == state.selectedLoteName,
                            onClick = {
                                viewModel.selectLote(lote)
                                viewModel.selectTab(1)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForzadosTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {

        // Independent View Mode Selector for Forzados
        ViewModeSelector(
            selectedMode = state.forcedViewMode,
            onModeSelected = { viewModel.setForcedViewMode(it) }
        )

        // Independent Dual Inputs with Live Auto-Suggestions for Forzados
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AutoSuggestSearchField(
                value = state.forcedSearchQuery,
                onValueChange = { viewModel.onForcedSearchQueryChanged(it) },
                placeholder = "Buscar Bloque...",
                suggestions = state.bloqueSuggestions,
                onSuggestionSelected = { viewModel.onForcedSearchQueryChanged(it) },
                modifier = Modifier.weight(1f)
            )

            AutoSuggestSearchField(
                value = if (state.forcedViewMode == ViewMode.LOTE) state.forcedLoteFilter else state.forcedGrupoSiembraFilter,
                onValueChange = {
                    if (state.forcedViewMode == ViewMode.LOTE) viewModel.onForcedLoteFilterChanged(it)
                    else viewModel.onForcedGrupoSiembraFilterChanged(it)
                },
                placeholder = if (state.forcedViewMode == ViewMode.LOTE) "Filtrar Lote..." else "Grupo Siembra...",
                suggestions = if (state.forcedViewMode == ViewMode.LOTE) state.loteSuggestions else state.grupoSiembraSuggestions,
                onSuggestionSelected = {
                    if (state.forcedViewMode == ViewMode.LOTE) viewModel.onForcedLoteFilterChanged(it)
                    else viewModel.onForcedGrupoSiembraFilterChanged(it)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "Consulta de Grupos Forzados - Ordenado por Siembra Más Antigua",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = AgroOrangeAccent,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.forcedViewMode) {
                ViewMode.BLOQUE -> {
                    items(state.filteredForcedBlocks) { block ->
                        CleanForcedBlockCardItem(
                            block = block,
                            isSelected = block.bloque == state.selectedBlockName,
                            onClick = {
                                viewModel.selectBlock(block.bloque)
                                viewModel.selectTab(3)
                            }
                        )
                    }
                }
                ViewMode.GRUPO -> {
                    items(state.groupedBySiembraForced) { group ->
                        GroupSummaryCardItem(
                            group = group,
                            isSelected = group.name == state.selectedGroupName,
                            onClick = {
                                viewModel.selectGroup(group)
                                viewModel.selectTab(1)
                            }
                        )
                    }
                }
                ViewMode.LOTE -> {
                    items(state.groupedByLoteForced) { lote ->
                        LoteSummaryCardItem(
                            lote = lote,
                            isSelected = lote.name == state.selectedLoteName,
                            onClick = {
                                viewModel.selectLote(lote)
                                viewModel.selectTab(1)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSummaryCardItem(
    group: GroupSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AgroOrangeAccent else AgroBorderSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Grupo: ${group.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroDarkCharcoal
                )
                Text(
                    "${group.totalBloques} bloques",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AgroOrangeAccent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Área Total: ${format1Dec(group.totalArea / 10000)} ha", style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
                Text("Población: ${group.totalPoblacion} plantas", style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
            }
        }
    }
}

@Composable
fun LoteSummaryCardItem(
    lote: LoteSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AgroOrangeAccent else AgroBorderSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lote.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroDarkCharcoal
                )
                Text(
                    "${lote.totalBloques} bloques",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AgroOrangeAccent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Área Total: ${format1Dec(lote.totalArea / 10000)} ha", style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
                Text("Población: ${lote.totalPoblacion} plantas", style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
            }
        }
    }
}

@Composable
fun AutoSuggestSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            placeholder = { Text(placeholder, color = AgroGrayLight, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AgroCardWhite,
                unfocusedContainerColor = AgroCardWhite,
                focusedBorderColor = AgroOrangeAccent,
                unfocusedBorderColor = AgroBorderSoft,
                focusedTextColor = AgroDarkCharcoal,
                unfocusedTextColor = AgroDarkCharcoal
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        if (expanded && suggestions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Column {
                    suggestions.forEach { item ->
                        Text(
                            text = item,
                            color = AgroDarkCharcoal,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionSelected(item)
                                    expanded = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CleanBlockCardItem(
    block: UnforcedBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AgroOrangeAccent else AgroBorderSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${block.bloque} (Lote ${block.loteCalculado})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroDarkCharcoal
                )

                if (!block.grupoSiembra.isNull_or_empty()) {
                    Text(
                        "Grupo: ${block.grupoSiembra}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AgroOrangeAccent
                    )
                }
            }

            if (!block.descripcion.isNull_or_empty()) {
                Text(
                    block.descripcion ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroGrayText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Fecha Siembra", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text(block.fechaSiembra ?: "N/D", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
                Column {
                    Text("Fin Inducción", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text(block.finduccion ?: "Sin asignar", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
                Column {
                    Text("Área / Población", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text("${format1Dec(block.area)} m² (${block.poblacion ?: 0})", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
            }
        }
    }
}

@Composable
fun CleanForcedBlockCardItem(
    block: UnforcedBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AgroOrangeAccent else AgroBorderSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${block.bloque} (Lote ${block.loteCalculado})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroDarkCharcoal
                )

                Text(
                    "Grupo Forza: ${block.grupoForza ?: "N/D"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AgroOrangeAccent
                )
            }

            if (!block.descripcion.isNull_or_empty()) {
                Text(
                    block.descripcion ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroGrayText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Grupo Siembra", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text(block.grupoSiembra ?: "N/D", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
                Column {
                    Text("Fecha Siembra", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text(block.fechaSiembra ?: "N/D", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
                Column {
                    Text("Fin Inducción", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                    Text(block.finduccion ?: "N/D", style = MaterialTheme.typography.bodySmall, color = AgroDarkCharcoal)
                }
            }
        }
    }
}

@Composable
fun PesoTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val analytics = state.selectedWeightAnalytics

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        BlockSelectorHeader(state = state, onSelectBlock = { viewModel.selectBlock(it) })
        Spacer(modifier = Modifier.height(8.dp))

        if (analytics == null || analytics.totalMuestreos == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Selecciona un bloque o grupo con muestreos para ver el análisis y gráfica de peso.",
                    color = AgroGrayText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    GrowthCurveChart(series = analytics.serieHistorica)
                }
                item {
                    TrendSummaryCard(analytics = analytics)
                }
                item {
                    GrowthMetricsCard(analytics = analytics)
                }
                item {
                    Text(
                        "Curva Promedio por Edad en Meses (${analytics.serieHistorica.size} puntos)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AgroDarkCharcoal,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(analytics.serieHistorica) { entry ->
                    WeightSeriesItem(entry = entry)
                }
            }
        }
    }
}

@Composable
fun TrendSummaryCard(analytics: WeightAnalytics) {
    val tendenciaSegura = analytics.tendencia ?: "SIN_DATOS"

    val (trendText, trendColor, trendIcon) = when (tendenciaSegura) {
        "CRECIENDO_ACELERADO" -> Triple("Creciendo Acelerado", AgroOrangeAccent, Icons.Default.TrendingUp)
        "CRECIENDO_ESTABLE" -> Triple("Creciendo Estable", AgroOrangeAccent, Icons.Default.TrendingUp)
        "ESTABLE" -> Triple("Tasa Estable / Estancado", AgroGrayText, Icons.Default.TrendingFlat)
        "DISMINUYENDO" -> Triple("Disminuyendo de Peso", AgroDangerSoft, Icons.Default.TrendingDown)
        else -> Triple("Sin Datos", AgroGrayText, Icons.Default.Help)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Tendencia de Crecimiento", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                Text(trendText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AgroDarkCharcoal)
                Text(
                    "Desv. Estándar: σ = ${format1Dec(analytics.desviacionEstandarGeneral)} g | Muestreos: ${analytics.totalMuestreos}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroOrangeAccent
                )
            }
        }
    }
}

@Composable
fun GrowthMetricsCard(analytics: WeightAnalytics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Métricas de Ganancia de Peso",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AgroDarkCharcoal
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("Peso Inicial", "${format1Dec(analytics.pesoInicialG)} g", "Primer muestreo")
                MetricColumn("Peso Actual", "${format1Dec(analytics.pesoActualG)} g", "Último muestreo")
                MetricColumn("Ganancia Total", "+${format1Dec(analytics.gananciaTotalG)} g", "${format1Dec(analytics.porcentajeIncremento)}% inc.")
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, sub: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AgroDarkCharcoal)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = AgroOrangeAccent)
    }
}

@Composable
fun WeightSeriesItem(entry: WeightSeriesEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(if (entry.fecha.isNotEmpty()) entry.fecha else "Edad: ${format1Dec(entry.edadMeses)} m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AgroDarkCharcoal)
                Text(
                    "Edad Planta: ${format1Dec(entry.edadMeses)} meses (${entry.cantidadMuestras} muestras)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroOrangeAccent
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${format1Dec(entry.pesoPromedio)} g", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AgroOrangeAccent)
                Text(
                    "σ = ${format1Dec(entry.desviacionEstandar)} g",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgroGrayText
                )
            }
        }
    }
}

@Composable
fun FitosanitarioTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val phyto = state.selectedPhytosanitaryAnalytics

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        BlockSelectorHeader(state = state, onSelectBlock = { viewModel.selectBlock(it) })
        Spacer(modifier = Modifier.height(8.dp))

        if (phyto == null || phyto.totalPlantasMuestreadas == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Selecciona un bloque muestreado para analizar Fusarium y Plagas (%).",
                    color = AgroGrayText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    FusariumCard(phyto = phyto)
                }
                item {
                    PestPercentageBreakdownCard(plagas = phyto.plagas)
                }
            }
        }
    }
}

@Composable
fun FusariumCard(phyto: PhytosanitaryAnalytics) {
    val isHigh = phyto.porcentajeFusarium > 5.0
    val cardColor = if (isHigh) AgroDangerSoft else AgroOrangeAccent

    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Incidencia de Fusarium", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                Text(
                    "${format1Dec(phyto.porcentajeFusarium)}% Fusarium",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = cardColor
                )
                Text(
                    "${phyto.casosFusarium} de ${phyto.totalPlantasMuestreadas} plantas muestreadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroDarkCharcoal
                )
            }
        }
    }
}

@Composable
fun PestPercentageBreakdownCard(plagas: PestBreakdown) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Detección de Plagas (% Incidencia)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AgroDarkCharcoal
            )
            Spacer(modifier = Modifier.height(8.dp))

            val pestList = listOf(
                "Sinfílido" to plagas.sinfilido,
                "Caracol" to plagas.caracol,
                "Babosa" to plagas.babosa,
                "Hormiga" to plagas.hormiga,
                "Cochinilla" to plagas.cochinilla,
                "Gusano C. Roja" to plagas.gusanoCabezaRoja
            )

            pestList.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PestPercentageItem(name = pair[0].first, detail = pair[0].second, modifier = Modifier.weight(1f))
                    if (pair.size > 1) {
                        PestPercentageItem(name = pair[1].first, detail = pair[1].second, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PestPercentageItem(name: String, detail: PestItemDetail, modifier: Modifier = Modifier) {
    val hasPest = detail.casos > 0
    Row(
        modifier = modifier.padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "${detail.casos} (${format1Dec(detail.pct)}%)",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (hasPest) AgroDangerSoft else AgroDarkCharcoal
        )
    }
}

@Composable
fun DetalleBloqueTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val summary = state.selectedBlockSummary
    val groupSummary = state.selectedGroupSummary
    val loteSummary = state.selectedLoteSummary

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        BlockSelectorHeader(state = state, onSelectBlock = { viewModel.selectBlock(it) })
        Spacer(modifier = Modifier.height(8.dp))

        if (groupSummary != null) {
            // Group Technical Sheet (Ficha Agronómica de Grupo)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (state.selectedWeightAnalytics != null) {
                        GrowthCurveChart(series = state.selectedWeightAnalytics.serieHistorica)
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Ficha Técnica Consolidada - Grupo: ${groupSummary.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AgroOrangeAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            RowDetail("Total Bloques en Grupo", "${groupSummary.totalBloques} bloques")
                            RowDetail("Área Total Acumulada", "${format1Dec(groupSummary.totalArea / 10000)} ha (${format1Dec(groupSummary.totalArea)} m²)")
                            RowDetail("Población Total", "${groupSummary.totalPoblacion} plantas")
                        }
                    }
                }
                item {
                    Text(
                        "Bloques en el Grupo (${groupSummary.blocks.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AgroDarkCharcoal,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(groupSummary.blocks) { block ->
                    CleanBlockCardItem(
                        block = block,
                        isSelected = block.bloque == state.selectedBlockName,
                        onClick = { viewModel.selectBlock(block.bloque) }
                    )
                }
            }
        } else if (loteSummary != null) {
            // Lote Technical Sheet (Ficha Agronómica de Lote)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (state.selectedWeightAnalytics != null) {
                        GrowthCurveChart(series = state.selectedWeightAnalytics.serieHistorica)
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Ficha Técnica Consolidada - ${loteSummary.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AgroOrangeAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            RowDetail("Total Bloques en Lote", "${loteSummary.totalBloques} bloques")
                            RowDetail("Área Total Acumulada", "${format1Dec(loteSummary.totalArea / 10000)} ha (${format1Dec(loteSummary.totalArea)} m²)")
                            RowDetail("Población Total", "${loteSummary.totalPoblacion} plantas")
                        }
                    }
                }
                item {
                    Text(
                        "Bloques en el Lote (${loteSummary.blocks.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AgroDarkCharcoal,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(loteSummary.blocks) { block ->
                    CleanBlockCardItem(
                        block = block,
                        isSelected = block.bloque == state.selectedBlockName,
                        onClick = { viewModel.selectBlock(block.bloque) }
                    )
                }
            }
        } else if (summary != null) {
            // Individual Block Technical Sheet
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    GrowthCurveChart(series = summary.pesoAnalitica.serieHistorica)
                }
                item {
                    val agro = summary.agronomico
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Ficha Agronómica - ${summary.bloque}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AgroOrangeAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (agro != null) {
                                RowDetail("Lote (Dígitos 3 y 4)", "Lote ${agro.loteCalculado}")
                                RowDetail("Descripción", agro.descripcion ?: "N/D")
                                RowDetail("Desarrollo", agro.desarrollo ?: "N/D")
                                RowDetail("Población", "${agro.poblacion ?: 0} plantas")
                                RowDetail("Área", "${format1Dec(agro.area)} m²")
                                RowDetail("Grupo Siembra", agro.grupoSiembra ?: "N/D")
                                RowDetail("Fecha Siembra", agro.fechaSiembra ?: "N/D")
                                RowDetail("Fin Inducción", agro.finduccion ?: "N/D")
                                RowDetail("Grupo Forzamiento", agro.grupoForza ?: "SIN FORZAR (NULL)")
                                RowDetail("Días Pre-Forza", format1Dec(agro.diasPreforza))
                                RowDetail("Días Pos-Forza", format1Dec(agro.diasPosforza))
                            } else {
                                Text("Sin datos agronómicos en blocks_desarrollo.", color = AgroGrayText)
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Selecciona un bloque o grupo para ver la ficha completa.", color = AgroGrayText)
            }
        }
    }
}

@Composable
fun RowDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AgroGrayText)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AgroDarkCharcoal)
    }
}

@Composable
fun BlockSelectorHeader(
    state: DashboardUiState,
    onSelectBlock: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardWhite),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AgroBorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Seleccionado Actual", style = MaterialTheme.typography.labelSmall, color = AgroGrayText)
                Text(
                    if (state.selectedBlockName.isNotEmpty()) state.selectedBlockName else "Ninguno",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroOrangeAccent
                )
            }
        }
    }
}

fun format1Dec(value: Double?): String {
    if (value == null) return "0.0"
    return String.format(Locale.US, "%.1f", value)
}

fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
