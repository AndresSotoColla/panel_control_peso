package com.example.panel_control_peso.data.repository

import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties
import kotlin.math.round

class DirectDbRepository {

    private val dbUrl = "jdbc:postgresql://guapa-db.cthhnvwvqxrw.us-east-2.rds.amazonaws.com:5432/postgres"
    private val dbUser = "postgres"
    private val dbPassword = "GuapaBI.2023*"

    private fun getConnection(): Connection {
        Class.forName("org.postgresql.Driver")
        DriverManager.setLoginTimeout(10)

        val props = Properties()
        props.setProperty("user", dbUser)
        props.setProperty("password", dbPassword)
        props.setProperty("loginTimeout", "10")
        props.setProperty("connectTimeout", "10")
        props.setProperty("socketTimeout", "15")
        
        props.setProperty("ssl", "true")
        props.setProperty("sslmode", "prefer")
        props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory")

        return DriverManager.getConnection(dbUrl, props)
    }

    suspend fun getGlobalKpis(): Result<GlobalKpis> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt1 = conn.prepareStatement("""
                    SELECT 
                        COUNT(*) as total_bloques_sin_forzar,
                        COALESCE(SUM(area), 0) as area_total_sin_forzar,
                        COALESCE(SUM(poblacion), 0) as poblacion_total_sin_forzar
                    FROM public.blocks_desarrollo
                    WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01';
                """.trimIndent())
                val rs1 = stmt1.executeQuery()
                var totalSinForzar = 0
                var areaTotal = 0.0
                var poblacionTotal = 0L
                if (rs1.next()) {
                    totalSinForzar = rs1.getInt("total_bloques_sin_forzar")
                    areaTotal = round(rs1.getDouble("area_total_sin_forzar") * 10) / 10.0
                    poblacionTotal = rs1.getLong("poblacion_total_sin_forzar")
                }

                val stmt2 = conn.prepareStatement("""
                    SELECT COUNT(*) as induccion_ultimo_mes
                    FROM public.blocks_desarrollo
                    WHERE finduccion IS NOT NULL AND finduccion > '2026-06-01';
                """.trimIndent())
                val rs2 = stmt2.executeQuery()
                var induccionUltimoMes = 0
                if (rs2.next()) {
                    induccionUltimoMes = rs2.getInt("induccion_ultimo_mes")
                }

                val stmt3 = conn.prepareStatement("""
                    SELECT COUNT(DISTINCT bloque) as total_bloques_muestreados
                    FROM public.mu_peso_planta;
                """.trimIndent())
                val rs3 = stmt3.executeQuery()
                var totalMuestreados = 0
                if (rs3.next()) {
                    totalMuestreados = rs3.getInt("total_bloques_muestreados")
                }

                Result.success(
                    GlobalKpis(
                        totalBloquesSinForzar = totalSinForzar,
                        areaTotalSinForzar = areaTotal,
                        poblacionTotalSinForzar = poblacionTotal,
                        induccionUltimoMes = induccionUltimoMes,
                        totalBloquesMuestreados = totalMuestreados
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al conectar directamente a PostgreSQL", e))
        }
    }

    suspend fun getUnforcedBlocks(search: String? = null, grupoSiembra: String? = null, lote: String? = null, ultimoMes: Boolean = false): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val baseWhere = if (ultimoMes) {
                    "WHERE finduccion IS NOT NULL AND finduccion > '2026-06-01'"
                } else {
                    "WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01'"
                }

                var sql = """
                    SELECT 
                        blocknumber, descripcion, desarrollo, bloque, poblacion, area, drenajes, 
                        grupo_siembra, fecha_siembra, grupo_forza, finduccion, grupo_semillero, 
                        mediana_fecha_cosecha, kilos_cosechados, frutas, dias_preforza, dias_posforza,
                        SUBSTRING(bloque, 3, 2) AS lote
                    FROM public.blocks_desarrollo
                    $baseWhere
                """.trimIndent()

                val paramList = mutableListOf<String>()
                if (!search.isNull_or_empty()) {
                    sql += " AND (bloque ILIKE ? OR descripcion ILIKE ? OR grupo_siembra ILIKE ?)"
                    paramList.add("%$search%")
                    paramList.add("%$search%")
                    paramList.add("%$search%")
                }

