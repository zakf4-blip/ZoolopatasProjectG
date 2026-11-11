package com.example.ProyectoZoloopatas;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper centralizado para el bucket "usuarios-perfiles-avatars" de Supabase Storage.
 *
 * Convención de nombres:
 *  - Foto de perfil : {UID}_{timestamp}.jpg
 *  - Foto de mascota: {UID}_{ID_MASCOTA}_{timestamp}.jpg
 *
 * Cada subida devuelve tanto la URL pública como la ruta interna del objeto. Esa ruta
 * interna es la que se guarda en Firestore (campo "rutaFoto") y la que permite eliminar
 * el archivo con seguridad, sin tener que reconstruirla a partir de la URL.
 */
public class SupabaseStorageHelper {
    private static final String SUPABASE_URL = "https://jur.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbG";
    private static final String BUCKET_NAME = "usuarios-perfiles-avatars";

    private static final MediaType TIPO_JPEG = MediaType.parse("image/jpeg");
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final OkHttpClient CLIENTE = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /** Callback clásico. Se conserva para no romper el código que ya lo utilizaba. */
    public interface UploadCallback {
        void onSuccess(String publicUrl);

        void onError(String error);
    }

    /** Callback con la ruta interna del objeto en el bucket. */
    public interface UploadWithPathCallback {
        void onSuccess(String publicUrl, String storagePath);

        void onError(String error);
    }

    /** Callback para operaciones de borrado. */
    public interface DeleteCallback {
        void onSuccess();

        void onError(String error);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SUBIDAS
    // ─────────────────────────────────────────────────────────────────────────────

