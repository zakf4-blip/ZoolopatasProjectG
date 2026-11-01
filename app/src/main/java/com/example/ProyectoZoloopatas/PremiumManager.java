package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de estado de suscripción Premium individualizado por usuario (UID).
 * Garantiza que cada cuenta mantenga su propio plan independiente.
 */
public class PremiumManager {

    private static final String TAG = "PremiumManager";
    private static final String PREF_NAME = "ZoolopatasPrefs";

    // Prefijos individualizados por UID de Firebase
    private static final String KEY_PREFIX_ES_PREMIUM = "es_premium_";
    private static final String KEY_PREFIX_PLAN = "plan_premium_";
    private static final String KEY_PREFIX_MONTO = "monto_premium_";
    private static final String KEY_PREFIX_PURCHASE = "purchase_number_premium_";
    private static final String KEY_PREFIX_FECHA = "fecha_suscripcion_premium_";
    private static final String KEY_PREFIX_FECHA_FIN = "fecha_fin_suscripcion_premium_";
    private static final String KEY_PREFIX_DURACION = "duracion_dias_premium_";

    public static String getUidActual() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : "usuario_default";
    }

    public static boolean isPremium(Context context) {
        return isPremium(context, getUidActual());
    }

    public static boolean isPremium(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return false;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean premium = sp.getBoolean(KEY_PREFIX_ES_PREMIUM + uid, false);
        if (!premium) return false;

        // Comprobar si el periodo de suscripción ya venció
        long fechaFin = sp.getLong(KEY_PREFIX_FECHA_FIN + uid, 0L);
        if (fechaFin > 0L && System.currentTimeMillis() >= fechaFin) {
            sp.edit().putBoolean(KEY_PREFIX_ES_PREMIUM + uid, false).apply();
            return false;
        }
        return true;
    }

    public static String getPlan(Context context) {
        return getPlan(context, getUidActual());
    }

    public static String getPlan(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return "Plan Gratuito";
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_PREFIX_PLAN + uid, "Plan Gratuito");
    }

    public static int getLimiteMascotas(Context context) {
        return isPremium(context) ? 20 : 3;
    }

    public static int getLimiteMascotas(Context context, String uid) {
        return isPremium(context, uid) ? 20 : 3;
    }

    public static int getDiasRestantes(Context context) {
        return getDiasRestantes(context, getUidActual());
    }

    public static int getDiasRestantes(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return 0;
        if (!isPremium(context, uid)) return 0;

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long ahora = System.currentTimeMillis();
        long fechaFin = sp.getLong(KEY_PREFIX_FECHA_FIN + uid, 0L);

        if (fechaFin == 0L) {
            long fechaInicio = sp.getLong(KEY_PREFIX_FECHA + uid, 0L);
            String plan = getPlan(context, uid);
            int duracion = (plan != null && plan.toLowerCase().contains("anual")) ? 365 : 30;
            if (fechaInicio > 0L) {
                fechaFin = fechaInicio + (duracion * 24L * 60L * 60L * 1000L);
                sp.edit().putLong(KEY_PREFIX_FECHA_FIN + uid, fechaFin).apply();
            } else {
                return duracion;
            }
        }

        long diffMillis = fechaFin - ahora;
        if (diffMillis <= 0) {
            sp.edit().putBoolean(KEY_PREFIX_ES_PREMIUM + uid, false).apply();
            return 0;
        }

        int dias = (int) Math.ceil((double) diffMillis / (24.0 * 60.0 * 60.0 * 1000.0));
        return Math.max(1, dias);
    }

    public static String getTextoBotonPremium(Context context) {
        int dias = getDiasRestantes(context);
        if (dias <= 0) {
            return "Tu plan Premium está activado ✓";
        } else if (dias == 1) {
            return "Tu plan Premium está activado ✓\n(1 día restante)";
        } else {
            return "Tu plan Premium está activado ✓\n(" + dias + " días restantes)";
        }
    }

    public static String getTextoDiasRestantes(Context context) {
        int dias = getDiasRestantes(context);
        if (dias <= 0) {
            return "Plan activo";
        } else if (dias == 1) {
            return "Te queda 1 día de suscripción";
        } else {
            return "Te quedan " + dias + " días de suscripción";
        }
    }

    public static void activarPremium(Context context, String plan, double amount, String purchaseNumber, Runnable onComplete) {
        String uid = getUidActual();
        long ahora = System.currentTimeMillis();
        int duracionDias = (plan != null && plan.toLowerCase().contains("anual")) ? 365 : 30;
        long duracionMillis = duracionDias * 24L * 60L * 60L * 1000L;
        long fechaFin = ahora + duracionMillis;

        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit()
                    .putBoolean(KEY_PREFIX_ES_PREMIUM + uid, true)
                    .putString(KEY_PREFIX_PLAN + uid, plan)
                    .putFloat(KEY_PREFIX_MONTO + uid, (float) amount)
                    .putString(KEY_PREFIX_PURCHASE + uid, purchaseNumber)
                    .putLong(KEY_PREFIX_FECHA + uid, ahora)
                    .putLong(KEY_PREFIX_FECHA_FIN + uid, fechaFin)
                    .putInt(KEY_PREFIX_DURACION + uid, duracionDias)
                    // Limpiar posibles claves globales antiguas para evitar interferencias
                    .remove("es_premium")
                    .remove("plan_premium")
                    .apply();
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Objeto suscripcion estructurado según BDNoSQLZoolopatas.json
            Map<String, Object> suscripcion = new HashMap<>();
            suscripcion.put("nivel", "premium");
            suscripcion.put("plan", plan);
            suscripcion.put("esPremium", true);
            suscripcion.put("periodoFacturacion", (plan != null && plan.toLowerCase().contains("anual")) ? "anual" : "mensual");
            suscripcion.put("estado", "active");
            suscripcion.put("fechaInicio", FieldValue.serverTimestamp());
            suscripcion.put("fechaExpiracion", new com.google.firebase.Timestamp(new java.util.Date(fechaFin)));

            Map<String, Object> data = new HashMap<>();
            data.put("suscripcion", suscripcion);
            data.put("fechaActualizacionCuenta", FieldValue.serverTimestamp());

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Guardar en documento del usuario
            db.collection("usuarios").document(uid)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // 2. Registrar en subcolección usuarios/{uid}/historialPagos
                        Map<String, Object> pago = new HashMap<>();
                        pago.put("idPlan", plan);
                        pago.put("importe", amount);
                        pago.put("moneda", "PEN");
                        pago.put("estado", "pago realizado exitosamente");
                        pago.put("fechaCreacion", FieldValue.serverTimestamp());

                        db.collection("usuarios").document(uid)
                                .collection("historialPagos").document(purchaseNumber)
                                .set(pago, SetOptions.merge());

                        Log.d(TAG, "Plan Premium activado individualmente en Firestore para usuario: " + uid);
                        if (onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error guardando en Firestore para " + uid, e);
                        if (onComplete != null) {
                            new Handler(Looper.getMainLooper()).post(onComplete);
                        }
                    });
        } else {
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        }
    }

    public static void sincronizarConFirestore(Context context, Runnable onSynced) {
        if (context == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (onSynced != null) onSynced.run();
            return;
        }

        final String uid = user.getUid();
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

                    boolean isPrem = false;
                    String plan = "Plan Gratuito";
                    long fechaFin = 0L;
                    long ahora = System.currentTimeMillis();

                    if (doc != null && doc.exists()) {
                        // Única fuente oficial del plan: objeto "suscripcion"
                        Object subObj = doc.get("suscripcion");
                        if (subObj instanceof Map) {
                            Map<?, ?> subMap = (Map<?, ?>) subObj;
                            String nivel = subMap.get("nivel") != null ? String.valueOf(subMap.get("nivel")) : "";
                            String estado = subMap.get("estado") != null ? String.valueOf(subMap.get("estado")) : "";

                            isPrem = "premium".equalsIgnoreCase(nivel) && "active".equalsIgnoreCase(estado);

                            if (subMap.get("plan") != null) {
                                plan = String.valueOf(subMap.get("plan"));
                            }

                            Object expObj = subMap.get("fechaExpiracion");
                            if (expObj instanceof com.google.firebase.Timestamp) {
                                fechaFin = ((com.google.firebase.Timestamp) expObj).toDate().getTime();
                            }

                            // Si no hay fecha de expiración, calcularla a partir de fechaInicio
                            if (isPrem && fechaFin <= 0) {
                                long inicio = ahora;
                                Object iniObj = subMap.get("fechaInicio");
                                if (iniObj instanceof com.google.firebase.Timestamp) {
                                    inicio = ((com.google.firebase.Timestamp) iniObj).toDate().getTime();
                                }
                                int dur = plan.toLowerCase().contains("anual") ? 365 : 30;
                                fechaFin = inicio + (dur * 24L * 60L * 60L * 1000L);
                            }
                        }
                    }

                    // Verificar expiración automática
                    if (isPrem && fechaFin <= ahora) {
                        isPrem = false;
                        plan = "Plan Gratuito";
                    }

                    sp.edit()
                            .putBoolean(KEY_PREFIX_ES_PREMIUM + uid, isPrem)
                            .putString(KEY_PREFIX_PLAN + uid, isPrem ? plan : "Plan Gratuito")
                            .putLong(KEY_PREFIX_FECHA_FIN + uid, isPrem ? fechaFin : 0L)
                            .remove("es_premium")
                            .remove("plan_premium")
                            .apply();

                    if (onSynced != null) {
                        onSynced.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (onSynced != null) {
                        onSynced.run();
                    }
                });
    }

    public static void limpiarSesionUsuario(Context context) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .remove("es_premium")
                .remove("plan_premium")
                .apply();
    }

    public static void cargarFotoAvatar(android.content.Context context, android.widget.ImageButton btnAvatar) {
        if (btnAvatar == null || context == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String urlFoto = doc.getString("urlFoto");
                        if (urlFoto != null && !urlFoto.isEmpty()) {
                            // Quitar padding para que la foto llene el círculo
                            btnAvatar.setPadding(0, 0, 0, 0);

                            // Usar Glide para cargar la imagen desde Supabase
                            com.bumptech.glide.Glide.with(context)
                                    .load(urlFoto)
                                    .placeholder(R.drawable.ic_person)
                                    .into(btnAvatar);
                        }
                    }
                });
    }
}
