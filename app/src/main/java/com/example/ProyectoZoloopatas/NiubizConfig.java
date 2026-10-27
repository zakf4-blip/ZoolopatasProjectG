package com.example.ProyectoZoloopatas;

/**
 * Configuración de integración para la Pasarela de Pago Niubiz (Entorno Sandbox / Desarrollo)
 * Basado en la especificación de niubiz-main.
 */
public final class NiubizConfig {

    private NiubizConfig() {}

    // ── Credenciales de Desarrollo (Sandbox) ──
    public static final String MERCHANT_ID = "456879852";
    public static final String USER = "integraciones@niubiz.com.pe";
    public static final String PASSWORD = "_7z3@8fF";

    // ── Endpoints de la API Niubiz ──
    public static final String URL_SECURITY = "https://apisandbox.vnforappstest.com/api.security/v1/security";
    public static final String URL_SESSION = "https://apisandbox.vnforappstest.com/api.ecommerce/v2/ecommerce/token/session/";
    public static final String URL_CHECKOUT_JS = "https://static-content-qas.vnforapps.com/env/sandbox/js/checkout.js";
    public static final String URL_AUTHORIZATION = "https://apisandbox.vnforappstest.com/api.authorization/v3/authorization/ecommerce/";
    public static final String PROXY_URL = "https://zoloopatas.app/niubiz/callback";
    public static final String RESPONSE_URL = "https://zoloopatas.app/niubiz/response";
    public static final String LOGO_URL = "https://i.ibb.co/1JtQKJNk/logo-with.png";

    // ── Precios de los planes en Soles (PEN) ──
    public static final double PRECIO_MENSUAL = 16.99;
    public static final double PRECIO_ANUAL = 134.99;

    public static final String PLAN_MENSUAL = "Plan Mensual";
    public static final String PLAN_ANUAL = "Plan Anual";

    // ── Webhook / Servicio en la nube para despacho de correos por Internet ──
    // Se conecta a Google Apps Script Web App para envío 100% automático al correo
    public static final String EMAIL_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbw7joQ7fps0gQVKWRiJaxaj-1peDDfAyo1OaLkqHek7fKpSFcqQyrdnZx5VxTW-oeRP/exec";
}
