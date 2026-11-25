package com.veterinaria.clientes.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CLIENTES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    // Identificador único del cliente (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    // Referencia opcional al ID de usuario para acceso al sistema
    @Column(name = "id_usuario", unique = true)
    private Integer idUsuario;

    // Nombre(s) del cliente
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // Apellidos del cliente
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    // Teléfono principal de contacto
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    // Dirección física del domicilio
    @Column(name = "direccion", length = 255)
    private String direccion;

    // URL del documento de responsiva firmado
    @Column(name = "responsiva_url", length = 255)
    private String responsivaUrl;

    // Lista de contactos adicionales o de emergencia asociados
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResponsableExtra> responsablesExtra = new ArrayList<>();

    // Lista de mascotas registradas a nombre del cliente
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mascota> mascotas = new ArrayList<>();
}
