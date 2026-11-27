package com.veterinaria.servicios.repository;

import com.veterinaria.servicios.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Repositorio Spring Data JPA para el catálogo de servicios (SERVICIOS).
public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    // Lista todos los servicios activos disponibles en el catálogo.
    List<Servicio> findByActivoTrue();

    // Lista todos los servicios (activos e inactivos) asociados a una categoría.
    List<Servicio> findByCategoriaIdCategoria(Integer idCategoria);

    // Lista únicamente los servicios activos asociados a una categoría específica.
    List<Servicio> findByCategoriaIdCategoriaAndActivoTrue(Integer idCategoria);

    // Busca servicios activos cuyo nombre contenga el texto especificado (sin distinguir mayúsculas).
    List<Servicio> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Verifica si ya existe un servicio con el mismo nombre dentro de una categoría.
    boolean existsByNombreIgnoreCaseAndCategoriaIdCategoria(String nombre, Integer idCategoria);

    // Verifica si existe otro servicio con el mismo nombre en la misma categoría,
    // excluyendo el identificador del servicio que se está actualizando.
    boolean existsByNombreIgnoreCaseAndCategoriaIdCategoriaAndIdServicioNot(
            String nombre,
            Integer idCategoria,
            Integer idServicio
    );

    // Consulta nativa para verificar la existencia de un tipo de consultorio activo
    // en la tabla TIPOS_CONSULTORIO sin acoplar este módulo a la entidad TipoConsultorio (Dev A).
    @Query(value = "SELECT EXISTS(SELECT 1 FROM TIPOS_CONSULTORIO WHERE id_tipo_consultorio = :id AND activo = TRUE)", nativeQuery = true)
    boolean existeTipoConsultorioActivo(@Param("id") Integer id);
}
