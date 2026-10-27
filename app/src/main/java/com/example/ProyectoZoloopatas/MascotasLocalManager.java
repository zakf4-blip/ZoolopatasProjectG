package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestor local de almacenamiento persistente de mascotas individualizado por usuario (UID).
 * Garantiza que cada cuenta mantenga su propia lista independiente sin compartir datos entre usuarios.
 */
public class MascotasLocalManager {

    private static final String PREF_NAME = "zoloopatas_mascotas_store";
    private static final String KEY_LIST_PREFIX = "mascotas_lista_";
    private static final String KEY_DELETED_PREFIX = "eliminadas_";

    public static Set<String> getEliminadas(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return new HashSet<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_DELETED_PREFIX + uid, new HashSet<>()));
    }

    public static boolean isEliminada(Mascota m, Set<String> eliminadas) {
        if (m == null || eliminadas == null) return false;
        if (m.getId() != null && eliminadas.contains(m.getId())) return true;
        String nom = m.getNombre();
        if (nom != null && !nom.trim().isEmpty()) {
            if (eliminadas.contains("nombre_" + nom.trim())) return true;
            if (eliminadas.contains(nom.trim().toLowerCase())) return true;
        }
        return false;
    }

    public static synchronized List<Mascota> getMascotas(Context context, String uid) {
        List<Mascota> resultado = new ArrayList<>();
        if (context == null || uid == null || uid.trim().isEmpty()) return resultado;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> eliminadas = getEliminadas(context, uid);
        String jsonStr = prefs.getString(KEY_LIST_PREFIX + uid, null);

        if (jsonStr == null) {
            // Toda cuenta inicia con lista vacía sin mascotas por defecto
            guardarListaDirecta(prefs, uid, resultado);
            return resultado;
        }

        boolean requiriLimpiezaDemo = false;
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Mascota m = Mascota.fromJson(obj);
                if (m != null && !isEliminada(m, eliminadas)) {
                    // Omitir completamente cualquier mascota de prueba/demo por defecto
                    if (m.getId() != null && (m.getId().startsWith("demo_") || m.getId().equals("demo_rocky") || m.getId().equals("demo_luna"))) {
                        requiriLimpiezaDemo = true;
                        continue;
                    }
                    if (m.getUsuarioId() == null || m.getUsuarioId().isEmpty()) {
                        m.setUsuarioId(uid);
                    }
                    if (uid.equals(m.getUsuarioId())) {
                        resultado.add(m);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Si se encontraron mascotas demo antiguas, reescribir la lista limpia
        if (requiriLimpiezaDemo) {
            guardarListaDirecta(prefs, uid, resultado);
        }

        return resultado;
    }

    public static synchronized Mascota getMascotaById(Context context, String uid, String id) {
        if (context == null || id == null || id.trim().isEmpty() || uid == null) return null;
        List<Mascota> lista = getMascotas(context, uid);
        for (Mascota m : lista) {
            if (id.equals(m.getId())) return m;
        }
        return null;
    }

    public static synchronized void saveOrUpdateMascota(Context context, String uid, Mascota mascota) {
        if (context == null || mascota == null || uid == null || uid.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (mascota.getId() == null || mascota.getId().trim().isEmpty()) {
            mascota.setId("pet_" + System.currentTimeMillis());
        }
        mascota.setUsuarioId(uid);

        List<Mascota> listaActual = getMascotas(context, uid);
        boolean actualizado = false;

        for (int i = 0; i < listaActual.size(); i++) {
            Mascota existente = listaActual.get(i);
            boolean mismoId = existente.getId() != null && existente.getId().equals(mascota.getId());
            if (mismoId) {
                listaActual.set(i, mascota);
                actualizado = true;
                break;
            }
        }

        if (!actualizado) {
            listaActual.add(mascota);
        }

        guardarListaDirecta(prefs, uid, listaActual);
    }

    public static synchronized void deleteMascota(Context context, String uid, String id) {
        deleteMascota(context, uid, id, null);
    }

    public static synchronized void deleteMascota(Context context, String uid, String id, String nombre) {
        if (context == null || uid == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Set<String> eliminadas = getEliminadas(context, uid);
        if (id != null && !id.trim().isEmpty()) {
            eliminadas.add(id);
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            eliminadas.add("nombre_" + nombre.trim());
            eliminadas.add(nombre.trim().toLowerCase());
        }
        prefs.edit().putStringSet(KEY_DELETED_PREFIX + uid, eliminadas).apply();

        List<Mascota> listaActual = getMascotas(context, uid);
        List<Mascota> nuevaLista = new ArrayList<>();
        for (Mascota m : listaActual) {
            boolean coincideId = (id != null && id.equals(m.getId()));
            boolean coincideNombre = (nombre != null && m.getNombre() != null && nombre.trim().equalsIgnoreCase(m.getNombre().trim()));
            if (!coincideId && !coincideNombre) {
                nuevaLista.add(m);
            }
        }

        guardarListaDirecta(prefs, uid, nuevaLista);
    }

    public static synchronized void setMascotasFromFirestore(Context context, String uid, List<Mascota> firestoreList) {
        if (context == null || uid == null || uid.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> eliminadas = getEliminadas(context, uid);

        List<Mascota> listaFinal = new ArrayList<>();
        if (firestoreList != null) {
            for (Mascota remota : firestoreList) {
                if (remota != null && !isEliminada(remota, eliminadas)) {
                    // Omitir mascotas demo por defecto
                    if (remota.getId() != null && (remota.getId().startsWith("demo_") || remota.getId().equals("demo_rocky") || remota.getId().equals("demo_luna"))) {
                        continue;
                    }
                    remota.setUsuarioId(uid);
                    listaFinal.add(remota);
                }
            }
        }

        guardarListaDirecta(prefs, uid, listaFinal);
    }

    public static synchronized void mergeFromFirestore(Context context, String uid, List<Mascota> firestoreList) {
        setMascotasFromFirestore(context, uid, firestoreList);
    }

    private static void guardarListaDirecta(SharedPreferences prefs, String uid, List<Mascota> lista) {
        JSONArray arr = new JSONArray();
        for (Mascota m : lista) {
            arr.put(m.toJson());
        }
        prefs.edit().putString(KEY_LIST_PREFIX + uid, arr.toString()).apply();
    }
}
