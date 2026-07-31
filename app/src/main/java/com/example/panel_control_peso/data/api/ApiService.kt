package com.example.panel_control_peso.data.api

import com.example.panel_control_peso.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/dashboard/kpis")
    suspend fun getGlobalKpis(): GlobalKpis

    @GET("api/v1/blocks/unforced")
    suspend fun getUnforcedBlocks(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 100
    ): List<UnforcedBlock>

    @GET("api/v1/blocks/{bloque}/weight-analytics")
    suspend fun getWeightAnalytics(
        @Path("bloque") bloque: String
    ): WeightAnalytics

    @GET("api/v1/blocks/{bloque}/phytosanitary")
    suspend fun getPhytosanitaryAnalytics(
        @Path("bloque") bloque: String
    ): PhytosanitaryAnalytics

    @GET("api/v1/blocks/{bloque}/summary")
    suspend fun getBlockSummary(
        @Path("bloque") bloque: String
    ): BlockSummary
}
