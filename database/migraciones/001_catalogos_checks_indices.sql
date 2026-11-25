-- =============================================================================
-- MIGRACIÓN 001: catálogo de consultorios, validaciones CHECK e índices
-- Para bases creadas ANTES de este cambio (no borra datos). Las bases nuevas
-- creadas con veterinaria_db.sql ya lo incluyen: no hace falta correrla.
--
--   psql -U postgres -h localhost -p 5433 -d veterinaria_db -f database/migraciones/001_catalogos_checks_indices.sql
--
-- Es todo o nada: si algún dato existente rompe una validación, no se aplica
-- ningún cambio y PostgreSQL indica cuál restricción falló.
-- Correrla una sola vez: una segunda vez falla porque las validaciones ya existen.
-- =============================================================================

BEGIN;

-- =============================================================================
-- VALIDACIONES (CHECK): impiden guardar datos imposibles aunque falle la
-- validación del backend. Mismo bloque que database/veterinaria_db.sql
-- =============================================================================

-- Horarios y rangos de fechas: el fin siempre después del inicio
ALTER TABLE HORARIOS_PERSONAL       ADD CONSTRAINT chk_horario_personal_rango CHECK (hora_fin > hora_inicio);
ALTER TABLE HORARIOS_CLINICA        ADD CONSTRAINT chk_horario_clinica_rango  CHECK (hora_cierre > hora_apertura);
ALTER TABLE BLOQUEOS_DISPONIBILIDAD ADD CONSTRAINT chk_bloqueo_rango          CHECK (fecha_fin > fecha_inicio);
ALTER TABLE CITAS                   ADD CONSTRAINT chk_cita_rango             CHECK (fecha_hora_fin > fecha_hora_inicio);
ALTER TABLE CARNET_VACUNACION       ADD CONSTRAINT chk_carnet_proxima_dosis   CHECK (fecha_proxima_dosis IS NULL OR fecha_proxima_dosis >= fecha_aplicacion);

-- Pesos, duraciones, precios y cantidades
ALTER TABLE MASCOTAS             ADD CONSTRAINT chk_mascota_peso         CHECK (peso_actual_kg > 0);
ALTER TABLE CONSULTAS_SOAP       ADD CONSTRAINT chk_soap_peso            CHECK (peso_kg > 0);
ALTER TABLE SERVICIOS            ADD CONSTRAINT chk_servicio_duracion    CHECK (duracion_minutos > 0);
ALTER TABLE SERVICIOS            ADD CONSTRAINT chk_servicio_precio      CHECK (precio_base >= 0);
ALTER TABLE TARIFAS_SERVICIO     ADD CONSTRAINT chk_tarifa_pesos         CHECK (peso_min_kg >= 0 AND peso_max_kg > peso_min_kg);
ALTER TABLE TARIFAS_SERVICIO     ADD CONSTRAINT chk_tarifa_precio        CHECK (precio_calculado >= 0);
ALTER TABLE INVENTARIO_PRODUCTOS ADD CONSTRAINT chk_producto_precio      CHECK (precio_unitario >= 0);
ALTER TABLE INVENTARIO_PRODUCTOS ADD CONSTRAINT chk_producto_stock       CHECK (stock_actual >= 0 AND stock_minimo >= 0);
ALTER TABLE MOVIMIENTOS_INVENTARIO ADD CONSTRAINT chk_movimiento_cantidad CHECK (cantidad <> 0);
ALTER TABLE CITAS                ADD CONSTRAINT chk_cita_costo           CHECK (costo_estimado >= 0);
ALTER TABLE ORDENES_COBRO        ADD CONSTRAINT chk_orden_montos         CHECK (subtotal >= 0 AND impuestos >= 0 AND total >= 0);
ALTER TABLE ORDEN_DETALLES       ADD CONSTRAINT chk_detalle_montos       CHECK (cantidad > 0 AND precio_unitario_snapshot >= 0 AND subtotal >= 0);
ALTER TABLE PAGOS                ADD CONSTRAINT chk_pago_monto           CHECK (monto > 0);

-- Cada renglón de una orden apunta a lo que cobra: un servicio, o un producto (insumo o tienda)
ALTER TABLE ORDEN_DETALLES ADD CONSTRAINT chk_detalle_item CHECK (
    (tipo_item = 'SERVICIO' AND id_servicio IS NOT NULL) OR
    (tipo_item IN ('INSUMO_CLINICO', 'PRODUCTO_TIENDA') AND id_producto IS NOT NULL)
);

