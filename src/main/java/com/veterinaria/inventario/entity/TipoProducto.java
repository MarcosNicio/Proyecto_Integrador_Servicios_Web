package com.veterinaria.inventario.entity;

// Clasificacion de los articulos registrados en el inventario de la clinica.
// Corresponde a los valores permitidos por la columna tipo de INVENTARIO_PRODUCTOS.
public enum TipoProducto {
    MEDICAMENTO,     // Vacunas, antibioticos, desparasitantes y farmacos
    INSUMO_CLINICO,  // Material desechable o clinico (jeringas, gasas, sueros, suturas)
    TIENDA           // Articulos comerciales para venta directa (alimento, accesorios, higiene)
}
