package com.veterinaria.inventario.entity;

// Tipos de movimiento permitidos en el Kardex auditable de inventario.
// Corresponde a los valores permitidos por la columna tipo_movimiento de MOVIMIENTOS_INVENTARIO.
public enum TipoMovimiento {
    ENTRADA,          // Abastecimiento, compras a proveedor o stock inicial
    SALIDA_CONSULTA,  // Consumo de medicamentos o insumos durante una consulta clinica
    SALIDA_VENTA,     // Descuento por venta directa de productos en caja o tienda
    AJUSTE            // Correccion manual de existencias (positivo por sobrante o negativo por merma)
}
