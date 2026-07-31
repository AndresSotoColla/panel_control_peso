import json
import numpy as np
import pandas as pd
from datetime import datetime
from flask import request, jsonify
from sqlalchemy import text

# ==============================================================================
# RUTAS DE LA API DASHBOARD AGRÍCOLA (Blueprint: consultor)
# URL Base pública: https://interno.control.agricolaguapa.com/consultor/
# ==============================================================================

@consultor.route('/api/kpis_dashboard_agricola', methods=['GET'])
def api_kpis_dashboard_agricola():
    """Retorna los KPIs globales (Inducción Reciente = finduccion > '2026-06-01' / últimos 2 meses)."""
    try:
        query = text("""
            SELECT 
                (SELECT COUNT(*) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS total_bloques_sin_forzar,
                (SELECT COALESCE(SUM(area), 0) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS area_total_sin_forzar,
                (SELECT COALESCE(SUM(poblacion), 0) FROM public.blocks_desarrollo WHERE grupo_forza IS NULL AND fecha_siembra > '2025-01-01') AS poblacion_total_sin_forzar,
                (SELECT COUNT(*) FROM public.blocks_desarrollo WHERE finduccion IS NOT NULL AND finduccion > '2026-06-01') AS induccion_ultimo_mes,
                (SELECT COUNT(DISTINCT bloque) FROM public.mu_peso_planta) AS total_bloques_muestreados
        """)
        res = db.session.execute(query).fetchone()
        return jsonify({
            "total_bloques_sin_forzar": int(res.total_bloques_sin_forzar or 0),
            "area_total_sin_forzar": round(float(res.area_total_sin_forzar or 0.0), 1),
            "poblacion_total_sin_forzar": int(res.poblacion_total_sin_forzar or 0),
            "induccion_ultimo_mes": int(res.induccion_ultimo_mes or 0),
            "total_bloques_muestreados": int(res.total_bloques_muestreados or 0)
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@consultor.route('/api/bloques_sin_forzar', methods=['GET'])
def api_bloques_sin_forzar():
    """Retorna bloques no forzados u ordenados por FECHA SIEMBRA e inducción reciente."""
    try:
        search = request.args.get('search', '')
        grupo_siembra = request.args.get('grupo_siembra', '')
        lote = request.args.get('lote', '')
        filtro_ultimo_mes = request.args.get('ultimo_mes', 'false').lower() == 'true'
        limit = request.args.get('limit', 300, type=int)

        additional_filter = ""
        params = {'limit': limit}

        if search:
            additional_filter += " AND (bd.bloque ILIKE :search OR bd.descripcion ILIKE :search OR bd.grupo_siembra ILIKE :search)"
            params['search'] = f"%{search}%"

        if grupo_siembra:
            additional_filter += " AND bd.grupo_siembra ILIKE :grupo_siembra"
            params['grupo_siembra'] = f"%{grupo_siembra}%"

        if lote:
            additional_filter += " AND SUBSTRING(bd.bloque, 3, 2) = :lote"
            params['lote'] = lote

        if filtro_ultimo_mes:
            base_where = "WHERE bd.finduccion IS NOT NULL AND bd.finduccion > '2026-06-01'"
        else:
            base_where = "WHERE bd.grupo_forza IS NULL AND bd.fecha_siembra > '2025-01-01'"

        query = text(f"""
            SELECT 
                bd.blocknumber, bd.descripcion, bd.desarrollo, bd.bloque, bd.poblacion, bd.area, bd.drenajes, 
                bd.grupo_siembra, bd.fecha_siembra, bd.grupo_forza, bd.finduccion, bd.grupo_semillero, 
                bd.mediana_fecha_cosecha, bd.kilos_cosechados, bd.frutas, bd.dias_preforza, bd.dias_posforza,
                SUBSTRING(bd.bloque, 3, 2) AS lote,
                CASE 
                    WHEN bd.finduccion IS NOT NULL THEN (bd.finduccion - CURRENT_DATE)
                    ELSE NULL 
                END AS dias_hasta_induccion
            FROM public.blocks_desarrollo bd
            {base_where} {additional_filter}
            ORDER BY bd.fecha_siembra ASC NULLS LAST, bd.bloque ASC
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


@consultor.route('/api/bloques_forzados', methods=['GET'])
def api_bloques_forzados():
    """Retorna bloques forzados ordenados por FECHA SIEMBRA."""
    try:
        search = request.args.get('search', '')
        grupo_siembra = request.args.get('grupo_siembra', '')
        lote = request.args.get('lote', '')
        limit = request.args.get('limit', 300, type=int)

        additional_filter = ""
        params = {'limit': limit}

        if search:
            additional_filter += " AND (bd.bloque ILIKE :search OR bd.descripcion ILIKE :search OR bd.grupo_siembra ILIKE :search OR bd.grupo_forza ILIKE :search)"
            params['search'] = f"%{search}%"

        if grupo_siembra:
            additional_filter += " AND bd.grupo_siembra ILIKE :grupo_siembra"
            params['grupo_siembra'] = f"%{grupo_siembra}%"

        if lote:
            additional_filter += " AND SUBSTRING(bd.bloque, 3, 2) = :lote"
            params['lote'] = lote

        query = text(f"""
            SELECT 
                bd.blocknumber, bd.descripcion, bd.desarrollo, bd.bloque, bd.poblacion, bd.area, bd.drenajes, 
                bd.grupo_siembra, bd.fecha_siembra, bd.grupo_forza, bd.finduccion, bd.grupo_semillero, 
                bd.mediana_fecha_cosecha, bd.kilos_cosechados, bd.frutas, bd.dias_preforza, bd.dias_posforza,
                SUBSTRING(bd.bloque, 3, 2) AS lote
            FROM public.blocks_desarrollo bd
            WHERE bd.grupo_forza IS NOT NULL {additional_filter}
            ORDER BY bd.fecha_siembra ASC NULLS LAST, bd.bloque ASC
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


@consultor.route('/api/analitica_peso_bloque/<bloque>', methods=['GET'])
def api_analitica_peso_bloque(bloque):
    """Calcula la curva de crecimiento con EDAD EN MESES y desviación estándar por BLOQUE."""
    try:
        query_siembra = text("SELECT fecha_siembra FROM public.blocks_desarrollo WHERE bloque = :bloque LIMIT 1;")
        siembra_res = db.session.execute(query_siembra, {'bloque': bloque}).fetchone()
        fecha_siembra = siembra_res.fecha_siembra if siembra_res else None

        query = text("""
            SELECT 
                fecha, 
                COUNT(*) as cantidad_muestras,
                ROUND(AVG(peso)::numeric, 1) as peso_promedio,
                ROUND(STDDEV_SAMP(peso)::numeric, 1) as desviacion_estandar,
                ROUND(MIN(peso)::numeric, 1) as peso_min,
                ROUND(MAX(peso)::numeric, 1) as peso_max
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
                "desviacion_estandar_general": 0.0,
                "peso_inicial_g": 0.0,
                "peso_actual_g": 0.0,
                "ganancia_total_g": 0.0,
                "tasa_crecimiento_diario_g_dia": 0.0,
                "porcentaje_incremento": 0.0,
                "tendencia": "SIN_DATOS",
                "serie_historica": []
            })

        df = pd.DataFrame(rows, columns=result.keys())
        df['desviacion_estandar'] = df['desviacion_estandar'].fillna(0.0).astype(float)
        df['fecha'] = df['fecha'].astype(str)

        if fecha_siembra:
            siembra_dt = pd.to_datetime(fecha_siembra)
            df['edad_meses'] = df['fecha'].apply(lambda f: round(max(0.0, (pd.to_datetime(f) - siembra_dt).days / 30.4375), 1))
        else:
            df['edad_meses'] = 0.0

        peso_ini = round(float(df.iloc[0]['peso_promedio']), 1)
        peso_fin = round(float(df.iloc[-1]['peso_promedio']), 1)

        d1 = datetime.strptime(df.iloc[0]['fecha'], '%Y-%m-%d')
        d2 = datetime.strptime(df.iloc[-1]['fecha'], '%Y-%m-%d')
        dias = (d2 - d1).days

        ganancia = round(peso_fin - peso_ini, 1)
        tasa_diaria = round(ganancia / dias, 1) if dias > 0 else 0.0
        pct_incremento = round((ganancia / peso_ini * 100), 1) if peso_ini > 0 else 0.0

        query_std_gen = text("SELECT ROUND(STDDEV_SAMP(peso)::numeric, 1) as std_gen FROM public.mu_peso_planta WHERE bloque = :bloque;")
        std_gen_res = db.session.execute(query_std_gen, {'bloque': bloque}).fetchone()
        std_general = float(std_gen_res.std_gen or 0.0) if std_gen_res else 0.0

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
            "desviacion_estandar_general": std_general,
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


@consultor.route('/api/analitica_peso_grupo/<grupo_siembra>', methods=['GET'])
def api_analitica_peso_grupo(grupo_siembra):
    """Calcula la curva de crecimiento PROMEDIO POR EDAD (meses) agregando todos los bloques del GRUPO."""
    try:
        query = text("""
            WITH bloque_curvas AS (
                SELECT 
                    mp.bloque,
                    ROUND(((mp.fecha - bd.fecha_siembra) / 30.4375)::numeric, 1) AS edad_meses,
                    ROUND(AVG(mp.peso)::numeric, 1) AS peso_promedio_bloque,
                    MIN(mp.fecha) AS fecha_muestreo
                FROM public.mu_peso_planta mp
                JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
                WHERE bd.grupo_siembra = :grupo_siembra
                GROUP BY mp.bloque, mp.fecha, bd.fecha_siembra
            )
            SELECT 
                edad_meses,
                COUNT(DISTINCT bloque) AS cantidad_bloques,
                COUNT(*) AS cantidad_muestras,
                ROUND(AVG(peso_promedio_bloque)::numeric, 1) AS peso_promedio,
                ROUND(STDDEV_SAMP(peso_promedio_bloque)::numeric, 1) AS desviacion_estandar,
                ROUND(MIN(peso_promedio_bloque)::numeric, 1) AS peso_min,
                ROUND(MAX(peso_promedio_bloque)::numeric, 1) AS peso_max,
                MIN(fecha_muestreo)::text AS fecha
            FROM bloque_curvas
            GROUP BY edad_meses
            ORDER BY edad_meses ASC;
        """)
        result = db.session.execute(query, {'grupo_siembra': grupo_siembra})
        rows = result.fetchall()

        if not rows:
            return jsonify({
                "bloque": f"Grupo: {grupo_siembra}",
                "total_muestreos": 0,
                "desviacion_estandar_general": 0.0,
                "peso_inicial_g": 0.0,
                "peso_actual_g": 0.0,
                "ganancia_total_g": 0.0,
                "tasa_crecimiento_diario_g_dia": 0.0,
                "porcentaje_incremento": 0.0,
                "tendencia": "SIN_DATOS",
                "serie_historica": []
            })

        df = pd.DataFrame(rows, columns=result.keys())
        df['desviacion_estandar'] = df['desviacion_estandar'].fillna(0.0).astype(float)
        df['edad_meses'] = df['edad_meses'].fillna(0.0).astype(float)
        df['fecha'] = df['fecha'].astype(str)

        peso_ini = round(float(df.iloc[0]['peso_promedio']), 1)
        peso_fin = round(float(df.iloc[-1]['peso_promedio']), 1)

        ganancia = round(peso_fin - peso_ini, 1)
        pct_incremento = round((ganancia / peso_ini * 100), 1) if peso_ini > 0 else 0.0

        query_std_gen = text("""
            SELECT ROUND(STDDEV_SAMP(mp.peso)::numeric, 1) as std_gen 
            FROM public.mu_peso_planta mp
            JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
            WHERE bd.grupo_siembra = :grupo_siembra;
        """)
        std_gen_res = db.session.execute(query_std_gen, {'grupo_siembra': grupo_siembra}).fetchone()
        std_general = float(std_gen_res.std_gen or 0.0) if std_gen_res else 0.0

        return jsonify({
            "bloque": f"Grupo: {grupo_siembra}",
            "total_muestreos": int(df['cantidad_muestras'].sum()),
            "fecha_primer_muestreo": df.iloc[0]['fecha'],
            "fecha_ultimo_muestreo": df.iloc[-1]['fecha'],
            "dias_monitoreados": 0,
            "desviacion_estandar_general": std_general,
            "peso_inicial_g": peso_ini,
            "peso_actual_g": peso_fin,
            "ganancia_total_g": ganancia,
            "tasa_crecimiento_diario_g_dia": 0.0,
            "porcentaje_incremento": pct_incremento,
            "tendencia": "CRECIENDO_ESTABLE",
            "serie_historica": df.to_dict(orient='records')
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@consultor.route('/api/analitica_peso_lote/<lote>', methods=['GET'])
def api_analitica_peso_lote(lote):
    """Calcula la curva de crecimiento PROMEDIO POR EDAD (meses) agregando todos los bloques del LOTE."""
    try:
        query = text("""
            WITH bloque_curvas AS (
                SELECT 
                    mp.bloque,
                    ROUND(((mp.fecha - bd.fecha_siembra) / 30.4375)::numeric, 1) AS edad_meses,
                    ROUND(AVG(mp.peso)::numeric, 1) AS peso_promedio_bloque,
                    MIN(mp.fecha) AS fecha_muestreo
                FROM public.mu_peso_planta mp
                JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
                WHERE SUBSTRING(bd.bloque, 3, 2) = :lote
                GROUP BY mp.bloque, mp.fecha, bd.fecha_siembra
            )
            SELECT 
                edad_meses,
                COUNT(DISTINCT bloque) AS cantidad_bloques,
                COUNT(*) AS cantidad_muestras,
                ROUND(AVG(peso_promedio_bloque)::numeric, 1) AS peso_promedio,
                ROUND(STDDEV_SAMP(peso_promedio_bloque)::numeric, 1) AS desviacion_estandar,
                ROUND(MIN(peso_promedio_bloque)::numeric, 1) AS peso_min,
                ROUND(MAX(peso_promedio_bloque)::numeric, 1) AS peso_max,
                MIN(fecha_muestreo)::text AS fecha
            FROM bloque_curvas
            GROUP BY edad_meses
            ORDER BY edad_meses ASC;
        """)
        result = db.session.execute(query, {'lote': lote})
        rows = result.fetchall()

        if not rows:
            return jsonify({
                "bloque": f"Lote {lote}",
                "total_muestreos": 0,
                "desviacion_estandar_general": 0.0,
                "peso_inicial_g": 0.0,
                "peso_actual_g": 0.0,
                "ganancia_total_g": 0.0,
                "tasa_crecimiento_diario_g_dia": 0.0,
                "porcentaje_incremento": 0.0,
                "tendencia": "SIN_DATOS",
                "serie_historica": []
            })

        df = pd.DataFrame(rows, columns=result.keys())
        df['desviacion_estandar'] = df['desviacion_estandar'].fillna(0.0).astype(float)
        df['edad_meses'] = df['edad_meses'].fillna(0.0).astype(float)
        df['fecha'] = df['fecha'].astype(str)

        peso_ini = round(float(df.iloc[0]['peso_promedio']), 1)
        peso_fin = round(float(df.iloc[-1]['peso_promedio']), 1)

        ganancia = round(peso_fin - peso_ini, 1)
        pct_incremento = round((ganancia / peso_ini * 100), 1) if peso_ini > 0 else 0.0

        query_std_gen = text("""
            SELECT ROUND(STDDEV_SAMP(mp.peso)::numeric, 1) as std_gen 
            FROM public.mu_peso_planta mp
            JOIN public.blocks_desarrollo bd ON mp.bloque = bd.bloque
            WHERE SUBSTRING(bd.bloque, 3, 2) = :lote;
        """)
        std_gen_res = db.session.execute(query_std_gen, {'lote': lote}).fetchone()
        std_general = float(std_gen_res.std_gen or 0.0) if std_gen_res else 0.0

        return jsonify({
            "bloque": f"Lote {lote}",
            "total_muestreos": int(df['cantidad_muestras'].sum()),
            "fecha_primer_muestreo": df.iloc[0]['fecha'],
            "fecha_ultimo_muestreo": df.iloc[-1]['fecha'],
            "dias_monitoreados": 0,
            "desviacion_estandar_general": std_general,
            "peso_inicial_g": peso_ini,
            "peso_actual_g": peso_fin,
            "ganancia_total_g": ganancia,
            "tasa_crecimiento_diario_g_dia": 0.0,
            "porcentaje_incremento": pct_incremento,
            "tendencia": "CRECIENDO_ESTABLE",
            "serie_historica": df.to_dict(orient='records')
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@consultor.route('/api/fitosanitario_bloque/<bloque>', methods=['GET'])
def api_fitosanitario_bloque(bloque):
    """Retorna incidencia de Fusarium y reporte de plagas en PORCENTAJE (%)."""
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
                "plagas": {}
            })

        total = int(res.total_plantas_muestreadas or 0)
        casos_fusarium = int(res.casos_fusarium or 0)
        pct_fusarium = round((casos_fusarium / total) * 100, 1)

        def calc_pct(count):
            return round((count / total) * 100, 1) if total > 0 else 0.0

        return jsonify({
            "bloque": bloque,
            "total_plantas_muestreadas": total,
            "casos_fusarium": casos_fusarium,
            "porcentaje_fusarium": pct_fusarium,
            "plagas": {
                "sinfilido": {"casos": int(res.casos_sinfilido or 0), "pct": calc_pct(int(res.casos_sinfilido or 0))},
                "caracol": {"casos": int(res.casos_caracol or 0), "pct": calc_pct(int(res.casos_caracol or 0))},
                "babosa": {"casos": int(res.casos_babosa or 0), "pct": calc_pct(int(res.casos_babosa or 0))},
                "hormiga": {"casos": int(res.casos_hormiga or 0), "pct": calc_pct(int(res.casos_hormiga or 0))},
                "cochinilla": {"casos": int(res.casos_cochinilla or 0), "pct": calc_pct(int(res.casos_cochinilla or 0))},
                "gusano_cabeza_roja": {"casos": int(res.casos_gusano or 0), "pct": calc_pct(int(res.casos_gusano or 0))}
            }
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500
