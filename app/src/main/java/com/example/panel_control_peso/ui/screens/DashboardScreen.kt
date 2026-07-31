package com.example.panel_control_peso.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.panel_control_peso.data.model.*
import com.example.panel_control_peso.ui.viewmodel.DashboardUiState
import com.example.panel_control_peso.ui.viewmodel.DashboardViewModel

val AgroGreenDark = Color(0xFF0F172A)
val AgroCardBg = Color(0xFF1E293B)
val AgroAccent = Color(0xFF10B981)
val AgroWarning = Color(0xFFF59E0B)
val AgroDanger = Color(0xFFEF4444)
val AgroBlue = Color(0xFF3B82F6)
val AgroTextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Control de Peso & Forzamiento",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            if (state.isDirectDbMode) "🌐 Conexión Directa AWS RDS (Global 4G/5G/WiFi)" else "⚡ Servidor API REST",
                            style = MaterialTheme.typography.labelSmall,
                            color = AgroAccent
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                    IconButton(onClick = { showServerDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar Servidor", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroGreenDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AgroGreenDark) {
                NavigationBarItem(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Agriculture, contentDescription = null) },
                    label = { Text("Forzamiento", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroAccent,
                        selectedTextColor = AgroAccent,
                        unselectedIconColor = AgroTextMuted,
                        unselectedTextColor = AgroTextMuted,
                        indicatorColor = AgroCardBg
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                    label = { Text("Peso & Tasa", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroAccent,
                        selectedTextColor = AgroAccent,
                        unselectedIconColor = AgroTextMuted,
                        unselectedTextColor = AgroTextMuted,
                        indicatorColor = AgroCardBg
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    label = { Text("Fitosanitario", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroAccent,
                        selectedTextColor = AgroAccent,
                        unselectedIconColor = AgroTextMuted,
                        unselectedTextColor = AgroTextMuted,
                        indicatorColor = AgroCardBg
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Ficha Bloque", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AgroAccent,
                        selectedTextColor = AgroAccent,
                        unselectedIconColor = AgroTextMuted,
                        unselectedTextColor = AgroTextMuted,
                        indicatorColor = AgroCardBg
                    )
                )
            }
        },
        containerColor = AgroGreenDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // KPI Header Banner
                KpiHeaderSection(kpis = state.kpis)

                if (state.errorMessage != null) {
                    ErrorBanner(
                        message = state.errorMessage!!,
                        onRetry = { viewModel.loadDashboardData() },
                        onConfigServer = { showServerDialog = true }
                    )
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AgroAccent,
                        trackColor = AgroCardBg
                    )
                }

                // Main Content Body based on selected Tab
                Box(modifier = Modifier.weight(1f)) {
                    when (state.selectedTab) {
                        0 -> ForzamientoTabContent(state = state, viewModel = viewModel)
                        1 -> PesoTabContent(state = state, viewModel = viewModel)
                        2 -> FitosanitarioTabContent(state = state, viewModel = viewModel)
                        3 -> DetalleBloqueTabContent(state = state, viewModel = viewModel)
                    }
                }
            }

            if (showServerDialog) {
                ServerUrlDialog(
                    currentUrl = state.currentServerUrl,
                    isDirectDbMode = state.isDirectDbMode,
                    onDismiss = { showServerDialog = false },
                    onToggleDirectDb = { useDirect ->
                        viewModel.toggleConnectionMode(useDirect)
                        showServerDialog = false
                    },
                    onConfirmApiUrl = { newUrl ->
                        viewModel.setServerUrl(newUrl)
                        showServerDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun KpiHeaderSection(kpis: GlobalKpis) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            KpiCard(
                title = "Sin Forzar",
                value = "${kpis.totalBloquesSinForzar}",
                subtitle = "Bloques pendientes",
                icon = Icons.Default.PendingActions,
                color = AgroAccent
            )
        }
        item {
            KpiCard(
                title = "Forza Urgente",
                value = "${kpis.forzamientoUrgente}",
                subtitle = "< 30 días a inducción",
                icon = Icons.Default.Warning,
                color = AgroWarning
            )
        }
        item {
            KpiCard(
                title = "Área Sin Forzar",
                value = "${String.format("%.1f", kpis.areaTotalSinForzar / 10000)} ha",
                subtitle = "${kpis.areaTotalSinForzar} m²",
                icon = Icons.Default.SquareFoot,
                color = AgroBlue
            )
        }
        item {
            KpiCard(
                title = "Bloques Muestreados",
                value = "${kpis.totalBloquesMuestreados}",
                subtitle = "En base de pesaje",
                icon = Icons.Default.MonitorWeight,
                color = Color(0xFFA855F7)
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(155.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = AgroTextMuted
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AgroTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onConfigServer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroDanger.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = AgroDanger)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Error de Conexión", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(message, style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
            }
            TextButton(onClick = onConfigServer) {
                Text("Config IP", color = AgroAccent, fontSize = 12.sp)
            }
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForzamientoTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Search & Filter Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Buscar bloque o descripción...", color = AgroTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AgroAccent) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AgroCardBg,
                unfocusedContainerColor = AgroCardBg,
                focusedBorderColor = AgroAccent,
                unfocusedBorderColor = AgroCardBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("TODOS", "URGENTE", "PROXIMO", "NORMAL")
            categories.forEach { cat ->
                val selected = state.categoryFilter == cat
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onCategoryFilterChanged(cat) },
                    label = { Text(cat, fontSize = 11.sp, color = if (selected) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AgroAccent,
                        containerColor = AgroCardBg
                    ),
                    border = null
                )
            }
        }

        // List of blocks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.filteredBlocks) { block ->
                BlockCardItem(
                    block = block,
                    isSelected = block.bloque == state.selectedBlockName,
                    onClick = {
                        viewModel.selectBlock(block.bloque)
                        viewModel.selectTab(3) // Switch to Detail tab
                    }
                )
            }
        }
    }
}

