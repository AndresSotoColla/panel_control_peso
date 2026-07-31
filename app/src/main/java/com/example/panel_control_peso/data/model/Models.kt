package com.example.panel_control_peso.data.model

import com.google.gson.annotations.SerializedName

data class GlobalKpis(
    @SerializedName("total_bloques_sin_forzar") val totalBloquesSinForzar: Int = 0,
    @SerializedName("area_total_sin_forzar") val areaTotalSinForzar: Double = 0.0,
    @SerializedName("poblacion_total_sin_forzar") val poblacionTotalSinForzar: Long = 0,
    @SerializedName("induccion_ultimo_mes") val induccionUltimoMes: Int = 0,
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
    @SerializedName("lote") val lote: String? = null,
    @SerializedName("dias_hasta_induccion") val diasHastaInduccion: Int?,
    @SerializedName("categoria_forzamiento") val categoriaForzamiento: String?
) {
    // Extract Lote if null (digits 3 and 4 of bloque, e.g. PC123456 -> Lote 12)
    val loteCalculado: String
        get() {
            if (!lote.isNullOrEmpty()) return lote!!
            if (bloque.length >= 4) {
                return bloque.substring(2, 4)
            }
            return "N/A"
        }
}

data class WeightSeriesEntry(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("cantidad_muestras") val cantidadMuestras: Int,
    @SerializedName("peso_promedio") val pesoPromedio: Double,
    @SerializedName("desviacion_estandar") val desviacionEstandar: Double = 0.0,
    @SerializedName("edad_meses") val edadMeses: Double = 0.0,
    @SerializedName("peso_min") val pesoMin: Double?,
    @SerializedName("peso_max") val pesoMax: Double?
)

data class WeightAnalytics(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("total_muestreos") val totalMuestreos: Int = 0,
    @SerializedName("fecha_primer_muestreo") val fechaPrimerMuestreo: String? = null,
    @SerializedName("fecha_ultimo_muestreo") val fechaUltimoMuestreo: String? = null,
    @SerializedName("dias_monitoreados") val diasMonitoreados: Int = 0,
    @SerializedName("desviacion_estandar_general") val desviacionEstandarGeneral: Double = 0.0,
    @SerializedName("peso_inicial_g") val pesoInicialG: Double = 0.0,
    @SerializedName("peso_actual_g") val pesoActualG: Double = 0.0,
    @SerializedName("ganancia_total_g") val gananciaTotalG: Double = 0.0,
    @SerializedName("tasa_crecimiento_diario_g_dia") val tasaCrecimientoDiarioGDia: Double = 0.0,
    @SerializedName("porcentaje_incremento") val porcentajeIncremento: Double = 0.0,
    @SerializedName("tendencia") val tendencia: String = "SIN_DATOS",
    @SerializedName("serie_historica") val serieHistorica: List<WeightSeriesEntry> = emptyList()
)

data class PestItemDetail(
    @SerializedName("casos") val casos: Int = 0,
    @SerializedName("pct") val pct: Double = 0.0
)

data class PestBreakdown(
    @SerializedName("sinfilido") val sinfilido: PestItemDetail = PestItemDetail(),
    @SerializedName("caracol") val caracol: PestItemDetail = PestItemDetail(),
    @SerializedName("babosa") val babosa: PestItemDetail = PestItemDetail(),
    @SerializedName("hormiga") val hormiga: PestItemDetail = PestItemDetail(),
    @SerializedName("cochinilla") val cochinilla: PestItemDetail = PestItemDetail(),
    @SerializedName("gusano_cabeza_roja") val gusanoCabezaRoja: PestItemDetail = PestItemDetail()
)

data class PhytosanitaryAnalytics(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("total_plantas_muestreadas") val totalPlantasMuestreadas: Int = 0,
    @SerializedName("casos_fusarium") val casosFusarium: Int = 0,
    @SerializedName("porcentaje_fusarium") val porcentajeFusarium: Double = 0.0,
    @SerializedName("plagas") val plagas: PestBreakdown = PestBreakdown()
)

data class BlockSummary(
    @SerializedName("bloque") val bloque: String,
    @SerializedName("agronomico") val agronomico: UnforcedBlock?,
    @SerializedName("peso_analitica") val pesoAnalitica: WeightAnalytics,
    @SerializedName("fitosanitario") val fitosanitario: PhytosanitaryAnalytics
)
