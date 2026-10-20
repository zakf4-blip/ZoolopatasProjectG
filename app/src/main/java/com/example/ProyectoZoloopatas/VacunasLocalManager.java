package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class VacunasLocalManager {

    private static final String PREF_NAME = "zoloopatas_vacunas_store";
    private static final String COL_USUARIOS = "usuarios";
    private static final String COL_MASCOTAS = "mascotas";
    private static final String COL_VACUNAS = "vacunas";
    private static String key(String uid, String petId) {
        return "vacunas_" + uid + "_" + petId;
    }

    public static synchronized List<Vacuna> getVacunas(Context context, String uid, String petId) {
        List<Vacuna> resultado = new ArrayList<>();
        if (context == null || petId == null) return resultado;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(key(uid, petId), null);
        if (jsonStr == null) return resultado;
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                Vacuna v = Vacuna.fromJson(arr.getJSONObject(i));
                if (v != null) resultado.add(v);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultado;
    }

    public static synchronized void saveOrUpdateVacuna(Context context, String uid, Vacuna vacuna) {
        if (context == null || vacuna == null || vacuna.getMascotaId() == null) return;

        if (vacuna.getId() == null || vacuna.getId().trim().isEmpty()) {
            vacuna.setId("vac_" + System.currentTimeMillis());
        }
        vacuna.setUsuarioId(uid);
        if (vacuna.getCreadoEn() == 0L) vacuna.setCreadoEn(System.currentTimeMillis());

        List<Vacuna> lista = getVacunas(context, uid, vacuna.getMascotaId());
        boolean actualizada = false;
        for (int i = 0; i < lista.size(); i++) {
            if (vacuna.getId().equals(lista.get(i).getId())) {
                lista.set(i, vacuna);
                actualizada = true;
                break;
            }
        }
        if (!actualizada) lista.add(vacuna);

        guardar(context, key(uid, vacuna.getMascotaId()), lista);
    }

    public static synchronized void deleteVacuna(Context context, String uid, String petId, String vacunaId) {
        if (context == null || petId == null || vacunaId == null) return;
        List<Vacuna> lista = getVacunas(context, uid, petId);
        List<Vacuna> nueva = new ArrayList<>();
        for (Vacuna v : lista) {
            if (!vacunaId.equals(v.getId())) nueva.add(v);
        }
        guardar(context, key(uid, petId), nueva);
    }

    private static void guardar(Context context, String key, List<Vacuna> lista) {
        JSONArray arr = new JSONArray();
        for (Vacuna v : lista) arr.put(v.toJson());
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, arr.toString()).apply();
    }

    private static DocumentReference refVacuna(String uid, String petId, String vacunaId) {
        return FirebaseFirestore.getInstance()
                .collection(COL_USUARIOS).document(uid)
                .collection(COL_MASCOTAS).document(petId)
                .collection(COL_VACUNAS).document(vacunaId);
    }

    public static void sincronizarConFirestore(String uid, Vacuna v) {
        try {
            refVacuna(uid, v.getMascotaId(), v.getId()).set(v.toFirestoreMap());
        } catch (Exception ignored) {
            // Si falla (sin red o reglas), el registro queda guardado localmente
        }
    }

    public static void eliminarEnFirestore(String uid, String petId, String vacunaId) {
        try {
            refVacuna(uid, petId, vacunaId).delete();
        } catch (Exception ignored) {}
    }
}