package com.example.panel_control_peso.data.model

import com.google.gson.annotations.SerializedName

data class GlobalKpis(
    @SerializedName("total_bloques_sin_forzar") val totalBloquesSinForzar: Int = 0,
    @SerializedName("area_total_sin_forzar") val areaTotalSinForzar: Double = 0.0,
    @SerializedName("poblacion_total_sin_forzar") val poblacionTotalSinForzar: Long = 0,
    @SerializedName("forzamiento_urgente") val forzamientoUrgente: Int = 0,
    @SerializedName("total_bloques_muestreados") val totalBloquesMuestreados: Int = 0
)

data class UnforcedBlock(
    @SerializedName("blocknumber") val blocknumber: String?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("desarrollo") val desarrollo: String?,
    @SerializedName("bloque") val bloque: String,
    @SerializedName("poblacion") val poblacion: Long?,
    @SerializedName("area") val area: Double?,
    @SerializedName("drenajes") val drenajes: Double?,
    @SerializedName("grupo_siembra") val grupoSiembra: String?,
    @SerializedName("fecha_siembra") val fechaSiembra: String?,
    @SerializedName("grupo_forza") val grupoForza: String?,
    @SerializedName("finduccion") val finduccion: String?,
    @SerializedName("grupo_semillero") val grupoSemillero: String?,
    @SerializedName("mediana_fecha_cosecha") val medianaFechaCosecha: String?,
    @SerializedName("kilos_cosechados") val kilosCosechados: Double?,
    @SerializedName("frutas") val frutas: Double?,
    @SerializedName("dias_preforza") val diasPreforza: Double?,
    @SerializedName("dias_posforza") val diasPosforza: Double?,
    @SerializedName("dias_hasta_induccion") val diasHastaInduccion: Int?,
    @SerializedName("categoria_forzamiento") val categoriaForzamiento: String?
)

data class WeightSeriesEntry(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("cantidad_muestras") val cantidadMuestras: Int,
    @SerializedName("peso_promedio") val pesoPromedio: Double,
    @SerializedName("peso_min") val pesoMin: Double?,
    @SerializedName("peso_max") val pesoMax: Double?
)

data class WeightAnalytics(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("total_muestreos") val totalMuestreos: Int = 0,
    @SerializedName("fecha_primer_muestreo") val fechaPrimerMuestreo: String? = null,
    @SerializedName("fecha_ultimo_muestreo") val fechaUltimoMuestreo: String? = null,
    @SerializedName("dias_monitoreados") val diasMonitoreados: Int = 0,
    @SerializedName("peso_inicial_g") val pesoInicialG: Double = 0.0,
    @SerializedName("peso_actual_g") val pesoActualG: Double = 0.0,
    @SerializedName("ganancia_total_g") val gananciaTotalG: Double = 0.0,
    @SerializedName("tasa_crecimiento_diario_g_dia") val tasaCrecimientoDiarioGDia: Double = 0.0,
    @SerializedName("porcentaje_incremento") val porcentajeIncremento: Double = 0.0,
    @SerializedName("tendencia") val tendencia: String = "SIN_DATOS",
    @SerializedName("serie_historica") val serieHistorica: List<WeightSeriesEntry> = emptyList()
)

data class PestBreakdown(
    @SerializedName("sinfilido") val sinfilido: Int = 0,
    @SerializedName("caracol") val caracol: Int = 0,
    @SerializedName("babosa") val babosa: Int = 0,
    @SerializedName("hormiga") val hormiga: Int = 0,
    @SerializedName("cochinilla") val cochinilla: Int = 0,
    @SerializedName("gusano_cabeza_roja") val gusanoCabezaRoja: Int = 0
)

data class RootSystemEntry(
    @SerializedName("tipo_sistema_radicular_id") val tipoSistemaRadicularId: String?,
    @SerializedName("cantidad") val cantidad: Int
)

data class PhytosanitaryAnalytics(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("total_plantas_muestreadas") val totalPlantasMuestreadas: Int = 0,
    @SerializedName("casos_fusarium") val casosFusarium: Int = 0,
    @SerializedName("porcentaje_fusarium") val porcentajeFusarium: Double = 0.0,
    @SerializedName("plagas") val plagas: PestBreakdown = PestBreakdown(),
    @SerializedName("sistemas_radiculares") val sistemasRadiculares: List<RootSystemEntry> = emptyList()
)

data class BlockSummary(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("agronomico") val agronomico: UnforcedBlock?,
    @SerializedName("peso_analitica") val pesoAnalitica: WeightAnalytics,
    @SerializedName("fitosanitario") val fitosanitario: PhytosanitaryAnalytics
)