@Composable
fun BlockCardItem(
    block: UnforcedBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = when (block.categoriaForzamiento) {
        "URGENTE" -> AgroDanger
        "PROXIMO" -> AgroWarning
        "NORMAL" -> AgroAccent
        else -> AgroTextMuted
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) AgroCardBg.copy(alpha = 0.9f) else AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) AgroAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        block.bloque,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Badge(
                    containerColor = categoryColor.copy(alpha = 0.2f),
                    contentColor = categoryColor
                ) {
                    Text(
                        text = block.categoriaForzamiento ?: "SIN FECHA",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!block.descripcion.isNull_or_empty()) {
                Text(
                    block.descripcion ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Fecha Siembra", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                    Text(block.fechaSiembra ?: "N/D", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Column {
                    Text("Fin Inducción", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                    Text(block.finduccion ?: "Sin asignar", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Column {
                    Text("Días a Forza", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                    Text(
                        text = if (block.diasHastaInduccion != null) "${block.diasHastaInduccion} d" else "N/D",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = categoryColor
                    )
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
        // Block selector header
        BlockSelectorHeader(state = state, onSelectBlock = { viewModel.selectBlock(it) })

        Spacer(modifier = Modifier.height(10.dp))

        if (analytics == null || analytics.totalMuestreos == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Selecciona un bloque con muestreos de peso para ver el análisis de crecimiento.",
                    color = AgroTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    // Trend Summary Card
                    TrendSummaryCard(analytics = analytics)
                }

                item {
                    // Growth Rate Metrics Card
                    GrowthMetricsCard(analytics = analytics)
                }

                item {
                    Text(
                        "Historial de Pesajes por Fecha (${analytics.serieHistorica.size} muestreos)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
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
    val (trendText, trendColor, trendIcon) = when (analytics.tendencia) {
        "CRECIENDO_ACELERADO" -> Triple("🟢 Creciendo Acelerado", AgroAccent, Icons.Default.TrendingUp)
        "CRECIENDO_ESTABLE" -> Triple("🟢 Creciendo Estable", AgroAccent, Icons.Default.TrendingUp)
        "ESTABLE" -> Triple("🟡 Tasa Estable / Estancado", AgroWarning, Icons.Default.TrendingFlat)
        "DISMINUYENDO" -> Triple("🔴 Disminuyendo de Peso", AgroDanger, Icons.Default.TrendingDown)
        else -> Triple("⚪ Sin Datos", AgroTextMuted, Icons.Default.Help)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Tendencia de Crecimiento", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                Text(trendText, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(
                    "Tasa de ganancia diaria: ${analytics.tasaCrecimientoDiarioGDia} g/día",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroAccent
                )
            }
        }
    }
}

@Composable
fun GrowthMetricsCard(analytics: WeightAnalytics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Métricas de Ganancia de Peso",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("Peso Inicial", "${analytics.pesoInicialG} g", "Primer muestreo")
                MetricColumn("Peso Actual", "${analytics.pesoActualG} g", "Último muestreo")
                MetricColumn("Ganancia Total", "+${analytics.gananciaTotalG} g", "${analytics.porcentajeIncremento}% increment.")
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, sub: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = AgroAccent)
    }
}

@Composable
fun WeightSeriesItem(entry: WeightSeriesEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.fecha, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("${entry.cantidadMuestras} plantas muestreadas", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.pesoPromedio} g", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AgroAccent)
                if (entry.pesoMin != null && entry.pesoMax != null) {
                    Text("Rango: ${entry.pesoMin}g - ${entry.pesoMax}g", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                }
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
        Spacer(modifier = Modifier.height(10.dp))

        if (phyto == null || phyto.totalPlantasMuestreadas == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Selecciona un bloque muestreado para analizar Fusarium y Plagas.",
                    color = AgroTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    // Fusarium Card
                    FusariumCard(phyto = phyto)
                }

                item {
                    // Pest Breakdown Card
                    PestBreakdownCard(plagas = phyto.plagas)
                }

                item {
                    // Root system breakdown
                    RootSystemCard(roots = phyto.sistemasRadiculares)
                }
            }
        }
    }
}

@Composable
fun FusariumCard(phyto: PhytosanitaryAnalytics) {
    val isHigh = phyto.porcentajeFusarium > 5.0
    val cardColor = if (isHigh) AgroDanger else AgroAccent

    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Coronavirus, contentDescription = null, tint = cardColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Incidencia de Fusarium", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                Text(
                    "${phyto.porcentajeFusarium}% Fusarium",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = cardColor
                )
                Text(
                    "${phyto.casosFusarium} de ${phyto.totalPlantasMuestreadas} plantas muestreadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PestBreakdownCard(plagas: PestBreakdown) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Detección de Plagas y Novedades",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                    PestItem(name = pair[0].first, count = pair[0].second, modifier = Modifier.weight(1f))
                    if (pair.size > 1) {
                        PestItem(name = pair[1].first, count = pair[1].second, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PestItem(name: String, count: Int, modifier: Modifier = Modifier) {
    val hasPest = count > 0
    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (hasPest) AgroDanger else AgroAccent)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = AgroTextMuted)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "$count",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (hasPest) AgroDanger else Color.White
        )
    }
}