-- =============================================================================
-- ÍNDICES: PostgreSQL no indexa solo las llaves foráneas. Estos cubren las
-- búsquedas más frecuentes del sistema (por dueño, por veterinario y fecha, etc.)
-- =============================================================================

-- Clientes y mascotas
CREATE INDEX IF NOT EXISTS idx_responsables_cliente     ON RESPONSABLES_EXTRA (id_cliente);
CREATE INDEX IF NOT EXISTS idx_mascotas_cliente         ON MASCOTAS (id_cliente);
CREATE INDEX IF NOT EXISTS idx_antecedentes_mascota     ON ANTECEDENTES_CLINICOS (id_mascota);

-- Personal y disponibilidad (cálculo de horarios libres)
CREATE INDEX IF NOT EXISTS idx_horarios_personal_dia    ON HORARIOS_PERSONAL (id_personal, dia_semana);
CREATE INDEX IF NOT EXISTS idx_bloqueos_personal_fecha  ON BLOQUEOS_DISPONIBILIDAD (id_personal, fecha_inicio);

-- Infraestructura, servicios e inventario
CREATE INDEX IF NOT EXISTS idx_consultorios_tipo        ON CONSULTORIOS (id_tipo_consultorio);
CREATE INDEX IF NOT EXISTS idx_servicios_categoria      ON SERVICIOS (id_categoria);
CREATE INDEX IF NOT EXISTS idx_tarifas_servicio_especie ON TARIFAS_SERVICIO (id_servicio, especie);
CREATE INDEX IF NOT EXISTS idx_movimientos_producto     ON MOVIMIENTOS_INVENTARIO (id_producto, fecha_movimiento);

-- Agenda: citas de un veterinario o consultorio en un día, tablero diario e historial de la mascota
CREATE INDEX IF NOT EXISTS idx_citas_veterinario_fecha  ON CITAS (id_veterinario, fecha_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_citas_consultorio_fecha  ON CITAS (id_consultorio, fecha_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_citas_fecha              ON CITAS (fecha_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_citas_mascota            ON CITAS (id_mascota);

-- Expediente clínico
CREATE INDEX IF NOT EXISTS idx_soap_mascota             ON CONSULTAS_SOAP (id_mascota);
CREATE INDEX IF NOT EXISTS idx_recetas_consulta         ON RECETAS_MEDICAS (id_consulta);
CREATE INDEX IF NOT EXISTS idx_carnet_mascota           ON CARNET_VACUNACION (id_mascota);
CREATE INDEX IF NOT EXISTS idx_carnet_proxima_dosis     ON CARNET_VACUNACION (fecha_proxima_dosis);

-- Caja
CREATE INDEX IF NOT EXISTS idx_ordenes_cliente          ON ORDENES_COBRO (id_cliente);
CREATE INDEX IF NOT EXISTS idx_ordenes_estado           ON ORDENES_COBRO (estado);
CREATE INDEX IF NOT EXISTS idx_detalles_orden           ON ORDEN_DETALLES (id_orden);
CREATE INDEX IF NOT EXISTS idx_pagos_orden              ON PAGOS (id_orden);

-- Notificaciones (tarea programada que busca las pendientes)
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario   ON NOTIFICACIONES (id_usuario);
CREATE INDEX IF NOT EXISTS idx_notificaciones_pendientes ON NOTIFICACIONES (estado, fecha_programada);

-- =============================================================================
-- CATÁLOGO DE CONSULTORIOS (solo agrega los que falten)
-- =============================================================================

INSERT INTO CONSULTORIOS (id_tipo_consultorio, nombre)
SELECT t.id_tipo_consultorio, c.nombre
FROM (VALUES
    (1, 'Consulta', 'Consultorio 1'),
    (2, 'Consulta', 'Consultorio 2'),
    (3, 'Cirugía',  'Quirófano'),
    (4, 'Estética', 'Sala de Estética')
) AS c(orden, tipo, nombre)
JOIN TIPOS_CONSULTORIO t ON t.nombre = c.tipo
WHERE NOT EXISTS (SELECT 1 FROM CONSULTORIOS x WHERE x.nombre = c.nombre)
ORDER BY c.orden;

COMMIT;
