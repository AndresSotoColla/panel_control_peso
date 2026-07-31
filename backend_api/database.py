import psycopg2
from psycopg2.extras import RealDictCursor
import datetime
from typing import List, Dict, Any, Optional

DB_CONFIG = {
    "host": "guapa-db.cthhnvwvqxrw.us-east-2.rds.amazonaws.com",
    "database": "postgres",
    "user": "postgres",
    "password": "GuapaBI.2023*",
    "port": 5432,
    "connect_timeout": 10
}

def get_connection():
    return psycopg2.connect(**DB_CONFIG)

def serialize_item(item: Dict[str, Any]) -> Dict[str, Any]:
    """Helper to convert dates and decimals to json-friendly formats."""
    result = {}
    for k, v in item.items():
        if isinstance(v, (datetime.date, datetime.datetime)):
            result[k] = v.isoformat()
        elif isinstance(v, (float,)) and (v != v): # NaN check
            result[k] = None
        else:
            result[k] = v
    return result

def get_global_kpis_db() -> Dict[str, Any]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            # Total unforced blocks
            cur.execute("""
                SELECT 
                    COUNT(*) as total_bloques_sin_forzar,
                    COALESCE(SUM(area), 0) as area_total_sin_forzar,
                    COALESCE(SUM(poblacion), 0) as poblacion_total_sin_forzar
                FROM public.blocks_desarrollo
                WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01';
            """)
            res1 = dict(cur.fetchone())

            # Urgent forcing count (< 30 days to finduccion)
            cur.execute("""
                SELECT COUNT(*) as forzamiento_urgente
                FROM public.blocks_desarrollo
                WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01'
                  AND finduccion IS NOT NULL
                  AND finduccion <= (CURRENT_DATE + INTERVAL '30 days');
            """)
            res2 = dict(cur.fetchone())

            # Total sampled blocks in weight DB
            cur.execute("""
                SELECT COUNT(DISTINCT bloque) as total_bloques_muestreados
                FROM public.mu_peso_planta;
            """)
            res3 = dict(cur.fetchone())

            return {
                "total_bloques_sin_forzar": res1.get("total_bloques_sin_forzar", 0),
                "area_total_sin_forzar": round(float(res1.get("area_total_sin_forzar", 0)), 2),
                "poblacion_total_sin_forzar": int(res1.get("poblacion_total_sin_forzar", 0)),
                "forzamiento_urgente": res2.get("forzamiento_urgente", 0),
                "total_bloques_muestreados": res3.get("total_bloques_muestreados", 0)
            }
    finally:
        conn.close()

def get_unforced_blocks_db(query_search: Optional[str] = None, limit: int = 100) -> List[Dict[str, Any]]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            sql = """
                SELECT 
                    blocknumber, descripcion, desarrollo, bloque, poblacion, area, drenajes, 
                    grupo_siembra, fecha_siembra, grupo_forza, finduccion, grupo_semillero, 
                    mediana_fecha_cosecha, kilos_cosechados, frutas, dias_preforza, dias_posforza
                FROM public.blocks_desarrollo
                WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01'
            """
            params = []
            if query_search:
                sql += " AND (bloque ILIKE %s OR descripcion ILIKE %s)"
                params.extend([f"%{query_search}%", f"%{query_search}%"])

            sql += " ORDER BY finduccion ASC NULLS LAST LIMIT %s;"
            params.append(limit)

            cur.execute(sql, params)
            rows = cur.fetchall()

            today = datetime.date.today()
            processed = []
            for row in rows:
                item = serialize_item(dict(row))
                finduccion = row["finduccion"]
                if finduccion:
                    dias_hasta = (finduccion - today).days
                    item["dias_hasta_induccion"] = dias_hasta
                    if dias_hasta <= 15:
                        item["categoria_forzamiento"] = "URGENTE"
                    elif dias_hasta <= 45:
                        item["categoria_forzamiento"] = "PROXIMO"
                    else:
                        item["categoria_forzamiento"] = "NORMAL"
                else:
                    item["dias_hasta_induccion"] = None
                    item["categoria_forzamiento"] = "SIN_FECHA"
                
                processed.append(item)
            return processed
    finally:
        conn.close()

