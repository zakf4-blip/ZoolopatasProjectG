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

public class MascotasLocalManager {

    private static final String PREF_NAME = "zoloopatas_mascotas_store";
    private static final String KEY_LIST_PREFIX = "mascotas_lista_";
    private static final String KEY_DELETED_PREFIX = "eliminadas_";

    public static Set<String> getEliminadas(Context context, String uid) {
        if (context == null) return new HashSet<>();
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
        if (context == null) return resultado;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> eliminadas = getEliminadas(context, uid);
        String jsonStr = prefs.getString(KEY_LIST_PREFIX + uid, null);

        if (jsonStr == null) {
            // Primera vez: sembrar Rocky y Luna si no han sido eliminados
            if (!eliminadas.contains("demo_rocky") && !eliminadas.contains("nombre_Rocky") && !eliminadas.contains("rocky")) {
                resultado.add(new Mascota("demo_rocky", "Rocky", "Perro", "Macho", true, "Golden Retriever", "18/09/2023", Arrays.asList("Dorado", "Blanco")));
            }
            if (!eliminadas.contains("demo_luna") && !eliminadas.contains("nombre_Luna") && !eliminadas.contains("luna")) {
                resultado.add(new Mascota("demo_luna", "Luna", "Gato", "Hembra", false, "Siamés", "15/05/2025", Arrays.asList("Café", "Blanco")));
            }
            guardarListaDirecta(prefs, uid, resultado);
            return resultado;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Mascota m = Mascota.fromJson(obj);
                if (m != null && !isEliminada(m, eliminadas)) {
                    resultado.add(m);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return resultado;
    }

    public static synchronized Mascota getMascotaById(Context context, String uid, String id) {
        if (context == null || id == null || id.trim().isEmpty()) return null;
        List<Mascota> lista = getMascotas(context, uid);
        for (Mascota m : lista) {
            if (id.equals(m.getId())) return m;
        }
        return null;
    }

    public static synchronized void saveOrUpdateMascota(Context context, String uid, Mascota mascota) {
        if (context == null || mascota == null) return;
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
            boolean mismoNombre = existente.getNombre() != null && mascota.getNombre() != null &&
                    existente.getNombre().trim().equalsIgnoreCase(mascota.getNombre().trim());

            if (mismoId || mismoNombre) {
                listaActual.set(i, mascota);
                actualizado = true;
                break;
            }
        }

        if (!actualizado) {
            listaActual.add(mascota);
        }

        // Si la mascota estaba en la lista de eliminadas, desmarcarla
        Set<String> eliminadas = getEliminadas(context, uid);
        boolean huboCambiosEliminadas = false;
        if (mascota.getId() != null && eliminadas.remove(mascota.getId())) {
            huboCambiosEliminadas = true;
        }
        if (mascota.getNombre() != null) {
            huboCambiosEliminadas |= eliminadas.remove("nombre_" + mascota.getNombre().trim());
            huboCambiosEliminadas |= eliminadas.remove(mascota.getNombre().trim().toLowerCase());
        }
        if (huboCambiosEliminadas) {
            prefs.edit().putStringSet(KEY_DELETED_PREFIX + uid, eliminadas).apply();
        }

        guardarListaDirecta(prefs, uid, listaActual);
    }

    public static synchronized void deleteMascota(Context context, String uid, String petId, String petNombre) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 1. Agregar a eliminadas
        Set<String> eliminadas = getEliminadas(context, uid);
        if (petId != null && !petId.trim().isEmpty()) {
            eliminadas.add(petId);
        }
        if (petNombre != null && !petNombre.trim().isEmpty()) {
            eliminadas.add("nombre_" + petNombre.trim());
            eliminadas.add(petNombre.trim().toLowerCase());
        }
        prefs.edit().putStringSet(KEY_DELETED_PREFIX + uid, eliminadas).apply();

        // 2. Remover de lista local
        List<Mascota> listaActual = getMascotas(context, uid);
        List<Mascota> nuevaLista = new ArrayList<>();
        for (Mascota m : listaActual) {
            boolean coincideId = petId != null && petId.equals(m.getId());
            boolean coincideNombre = petNombre != null && petNombre.equalsIgnoreCase(m.getNombre());
            if (!coincideId && !coincideNombre) {
                nuevaLista.add(m);
            }
        }

        guardarListaDirecta(prefs, uid, nuevaLista);
    }

    public static synchronized void mergeFromFirestore(Context context, String uid, List<Mascota> firestoreList) {
        if (context == null || firestoreList == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> eliminadas = getEliminadas(context, uid);

        List<Mascota> locales = getMascotas(context, uid);
        for (Mascota remota : firestoreList) {
            if (isEliminada(remota, eliminadas)) continue;
            boolean existe = false;
            for (int i = 0; i < locales.size(); i++) {
                Mascota local = locales.get(i);
                if ((remota.getId() != null && remota.getId().equals(local.getId())) ||
                        (remota.getNombre() != null && remota.getNombre().equalsIgnoreCase(local.getNombre()))) {
                    locales.set(i, remota);
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                locales.add(remota);
            }
        }

        guardarListaDirecta(prefs, uid, locales);
    }

    private static void guardarListaDirecta(SharedPreferences prefs, String uid, List<Mascota> lista) {
        JSONArray arr = new JSONArray();
        for (Mascota m : lista) {
            arr.put(m.toJson());
        }
        prefs.edit().putString(KEY_LIST_PREFIX + uid, arr.toString()).apply();
    }
}
