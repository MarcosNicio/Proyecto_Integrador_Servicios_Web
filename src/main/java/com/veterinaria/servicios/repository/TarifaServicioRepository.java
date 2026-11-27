package com.veterinaria.servicios.repository;

import com.veterinaria.servicios.entity.TarifaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Repositorio Spring Data JPA para las reglas de tarifas dinámicas (TARIFAS_SERVICIO).
public interface TarifaServicioRepository extends JpaRepository<TarifaServicio, Integer> {

    // Lista todas las tarifas configuradas para un servicio, ordenadas por especie y peso mínimo ascendente.
    List<TarifaServicio> findByServicioIdServicioOrderByEspecieAscPesoMinKgAsc(Integer idServicio);

    // Lista las tarifas de un servicio filtradas por especie (sin distinguir mayúsculas/minúsculas),
    // ordenadas por peso mínimo ascendente.
    List<TarifaServicio> findByServicioIdServicioAndEspecieIgnoreCaseOrderByPesoMinKgAsc(
            Integer idServicio,
            String especie
    );

    // Verifica si existe algún solapamiento (traslape) de rangos de peso para un mismo servicio y especie.
    // Los rangos incluyen su límite inferior pero no el superior: [min, max). Por eso 0-10 y 10-25 NO se traslapan
    // (el 10 pertenece al segundo) y 0-10 con 5-15 sí. Dos rangos se traslapan si cada uno empieza antes de que termine el otro.
    // Permite excluir un ID de tarifa en operaciones de actualización.
    @Query("""
            SELECT COUNT(t) > 0 FROM TarifaServicio t
            WHERE t.servicio.idServicio = :idServicio
              AND LOWER(t.especie) = LOWER(:especie)
              AND (:idTarifaExcluir IS NULL OR t.idTarifa <> :idTarifaExcluir)
              AND t.pesoMinKg < :pesoMaxKg
              AND t.pesoMaxKg > :pesoMinKg
            """)
    boolean existeTraslape(
            @Param("idServicio") Integer idServicio,
            @Param("especie") String especie,
            @Param("pesoMinKg") BigDecimal pesoMinKg,
            @Param("pesoMaxKg") BigDecimal pesoMaxKg,
            @Param("idTarifaExcluir") Integer idTarifaExcluir
    );

    // Busca la regla tarifaria aplicable para un servicio, especie y peso específico en kilogramos.
    // Compara la especie de forma insensible a mayúsculas/minúsculas y valida que el peso
    // caiga dentro del intervalo [pesoMinKg, pesoMaxKg): incluye el límite inferior y excluye el superior,
    // que pertenece al rango siguiente.
    @Query("""
            SELECT t FROM TarifaServicio t
            WHERE t.servicio.idServicio = :idServicio
              AND LOWER(t.especie) = LOWER(:especie)
              AND :pesoKg >= t.pesoMinKg
              AND :pesoKg < t.pesoMaxKg
            """)
    Optional<TarifaServicio> findTarifaAplicable(
            @Param("idServicio") Integer idServicio,
            @Param("especie") String especie,
            @Param("pesoKg") BigDecimal pesoKg
    );
}
