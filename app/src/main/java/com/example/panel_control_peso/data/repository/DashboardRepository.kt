package com.example.panel_control_peso.data.repository

import android.content.Context
import com.example.panel_control_peso.data.api.RetrofitClient
import com.example.panel_control_peso.data.cache.OfflineCacheManager
import com.example.panel_control_peso.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    context: Context? = null,
    private val directDbRepository: DirectDbRepository = DirectDbRepository()
) {

    private val cacheManager: OfflineCacheManager? = context?.let { OfflineCacheManager(it) }
    var useDirectDbMode: Boolean = false

    suspend fun getGlobalKpis(): Result<GlobalKpis> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getGlobalKpis()
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.saveKpis(it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getGlobalKpis()
            cacheManager?.saveKpis(res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getKpis()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión y sin datos en caché", e))
            }
        }
    }

    suspend fun getUnforcedBlocks(
        search: String? = null,
        grupoSiembra: String? = null,
        lote: String? = null,
        ultimoMes: Boolean = false
    ): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getUnforcedBlocks(search, grupoSiembra, lote, ultimoMes)
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.saveUnforcedBlocks(it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getUnforcedBlocks(
                search = search,
                grupoSiembra = grupoSiembra,
                lote = lote,
                ultimoMes = if (ultimoMes) true else null
            )
            cacheManager?.saveUnforcedBlocks(res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getUnforcedBlocks()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión a bloques no forzados", e))
            }
        }
    }

    suspend fun getForcedBlocks(
        search: String? = null,
        grupoSiembra: String? = null,
        lote: String? = null
    ): Result<List<UnforcedBlock>> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getForcedBlocks(search, grupoSiembra, lote)
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.saveForcedBlocks(it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getForcedBlocks(
                search = search,
                grupoSiembra = grupoSiembra,
                lote = lote
            )
            cacheManager?.saveForcedBlocks(res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getForcedBlocks()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión a grupos forzados", e))
            }
        }
    }

    suspend fun getWeightAnalytics(bloque: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getWeightAnalytics(bloque)
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.saveWeightAnalytics(bloque, it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getWeightAnalytics(bloque)
            cacheManager?.saveWeightAnalytics(bloque, res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getWeightAnalytics(bloque)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión analítica peso", e))
            }
        }
    }

    suspend fun getGroupWeightAnalytics(grupoSiembra: String): Result<WeightAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getGroupWeightAnalytics(grupoSiembra)
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.saveWeightAnalytics("Grupo_$grupoSiembra", it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getGroupWeightAnalytics(grupoSiembra)
            cacheManager?.saveWeightAnalytics("Grupo_$grupoSiembra", res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getWeightAnalytics("Grupo_$grupoSiembra")
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión analítica peso grupo", e))
            }
        }
    }

    suspend fun getPhytosanitaryAnalytics(bloque: String): Result<PhytosanitaryAnalytics> = withContext(Dispatchers.IO) {
        if (useDirectDbMode) {
            val res = directDbRepository.getPhytosanitaryAnalytics(bloque)
            if (res.isSuccess) {
                res.getOrNull()?.let { cacheManager?.savePhytosanitaryAnalytics(bloque, it) }
                return@withContext res
            }
        }

        try {
            val res = RetrofitClient.apiService.getPhytosanitaryAnalytics(bloque)
            cacheManager?.savePhytosanitaryAnalytics(bloque, res)
            Result.success(res)
        } catch (e: Throwable) {
            val cached = cacheManager?.getPhytosanitaryAnalytics(bloque)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception(e.message ?: "Sin conexión fitosanitario", e))
            }
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
            Result.failure(Exception(e.message ?: "Error resumen bloque", e))
        }
    }
}
