import json
import numpy as np
import pandas as pd
from datetime import datetime
from flask import request, jsonify
from sqlalchemy import text

# ==============================================================================
# RUTAS DE LA API DEL DASHBOARD PARA COLOCAR EN TU SERVIDOR FLASK
# URL Servidor: https://interno.control.agricolaguapa.com/
# Blueprint: consultor
# ==============================================================================

@consultor.route('/api/kpis_dashboard_agricola', methods=['GET'])
def api_kpis_dashboard_agricola():
    """Retorna los KPIs globales para el tablero de control en Android."""
    if current_user.has_role('consultor'):
        try:
            query = text("""
                SELECT 
                    (SELECT COUNT(*) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS total_bloques_sin_forzar,
                    (SELECT COALESCE(SUM(area), 0) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS area_total_sin_forzar,
                    (SELECT COALESCE(SUM(poblacion), 0) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS poblacion_total_sin_forzar,
                    (SELECT COUNT(*) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01' AND finduccion IS NOT NULL AND finduccion <= (CURRENT_DATE + INTERVAL '30 days')) AS forzamiento_urgente,
                    (SELECT COUNT(DISTINCT bloque) FROM public.mu_peso_planta) AS total_bloques_muestreados
            """)
            res = db.session.execute(query).fetchone()
            return jsonify({
                "total_bloques_sin_forzar": int(res.total_bloques_sin_forzar or 0),
                "area_total_sin_forzar": round(float(res.area_total_sin_forzar or 0.0), 2),
                "poblacion_total_sin_forzar": int(res.poblacion_total_sin_forzar or 0),
                "forzamiento_urgente": int(res.forzamiento_urgente or 0),
                "total_bloques_muestreados": int(res.total_bloques_muestreados or 0)
            })
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return jsonify({"error": "Unauthorized"}), 401


@consultor.route('/api/bloques_sin_forzar', methods=['GET'])
def api_bloques_sin_forzar():
    """Retorna la lista de bloques no forzados (grupo_forza IS NULL y fecha_siembra > '2025-01-01')."""
    if current_user.has_role('consultor'):
        try:
            search = request.args.get('search', '')
            limit = request.args.get('limit', 100, type=int)

            additional_filter = ""
            params = {'limit': limit}
            if search:
                additional_filter += " AND (bd.bloque ILIKE :search OR bd.descripcion ILIKE :search)"
                params['search'] = f"%{search}%"

            query = text(f"""
                SELECT 
                    bd.blocknumber, bd.descripcion, bd.desarrollo, bd.bloque, bd.poblacion, bd.area, bd.drenajes, 
                    bd.grupo_siembra, bd.fecha_siembra, bd.grupo_forza, bd.finduccion, bd.grupo_semillero, 
                    bd.mediana_fecha_cosecha, bd.kilos_cosechados, bd.frutas, bd.dias_preforza, bd.dias_posforza,
                    CASE 
                        WHEN bd.finduccion IS NOT NULL THEN (bd.finduccion - CURRENT_DATE)
                        ELSE NULL 
                    END AS dias_hasta_induccion,
                    CASE 
                        WHEN bd.finduccion IS NOT NULL AND (bd.finduccion - CURRENT_DATE) <= 15 THEN 'URGENTE'
                        WHEN bd.finduccion IS NOT NULL AND (bd.finduccion - CURRENT_DATE) <= 45 THEN 'PROXIMO'
                        WHEN bd.finduccion IS NOT NULL THEN 'NORMAL'
                        ELSE 'SIN_FECHA'
                    END AS categoria_forzamiento
                FROM public.blocks_desarrollo bd
                WHERE bd.grupo_forza IS NULL AND bd.fecha_siembra > '2025-01-01' {additional_filter}
                ORDER BY bd.finduccion ASC NULLS LAST
                LIMIT :limit;
            """)

            result = db.session.execute(query, params)
            tabla = pd.DataFrame(result.fetchall(), columns=result.keys())

            for col in ['fecha_siembra', 'finduccion', 'mediana_fecha_cosecha']:
                if col in tabla.columns:
                    tabla[col] = tabla[col].astype(str).replace({'NaT': None, 'None': None, 'nan': None})

            tabla = tabla.replace({np.nan: None, pd.NaT: None})
            return jsonify(tabla.to_dict(orient='records'))
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return jsonify({"error": "Unauthorized"}), 401


