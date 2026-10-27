package com.example.ProyectoZoloopatas;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.IgnoreExtraProperties;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private double pesoKg = 0.0;
    private List<String> alergias;
    private List<String> afeccionesCronicas;
    private List<String> medicamentosActivos;

    public Mascota() {
        // Constructor vacío necesario para Firestore deserialization
        this.colores = new ArrayList<>();
        this.alergias = new ArrayList<>();
        this.afeccionesCronicas = new ArrayList<>();
        this.medicamentosActivos = new ArrayList<>();
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

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public List<String> getAlergias() {
        if (alergias == null) alergias = new ArrayList<>();
        return alergias;
    }

    public void setAlergias(List<String> alergias) {
        this.alergias = alergias;
    }

    public List<String> getAfeccionesCronicas() {
        if (afeccionesCronicas == null) afeccionesCronicas = new ArrayList<>();
        return afeccionesCronicas;
    }

    public void setAfeccionesCronicas(List<String> afeccionesCronicas) {
        this.afeccionesCronicas = afeccionesCronicas;
    }

    public List<String> getMedicamentosActivos() {
        if (medicamentosActivos == null) medicamentosActivos = new ArrayList<>();
        return medicamentosActivos;
    }

    public void setMedicamentosActivos(List<String> medicamentosActivos) {
        this.medicamentosActivos = medicamentosActivos;
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

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id != null ? id : "");
        map.put("idPropietario", usuarioId != null ? usuarioId : "");
        map.put("usuarioId", usuarioId != null ? usuarioId : ""); // compatibilidad
        map.put("nombreMascota", getNombre());
        map.put("nombre", getNombre()); // compatibilidad
        map.put("especie", getTipo());
        map.put("tipo", getTipo()); // compatibilidad
        map.put("sexo", getSexo());
        map.put("raza", getRaza());
        map.put("estaEsterilizado", castrado);
        map.put("castrado", castrado); // compatibilidad
        map.put("fechaNacimiento", getFechaNacimiento());
        map.put("colores", getColores());
        map.put("pesoKg", pesoKg);
        map.put("fotoUri", fotoUri != null ? fotoUri : "");
        map.put("urlFoto", fotoUri != null ? fotoUri : "");

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("alergias", getAlergias());
        resumen.put("afeccionesCronicas", getAfeccionesCronicas());
        resumen.put("medicamentosActivos", getMedicamentosActivos());
        map.put("resumenEmergencia", resumen);

        map.put("fechaCreacionMascota", FieldValue.serverTimestamp());
        map.put("fechaActualizacionMascota", FieldValue.serverTimestamp());
        return map;
    }

    public static Mascota fromFirestoreDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Mascota m = new Mascota();
        m.setId(doc.getId());

        String nom = doc.getString("nombreMascota");
        if (nom == null || nom.trim().isEmpty()) {
            nom = doc.getString("nombre");
        }
        m.setNombre(nom != null ? nom : "Mascota");

        String esp = doc.getString("especie");
        if (esp == null || esp.trim().isEmpty()) {
            esp = doc.getString("tipo");
        }
        m.setTipo(esp != null ? esp : "Perro");

        String sex = doc.getString("sexo");
        m.setSexo(sex != null ? sex : "Macho");

        Boolean est = doc.getBoolean("estaEsterilizado");
        if (est == null) {
            est = doc.getBoolean("castrado");
        }
        m.setCastrado(est != null && est);

        m.setRaza(doc.getString("raza"));

        Object fnObj = doc.get("fechaNacimiento");
        if (fnObj instanceof com.google.firebase.Timestamp) {
            java.util.Date date = ((com.google.firebase.Timestamp) fnObj).toDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            m.setFechaNacimiento(sdf.format(date));
        } else if (fnObj instanceof String) {
            m.setFechaNacimiento((String) fnObj);
        }

        Object colObj = doc.get("colores");
        if (colObj instanceof List) {
            m.setColores((List<String>) colObj);
        }

        Double peso = doc.getDouble("pesoKg");
        if (peso != null) {
            m.setPesoKg(peso);
        }

        String foto = doc.getString("fotoUri");
        if (foto == null || foto.trim().isEmpty()) {
            foto = doc.getString("urlFoto");
        }
        m.setFotoUri(foto != null ? foto : "");

        String uid = doc.getString("idPropietario");
        if (uid == null || uid.trim().isEmpty()) {
            uid = doc.getString("usuarioId");
        }
        m.setUsuarioId(uid != null ? uid : "");

        Map<String, Object> resumen = (Map<String, Object>) doc.get("resumenEmergencia");
        if (resumen != null) {
            Object al = resumen.get("alergias");
            if (al instanceof List) m.setAlergias((List<String>) al);
            Object af = resumen.get("afeccionesCronicas");
            if (af instanceof List) m.setAfeccionesCronicas((List<String>) af);
            Object med = resumen.get("medicamentosActivos");
            if (med instanceof List) m.setMedicamentosActivos((List<String>) med);
        }

        return m;
    }
}
