package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tituloSaludo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        tituloSaludo = findViewById(R.id.titulo_Saludo);

        actualizarSaludo();
        actualizarContadorMascotas();

        // ── Pantalla principal -> Cuidados y Bienestar ──
        findViewById(R.id.btnCuidadoBienestar).setOnClickListener(v ->
                startActivity(new Intent(this, CuidadosBienestar.class)));

        // ── Pantalla principal -> Perfil del Usuario (avatar y pestaña "Perfil") ──
        findViewById(R.id.btnProfileAvatar).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));
        findViewById(R.id.btnNavPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));

        // ── Pantalla principal -> Mis Mascotas (activity_pantalla_mascotas.xml) ──
        android.view.View.OnClickListener abrirMascotas = v ->
                startActivity(new Intent(this, PantallaMascotas.class));

        if (findViewById(R.id.btnMisMascotas) != null) {
            findViewById(R.id.btnMisMascotas).setOnClickListener(abrirMascotas);
        }
        if (findViewById(R.id.cardMisMascotas) != null) {
            findViewById(R.id.cardMisMascotas).setOnClickListener(abrirMascotas);
        }

        // ── Pantalla principal -> Historial de mis Mascotas (activity_historial_clinico.xml) ──
        android.view.View.OnClickListener abrirHistorialMascotas = v ->
                startActivity(new Intent(this, historial_clinico_mascotas.class));

        if (findViewById(R.id.btnHistorialClinico) != null) {
            findViewById(R.id.btnHistorialClinico).setOnClickListener(abrirHistorialMascotas);
        }
        if (findViewById(R.id.cardHistorialMascota) != null) {
            findViewById(R.id.cardHistorialMascota).setOnClickListener(abrirHistorialMascotas);
        }

        // ── Pantalla principal -> Suscripción Premium (activity_suscripcion_premium.xml) ──
        android.view.View.OnClickListener abrirSuscripcion = v ->
                startActivity(new Intent(this, SuscripcionPremium.class));

        if (findViewById(R.id.btnMejorarPlan) != null) {
            findViewById(R.id.btnMejorarPlan).setOnClickListener(abrirSuscripcion);
        }
        if (findViewById(R.id.cardPremium) != null) {
            findViewById(R.id.cardPremium).setOnClickListener(abrirSuscripcion);
        }

        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarSaludo();
        actualizarContadorMascotas();
        actualizarEstadoPlanUI();
        PremiumManager.sincronizarConFirestore(this, this::actualizarEstadoPlanUI);
    }

    private void actualizarContadorMascotas() {
        TextView tvCantidad = findViewById(R.id.tvCantidadMascotasHome);
        if (tvCantidad != null) {
            String uid = (mAuth != null && mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "usuario_default";
            int count = MascotasLocalManager.getMascotas(this, uid).size();
            tvCantidad.setText(count == 1 ? "1 registrada" : count + " registradas");

            if (uid != null && !"usuario_default".equals(uid)) {
                FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                        .collection("mascotas").get()
                        .addOnSuccessListener(querySnapshots -> {
                            if (querySnapshots != null) {
                                int remoteCount = querySnapshots.size();
                                tvCantidad.setText(remoteCount == 1 ? "1 registrada" : remoteCount + " registradas");
                            }
                        });
            }
        }
    }

    private void actualizarEstadoPlanUI() {
        boolean esPremium = PremiumManager.isPremium(this);
        View layoutBadge = findViewById(R.id.layoutAnuncioBadgeHome);
        View tvTitulo = findViewById(R.id.tvAnuncioTituloHome);
        View tvDesc = findViewById(R.id.tvAnuncioDescHome);
        android.widget.Button btn = findViewById(R.id.btnMejorarPlan);

        if (esPremium) {
            // Ocultar los anuncios de activar plan de la pantalla
            if (layoutBadge != null) layoutBadge.setVisibility(View.GONE);
            if (tvTitulo != null) tvTitulo.setVisibility(View.GONE);
            if (tvDesc != null) tvDesc.setVisibility(View.GONE);

            // Solo queda el botón indicando que el plan está activado con los días restantes y al presionarlo lleva a SuscripcionPremium
            if (btn != null) {
                btn.setText(PremiumManager.getTextoBotonPremium(this));
                btn.setEnabled(true);
                btn.setOnClickListener(v -> startActivity(new Intent(this, SuscripcionPremium.class)));
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btn.getLayoutParams();
                if (params != null) {
                    params.topMargin = 0;
                    btn.setLayoutParams(params);
                }
            }
        } else {
            // Mostrar los anuncios de activar plan
            if (layoutBadge != null) layoutBadge.setVisibility(View.VISIBLE);
            if (tvTitulo != null) tvTitulo.setVisibility(View.VISIBLE);
            if (tvDesc != null) tvDesc.setVisibility(View.VISIBLE);

            if (btn != null) {
                btn.setText("Mejorar plan  →");
                btn.setEnabled(true);
                btn.setOnClickListener(v -> startActivity(new Intent(this, SuscripcionPremium.class)));
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btn.getLayoutParams();
                if (params != null) {
                    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                    btn.setLayoutParams(params);
                }
            }
        }
    }

    private void actualizarSaludo() {
        if (tituloSaludo == null) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tituloSaludo.setText("Hola");
            return;
        }

        // 1. Mostrar de inmediato el nombre registrado en FirebaseAuth si ya está disponible
        String nombreAuth = user.getDisplayName();
        if (nombreAuth != null && !nombreAuth.trim().isEmpty()) {
            tituloSaludo.setText("Hola " + nombreAuth.trim());
        }

        // 2. Si el perfil local aún no tiene displayName, intentar refrescarlo
        if (nombreAuth == null || nombreAuth.trim().isEmpty()) {
            user.reload().addOnCompleteListener(task -> {
                FirebaseUser reloadedUser = mAuth.getCurrentUser();
                if (reloadedUser != null) {
                    String recargado = reloadedUser.getDisplayName();
                    if (recargado != null && !recargado.trim().isEmpty()) {
                        tituloSaludo.setText("Hola " + recargado.trim());
                    }
                }
            });
        }

        // 3. Consultar Firestore (colección "usuarios") por si fue guardado allí
        try {
            FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String nombreFirestore = documentSnapshot.getString("nombreUsuario");
                            if (nombreFirestore == null || nombreFirestore.trim().isEmpty()) {
                                nombreFirestore = documentSnapshot.getString("nombre");
                            }
                            if (nombreFirestore != null && !nombreFirestore.trim().isEmpty()) {
                                tituloSaludo.setText("Hola " + nombreFirestore.trim());
                                return;
                            }
                        }
                        // Fallback: Si no hay nombre en Auth ni en Firestore, mostrar la parte antes del @ del correo
                        if ((user.getDisplayName() == null || user.getDisplayName().trim().isEmpty())
                                && user.getEmail() != null && !user.getEmail().isEmpty()) {
                            String emailName = user.getEmail().split("@")[0];
                            tituloSaludo.setText("Hola " + emailName);
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
