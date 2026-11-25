package com.veterinaria.servicios.repository;

import com.veterinaria.servicios.entity.CategoriaServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repositorio Spring Data JPA para la gestión de categorías de servicio (CATEGORIAS_SERVICIO).

public interface CategoriaServicioRepository extends JpaRepository<CategoriaServicio, Integer> {

    // Lista todas las categorías de servicio que se encuentran activas.
    List<CategoriaServicio> findByActivoTrue();

    // Busca una categoría por su nombre exacto sin distinguir mayúsculas o minúsculas.
    Optional<CategoriaServicio> findByNombreIgnoreCase(String nombre);

    // Verifica si ya existe una categoría registrada con el nombre indicado (case-insensitive).
    boolean existsByNombreIgnoreCase(String nombre);

    // Verifica si existe otra categoría con el mismo nombre excluyendo un ID específico.
    // Útil al actualizar una categoría para evitar falsos positivos con su propio registro.
    boolean existsByNombreIgnoreCaseAndIdCategoriaNot(String nombre, Integer idCategoria);
}
