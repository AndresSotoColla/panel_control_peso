package com.example.panel_control_peso.data.repository

import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.Date
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
        
        // AWS RDS SSL configuration for Android
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
                    areaTotal = rs1.getDouble("area_total_sin_forzar")
                    poblacionTotal = rs1.getLong("poblacion_total_sin_forzar")
                }

                val stmt2 = conn.prepareStatement("""
                    SELECT COUNT(*) as forzamiento_urgente
                    FROM public.blocks_desarrollo
                    WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01'
                      AND finduccion IS NOT NULL
                      AND finduccion <= (CURRENT_DATE + INTERVAL '30 days');
                """.trimIndent())
                val rs2 = stmt2.executeQuery()
                var forzamientoUrgente = 0
                if (rs2.next()) {
                    forzamientoUrgente = rs2.getInt("forzamiento_urgente")
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
                        forzamientoUrgente = forzamientoUrgente,
                        totalBloquesMuestreados = totalMuestreados
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al conectar directamente a PostgreSQL", e))
        }
    }

    suspend fun getUnforcedBlocks(search: String? = null): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                var sql = """
                    SELECT 
                        blocknumber, descripcion, desarrollo, bloque, poblacion, area, drenajes, 
                        grupo_siembra, fecha_siembra, grupo_forza, finduccion, grupo_semillero, 
                        mediana_fecha_cosecha, kilos_cosechados, frutas, dias_preforza, dias_posforza
                    FROM public.blocks_desarrollo
                    WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01'
                """.trimIndent()

                if (!search.isNull_or_empty()) {
                    sql += " AND (bloque ILIKE ? OR descripcion ILIKE ?)"
                }
                sql += " ORDER BY finduccion ASC NULLS LAST LIMIT 100;"

                val stmt = conn.prepareStatement(sql)
                if (!search.isNull_or_empty()) {
                    stmt.setString(1, "%$search%")
                    stmt.setString(2, "%$search%")
                }

                val rs = stmt.executeQuery()
                val list = mutableListOf<UnforcedBlock>()
                val todayMs = System.currentTimeMillis()

                while (rs.next()) {
                    val finduccionDate = rs.getDate("finduccion")
                    var diasHasta: Int? = null
                    var catForzamiento: String? = "SIN_FECHA"

                    if (finduccionDate != null) {
                        val diffMs = finduccionDate.time - todayMs
                        diasHasta = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        catForzamiento = when {
                            diasHasta <= 15 -> "URGENTE"
                            diasHasta <= 45 -> "PROXIMO"
                            else -> "NORMAL"
                        }
                    }

                    list.add(
                        UnforcedBlock(
                            blocknumber = rs.getString("blocknumber"),
                            descripcion = rs.getString("descripcion"),
                            desarrollo = rs.getString("desarrollo"),
                            bloque = rs.getString("bloque") ?: "",
                            poblacion = rs.getLong("poblacion"),
                            area = rs.getDouble("area"),
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
                            diasHastaInduccion = diasHasta,
                            categoriaForzamiento = catForzamiento
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar bloques", e))
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                    SELECT 
                        fecha, 
                        COUNT(*) as cantidad_muestras,
                        ROUND(AVG(peso)::numeric, 2) as peso_promedio,
                        MIN(peso) as peso_min,
                        MAX(peso) as peso_max
                    FROM public.mu_peso_planta
                    WHERE bloque = ?
                    GROUP BY fecha
                    ORDER BY fecha ASC;
                """.trimIndent())
                stmt.setString(1, bloque)
                val rs = stmt.executeQuery()

                val series = mutableListOf<WeightSeriesEntry>()
                while (rs.next()) {
                    series.add(
                        WeightSeriesEntry(
                            fecha = rs.getDate("fecha")?.toString() ?: "",
                            cantidadMuestras = rs.getInt("cantidad_muestras"),
                            pesoPromedio = rs.getDouble("peso_promedio"),
                            pesoMin = rs.getDouble("peso_min"),
                            pesoMax = rs.getDouble("peso_max")
                        )
                    )
                }

                if (series.isEmpty()) {
                    return@withContext Result.success(WeightAnalytics(bloque = bloque))
                }

                val first = series.first()
                val last = series.last()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d1 = try { sdf.parse(first.fecha) } catch (e: Exception) { null }
                val d2 = try { sdf.parse(last.fecha) } catch (e: Exception) { null }

                val dias = if (d1 != null && d2 != null) {
                    ((d2.time - d1.time) / (1000 * 60 * 60 * 24)).toInt()
                } else 0

                val ganancia = round((last.pesoPromedio - first.pesoPromedio) * 100) / 100.0
                val tasaDiaria = if (dias > 0) round((ganancia / dias) * 100) / 100.0 else 0.0
                val pctIncremento = if (first.pesoPromedio > 0) round((ganancia / first.pesoPromedio * 100) * 100) / 100.0 else 0.0

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
                val pctFusarium = round((fusarium.toDouble() / total * 100) * 100) / 100.0

                val plagas = PestBreakdown(
                    sinfilido = rs.getInt("casos_sinfilido"),
                    caracol = rs.getInt("casos_caracol"),
                    babosa = rs.getInt("casos_babosa"),
                    hormiga = rs.getInt("casos_hormiga"),
                    cochinilla = rs.getInt("casos_cochinilla"),
                    gusanoCabezaRoja = rs.getInt("casos_gusano")
                )

                val stmtRoot = conn.prepareStatement("""
                    SELECT tipo_sistema_radicular_id, COUNT(*) as cantidad
                    FROM public.mu_peso_planta
                    WHERE bloque = ?
                    GROUP BY tipo_sistema_radicular_id
                    ORDER BY cantidad DESC;
                """.trimIndent())
                stmtRoot.setString(1, bloque)
                val rsRoot = stmtRoot.executeQuery()

                val roots = mutableListOf<RootSystemEntry>()
                while (rsRoot.next()) {
                    roots.add(
                        RootSystemEntry(
                            tipoSistemaRadicularId = rsRoot.getString("tipo_sistema_radicular_id"),
                            cantidad = rsRoot.getInt("cantidad")
                        )
                    )
                }

                Result.success(
                    PhytosanitaryAnalytics(
                        bloque = bloque,
                        totalPlantasMuestreadas = total,
                        casosFusarium = fusarium,
                        porcentajeFusarium = pctFusarium,
                        plagas = plagas,
                        sistemasRadiculares = roots
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar estado fitosanitario", e))
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        try {
            val unforcedRes = getUnforcedBlocks(bloque)
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
