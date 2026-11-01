package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class iniciosesion extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etCorreoLogin, etPasswordLogin;
    private Button btnIngresar; // Asumiendo el botón de ingresar en el xml
    private LinearLayout cardError;
    private Button btnCerrarError, btnRegistrarseGratis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_iniciosesion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();

        etCorreoLogin = findViewById(R.id.etCorreoLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnRegistrar3); // ID del botón de ingresar en tu XML de inicio de sesión
        cardError = findViewById(R.id.cardError);
        btnCerrarError = findViewById(R.id.btnCerrarError);
        btnRegistrarseGratis = findViewById(R.id.btnRegistrarseGratis);

        btnCerrarError.setOnClickListener(v -> cardError.setVisibility(View.GONE));

        btnRegistrarseGratis.setOnClickListener(v -> {
            Intent intent = new Intent(iniciosesion.this, registro_cuenta.class);
            startActivity(intent);
        });

        btnIngresar.setOnClickListener(v -> {
            String correo = etCorreoLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            if (correo.isEmpty() || password.isEmpty()) {
                cardError.setVisibility(View.VISIBLE);
                return;
            }

            mAuth.signInWithEmailAndPassword(correo, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Comprobar si el correo está verificado
                                if (user.isEmailVerified()) {
                                    // Asegurar existencia del documento del usuario en Firestore
                                    com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                                    db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(doc -> {
                                        if (doc == null || !doc.exists()) {
                                            java.util.Map<String, Object> initMap = new java.util.HashMap<>();
                                            initMap.put("nombreUsuario", user.getDisplayName() != null ? user.getDisplayName() : "");
                                            initMap.put("correo", user.getEmail() != null ? user.getEmail() : "");
                                            initMap.put("distrito", "");
                                            initMap.put("estadoCuenta", "active");
                                            initMap.put("fechaCreacionCuenta", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                            initMap.put("fechaActualizacionCuenta", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                            java.util.Map<String, Object> telefonoMap = new java.util.HashMap<>();
                                            telefonoMap.put("codigoPais", "");
                                            telefonoMap.put("numero", "");
                                            initMap.put("telefono", telefonoMap);

                                            java.util.Map<String, Object> preferenciasMap = new java.util.HashMap<>();
                                            preferenciasMap.put("notificacionesPush", true);
                                            preferenciasMap.put("vacunas", true);
                                            preferenciasMap.put("recordatorios", true);
                                            initMap.put("preferenciasNotificacion", preferenciasMap);

                                            java.util.Map<String, Object> suscripcionMap = new java.util.HashMap<>();
                                            suscripcionMap.put("nivel", "free");
                                            suscripcionMap.put("plan", "Plan Gratuito");
                                            suscripcionMap.put("esPremium", false);
                                            suscripcionMap.put("periodoFacturacion", null);
                                            suscripcionMap.put("estado", "none");
                                            suscripcionMap.put("fechaInicio", null);
                                            suscripcionMap.put("fechaExpiracion", null);
                                            initMap.put("suscripcion", suscripcionMap);

                                            java.util.Map<String, Object> estadisticasMap = new java.util.HashMap<>();
                                            estadisticasMap.put("cantidadMascotas", 0);
                                            estadisticasMap.put("recordatoriosPendientes", 0);
                                            initMap.put("estadisticas", estadisticasMap);

                                            db.collection("usuarios").document(user.getUid()).set(initMap, com.google.firebase.firestore.SetOptions.merge());
                                        }
                                    });

                                    // Sincronizar de inmediato el plan de este usuario
                                    PremiumManager.sincronizarConFirestore(iniciosesion.this, null);

                                    // Acceso normal a la app con pila limpia
                                    Intent intent = new Intent(iniciosesion.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Bloqueo total si no está verificado: signOut + Redirigir a confirmación
                                    mAuth.signOut();
                                    Intent intent = new Intent(iniciosesion.this, confirmacion_registro.class);
                                    intent.putExtra("USER_EMAIL", correo);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        } else {
                            // Credenciales no válidas (Criterio de aceptación HU02)
                            cardError.setVisibility(View.VISIBLE);
                        }
                    });
        });
        precargarCorreoOfConfirmacion();
    }

    private void precargarCorreoOfConfirmacion() {
        String correoPrefill = getIntent().getStringExtra("USER_EMAIL");
        if (correoPrefill == null || correoPrefill.isEmpty()) return;

        etCorreoLogin.setText(correoPrefill);
        etPasswordLogin.requestFocus();

        etPasswordLogin.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etPasswordLogin, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }
}