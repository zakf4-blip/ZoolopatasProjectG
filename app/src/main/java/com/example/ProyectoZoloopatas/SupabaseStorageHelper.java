package com.example.ProyectoZoloopatas;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseStorageHelper {
    private static final String SUPABASE_URL = "https://jurrdzyvhpgozjwhdyzr.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp1cnJkenl2aHBnb3pqd2hkeXpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMxNDQxMTgsImV4cCI6MjEwODcyMDExOH0.jisb5E_g9qMtz0EU_dq-OJ7aQr-Uqro2ktWua6f23Lc";
    private static final String BUCKET_NAME = "usuarios-perfiles-avatars"; // Nombre de tu bucket en Supabase

    public interface UploadCallback {
        void onSuccess(String publicUrl);

        void onError(String error);
    }

    public static void subirFoto(File archivoFoto, String uidUsuario, UploadCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        String nombreArchivo = uidUsuario + "_" + System.currentTimeMillis() + ".jpg";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", nombreArchivo,
                        RequestBody.create(archivoFoto, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + nombreArchivo)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                // .addHeader("x-upsert", "true") // Descomenta solo si quieres sobrescribir
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + nombreArchivo;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(publicUrl));
                } else {
                    // Esto te dará más detalles del error 400
                    String errorBody = response.body() != null ? response.body().string() : "Sin cuerpo";
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Error: " + response.code() + " - " + response.message() + " - Body: " + errorBody)
                    );
                }
            }
        });
    }
}