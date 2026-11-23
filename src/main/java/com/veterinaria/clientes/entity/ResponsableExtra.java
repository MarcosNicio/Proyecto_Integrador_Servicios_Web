package com.veterinaria.clientes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RESPONSABLES_EXTRA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsableExtra {

    // Identificador único del responsable extra (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_responsable")
    private Integer idResponsable;

    // Cliente titular al que pertenece este contacto
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    // Nombre completo del responsable adicional
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    // Teléfono de contacto del responsable
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    // Relación o parentesco con el cliente titular
    @Column(name = "parentesco", length = 50)
    private String parentesco;
}
