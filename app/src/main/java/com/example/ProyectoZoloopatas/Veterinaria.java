package com.example.ProyectoZoloopatas;

import java.io.Serializable;

public class Veterinaria implements Serializable {
    private String id;
    private String nombre;
    private String tipo; // "Clínica Especializada" o "Clínica Externa"
    private String direccion;
    private String distrito;
    private double calificacion;
    private int cantidadResenas;
    private boolean abierto;
    private String horarioTexto;
    private double distanciaKm;
    private String especialidad;
    private String telefono;
    private String precioConsulta;
    private double latitud;
    private double longitud;
    private boolean esFavorita;
    private int imagenResId;
    private String urlImagen;

    public Veterinaria(String id, String nombre, String tipo, String direccion, String distrito,
                       double calificacion, int cantidadResenas, boolean abierto, String horarioTexto,
                       double distanciaKm, String especialidad, String telefono, String precioConsulta,
                       double latitud, double longitud, boolean esFavorita, int imagenResId) {
        this(id, nombre, tipo, direccion, distrito, calificacion, cantidadResenas, abierto, horarioTexto,
                distanciaKm, especialidad, telefono, precioConsulta, latitud, longitud, esFavorita, imagenResId, null);
    }

    public Veterinaria(String id, String nombre, String tipo, String direccion, String distrito,
                       double calificacion, int cantidadResenas, boolean abierto, String horarioTexto,
                       double distanciaKm, String especialidad, String telefono, String precioConsulta,
                       double latitud, double longitud, boolean esFavorita, int imagenResId, String urlImagen) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.direccion = direccion;
        this.distrito = distrito;
        this.calificacion = calificacion;
        this.cantidadResenas = cantidadResenas;
        this.abierto = abierto;
        this.horarioTexto = horarioTexto;
        this.distanciaKm = distanciaKm;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.precioConsulta = precioConsulta;
        this.latitud = latitud;
        this.longitud = longitud;
        this.esFavorita = esFavorita;
        this.imagenResId = imagenResId;
        this.urlImagen = urlImagen;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isEspecializada() {
        return tipo != null && tipo.toLowerCase().contains("especializada");
    }

    public boolean isExterna() {
        return tipo != null && tipo.toLowerCase().contains("externa");
    }

    public String getDireccion() {
        return direccion;
    }

    public String getDistrito() {
        return distrito;
    }

    public double getCalificacion() {
        return calificacion;
    }

    public int getCantidadResenas() {
        return cantidadResenas;
    }

    public boolean isAbierto() {
        return abierto;
    }

    public String getHorarioTexto() {
        return horarioTexto;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getPrecioConsulta() {
        return precioConsulta;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public boolean isEsFavorita() {
        return esFavorita;
    }

    public void setEsFavorita(boolean esFavorita) {
        this.esFavorita = esFavorita;
    }

    public int getImagenResId() {
        return imagenResId;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public boolean hasUrlImagen() {
        return urlImagen != null && !urlImagen.trim().isEmpty();
    }
}
