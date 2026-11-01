package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class SuscripcionPremium extends AppCompatActivity {

    private boolean isPlanAnual = false;

    private MaterialButton btnPlanMensual;
    private MaterialButton btnPlanAnual;
    private TextView tvPrecioPrincipal;
    private TextView tvPeriodoPrecio;
    private TextView tvPrecioSecundario;
    private TextView tvBadgeAhorro;
    private MaterialButton btnSuscribirmeAhora;
    private MaterialButton btnPrefieroAnual;

    private ActivityResultLauncher<Intent> paymentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_suscripcion_premium);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ── Launcher para el resultado del pago Niubiz ──
        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        actualizarVistaPremium();
                        Toast.makeText(this, "¡Bienvenido a Zoolopatas Premium!", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // ── Referencias a vistas ──
        btnPlanMensual = findViewById(R.id.btnPlanMensual);
        btnPlanAnual = findViewById(R.id.btnPlanAnual);
        tvPrecioPrincipal = findViewById(R.id.tvPrecioPrincipal);
        tvPeriodoPrecio = findViewById(R.id.tvPeriodoPrecio);
        tvPrecioSecundario = findViewById(R.id.tvPrecioSecundario);
        tvBadgeAhorro = findViewById(R.id.tvBadgeAhorro);
        btnSuscribirmeAhora = findViewById(R.id.btnSuscribirmeAhora);
        btnPrefieroAnual = findViewById(R.id.btnPrefieroAnual);

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        ImageButton btnProfileAvatar = findViewById(R.id.btnProfileAvatar);

        // ── Botón Volver ──
        btnVolver.setOnClickListener(v -> finish());

        // ── Botón Avatar en la cabecera ──
        btnProfileAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilUsuario.class);
            startActivity(intent);
        });

        // ── Alternancia de planes con Botones (Mensual vs Anual) ──
        btnPlanMensual.setOnClickListener(v -> seleccionarPlan(false));
        btnPlanAnual.setOnClickListener(v -> seleccionarPlan(true));
        btnPrefieroAnual.setOnClickListener(v -> seleccionarPlan(true));

        // ── Botón de suscripción: Conexión con Pasarela Niubiz ──
        btnSuscribirmeAhora.setOnClickListener(v -> {
            if (PremiumManager.isPremium(this)) {
                Toast.makeText(this, "¡Tu cuenta ya tiene una suscripción activa a Zoolopatas Premium!", Toast.LENGTH_SHORT).show();
                return;
            }

            String planSeleccionado = isPlanAnual ? NiubizConfig.PLAN_ANUAL : NiubizConfig.PLAN_MENSUAL;
            double monto = isPlanAnual ? NiubizConfig.PRECIO_ANUAL : NiubizConfig.PRECIO_MENSUAL;

            Intent intent = new Intent(this, NiubizPaymentActivity.class);
            intent.putExtra(NiubizPaymentActivity.EXTRA_PLAN_NAME, planSeleccionado);
            intent.putExtra(NiubizPaymentActivity.EXTRA_AMOUNT, monto);
            paymentLauncher.launch(intent);
        });

        // ── Clic en tarjetas de beneficios para más información ──
        findViewById(R.id.cardBeneficioMascotas).setOnClickListener(v ->
                Toast.makeText(this, "Hasta 20 mascotas: registra y gestiona todos tus compañeros.", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardBeneficioHistorial).setOnClickListener(v ->
                Toast.makeText(this, "Historial ilimitado: consultas, tratamientos y recetas sin límites.", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardBeneficioRecordatorios).setOnClickListener(v ->
                Toast.makeText(this, "Recordatorios ilimitados: vacunas y antiparasitarios a tiempo.", Toast.LENGTH_SHORT).show());

        // ── Barra inferior de navegación ──
        findViewById(R.id.btnNavInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnNavPerfil).setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilUsuario.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Estado inicial
        actualizarEstadoBotonesPlan();

        // Sincronizar estado con Firestore por si ya era premium
        PremiumManager.sincronizarConFirestore(this, () -> {
            if (PremiumManager.isPremium(this)) {
                actualizarVistaPremium();
            }
        });

        // Cargar el avatar del usuario en la esquina superior
        android.widget.ImageButton btnAvatar = findViewById(R.id.btnProfileAvatar);
        if (btnAvatar != null) {
            PremiumManager.cargarFotoAvatar(this, btnAvatar);

            // Asegurarte de que el botón lleve al perfil
            btnAvatar.setOnClickListener(v ->
                    startActivity(new Intent(this, PerfilUsuario.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PremiumManager.isPremium(this)) {
            actualizarVistaPremium();
        }
    }

    private void actualizarVistaPremium() {
        // Ocultar anuncios, toggles de precios y botones secundarios
        TextView tvTituloPantalla = findViewById(R.id.tvTituloPantalla);
        if (tvTituloPantalla != null) {
            tvTituloPantalla.setText("Tu Suscripción Activa");
        }
        TextView tvSub = findViewById(R.id.tvSubtituloPlan);
        if (tvSub != null) {
            tvSub.setText(PremiumManager.getTextoDiasRestantes(this));
        }

        android.view.View layoutPrecio = findViewById(R.id.layoutPrecioPrincipalContenedor);
        if (layoutPrecio != null) layoutPrecio.setVisibility(android.view.View.GONE);
        if (tvPrecioSecundario != null) tvPrecioSecundario.setVisibility(android.view.View.GONE);
        if (tvBadgeAhorro != null) tvBadgeAhorro.setVisibility(android.view.View.GONE);
        android.view.View layoutToggle = findViewById(R.id.layoutTogglePlanes);
        if (layoutToggle != null) layoutToggle.setVisibility(android.view.View.GONE);
        if (btnPrefieroAnual != null) btnPrefieroAnual.setVisibility(android.view.View.GONE);

        // Solo queda el botón indicando que tu plan Premium está activado con los días restantes
        if (btnSuscribirmeAhora != null) {
            btnSuscribirmeAhora.setText(PremiumManager.getTextoBotonPremium(this));
            btnSuscribirmeAhora.setIcon(null);
            btnSuscribirmeAhora.setEnabled(true);
            btnSuscribirmeAhora.setOnClickListener(v ->
                    Toast.makeText(this, "Tu plan Premium ya se encuentra activo. " + PremiumManager.getTextoDiasRestantes(this) + ".", Toast.LENGTH_SHORT).show());
            btnSuscribirmeAhora.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1B5E55")));
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btnSuscribirmeAhora.getLayoutParams();
            if (params != null) {
                params.topMargin = 0;
                btnSuscribirmeAhora.setLayoutParams(params);
            }
        }
    }

    private void seleccionarPlan(boolean anual) {
        if (this.isPlanAnual == anual) return;
        this.isPlanAnual = anual;
        actualizarEstadoBotonesPlan();
    }

    private void actualizarEstadoBotonesPlan() {
        int tealColor = ContextCompat.getColor(this, R.color.teal_primary);
        int graySecondaryColor = ContextCompat.getColor(this, R.color.sub_text_secondary);

        if (!isPlanAnual) {
            // Plan Mensual seleccionado
            btnPlanMensual.setBackgroundTintList(ColorStateList.valueOf(tealColor));
            btnPlanMensual.setTextColor(Color.WHITE);

            btnPlanAnual.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnPlanAnual.setTextColor(graySecondaryColor);

            tvPrecioPrincipal.setText("S/ 16.99");
            tvPeriodoPrecio.setText("/ mes");
            tvPrecioSecundario.setText("o S/ 134.99 al año");
            tvBadgeAhorro.setText("Ahorras S/ 68.89 al año");
            if (!PremiumManager.isPremium(this)) {
                btnPrefieroAnual.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            // Plan Anual seleccionado
            btnPlanAnual.setBackgroundTintList(ColorStateList.valueOf(tealColor));
            btnPlanAnual.setTextColor(Color.WHITE);

            btnPlanMensual.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnPlanMensual.setTextColor(graySecondaryColor);

            tvPrecioPrincipal.setText("S/ 11.25");
            tvPeriodoPrecio.setText("/ mes");
            tvPrecioSecundario.setText("Facturado a S/ 134.99 al año");
            tvBadgeAhorro.setText("34% DE DESCUENTO APLICADO");
            btnPrefieroAnual.setVisibility(android.view.View.GONE);
        }

        if (PremiumManager.isPremium(this)) {
            actualizarVistaPremium();
        }
    }
}