@Composable
fun RootSystemCard(roots: List<RootSystemEntry>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Sistema Radicular",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (roots.isEmpty()) {
                Text("Sin registros de sistema radicular.", style = MaterialTheme.typography.bodySmall, color = AgroTextMuted)
            } else {
                roots.forEach { root ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(root.tipoSistemaRadicularId ?: "No especificado", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text("${root.cantidad} plantas", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AgroAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun DetalleBloqueTabContent(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    val summary = state.selectedBlockSummary

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        BlockSelectorHeader(state = state, onSelectBlock = { viewModel.selectBlock(it) })
        Spacer(modifier = Modifier.height(10.dp))

        if (summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Selecciona un bloque para ver la ficha completa.", color = AgroTextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    val agro = summary.agronomico
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Ficha Agronómica - ${summary.bloque}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AgroAccent
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (agro != null) {
                                RowDetail("Descripción", agro.descripcion ?: "N/D")
                                RowDetail("Desarrollo", agro.desarrollo ?: "N/D")
                                RowDetail("Población", "${agro.poblacion ?: 0} plantas")
                                RowDetail("Área", "${agro.area ?: 0.0} m²")
                                RowDetail("Grupo Siembra", agro.grupoSiembra ?: "N/D")
                                RowDetail("Fecha Siembra", agro.fechaSiembra ?: "N/D")
                                RowDetail("Fin Inducción", agro.finduccion ?: "N/D")
                                RowDetail("Grupo Forzamiento", agro.grupoForza ?: "SIN FORZAR (NULL)")
                                RowDetail("Días Pre-Forza", "${agro.diasPreforza ?: '0'}")
                                RowDetail("Días Pos-Forza", "${agro.diasPosforza ?: '0'}")
                            } else {
                                Text("Sin datos agronómicos en blocks_desarrollo.", color = AgroTextMuted)
                            }
                        }
                    }
                }
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = AgroTextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun BlockSelectorHeader(
    state: DashboardUiState,
    onSelectBlock: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgroCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Bloque Seleccionado", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                Text(
                    if (state.selectedBlockName.isNotEmpty()) state.selectedBlockName else "Ninguno",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AgroAccent
                )
            }
        }
    }
}

@Composable
fun ServerUrlDialog(
    currentUrl: String,
    isDirectDbMode: Boolean,
    onDismiss: () -> Unit,
    onToggleDirectDb: (Boolean) -> Unit,
    onConfirmApiUrl: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentUrl) }
    var useDirect by remember { mutableStateOf(isDirectDbMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modo de Conexión (Global)", color = Color.White) },
        text = {
            Column {
                Text(
                    "Selecciona el modo de consulta a los datos:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroTextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Direct Mode Option
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (useDirect) AgroAccent.copy(alpha = 0.2f) else AgroCardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useDirect = true }
                        .border(
                            width = if (useDirect) 2.dp else 0.dp,
                            color = if (useDirect) AgroAccent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🌐 Conexión Directa AWS RDS (Global)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Funciona desde CUALQUIER lugar (4G/5G/WiFi) sin necesidad de tener un servidor encendido.", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // API Server Option
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (!useDirect) AgroAccent.copy(alpha = 0.2f) else AgroCardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useDirect = false }
                        .border(
                            width = if (!useDirect) 2.dp else 0.dp,
                            color = if (!useDirect) AgroAccent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("⚡ Servidor API REST (Python FastAPI)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Conexión mediante API local, Ngrok o servidor en la nube.", style = MaterialTheme.typography.labelSmall, color = AgroTextMuted)
                        if (!useDirect) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                singleLine = true,
                                label = { Text("URL de la API", color = AgroTextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (useDirect) {
                        onToggleDirectDb(true)
                    } else {
                        onConfirmApiUrl(text)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AgroAccent)
            ) {
                Text("Guardar Modo", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = AgroGreenDark
    )
}

fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
