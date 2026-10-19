package com.example.ProyectoZoloopatas;

import com.google.firebase.firestore.IgnoreExtraProperties;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Mascota implements Serializable {
    private String id;
    private String nombre;
    private String tipo; // "Perro" o "Gato" (o "Canino" / "Felino")
    private String sexo; // "Macho" o "Hembra"
    private boolean castrado;
    private String raza;
    private String fechaNacimiento;
    private List<String> colores;
    private String usuarioId;
    private Object fechaRegistro;
    private Object fechaActualizacion;
    private String fotoUri;

    public Mascota() {
        // Constructor vacío necesario para Firestore deserialization
        this.colores = new ArrayList<>();
    }

    public Mascota(String id, String nombre, String tipo, String sexo, boolean castrado, String raza, String fechaNacimiento, List<String> colores) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.sexo = sexo;
        this.castrado = castrado;
        this.raza = raza;
        this.fechaNacimiento = fechaNacimiento;
        this.colores = colores != null ? colores : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre != null ? nombre : "";
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo != null ? tipo : "Perro";
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSexo() {
        return sexo != null ? sexo : "Macho";
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public boolean isCastrado() {
        return castrado;
    }

    public void setCastrado(boolean castrado) {
        this.castrado = castrado;
    }

    public String getRaza() {
        return raza != null ? raza : "";
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento != null ? fechaNacimiento : "";
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public List<String> getColores() {
        if (colores == null) colores = new ArrayList<>();
        return colores;
    }

    public void setColores(List<String> colores) {
        this.colores = colores;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Object getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Object fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Object getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Object fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getFotoUri() {
        return fotoUri;
    }

    public void setFotoUri(String fotoUri) {
        this.fotoUri = fotoUri;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id != null ? id : "");
            json.put("nombre", nombre != null ? nombre : "");
            json.put("tipo", tipo != null ? tipo : "Perro");
            json.put("sexo", sexo != null ? sexo : "Macho");
            json.put("castrado", castrado);
            json.put("raza", raza != null ? raza : "");
            json.put("fechaNacimiento", fechaNacimiento != null ? fechaNacimiento : "");
            json.put("usuarioId", usuarioId != null ? usuarioId : "");
            json.put("fotoUri", fotoUri != null ? fotoUri : "");
            JSONArray colArr = new JSONArray();
            if (colores != null) {
                for (String c : colores) colArr.put(c);
            }
            json.put("colores", colArr);
        } catch (Exception ignored) {}
        return json;
    }

    public static Mascota fromJson(JSONObject json) {
        if (json == null) return null;
        Mascota m = new Mascota();
        m.setId(json.optString("id", ""));
        m.setNombre(json.optString("nombre", ""));
        m.setTipo(json.optString("tipo", "Perro"));
        m.setSexo(json.optString("sexo", "Macho"));
        m.setCastrado(json.optBoolean("castrado", true));
        m.setRaza(json.optString("raza", ""));
        m.setFechaNacimiento(json.optString("fechaNacimiento", ""));
        m.setUsuarioId(json.optString("usuarioId", ""));
        String foto = json.optString("fotoUri", "");
        if (!foto.isEmpty()) {
            m.setFotoUri(foto);
        }
        JSONArray colArr = json.optJSONArray("colores");
        List<String> cols = new ArrayList<>();
        if (colArr != null) {
            for (int i = 0; i < colArr.length(); i++) {
                cols.add(colArr.optString(i));
            }
        }
        m.setColores(cols);
        return m;
    }
}
