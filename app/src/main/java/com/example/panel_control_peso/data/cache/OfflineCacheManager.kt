package com.example.panel_control_peso.data.cache

import android.content.Context
import com.example.panel_control_peso.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class OfflineCacheManager(private val context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("agri_cache_prefs", Context.MODE_PRIVATE)

    fun saveKpis(kpis: GlobalKpis) {
        val json = gson.toJson(kpis)
        prefs.edit().putString("cache_kpis", json).apply()
    }

    fun getKpis(): GlobalKpis? {
        val json = prefs.getString("cache_kpis", null) ?: return null
        return try {
            gson.fromJson(json, GlobalKpis::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUnforcedBlocks(blocks: List<UnforcedBlock>) {
        val json = gson.toJson(blocks)
        prefs.edit().putString("cache_unforced_blocks", json).apply()
    }

    fun getUnforcedBlocks(): List<UnforcedBlock>? {
        val json = prefs.getString("cache_unforced_blocks", null) ?: return null
        return try {
            val type = object : TypeToken<List<UnforcedBlock>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveForcedBlocks(blocks: List<UnforcedBlock>) {
        val json = gson.toJson(blocks)
        prefs.edit().putString("cache_forced_blocks", json).apply()
    }

    fun getForcedBlocks(): List<UnforcedBlock>? {
        val json = prefs.getString("cache_forced_blocks", null) ?: return null
        return try {
            val type = object : TypeToken<List<UnforcedBlock>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveWeightAnalytics(bloque: String, weight: WeightAnalytics) {
        val json = gson.toJson(weight)
        prefs.edit().putString("cache_weight_$bloque", json).apply()
    }

    fun getWeightAnalytics(bloque: String): WeightAnalytics? {
        val json = prefs.getString("cache_weight_$bloque", null) ?: return null
        return try {
            gson.fromJson(json, WeightAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun savePhytosanitaryAnalytics(bloque: String, phyto: PhytosanitaryAnalytics) {
        val json = gson.toJson(phyto)
        prefs.edit().putString("cache_phyto_$bloque", json).apply()
    }

    fun getPhytosanitaryAnalytics(bloque: String): PhytosanitaryAnalytics? {
        val json = prefs.getString("cache_phyto_$bloque", null) ?: return null
        return try {
            gson.fromJson(json, PhytosanitaryAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
