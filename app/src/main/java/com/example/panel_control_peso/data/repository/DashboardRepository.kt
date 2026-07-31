package com.example.panel_control_peso.data.repository

import com.example.panel_control_peso.data.api.RetrofitClient
import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    private val directDbRepository: DirectDbRepository = DirectDbRepository()
) {

    // Toggle for connection mode: true = Direct AWS RDS PostgreSQL, false = REST API
    var useDirectDbMode: Boolean = true

    suspend fun getGlobalKpis(): Result<GlobalKpis> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getGlobalKpis()
        }

        try {
            val res = RetrofitClient.apiService.getGlobalKpis()
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error al conectar a la API REST", e))
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
            Result.failure(Exception(e.message ?: "Error al consultar la API REST", e))
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
            Result.failure(Exception(e.message ?: "Error al consultar analítica de peso en API", e))
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
            Result.failure(Exception(e.message ?: "Error fitosanitario en API", e))
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            return@withContext directDbRepository.getBlockSummary(bloque)
        }

        try {
            val res = RetrofitClient.apiService.getBlockSummary(bloque)
            Result.success(res)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Error resumen de bloque en API", e))
        }
    }
}
