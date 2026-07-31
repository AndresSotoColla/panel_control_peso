package com.example.panel_control_peso.data.repository

import com.example.panel_control_peso.data.api.RetrofitClient
import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository {

    suspend fun getGlobalKpis(): Result<GlobalKpis> = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.apiService.getGlobalKpis()
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnforcedBlocks(search: String? = null): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.apiService.getUnforcedBlocks(search = search)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.apiService.getWeightAnalytics(bloque)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhytosanitaryAnalytics(bloque: String): Result<PhytosanitaryAnalytics> = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.apiService.getPhytosanitaryAnalytics(bloque)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlockSummary(bloque: String): Result<BlockSummary> = withContext(Dispatchers.IO) {
        try {
            val res = RetrofitClient.apiService.getBlockSummary(bloque)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