                if (!grupoSiembra.isNull_or_empty()) {
                    sql += " AND grupo_siembra ILIKE ?"
                    paramList.add("%$grupoSiembra%")
                }

                if (!lote.isNull_or_empty()) {
                    sql += " AND SUBSTRING(bloque, 3, 2) = ?"
                    paramList.add(lote!!)
                }

                sql += " ORDER BY fecha_siembra ASC NULLS LAST, bloque ASC LIMIT 300;"

                val stmt = conn.prepareStatement(sql)
                paramList.forEachIndexed { idx, p -> stmt.setString(idx + 1, p) }

                val rs = stmt.executeQuery()
                val list = mutableListOf<UnforcedBlock>()
                val todayMs = System.currentTimeMillis()

                while (rs.next()) {
                    val finduccionDate = rs.getDate("finduccion")
                    var diasHasta: Int? = null

                    if (finduccionDate != null) {
                        val diffMs = finduccionDate.time - todayMs
                        diasHasta = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                    }

                    list.add(
                        UnforcedBlock(
                            blocknumber = rs.getString("blocknumber"),
                            descripcion = rs.getString("descripcion"),
                            desarrollo = rs.getString("desarrollo"),
                            bloque = rs.getString("bloque") ?: "",
                            poblacion = rs.getLong("poblacion"),
                            area = round(rs.getDouble("area") * 10) / 10.0,
                            drenajes = rs.getDouble("drenajes"),
                            grupoSiembra = rs.getString("grupo_siembra"),
                            fechaSiembra = rs.getDate("fecha_siembra")?.toString(),
                            grupoForza = rs.getString("grupo_forza"),
                            finduccion = finduccionDate?.toString(),
                            grupoSemillero = rs.getString("grupo_semillero"),
                            medianaFechaCosecha = rs.getDate("mediana_fecha_cosecha")?.toString(),
                            kilosCosechados = rs.getDouble("kilos_cosechados"),
                            frutas = rs.getDouble("frutas"),
                            diasPreforza = rs.getDouble("dias_preforza"),
                            diasPosforza = rs.getDouble("dias_posforza"),
                            lote = rs.getString("lote"),
                            diasHastaInduccion = diasHasta,
                            categoriaForzamiento = if (ultimoMes) "ULTIMO_MES" else "PROGRAMADO"
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar bloques", e))
        }
    }

    suspend fun getForcedBlocks(search: String? = null, grupoSiembra: String? = null, lote: String? = null): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                var sql = """
                    SELECT 
                        blocknumber, descripcion, desarrollo, bloque, poblacion, area, drenajes, 
                        grupo_siembra, fecha_siembra, grupo_forza, finduccion, grupo_semillero, 
                        mediana_fecha_cosecha, kilos_cosechados, frutas, dias_preforza, dias_posforza,
                        SUBSTRING(bloque, 3, 2) AS lote
                    FROM public.blocks_desarrollo
                    WHERE grupo_forza IS NOT NULL
                """.trimIndent()

                val paramList = mutableListOf<String>()
                if (!search.isNull_or_empty()) {
                    sql += " AND (bloque ILIKE ? OR descripcion ILIKE ? OR grupo_siembra ILIKE ? OR grupo_forza ILIKE ?)"
                    paramList.add("%$search%")
                    paramList.add("%$search%")
                    paramList.add("%$search%")
                    paramList.add("%$search%")
                }

                if (!grupoSiembra.isNull_or_empty()) {
                    sql += " AND grupo_siembra ILIKE ?"
                    paramList.add("%$grupoSiembra%")
                }

                if (!lote.isNull_or_empty()) {
                    sql += " AND SUBSTRING(bloque, 3, 2) = ?"
                    paramList.add(lote!!)
                }

                sql += " ORDER BY fecha_siembra ASC NULLS LAST, bloque ASC LIMIT 300;"

                val stmt = conn.prepareStatement(sql)
                paramList.forEachIndexed { idx, p -> stmt.setString(idx + 1, p) }

                val rs = stmt.executeQuery()
                val list = mutableListOf<UnforcedBlock>()

                while (rs.next()) {
                    list.add(
                        UnforcedBlock(
                            blocknumber = rs.getString("blocknumber"),
                            descripcion = rs.getString("descripcion"),
                            desarrollo = rs.getString("desarrollo"),
                            bloque = rs.getString("bloque") ?: "",
                            poblacion = rs.getLong("poblacion"),
                            area = round(rs.getDouble("area") * 10) / 10.0,
                            drenajes = rs.getDouble("drenajes"),
                            grupoSiembra = rs.getString("grupo_siembra"),
                            fechaSiembra = rs.getDate("fecha_siembra")?.toString(),
                            grupoForza = rs.getString("grupo_forza"),
                            finduccion = rs.getDate("finduccion")?.toString(),
                            grupoSemillero = rs.getString("grupo_semillero"),
                            medianaFechaCosecha = rs.getDate("mediana_fecha_cosecha")?.toString(),
                            kilosCosechados = rs.getDouble("kilos_cosechados"),
                            frutas = rs.getDouble("frutas"),
                            diasPreforza = rs.getDouble("dias_preforza"),
                            diasPosforza = rs.getDouble("dias_posforza"),
                            lote = rs.getString("lote"),
                            diasHastaInduccion = null,
                            categoriaForzamiento = "FORZADO"
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar grupos forzados", e))
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmtSiembra = conn.prepareStatement("SELECT fecha_siembra FROM public.blocks_desarrollo WHERE bloque = ? LIMIT 1;")
                stmtSiembra.setString(1, bloque)
                val rsSiembra = stmtSiembra.executeQuery()
                var fechaSiembra: java.sql.Date? = null
                if (rsSiembra.next()) {
                    fechaSiembra = rsSiembra.getDate("fecha_siembra")
                }

                val stmt = conn.prepareStatement("""
                    SELECT 
                        fecha, 
                        COUNT(*) as cantidad_muestras,
                        ROUND(AVG(peso)::numeric, 1) as peso_promedio,
                        ROUND(STDDEV_SAMP(peso)::numeric, 1) as desviacion_estandar,
                        ROUND(MIN(peso)::numeric, 1) as peso_min,
                        ROUND(MAX(peso)::numeric, 1) as peso_max
                    FROM public.mu_peso_planta
                    WHERE bloque = ?
                    GROUP BY fecha
                    ORDER BY fecha ASC;
                """.trimIndent())
                stmt.setString(1, bloque)
                val rs = stmt.executeQuery()

                val series = mutableListOf<WeightSeriesEntry>()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                while (rs.next()) {
                    val fechaStr = rs.getDate("fecha")?.toString() ?: ""
                    var edadMeses = 0.0

                    if (fechaSiembra != null && fechaStr.isNotEmpty()) {
                        try {
                            val dMuestreo = sdf.parse(fechaStr)
                            if (dMuestreo != null) {
                                val diffMs = dMuestreo.time - fechaSiembra.time
                                val dias = diffMs / (1000 * 60 * 60 * 24)
                                edadMeses = round((dias / 30.4375) * 10) / 10.0
                                if (edadMeses < 0) edadMeses = 0.0
                            }
                        } catch (e: Exception) {}
                    }

                    series.add(
                        WeightSeriesEntry(
                            fecha = fechaStr,
                            cantidadMuestras = rs.getInt("cantidad_muestras"),
                            pesoPromedio = round(rs.getDouble("peso_promedio") * 10) / 10.0,
                            desviacionEstandar = round(rs.getDouble("desviacion_estandar") * 10) / 10.0,
                            edadMeses = edadMeses,
                            pesoMin = round(rs.getDouble("peso_min") * 10) / 10.0,
                            pesoMax = round(rs.getDouble("peso_max") * 10) / 10.0
                        )
                    )
                }

                if (series.isEmpty()) {
                    return@withContext Result.success(WeightAnalytics(bloque = bloque))
                }

                val first = series.first()
                val last = series.last()

                val d1 = try { sdf.parse(first.fecha) } catch (e: Exception) { null }
                val d2 = try { sdf.parse(last.fecha) } catch (e: Exception) { null }

                val dias = if (d1 != null && d2 != null) {
                    ((d2.time - d1.time) / (1000 * 60 * 60 * 24)).toInt()
                } else 0

                val ganancia = round((last.pesoPromedio - first.pesoPromedio) * 10) / 10.0
                val tasaDiaria = if (dias > 0) round((ganancia / dias) * 10) / 10.0 else 0.0
                val pctIncremento = if (first.pesoPromedio > 0) round((ganancia / first.pesoPromedio * 100) * 10) / 10.0 else 0.0

                val stmtStdGen = conn.prepareStatement("SELECT ROUND(STDDEV_SAMP(peso)::numeric, 1) as std_gen FROM public.mu_peso_planta WHERE bloque = ?;")
                stmtStdGen.setString(1, bloque)
                val rsStdGen = stmtStdGen.executeQuery()
                val stdGen = if (rsStdGen.next()) round(rsStdGen.getDouble("std_gen") * 10) / 10.0 else 0.0

                val tendencia = when {
                    tasaDiaria > 5.0 -> "CRECIENDO_ACELERADO"
                    tasaDiaria > 0.5 -> "CRECIENDO_ESTABLE"
                    tasaDiaria >= -0.5 -> "ESTABLE"
                    else -> "DISMINUYENDO"
                }

                val stmtCount = conn.prepareStatement("SELECT COUNT(*) as total FROM public.mu_peso_planta WHERE bloque = ?;")
                stmtCount.setString(1, bloque)
                val rsCount = stmtCount.executeQuery()
                val totalCount = if (rsCount.next()) rsCount.getInt("total") else series.size

                Result.success(
                    WeightAnalytics(
                        bloque = bloque,
                        totalMuestreos = totalCount,
                        fechaPrimerMuestreo = first.fecha,
                        fechaUltimoMuestreo = last.fecha,
                        diasMonitoreados = dias,
                        desviacionEstandarGeneral = stdGen,
                        pesoInicialG = first.pesoPromedio,
                        pesoActualG = last.pesoPromedio,
                        gananciaTotalG = ganancia,
                        tasaCrecimientoDiarioGDia = tasaDiaria,
                        porcentajeIncremento = pctIncremento,
                        tendencia = tendencia,
                        serieHistorica = series
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar analítica de peso", e))
        }
    }

    suspend fun getGroupWeightAnalytics(grupoSiembra: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                    WITH bloque_curvas AS (
                        SELECT 
                            mp.bloque,
                            ROUND(((mp.fecha - bd.fecha_siembra) / 30.4375)::numeric, 1) AS edad_meses,
                            ROUND(AVG(mp.peso)::numeric, 1) AS peso_promedio_bloque,
                            MIN(mp.fecha) AS fecha_muestreo
                        FROM public.mu_peso_planta mp
                        JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
                        WHERE bd.grupo_siembra = ?
                        GROUP BY mp.bloque, mp.fecha, bd.fecha_siembra
                    )
                    SELECT 
                        edad_meses,
                        COUNT(DISTINCT bloque) AS cantidad_bloques,
                        COUNT(*) AS cantidad_muestras,
                        ROUND(AVG(peso_promedio_bloque)::numeric, 1) AS peso_promedio,
                        ROUND(STDDEV_SAMP(peso_promedio_bloque)::numeric, 1) AS desviacion_estandar,
                        ROUND(MIN(peso_promedio_bloque)::numeric, 1) AS peso_min,
                        ROUND(MAX(peso_promedio_bloque)::numeric, 1) AS peso_max,
                        MIN(fecha_muestreo)::text AS fecha
                    FROM bloque_curvas
                    GROUP BY edad_meses
                    ORDER BY edad_meses ASC;
                """.trimIndent())
                stmt.setString(1, grupoSiembra)
                val rs = stmt.executeQuery()

                val series = mutableListOf<WeightSeriesEntry>()
                while (rs.next()) {
                    series.add(
                        WeightSeriesEntry(
                            fecha = rs.getString("fecha") ?: "",
                            cantidadMuestras = rs.getInt("cantidad_muestras"),
                            pesoPromedio = round(rs.getDouble("peso_promedio") * 10) / 10.0,
                            desviacionEstandar = round(rs.getDouble("desviacion_estandar") * 10) / 10.0,
                            edadMeses = round(rs.getDouble("edad_meses") * 10) / 10.0,
                            pesoMin = round(rs.getDouble("peso_min") * 10) / 10.0,
                            pesoMax = round(rs.getDouble("peso_max") * 10) / 10.0
                        )
                    )
                }

                if (series.isEmpty()) {
                    return@withContext Result.success(WeightAnalytics(bloque = "Grupo: $grupoSiembra"))
                }

                val first = series.first()
                val last = series.last()

                val ganancia = round((last.pesoPromedio - first.pesoPromedio) * 10) / 10.0
                val pctIncremento = if (first.pesoPromedio > 0) round((ganancia / first.pesoPromedio * 100) * 10) / 10.0 else 0.0

                val totalMuestras = series.sumOf { it.cantidadMuestras }

                Result.success(
                    WeightAnalytics(
                        bloque = "Grupo: $grupoSiembra",
                        totalMuestreos = totalMuestras,
                        fechaPrimerMuestreo = first.fecha,
                        fechaUltimoMuestreo = last.fecha,
                        diasMonitoreados = 0,
                        desviacionEstandarGeneral = 0.0,
                        pesoInicialG = first.pesoPromedio,
                        pesoActualG = last.pesoPromedio,
                        gananciaTotalG = ganancia,
                        tasaCrecimientoDiarioGDia = 0.0,
                        porcentajeIncremento = pctIncremento,
                        tendencia = "CRECIENDO_ESTABLE",
                        serieHistorica = series
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error analítica grupo", e))
        }
    }

    suspend fun getLoteWeightAnalytics(lote: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                    WITH bloque_curvas AS (
                        SELECT 
                            mp.bloque,
                            ROUND(((mp.fecha - bd.fecha_siembra) / 30.4375)::numeric, 1) AS edad_meses,
                            ROUND(AVG(mp.peso)::numeric, 1) AS peso_promedio_bloque,
                            MIN(mp.fecha) AS fecha_muestreo
                        FROM public.mu_peso_planta mp
                        JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
                        WHERE SUBSTRING(bd.bloque, 3, 2) = ?
                        GROUP BY mp.bloque, mp.fecha, bd.fecha_siembra
                    )
                    SELECT 
                        edad_meses,
                        COUNT(DISTINCT bloque) AS cantidad_bloques,
                        COUNT(*) AS cantidad_muestras,
                        ROUND(AVG(peso_promedio_bloque)::numeric, 1) AS peso_promedio,
                        ROUND(STDDEV_SAMP(peso_promedio_bloque)::numeric, 1) AS desviacion_estandar,
                        ROUND(MIN(peso_promedio_bloque)::numeric, 1) AS peso_min,
                        ROUND(MAX(peso_promedio_bloque)::numeric, 1) AS peso_max,
                        MIN(fecha_muestreo)::text AS fecha
                    FROM bloque_curvas
                    GROUP BY edad_meses
                    ORDER BY edad_meses ASC;
                """.trimIndent())
                stmt.setString(1, lote)
                val rs = stmt.executeQuery()

                val series = mutableListOf<WeightSeriesEntry>()
                while (rs.next()) {
                    series.add(
                        WeightSeriesEntry(
                            fecha = rs.getString("fecha") ?: "",
                            cantidadMuestras = rs.getInt("cantidad_muestras"),
                            pesoPromedio = round(rs.getDouble("peso_promedio") * 10) / 10.0,
                            desviacionEstandar = round(rs.getDouble("desviacion_estandar") * 10) / 10.0,
                            edadMeses = round(rs.getDouble("edad_meses") * 10) / 10.0,
                            pesoMin = round(rs.getDouble("peso_min") * 10) / 10.0,
                            pesoMax = round(rs.getDouble("peso_max") * 10) / 10.0
                        )
                    )
                }

                if (series.isEmpty()) {
                    return@withContext Result.success(WeightAnalytics(bloque = "Lote $lote"))
                }

                val first = series.first()
                val last = series.last()

                val ganancia = round((last.pesoPromedio - first.pesoPromedio) * 10) / 10.0
                val pctIncremento = if (first.pesoPromedio > 0) round((ganancia / first.pesoPromedio * 100) * 10) / 10.0 else 0.0

                val totalMuestras = series.sumOf { it.cantidadMuestras }

                Result.success(
                    WeightAnalytics(
                        bloque = "Lote $lote",
                        totalMuestreos = totalMuestras,
                        fechaPrimerMuestreo = first.fecha,
                        fechaUltimoMuestreo = last.fecha,
                        diasMonitoreados = 0,
                        desviacionEstandarGeneral = 0.0,
                        pesoInicialG = first.pesoPromedio,
                        pesoActualG = last.pesoPromedio,
                        gananciaTotalG = ganancia,
                        tasaCrecimientoDiarioGDia = 0.0,
                        porcentajeIncremento = pctIncremento,
                        tendencia = "CRECIENDO_ESTABLE",
                        serieHistorica = series
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error analítica lote", e))
        }
    }

    suspend fun getPhytosanitaryAnalytics(bloque: String): Result<PhytosanitaryAnalytics> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                    SELECT 
                        COUNT(*) as total_plantas_muestreadas,
                        SUM(CASE WHEN fusarium = true OR fusarium = '1' THEN 1 ELSE 0 END) as casos_fusarium,
                        SUM(CASE WHEN sinfilido = true OR sinfilido = '1' THEN 1 ELSE 0 END) as casos_sinfilido,
                        SUM(CASE WHEN caracol = true OR caracol = '1' THEN 1 ELSE 0 END) as casos_caracol,
                        SUM(CASE WHEN babosa = true OR babosa = '1' THEN 1 ELSE 0 END) as casos_babosa,
                        SUM(CASE WHEN hormiga = true OR hormiga = '1' THEN 1 ELSE 0 END) as casos_hormiga,
                        SUM(CASE WHEN cochinilla = true OR cochinilla = '1' THEN 1 ELSE 0 END) as casos_cochinilla,
                        SUM(CASE WHEN gusano_cabeza_roja = true OR gusano_cabeza_roja = '1' THEN 1 ELSE 0 END) as casos_gusano
                    FROM public.mu_peso_planta
                    WHERE bloque = ?;
                """.trimIndent())
                stmt.setString(1, bloque)
                val rs = stmt.executeQuery()

                if (!rs.next() || rs.getInt("total_plantas_muestreadas") == 0) {
                    return@withContext Result.success(PhytosanitaryAnalytics(bloque = bloque))
                }

                val total = rs.getInt("total_plantas_muestreadas")
                val fusarium = rs.getInt("casos_fusarium")
                val pctFusarium = round((fusarium.toDouble() / total * 100) * 10) / 10.0

                fun calcPct(count: Int): Double {
                    return if (total > 0) round((count.toDouble() / total * 100) * 10) / 10.0 else 0.0
                }

                val plagas = PestBreakdown(
                    sinfilido = PestItemDetail(rs.getInt("casos_sinfilido"), calcPct(rs.getInt("casos_sinfilido"))),
                    caracol = PestItemDetail(rs.getInt("casos_caracol"), calcPct(rs.getInt("casos_caracol"))),
                    babosa = PestItemDetail(rs.getInt("casos_babosa"), calcPct(rs.getInt("casos_babosa"))),
                    hormiga = PestItemDetail(rs.getInt("casos_hormiga"), calcPct(rs.getInt("casos_hormiga"))),
                    cochinilla = PestItemDetail(rs.getInt("casos_cochinilla"), calcPct(rs.getInt("casos_cochinilla"))),
                    gusanoCabezaRoja = PestItemDetail(rs.getInt("casos_gusano"), calcPct(rs.getInt("casos_gusano")))
                )

                Result.success(
                    PhytosanitaryAnalytics(
                        bloque = bloque,
                        totalPlantasMuestreadas = total,
                        casosFusarium = fusarium,
                        porcentajeFusarium = pctFusarium,
                        plagas = plagas
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar estado fitosanitario", e))
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        try {
            val unforcedRes = getUnforcedBlocks(search = bloque)
            val agro = unforcedRes.getOrNull()?.firstOrNull { it.bloque == bloque }
            val weight = getWeightAnalytics(bloque).getOrDefault(WeightAnalytics(bloque = bloque))
            val phyto = getPhytosanitaryAnalytics(bloque).getOrDefault(PhytosanitaryAnalytics(bloque = bloque))

            Result.success(
                BlockSummary(
                    bloque = bloque,
                    agronomico = agro,
                    pesoAnalitica = weight,
                    fitosanitario = phyto
                )
            )
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar resumen del bloque", e))
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
