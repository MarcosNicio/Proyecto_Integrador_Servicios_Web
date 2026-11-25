package com.veterinaria.clientes.repository;

import com.veterinaria.clientes.entity.AntecedenteClinico;
import com.veterinaria.clientes.entity.TipoAntecedente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AntecedenteClinicoRepository extends JpaRepository<AntecedenteClinico, Integer> {

    // Lista los antecedentes clínicos de una mascota ordenados del más reciente al más antiguo
    List<AntecedenteClinico> findByMascotaIdMascotaOrderByFechaRegistroDesc(Integer idMascota);

    // Filtra los antecedentes de una mascota por tipo (ALERGIA, ENFERMEDAD_CRONICA, CIRUGIA_PREVIA)
    List<AntecedenteClinico> findByMascotaIdMascotaAndTipo(Integer idMascota, TipoAntecedente tipo);

    // Busca un antecedente específico validando que pertenezca a la mascota indicada
    Optional<AntecedenteClinico> findByIdAntecedenteAndMascotaIdMascota(Integer idAntecedente, Integer idMascota);
}