    /** Sube una foto de perfil usando el formato {UID}_{timestamp}.jpg. */
    public static void subirFoto(@NonNull File archivoFoto, @NonNull String uidUsuario,
                                 @NonNull UploadCallback callback) {
        subirFotoPerfil(archivoFoto, uidUsuario, new UploadWithPathCallback() {
            @Override
            public void onSuccess(String publicUrl, String storagePath) {
                callback.onSuccess(publicUrl);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /** Sube una foto de perfil y devuelve también la ruta interna del objeto. */
    public static void subirFotoPerfil(@NonNull File archivoFoto, @NonNull String uidUsuario,
                                       @NonNull UploadWithPathCallback callback) {
        subirArchivo(archivoFoto, construirNombre(uidUsuario, null), callback);
    }

    /**
     * Sube la foto de una mascota usando el formato {UID}_{ID_MASCOTA}_{timestamp}.jpg.
     * El ID de la mascota se usa como parte del nombre para que sea fácil de auditar.
     */
    public static void subirFotoMascota(@NonNull File archivoFoto, @NonNull String uidUsuario,
                                        @Nullable String mascotaId,
                                        @NonNull UploadWithPathCallback callback) {
        subirArchivo(archivoFoto, construirNombre(uidUsuario, mascotaId), callback);
    }

    private static void subirArchivo(@NonNull File archivoFoto, @NonNull String nombreArchivo,
                                     @NonNull UploadWithPathCallback callback) {
        if (!archivoFoto.exists() || !archivoFoto.isFile()) {
            MAIN_HANDLER.post(() -> callback.onError("El archivo de imagen no existe."));
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", nombreArchivo, RequestBody.create(archivoFoto, TIPO_JPEG))
                .build();

        HttpUrl urlSubida = ObjectsCompat.urlObjeto(nombreArchivo);
        if (urlSubida == null) {
            MAIN_HANDLER.post(() -> callback.onError("No se pudo construir la URL de subida."));
            return;
        }

        Request request = new Request.Builder()
                .url(urlSubida)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                // No se activa x-upsert: cada imagen nueva tiene un nombre único.
                .post(requestBody)
                .build();

        CLIENTE.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN_HANDLER.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Error de conexión con Supabase."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response respuesta = response) {
                    if (respuesta.isSuccessful()) {
                        String publicUrl = SUPABASE_URL + "/storage/v1/object/public/"
                                + BUCKET_NAME + "/" + nombreArchivo;
                        MAIN_HANDLER.post(() -> callback.onSuccess(publicUrl, nombreArchivo));
                    } else {
                        String errorBody = respuesta.body() != null ? respuesta.body().string() : "Sin cuerpo";
                        MAIN_HANDLER.post(() -> callback.onError(
                                "Error: " + respuesta.code() + " - " + respuesta.message()
                                        + " - Body: " + errorBody));
                    }
                } catch (IOException e) {
                    MAIN_HANDLER.post(() -> callback.onError(
                            e.getMessage() != null ? e.getMessage() : "Error leyendo la respuesta de Supabase."));
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // BORRADOS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Elimina un objeto del bucket. Acepta tanto la ruta interna ("uid_123.jpg")
     * como la URL pública completa. Un 404 se considera éxito porque el archivo
     * ya no existe (operación idempotente).
     */
    public static void eliminarFoto(@Nullable String rutaOUrl) {
        eliminarFoto(rutaOUrl, null);
    }

    public static void eliminarFoto(@Nullable String rutaOUrl, @Nullable DeleteCallback callback) {
        String ruta = extraerRuta(rutaOUrl);
        if (ruta == null || ruta.isEmpty()) {
            if (callback != null) {
                MAIN_HANDLER.post(callback::onSuccess);
            }
            return;
        }
        eliminarRutaInterna(ruta, callback);
    }

    /**
     * Elimina varias fotos. El callback se ejecuta una sola vez cuando terminan
     * todas las peticiones, aunque algunas hayan fallado. De esta forma el flujo
     * de "Eliminar cuenta" nunca queda bloqueado por un error de Storage.
     */
    public static void eliminarFotos(@Nullable Collection<String> rutas,
                                     @Nullable DeleteCallback callback) {
        List<String> rutasLimpias = new ArrayList<>();
        if (rutas != null) {
            for (String rutaOUrl : rutas) {
                String ruta = extraerRuta(rutaOUrl);
                if (ruta != null && !ruta.isEmpty() && !rutasLimpias.contains(ruta)) {
                    rutasLimpias.add(ruta);
                }
            }
        }

        if (rutasLimpias.isEmpty()) {
            if (callback != null) {
                MAIN_HANDLER.post(callback::onSuccess);
            }
            return;
        }

        AtomicInteger pendientes = new AtomicInteger(rutasLimpias.size());
        List<String> errores = Collections.synchronizedList(new ArrayList<>());

        for (String ruta : rutasLimpias) {
            eliminarRutaInterna(ruta, new DeleteCallback() {
                @Override
                public void onSuccess() {
                    finalizarSiCorresponde();
                }

                @Override
                public void onError(String error) {
                    errores.add(ruta + ": " + error);
                    finalizarSiCorresponde();
                }

                private void finalizarSiCorresponde() {
                    if (pendientes.decrementAndGet() != 0) return;
                    if (callback == null) return;
                    if (errores.isEmpty()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("No se pudieron eliminar " + errores.size()
                                + " archivo(s): " + String.join("; ", errores));
                    }
                }
            });
        }
    }

    private static void eliminarRutaInterna(@NonNull String ruta,
                                            @Nullable DeleteCallback callback) {
        HttpUrl urlBorrado = ObjectsCompat.urlObjeto(ruta);
        if (urlBorrado == null) {
            if (callback != null) {
                MAIN_HANDLER.post(() -> callback.onError("Ruta de Storage inválida: " + ruta));
            }
            return;
        }

        Request request = new Request.Builder()
                .url(urlBorrado)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                .delete()
                .build();

        CLIENTE.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) {
                    MAIN_HANDLER.post(() -> callback.onError(
                            e.getMessage() != null ? e.getMessage() : "Error de conexión con Supabase."));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response respuesta = response) {
                    // 200/204 = eliminado. 404 = ya no existía, también es un resultado válido.
                    if (respuesta.isSuccessful() || respuesta.code() == 404) {
                        if (callback != null) {
                            MAIN_HANDLER.post(callback::onSuccess);
                        }
                    } else {
                        String errorBody = respuesta.body() != null ? respuesta.body().string() : "Sin cuerpo";
                        if (callback != null) {
                            MAIN_HANDLER.post(() -> callback.onError(
                                    "Error: " + respuesta.code() + " - " + respuesta.message()
                                            + " - Body: " + errorBody));
                        }
                    }
                } catch (IOException e) {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onError(
                                e.getMessage() != null ? e.getMessage() : "Error leyendo la respuesta de Supabase."));
                    }
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Devuelve la ruta interna del objeto a partir de una URL pública o de una
     * ruta ya almacenada. Ejemplos:
     *  - https://.../storage/v1/object/public/bucket/uid_1.jpg -> uid_1.jpg
     *  - bucket/uid_1.jpg -> uid_1.jpg
     *  - /uid_1.jpg -> uid_1.jpg
     */
    @Nullable
    public static String extraerRuta(@Nullable String rutaOUrl) {
        if (rutaOUrl == null) return null;
        String ruta = rutaOUrl.trim();
        if (ruta.isEmpty()) return null;

        int query = ruta.indexOf('?');
        if (query >= 0) ruta = ruta.substring(0, query);
        int hash = ruta.indexOf('#');
        if (hash >= 0) ruta = ruta.substring(0, hash);

        int indiceObjeto = ruta.indexOf("/storage/v1/object/");
        if (indiceObjeto >= 0) {
            ruta = ruta.substring(indiceObjeto + "/storage/v1/object/".length());
            if (ruta.startsWith("public/")) ruta = ruta.substring("public/".length());
            else if (ruta.startsWith("authenticated/")) ruta = ruta.substring("authenticated/".length());
        } else if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
            // URL de otro dominio: no es un objeto de este bucket.
            return null;
        }

        while (ruta.startsWith("/")) ruta = ruta.substring(1);
        if (ruta.startsWith(BUCKET_NAME + "/")) {
            ruta = ruta.substring(BUCKET_NAME.length() + 1);
        }

        if (ruta.isEmpty()) return null;

        try {
            // URLDecoder convierte '+' en espacio; se protege el '+' literal antes de decodificar.
            ruta = URLDecoder.decode(ruta.replace("+", "%2B"), StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            // Si la decodificación falla, se usa la ruta original.
        }
        return ruta;
    }

    @NonNull
    private static String construirNombre(@Nullable String uid, @Nullable String sufijo) {
        String base = limpiarSegmento(uid != null && !uid.trim().isEmpty() ? uid : "usuario");
        if (sufijo != null && !sufijo.trim().isEmpty()) {
            base += "_" + limpiarSegmento(sufijo);
        }
        return String.format(Locale.US, "%s_%d.jpg", base, System.currentTimeMillis());
    }

    @NonNull
    private static String limpiarSegmento(@NonNull String valor) {
        return valor.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** Pequeña clase interna para no depender de utilidades adicionales. */
    private static final class ObjectsCompat {
        @Nullable
        static HttpUrl urlObjeto(@NonNull String ruta) {
            HttpUrl base = HttpUrl.parse(SUPABASE_URL);
            if (base == null) return null;
            try {
                return base.newBuilder()
                        .addPathSegments("storage/v1/object")
                        .addPathSegment(BUCKET_NAME)
                        .addPathSegments(ruta)
                        .build();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
