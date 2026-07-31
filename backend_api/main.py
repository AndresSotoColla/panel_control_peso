from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, List, Dict, Any

from database import (
    get_global_kpis_db,
    get_unforced_blocks_db,
    get_weight_analytics_db,
    get_phytosanitary_analytics_db,
    get_block_summary_db
)

app = FastAPI(
    title="API Control de Peso y Forzamiento Agrícola",
    description="API REST para el aplicativo móvil Dashboard Agrícola (PostgreSQL AWS RDS)",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {
        "status": "online",
        "service": "API Control de Peso y Forzamiento",
        "version": "1.0.0"
    }

@app.get("/api/v1/dashboard/kpis")
def get_global_kpis():
    """Obtiene los KPIs globales para las tarjetas superiores del aplicativo Android."""
    try:
        return get_global_kpis_db()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/blocks/unforced")
def get_unforced_blocks(
    search: Optional[str] = Query(None, description="Búsqueda por código de bloque o descripción"),
    limit: int = Query(100, ge=1, le=1000, description="Límite de resultados")
):
    """
    Obtiene los bloques no forzados (grupo_forza is null y fecha_siembra > '2025-01-01')
    ordenados por proximidad a la fecha de inducción (finduccion).
    """
    try:
        return get_unforced_blocks_db(query_search=search, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/blocks/{bloque}/weight-analytics")
def get_weight_analytics(bloque: str):
    """
    Obtiene la evolución del peso de un bloque, cálculo de tasa de crecimiento diario (g/día)
    y estado de tendencia (CRECIENDO_ACELERADO, CRECIENDO_ESTABLE, ESTABLE, DISMINUYENDO).
    """
    try:
        return get_weight_analytics_db(bloque)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/blocks/{bloque}/phytosanitary")
def get_phytosanitary_analytics(bloque: str):
    """
    Obtiene la incidencia de Fusarium (porcentaje), presencia de plagas y tipo de sistema radicular.
    """
    try:
        return get_phytosanitary_analytics_db(bloque)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/v1/blocks/{bloque}/summary")
def get_block_summary(bloque: str):
    """
    Obtiene una vista unificada del bloque uniendo datos agronómicos, de pesaje y fitosanitarios.
    """
    try:
        res = get_block_summary_db(bloque)
        if not res.get("agronomico") and res.get("peso_analitica", {}).get("total_muestreos") == 0:
            raise HTTPException(status_code=404, detail=f"No se encontró información para el bloque '{bloque}'.")
        return res
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
