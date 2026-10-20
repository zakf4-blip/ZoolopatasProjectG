package com.example.ProyectoZoloopatas;

import com.google.firebase.firestore.FieldValue;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Vacuna implements Serializable {
    private String id;
    private String mascotaId;
    private String usuarioId;
    private String tipoRegistro;
    private String tipoVacuna;
    private String fechaAplicacion;   // dd/MM/yyyy
    private String fechaVencimiento;  // dd/MM/yyyy
    private String comentarios;
    private long creadoEn;

    public Vacuna() {}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(String tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }

    public String getTipoVacuna() {
        return tipoVacuna;
    }

    public void setTipoVacuna(String tipoVacuna) {
        this.tipoVacuna = tipoVacuna;
    }

    public String getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(String fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(long creadoEn) {
        this.creadoEn = creadoEn;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id != null ? id : "");
            json.put("mascotaId", mascotaId != null ? mascotaId : "");
            json.put("usuarioId", usuarioId != null ? usuarioId : "");
            json.put("tipoRegistro", getTipoRegistro());
            json.put("tipoVacuna", getTipoVacuna());
            json.put("fechaAplicacion", getFechaAplicacion());
            json.put("fechaVencimiento", getFechaVencimiento());
            json.put("comentarios", getComentarios());
            json.put("creadoEn", creadoEn);
        } catch (Exception ignored) {}
        return json;
    }

    public static Vacuna fromJson(JSONObject json) {
        if (json == null) return null;
        Vacuna v = new Vacuna();
        v.setId(json.optString("id", ""));
        v.setMascotaId(json.optString("mascotaId", ""));
        v.setUsuarioId(json.optString("usuarioId", ""));
        v.setTipoRegistro(json.optString("tipoRegistro", ""));
        v.setTipoVacuna(json.optString("tipoVacuna", ""));
        v.setFechaAplicacion(json.optString("fechaAplicacion", ""));
        v.setFechaVencimiento(json.optString("fechaVencimiento", ""));
        v.setComentarios(json.optString("comentarios", ""));
        v.setCreadoEn(json.optLong("creadoEn", 0L));
        return v;
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("mascotaId", mascotaId);
        m.put("usuarioId", usuarioId);
        m.put("tipoRegistro", getTipoRegistro());
        m.put("tipoVacuna", getTipoVacuna());
        m.put("fechaAplicacion", getFechaAplicacion());
        m.put("fechaVencimiento", getFechaVencimiento());
        m.put("comentarios", getComentarios());
        m.put("fechaRegistro", FieldValue.serverTimestamp());
        return m;
    }
}