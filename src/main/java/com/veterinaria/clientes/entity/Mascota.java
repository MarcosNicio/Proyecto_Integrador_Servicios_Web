package com.veterinaria.clientes.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MASCOTAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mascota {

    // Identificador único de la mascota (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mascota")
    private Integer idMascota;

    // Cliente propietario de la mascota
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    // Nombre de la mascota
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // Especie animal (ej. Perro, Gato)
    @Column(name = "especie", nullable = false, length = 50)
    private String especie;

    // Raza de la mascota
    @Column(name = "raza", length = 80)
    private String raza;

    // Fecha de nacimiento aproximada o exacta
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // Peso más reciente registrado en kilogramos
    @Column(name = "peso_actual_kg", precision = 5, scale = 2)
    private BigDecimal pesoActualKg;

    // Sexo biológico de la mascota (MACHO o HEMBRA)
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 10)
    private SexoMascota sexo;

    // Indica si la mascota está esterilizada
    @Column(name = "esterilizado", nullable = false)
    @Builder.Default
    private Boolean esterilizado = false;

    // URL de la fotografía de perfil de la mascota
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    // Bandera de estado para baja lógica (true = activo)
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Lista de antecedentes médicos registrados (alergias, enfermedades, cirugías)
    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AntecedenteClinico> antecedentesClinicos = new ArrayList<>();
}
