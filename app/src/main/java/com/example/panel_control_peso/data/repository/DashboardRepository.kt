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
            val directRes = directDbRepository.getGlobalKpis()
            if (directRes.isSuccess) return@withContext directRes
        }

        try {
            val res = RetrofitClient.apiService.getGlobalKpis()
            Result.success(res)
        } catch (e: Exception) {
            // Fallback to direct DB if API fails
            directDbRepository.getGlobalKpis()
        }
    }

    suspend fun getUnforcedBlocks(search: String? = null): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val directRes = directDbRepository.getUnforcedBlocks(search)
            if (directRes.isSuccess) return@withContext directRes
        }

        try {
            val res = RetrofitClient.apiService.getUnforcedBlocks(search = search)
            Result.success(res)
        } catch (e: Exception) {
            directDbRepository.getUnforcedBlocks(search)
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val directRes = directDbRepository.getWeightAnalytics(bloque)
            if (directRes.isSuccess) return@withContext directRes
        }

        try {
            val res = RetrofitClient.apiService.getWeightAnalytics(bloque)
            Result.success(res)
        } catch (e: Exception) {
            directDbRepository.getWeightAnalytics(bloque)
        }
    }

    suspend fun getPhytosanitaryAnalytics(bloque: String): Result<PhytosanitaryAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val directRes = directDbRepository.getPhytosanitaryAnalytics(bloque)
            if (directRes.isSuccess) return@withContext directRes
        }

        try {
            val res = RetrofitClient.apiService.getPhytosanitaryAnalytics(bloque)
            Result.success(res)
        } catch (e: Exception) {
            directDbRepository.getPhytosanitaryAnalytics(bloque)
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val directRes = directDbRepository.getBlockSummary(bloque)
            if (directRes.isSuccess) return@withContext directRes
        }

        try {
            val res = RetrofitClient.apiService.getBlockSummary(bloque)
            Result.success(res)
        } catch (e: Exception) {
            directDbRepository.getBlockSummary(bloque)
        }
    }
}