@consultor.route('/api/analitica_peso_bloque/<bloque>', methods=['GET'])
def api_analitica_peso_bloque(bloque):
    """Calcula la evolución de peso, ganancia total y tasa de crecimiento diario (g/día) por bloque."""
    if current_user.has_role('consultor'):
        try:
            query = text("""
                SELECT 
                    fecha, 
                    COUNT(*) as cantidad_muestras,
                    ROUND(AVG(peso)::numeric, 2) as peso_promedio,
                    MIN(peso) as peso_min,
                    MAX(peso) as peso_max
                FROM public.mu_peso_planta
                WHERE bloque = :bloque
                GROUP BY fecha
                ORDER BY fecha ASC;
            """)
            result = db.session.execute(query, {'bloque': bloque})
            rows = result.fetchall()

            if not rows:
                return jsonify({
                    "bloque": bloque,
                    "total_muestreos": 0,
                    "peso_inicial_g": 0.0,
                    "peso_actual_g": 0.0,
                    "ganancia_total_g": 0.0,
                    "tasa_crecimiento_diario_g_dia": 0.0,
                    "porcentaje_incremento": 0.0,
                    "tendencia": "SIN_DATOS",
                    "serie_historica": []
                })

            df = pd.DataFrame(rows, columns=result.keys())
            df['fecha'] = df['fecha'].astype(str)

            peso_ini = float(df.iloc[0]['peso_promedio'])
            peso_fin = float(df.iloc[-1]['peso_promedio'])

            d1 = datetime.strptime(df.iloc[0]['fecha'], '%Y-%m-%d')
            d2 = datetime.strptime(df.iloc[-1]['fecha'], '%Y-%m-%d')
            dias = (d2 - d1).days

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

            return jsonify({
                "bloque": bloque,
                "total_muestreos": len(df),
                "fecha_primer_muestreo": df.iloc[0]['fecha'],
                "fecha_ultimo_muestreo": df.iloc[-1]['fecha'],
                "dias_monitoreados": dias,
                "peso_inicial_g": peso_ini,
                "peso_actual_g": peso_fin,
                "ganancia_total_g": ganancia,
                "tasa_crecimiento_diario_g_dia": tasa_diaria,
                "porcentaje_incremento": pct_incremento,
                "tendencia": tendencia,
                "serie_historica": df.to_dict(orient='records')
            })
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return jsonify({"error": "Unauthorized"}), 401


@consultor.route('/api/fitosanitario_bloque/<bloque>', methods=['GET'])
def api_fitosanitario_bloque(bloque):
    """Retorna la incidencia de Fusarium y reporte de plagas por bloque."""
    if current_user.has_role('consultor'):
        try:
            query = text("""
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
                WHERE bloque = :bloque;
            """)
            res = db.session.execute(query, {'bloque': bloque}).fetchone()

            if not res or res.total_plantas_muestreadas == 0:
                return jsonify({
                    "bloque": bloque,
                    "total_plantas_muestreadas": 0,
                    "casos_fusarium": 0,
                    "porcentaje_fusarium": 0.0,
                    "plagas": {},
                    "sistemas_radiculares": []
                })

            total = int(res.total_plantas_muestreadas or 0)
            casos_fusarium = int(res.casos_fusarium or 0)
            pct_fusarium = round((casos_fusarium / total) * 100, 2)

            query_root = text("""
                SELECT tipo_sistema_radicular_id, COUNT(*) as cantidad
                FROM public.mu_peso_planta
                WHERE bloque = :bloque
                GROUP BY tipo_sistema_radicular_id
                ORDER BY cantidad DESC;
            """)
            root_res = db.session.execute(query_root, {'bloque': bloque}).fetchall()
            roots = [{"tipo_sistema_radicular_id": r[0], "cantidad": r[1]} for r in root_res]

            return jsonify({
                "bloque": bloque,
                "total_plantas_muestreadas": total,
                "casos_fusarium": casos_fusarium,
                "porcentaje_fusarium": pct_fusarium,
                "plagas": {
                    "sinfilido": int(res.casos_sinfilido or 0),
                    "caracol": int(res.casos_caracol or 0),
                    "babosa": int(res.casos_babosa or 0),
                    "hormiga": int(res.casos_hormiga or 0),
                    "cochinilla": int(res.casos_cochinilla or 0),
                    "gusano_cabeza_roja": int(res.casos_gusano or 0)
                },
                "sistemas_radiculares": roots
            })
        except Exception as e:
            return jsonify({"error": str(e)}), 500

    return jsonify({"error": "Unauthorized"}), 401
