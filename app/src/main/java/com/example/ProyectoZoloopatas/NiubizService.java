package com.example.ProyectoZoloopatas;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NiubizService {

    private static final String TAG = "NiubizService";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SessionCallback {
        void onSuccess(String sessionKey, String purchaseNumber, String token);
        void onError(String errorMessage);
    }

    public interface AuthCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    public static String generatePurchaseNumber() {
        // Genera un número de operación bancario único de 8 dígitos que varía notablemente en cada transacción
        int randomPart = new java.security.SecureRandom().nextInt(90000000) + 10000000;
        return String.valueOf(randomPart);
    }

    public static void crearSesion(double amount, String userEmail, SessionCallback callback) {
        final String purchaseNumber = generatePurchaseNumber();
        final String email = (userEmail != null && !userEmail.trim().isEmpty()) ? userEmail.trim() : "cliente@zoolopatas.com";

        executor.execute(() -> {
            try {
                // 1. Obtener Token de Seguridad (Security Token)
                String authString = NiubizConfig.USER + ":" + NiubizConfig.PASSWORD;
                String authHeader = "Basic " + Base64.encodeToString(authString.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

                URL securityUrl = new URL(NiubizConfig.URL_SECURITY);
                HttpURLConnection connSecurity = (HttpURLConnection) securityUrl.openConnection();
                connSecurity.setRequestMethod("POST");
                connSecurity.setRequestProperty("Authorization", authHeader);
                connSecurity.setRequestProperty("Accept", "*/*");
                connSecurity.setConnectTimeout(10000);
                connSecurity.setReadTimeout(10000);

                int secCode = connSecurity.getResponseCode();
                if (secCode < 200 || secCode >= 300) {
                    throw new Exception("Error al autenticar con Niubiz (Código HTTP " + secCode + ")");
                }

                String token = readStream(connSecurity.getInputStream());
                connSecurity.disconnect();

                if (token == null || token.trim().isEmpty()) {
                    throw new Exception("No se recibió token de seguridad válido de Niubiz");
                }
                token = token.trim();

                // 2. Crear Sesión en Niubiz con el monto del plan
                URL sessionUrl = new URL(NiubizConfig.URL_SESSION + NiubizConfig.MERCHANT_ID);
                HttpURLConnection connSession = (HttpURLConnection) sessionUrl.openConnection();
                connSession.setRequestMethod("POST");
                connSession.setRequestProperty("Authorization", token);
                connSession.setRequestProperty("Content-Type", "application/json");
                connSession.setRequestProperty("Accept", "application/json");
                connSession.setDoOutput(true);
                connSession.setConnectTimeout(10000);
                connSession.setReadTimeout(10000);

                JSONObject antifraud = new JSONObject();
                antifraud.put("clientIp", "190.236.12.34");

                JSONObject mdd = new JSONObject();
                mdd.put("MDD4", email);
                mdd.put("MDD21", 1);
                mdd.put("MDD32", purchaseNumber);
                mdd.put("MDD75", "Registrado");
                mdd.put("MDD77", 450);
                antifraud.put("merchantDefineData", mdd);

                JSONObject body = new JSONObject();
                body.put("amount", amount);
                body.put("channel", "web");
                body.put("antifraud", antifraud);

                try (OutputStream os = connSession.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int sessionCode = connSession.getResponseCode();
                InputStream is = (sessionCode >= 200 && sessionCode < 300) ?
                        connSession.getInputStream() : connSession.getErrorStream();

                String sessionRespStr = readStream(is);
                connSession.disconnect();

                if (sessionCode < 200 || sessionCode >= 300) {
                    throw new Exception("Error al generar sesión Niubiz: " + sessionRespStr);
                }

                JSONObject sessionJson = new JSONObject(sessionRespStr);
                String sessionKey = sessionJson.optString("sessionKey", null);

                if (sessionKey == null || sessionKey.isEmpty()) {
                    throw new Exception("Niubiz no retornó sessionKey: " + sessionRespStr);
                }

                final String finalToken = token;
                mainHandler.post(() -> callback.onSuccess(sessionKey, purchaseNumber, finalToken));

            } catch (Exception e) {
                Log.e(TAG, "Error en comunicación con Niubiz", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Error de conexión"));
            }
        });
    }

    public static void autorizarPago(String securityToken, String purchaseNumber, double amount, String transactionToken, AuthCallback callback) {
        executor.execute(() -> {
            try {
                URL authUrl = new URL(NiubizConfig.URL_AUTHORIZATION + NiubizConfig.MERCHANT_ID);
                HttpURLConnection conn = (HttpURLConnection) authUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", securityToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject order = new JSONObject();
                order.put("amount", amount);
                order.put("currency", "PEN");
                order.put("purchaseNumber", purchaseNumber);
                order.put("tokenId", transactionToken);

                JSONObject body = new JSONObject();
                body.put("antifraud", JSONObject.NULL);
                body.put("captureType", "manual");
                body.put("channel", "web");
                body.put("countable", true);
                body.put("order", order);
                body.put("recurrence", JSONObject.NULL);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String respStr = readStream(is);
                conn.disconnect();

                JSONObject json = new JSONObject(respStr);
                mainHandler.post(() -> {
                    if (code >= 200 && code < 300) {
                        callback.onSuccess(json);
                    } else {
                        String desc = json.optJSONObject("dataMap") != null ?
                                json.optJSONObject("dataMap").optString("ACTION_DESCRIPTION", respStr) : respStr;
                        callback.onError(desc);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error en autorizarPago", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Error en autorización"));
            }
        });
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
