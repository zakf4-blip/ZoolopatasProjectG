package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio encargado de generar y despachar el comprobante de compra oficial por correo electrónico
 * a través de internet tras completarse una transacción en la pasarela Niubiz.
 */
public class NiubizEmailService {

    private static final String TAG = "NiubizEmailService";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface EmailCallback {
        void onCompleted(boolean enviado, String detalle);
    }

    /**
     * Retorna la fecha y hora oficial actual en la zona horaria de Niubiz Perú (America/Lima, UTC-5).
     */
    public static String obtenerFechaHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("es", "PE"));
        sdf.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return sdf.format(new Date());
    }

    /**
     * Envía por internet en segundo plano el correo electrónico con los datos de la compra en Niubiz,
     * registra el comprobante en Firestore en el perfil del usuario, y notifica el resultado.
     */
    public static void enviarComprobanteCompra(
            Context context,
            String userEmail,
            String userName,
            String planName,
            double amount,
            String purchaseNumber,
            String authCode,
            String cardMask,
            EmailCallback callback) {

        final String fechaHora = obtenerFechaHoraActual();
        final String nombreCliente = (userName != null && !userName.trim().isEmpty()) ? userName : "Estimado/a Usuario/a";
        final String tarjetaFinal = (cardMask != null && !cardMask.trim().isEmpty()) ? cardMask : "Método de Pago Niubiz";
        final String montoFormateado = String.format(Locale.US, "S/ %.2f PEN", amount);

        // Garantizar número de operación único y variado
        if (purchaseNumber == null || purchaseNumber.trim().isEmpty()) {
            purchaseNumber = NiubizService.generatePurchaseNumber();
        }
        final String finalPurchaseNumber = purchaseNumber;

        // Garantizar código de autorización estándar bancario de 6 dígitos que varía por cada operación
        boolean esYape = tarjetaFinal.toLowerCase().contains("yape");
        if (authCode == null || authCode.trim().isEmpty() || authCode.startsWith("AUTH_") || authCode.startsWith("SANDBOX_") || authCode.startsWith("NBZ_")) {
            authCode = NiubizPaymentActivity.generarCodigoAutorizacion(esYape);
        }
        final String finalAuthCode = authCode;

        final String textoComprobante = generarTextoComprobante(
                nombreCliente, userEmail, planName, montoFormateado,
                finalPurchaseNumber, finalAuthCode, tarjetaFinal, fechaHora
        );

        final String htmlComprobante = generarHtmlComprobante(
                nombreCliente, userEmail, planName, montoFormateado,
                finalPurchaseNumber, finalAuthCode, tarjetaFinal, fechaHora
        );

        // 1. Guardar comprobante de auditoría en Firestore dentro del documento del usuario usuarios/{uid}
        registrarComprobanteEnFirestore(userEmail, nombreCliente, planName, amount,
                finalPurchaseNumber, finalAuthCode, tarjetaFinal, fechaHora, textoComprobante, htmlComprobante);

        // 2. Despachar correo por internet mediante Webhook (Google Apps Script) en segundo plano
        executor.execute(() -> {
            boolean exitoEnvio = false;
            StringBuilder detalleEnvio = new StringBuilder();

            try {
                if (userEmail != null && userEmail.contains("@")) {
                    String webhookUrl = NiubizConfig.EMAIL_WEBHOOK_URL;

                    if (webhookUrl != null && !webhookUrl.trim().isEmpty() && !webhookUrl.contains("TU_WEBHOOK")) {
                        // Soportar envío a múltiples correos si se pasa lista separada por comas
                        String[] destinatarios = userEmail.split("[,;]");

                        for (String destino : destinatarios) {
                            String emailLimpio = destino.trim();
                            if (!emailLimpio.contains("@")) continue;

                            try {
                                URL url = new URL(webhookUrl.trim());
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                                conn.setConnectTimeout(15000);
                                conn.setReadTimeout(15000);
                                conn.setDoOutput(true);
                                conn.setInstanceFollowRedirects(false);

                                JSONObject payload = new JSONObject();
                                payload.put("to", emailLimpio);
                                payload.put("subject", "Comprobante de Compra Niubiz - Zoolopatas Premium");
                                payload.put("clientName", nombreCliente);
                                payload.put("plan", planName);
                                payload.put("amount", montoFormateado);
                                payload.put("purchaseNumber", finalPurchaseNumber);
                                payload.put("authCode", finalAuthCode);
                                payload.put("paymentMethod", tarjetaFinal);
                                payload.put("date", fechaHora);
                                payload.put("html", htmlComprobante);
                                payload.put("body", textoComprobante);
                                payload.put("text", textoComprobante);

                                try (OutputStream os = conn.getOutputStream()) {
                                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                                    os.flush();
                                }

                                int code = conn.getResponseCode();
                                Log.d(TAG, "Envío a " + emailLimpio + " -> Código HTTP Google Apps Script: " + code);

                                // En Google Apps Script Web App: un código 302 Found o 200 OK confirma
                                // que el servidor de Google ejecutó doPost() y despachó MailApp.sendEmail()
                                if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
                                        || code == 307 || code == 302 || (code >= 200 && code < 300)) {
                                    exitoEnvio = true;
                                    detalleEnvio.append("[").append(emailLimpio).append(": Enviado OK (HTTP ").append(code).append(")] ");
                                } else {
                                    detalleEnvio.append("[").append(emailLimpio).append(": Error HTTP ").append(code).append("] ");
                                }

                                conn.disconnect();
                            } catch (Exception errDestino) {
                                Log.e(TAG, "Error enviando a " + emailLimpio + ": " + errDestino.getMessage());
                                detalleEnvio.append("[").append(emailLimpio).append(": ").append(errDestino.getMessage()).append("] ");
                            }
                        }
                    } else {
                        Log.i(TAG, "EMAIL_WEBHOOK_URL no configurado aún en NiubizConfig.");
                        detalleEnvio.append("Webhook no configurado");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error global despachando correo por internet: " + e.getMessage(), e);
                detalleEnvio.append(e.getMessage());
            }

            final boolean finalExito = exitoEnvio;
            final String finalDetalle = detalleEnvio.toString();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    callback.onCompleted(finalExito, finalDetalle);
                }
            });
        });
    }

    /**
     * Construye el cuerpo del correo en texto claro y profesional.
     */
    public static String generarTextoComprobante(
            String nombreCliente,
            String userEmail,
            String planName,
            String montoFormateado,
            String purchaseNumber,
            String authCode,
            String tarjetaFinal,
            String fechaHora) {

        return "=======================================================\n"
                + "         COMPROBANTE DE PAGO - NIUBIZ PERÚ\n"
                + "               ZOOLOPATAS PREMIUM\n"
                + "=======================================================\n\n"
                + "Hola " + nombreCliente + ",\n"
                + "Te confirmamos que tu compra a través de la pasarela Niubiz\n"
                + "se ha procesado exitosamente por internet. A continuación el detalle:\n\n"
                + "DATOS DE LA COMPRA:\n"
                + "-------------------------------------------------------\n"
                + "• Concepto: Suscripción Zoolopatas Premium (" + planName + ")\n"
                + "• Monto total: " + montoFormateado + "\n"
                + "• Pasarela: Niubiz Perú (Comercio #456879852)\n"
                + "• N° de Operación: #" + purchaseNumber + "\n"
                + "• Código de Autorización: " + authCode + "\n"
                + "• Fecha y Hora: " + fechaHora + "\n"
                + "• Estado: APROBADO / PLAN ACTIVO\n\n"
                + "DATOS DEL MEDIO DE PAGO:\n"
                + "-------------------------------------------------------\n"
                + "• Medio de Pago: " + tarjetaFinal + "\n"
                + "• Titular registrado: " + nombreCliente + "\n"
                + "• Correo registrado: " + userEmail + "\n\n"
                + "BENEFICIOS ACTIVADOS EN TU CUENTA:\n"
                + "-------------------------------------------------------\n"
                + "✔ Capacidad para registrar hasta 20 mascotas\n"
                + "✔ Historiales clínicos completos y descargables en PDF\n"
                + "✔ Recordatorios ilimitados de vacunas y antiparasitarios\n"
                + "✔ Acceso prioritario a novedades y telemetría\n\n"
                + "¡Gracias por confiar en Zoolopatas para el bienestar de tus mascotas!\n"
                + "=======================================================";
    }

    /**
     * Construye el cuerpo del correo en formato HTML enriquecido y moderno.
     */
    public static String generarHtmlComprobante(
            String nombreCliente,
            String userEmail,
            String planName,
            String montoFormateado,
            String purchaseNumber,
            String authCode,
            String tarjetaFinal,
            String fechaHora) {

        return "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #2C3433; color: #FFFFFF; border-radius: 16px; overflow: hidden; border: 1px solid #2DD4BF;\">"
                + "<div style=\"background: linear-gradient(135deg, #0E7C7B, #2DD4BF); padding: 24px; text-align: center;\">"
                + "<h1 style=\"margin: 0; color: #000000; font-size: 24px; font-weight: 800;\">🐾 Zoolopatas Premium</h1>"
                + "<p style=\"margin: 6px 0 0 0; color: #143834; font-weight: 700; font-size: 14px;\">Comprobante Oficial de Compra - Niubiz</p>"
                + "</div>"
                + "<div style=\"padding: 24px;\">"
                + "<p style=\"font-size: 16px; margin-top: 0;\">Hola <b>" + nombreCliente + "</b>,</p>"
                + "<p style=\"font-size: 14px; color: #B2C2C1; line-height: 1.5;\">Tu pago ha sido procesado exitosamente a través de la pasarela oficial de <b>Niubiz Perú</b>. Tu suscripción a <b>Zoolopatas Premium</b> ya se encuentra activa.</p>"
                + "<div style=\"background-color: #3A4342; border-radius: 12px; padding: 18px; margin: 20px 0; border-left: 4px solid #2DD4BF;\">"
                + "<h3 style=\"margin: 0 0 12px 0; font-size: 16px; color: #2DD4BF;\">Detalle de la Transacción</h3>"
                + "<table style=\"width: 100%; font-size: 13px; color: #E0E7E6; border-collapse: collapse;\">"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Plan adquirido:</td><td style=\"text-align: right; font-weight: bold;\">" + planName + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Monto pagado:</td><td style=\"text-align: right; font-weight: bold; color: #2DD4BF;\">" + montoFormateado + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Medio de pago:</td><td style=\"text-align: right; font-weight: bold;\">" + tarjetaFinal + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">N° de Operación:</td><td style=\"text-align: right; font-family: monospace;\">#" + purchaseNumber + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Cód. Autorización:</td><td style=\"text-align: right; font-family: monospace;\">" + authCode + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Fecha y hora:</td><td style=\"text-align: right;\">" + fechaHora + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #B2C2C1;\">Estado:</td><td style=\"text-align: right; color: #4ADE80; font-weight: bold;\">APROBADO ✓</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style=\"background-color: #272F2E; border-radius: 12px; padding: 16px; margin-bottom: 20px;\">"
                + "<h4 style=\"margin: 0 0 10px 0; font-size: 14px; color: #FFFFFF;\">Tus beneficios desbloqueados:</h4>"
                + "<ul style=\"margin: 0; padding-left: 20px; font-size: 13px; color: #B2C2C1; line-height: 1.6;\">"
                + "<li>Hasta 20 mascotas registradas simultáneamente</li>"
                + "<li>Historial médico completo e ilimitado</li>"
                + "<li>Recordatorios automáticos de vacunas y desparasitación</li>"
                + "<li>Soporte prioritario para el cuidado de tus mascotas</li>"
                + "</ul>"
                + "</div>"
                + "<p style=\"font-size: 12px; color: #8E8E93; text-align: center; margin-bottom: 0;\">Este es un comprobante electrónico emitido para " + userEmail + ".<br>Procesado de forma segura por Niubiz Perú para Zoolopatas.</p>"
                + "</div>"
                + "</div>";
    }

    private static void registrarComprobanteEnFirestore(
            String userEmail,
            String nombreCliente,
            String planName,
            double amount,
            String purchaseNumber,
            String authCode,
            String tarjetaFinal,
            String fechaHora,
            String textoComprobante,
            String htmlComprobante) {

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            String uid = user.getUid();

            Map<String, Object> data = new HashMap<>();
            data.put("purchaseNumber", purchaseNumber);
            data.put("authorizationCode", authCode);
            data.put("plan", planName);
            data.put("monto", amount);
            data.put("moneda", "PEN");
            data.put("tarjeta", tarjetaFinal);
            data.put("metodoPago", tarjetaFinal);
            data.put("cliente", nombreCliente);
            data.put("correo", userEmail);
            data.put("fechaTexto", fechaHora);
            data.put("fechaTimestamp", FieldValue.serverTimestamp());
            data.put("textoComprobante", textoComprobante);
            data.put("estado", "COMPLETADO");

            // Guardar dentro del documento permitido del usuario usuarios/{uid}
            Map<String, Object> update = new HashMap<>();
            update.put("ultimoComprobante", data);
            update.put("comprobantes." + purchaseNumber, data);
            update.put("subscription.tier", "premium");
            update.put("subscription.status", "active");
            update.put("subscription.plan", planName);
            update.put("subscription.paymentMethod", tarjetaFinal);
            update.put("subscription.purchaseNumber", purchaseNumber);
            update.put("subscription.authCode", authCode);
            update.put("subscription.amount", amount);
            update.put("subscription.updatedAt", FieldValue.serverTimestamp());

            db.collection("usuarios").document(uid).set(update, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Comprobante guardado exitosamente en Firestore para: " + uid))
                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando comprobante en usuarios/" + uid + ": " + e.getMessage()));

        } catch (Exception e) {
            Log.e(TAG, "Excepción registrando comprobante en Firestore: " + e.getMessage());
        }
    }
}