def get_weight_analytics_db(bloque: str) -> Dict[str, Any]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            # Weight samplings by date
            cur.execute("""
                SELECT 
                    fecha, 
                    COUNT(*) as cantidad_muestras,
                    ROUND(AVG(peso)::numeric, 2) as peso_promedio,
                    MIN(peso) as peso_min,
                    MAX(peso) as peso_max
                FROM public.mu_peso_planta
                WHERE bloque = %s
                GROUP BY fecha
                ORDER BY fecha ASC;
            """, (bloque,))
            series_rows = cur.fetchall()

            series = [serialize_item(dict(r)) for r in series_rows]

            if not series:
                return {
                    "bloque": bloque,
                    "total_muestreos": 0,
                    "peso_inicial": 0,
                    "peso_actual": 0,
                    "ganancia_total_g": 0,
                    "tasa_crecimiento_diario_g_dia": 0,
                    "porcentaje_incremento": 0,
                    "tendencia": "SIN_DATOS",
                    "serie_historica": []
                }

            # Calculations
            first_entry = series_rows[0]
            last_entry = series_rows[-1]

            fecha_ini = first_entry["fecha"]
            fecha_fin = last_entry["fecha"]
            peso_ini = float(first_entry["peso_promedio"])
            peso_fin = float(last_entry["peso_promedio"])

            dias = (fecha_fin - fecha_ini).days if (fecha_fin and fecha_ini) else 0
            ganancia = round(peso_fin - peso_ini, 2)
            
            tasa_diaria = round(ganancia / dias, 2) if dias > 0 else 0.0
            pct_incremento = round((ganancia / peso_ini * 100), 2) if peso_ini > 0 else 0.0

            if tasa_diaria > 5.0:
                tendencia = "CRECIENDO_ACELERADO"
            elif tasa_diaria > 0.5:
                tendencia = "CRECIENDO_ESTABLE"
            elif tasa_diaria >= -0.5:
                tendencia = "ESTABLE"
            else:
                tendencia = "DISMINUYENDO"

            cur.execute("SELECT COUNT(*) as total FROM public.mu_peso_planta WHERE bloque = %s;", (bloque,))
            total_count = cur.fetchone()["total"]

            return {
                "bloque": bloque,
                "total_muestreos": total_count,
                "fecha_primer_muestreo": fecha_ini.isoformat() if fecha_ini else None,
                "fecha_ultimo_muestreo": fecha_fin.isoformat() if fecha_fin else None,
                "dias_monitoreados": dias,
                "peso_inicial_g": peso_ini,
                "peso_actual_g": peso_fin,
                "ganancia_total_g": ganancia,
                "tasa_crecimiento_diario_g_dia": tasa_diaria,
                "porcentaje_incremento": pct_incremento,
                "tendencia": tendencia,
                "serie_historica": series
            }
    finally:
        conn.close()

def get_phytosanitary_analytics_db(bloque: str) -> Dict[str, Any]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("""
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
                WHERE bloque = %s;
            """, (bloque,))
            row = cur.fetchone()

            if not row or row["total_plantas_muestreadas"] == 0:
                return {
                    "bloque": bloque,
                    "total_plantas_muestreadas": 0,
                    "casos_fusarium": 0,
                    "porcentaje_fusarium": 0.0,
                    "plagas": {},
                    "sistemas_radiculares": []
                }

            total = row["total_plantas_muestreadas"]
            casos_fusarium = row["casos_fusarium"] or 0
            pct_fusarium = round((casos_fusarium / total) * 100, 2)

            plagas = {
                "sinfilido": row["casos_sinfilido"] or 0,
                "caracol": row["casos_caracol"] or 0,
                "babosa": row["casos_babosa"] or 0,
                "hormiga": row["casos_hormiga"] or 0,
                "cochinilla": row["casos_cochinilla"] or 0,
                "gusano_cabeza_roja": row["casos_gusano"] or 0
            }

            # System root breakdown
            cur.execute("""
                SELECT 
                    tipo_sistema_radicular_id, 
                    COUNT(*) as cantidad
                FROM public.mu_peso_planta
                WHERE bloque = %s
                GROUP BY tipo_sistema_radicular_id
                ORDER BY cantidad DESC;
            """, (bloque,))
            roots = [dict(r) for r in cur.fetchall()]

            return {
                "bloque": bloque,
                "total_plantas_muestreadas": total,
                "casos_fusarium": casos_fusarium,
                "porcentaje_fusarium": pct_fusarium,
                "plagas": plagas,
                "sistemas_radiculares": roots
            }
    finally:
        conn.close()

def get_block_summary_db(bloque: str) -> Dict[str, Any]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("""
                SELECT 
                    blocknumber, descripcion, desarrollo, bloque, poblacion, area, drenajes, 
                    grupo_siembra, fecha_siembra, grupo_forza, finduccion, grupo_semillero, 
                    mediana_fecha_cosecha, kilos_cosechados, frutas, dias_preforza, dias_posforza
                FROM public.blocks_desarrollo
                WHERE bloque = %s LIMIT 1;
            """, (bloque,))
            agronomico_row = cur.fetchone()
            agronomico = serialize_item(dict(agronomico_row)) if agronomico_row else None

        weight = get_weight_analytics_db(bloque)
        phytosanitary = get_phytosanitary_analytics_db(bloque)

        return {
            "bloque": bloque,
            "agronomico": agronomico,
            "peso_analitica": weight,
            "fitosanitario": phytosanitary
        }
    finally:
        conn.close()
