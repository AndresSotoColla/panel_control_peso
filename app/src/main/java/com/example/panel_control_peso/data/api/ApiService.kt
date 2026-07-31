package com.example.panel_control_peso.data.api

import com.example.panel_control_peso.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/kpis_dashboard_agricola")
    suspend fun getGlobalKpis(): GlobalKpis

    @GET("api/bloques_sin_forzar")
    suspend fun getUnforcedBlocks(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 100
    ): List<UnforcedBlock>

    @GET("api/analitica_peso_bloque/{bloque}")
    suspend fun getWeightAnalytics(
        @Path("bloque") bloque: String
    ): WeightAnalytics

    @GET("api/fitosanitario_bloque/{bloque}")
    suspend fun getPhytosanitaryAnalytics(
        @Path("bloque") bloque: String
    ): PhytosanitaryAnalytics
}
