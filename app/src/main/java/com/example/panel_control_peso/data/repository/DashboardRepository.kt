package com.example.panel_control_peso.data.repository

import com.example.panel_control_peso.data.api.RetrofitClient
import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    private val directDbRepository: DirectDbRepository = DirectDbRepository()
) {

    // Default: false = API REST Mode (https://interno.control.agricolaguapa.com/), true = Direct AWS RDS PostgreSQL
    var useDirectDbMode: Boolean = false

    suspend fun getGlobalKpis(): Result<GlobalKpis> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getGlobalKpis()
        }

        try {
            val res = RetrofitClient.apiService.getGlobalKpis()
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al conectar con https://interno.control.agricolaguapa.com/", e))
        }
    }

    suspend fun getUnforcedBlocks(search: String? = null): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getUnforcedBlocks(search)
        }

        try {
            val res = RetrofitClient.apiService.getUnforcedBlocks(search = search)
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al consultar bloques en la API", e))
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getWeightAnalytics(bloque)
        }

        try {
            val res = RetrofitClient.apiService.getWeightAnalytics(bloque)
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error analítica peso en la API", e))
        }
    }

    suspend fun getPhytosanitaryAnalytics(bloque: String): Result<PhytosanitaryAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getPhytosanitaryAnalytics(bloque)
        }

        try {
            val res = RetrofitClient.apiService.getPhytosanitaryAnalytics(bloque)
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error fitosanitario en la API", e))
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getBlockSummary(bloque)
        }

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
            Result.failure(Exception(e.message ?: "Error resumen bloque en la API", e))
        }
    }
}
