package com.example.ProyectoZoloopatas;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;

public class registro_cuenta extends AppCompatActivity {

    private EditText etNombre, etCorreo, etPassword, etConfirmPassword;
    private TextView tvErrorCorreo, tvErrorPassword, tvErrorConfirmPassword;
    private Button btnRegistrar;
    private Button btnIniciarSesionOpt;
    private ImageButton btnVolverAtras;

    // Instancia de Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro_cuenta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvErrorCorreo = findViewById(R.id.tvErrorCorreo);
        tvErrorPassword = findViewById(R.id.tvErrorPassword);
        tvErrorConfirmPassword = findViewById(R.id.tvErrorConfirmPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnIniciarSesionOpt = findViewById(R.id.btnIniciarSesionOpt);
        btnVolverAtras = findViewById(R.id.btnVolverAtras);

        // ── Botón "Inicio sesión" → va a activity_iniciarsesion ──
        btnIniciarSesionOpt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(registro_cuenta.this, iniciosesion.class);
                startActivity(intent);
            }
        });

        // ── Botón "Volver atrás" → va a activity_main ──
        btnVolverAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(registro_cuenta.this, MainActivity.class);
                startActivity(intent);
                finish(); // Opcional: cierra registro_cuenta para no acumular Activities
            }
        });

        // ── Validar al presionar Crear cuenta ──
        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarYRegistrarFormulario();
            }
        });
    }

    private void validarYRegistrarFormulario() {
        boolean formValido = true;

        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombre.setBackgroundResource(R.drawable.bg_card_error);
            formValido = false;
        } else {
            etNombre.setBackgroundResource(R.drawable.bg_badge_gris);
        }

        // 1. Validar correo
        String correo = etCorreo.getText().toString().trim();
        if (!correoEsValido(correo)) {
            tvErrorCorreo.setVisibility(View.VISIBLE);
            etCorreo.setBackgroundResource(R.drawable.bg_card_error);
            formValido = false;
        } else {
            tvErrorCorreo.setVisibility(View.GONE);
            etCorreo.setBackgroundResource(R.drawable.bg_badge_gris);
        }

        // 2. Validar contraseña
        String password = etPassword.getText().toString();
        if (!passwordEsValida(password)) {
            tvErrorPassword.setVisibility(View.VISIBLE);
            etPassword.setBackgroundResource(R.drawable.bg_card_error);
            formValido = false;
        } else {
            tvErrorPassword.setVisibility(View.GONE);
            etPassword.setBackgroundResource(R.drawable.bg_badge_gris);
        }

        // 3. Validar confirmación de contraseña
        String confirmPassword = etConfirmPassword.getText().toString();
        if (!confirmPassword.equals(password)) {
            tvErrorConfirmPassword.setVisibility(View.VISIBLE);
            etConfirmPassword.setBackgroundResource(R.drawable.bg_card_error);
            formValido = false;
        } else {
            tvErrorConfirmPassword.setVisibility(View.GONE);
            etConfirmPassword.setBackgroundResource(R.drawable.bg_badge_gris);
        }

        // 4. Si todo lo ingresado es válido, proceder con Firebase Auth
        if (formValido) {
            registrarUsuarioEnFirebase(correo, password, nombre);
        }
    }

    @SuppressLint("LogConditional")
    private void registrarUsuarioEnFirebase(String email, String password, String nombre) {
        btnRegistrar.setEnabled(false); // Evitar doble clic

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnRegistrar.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Registro exitoso en Firebase
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Actualizar el perfil con el nombre del usuario
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nombre)
                                    .build();
                            user.updateProfile(profileUpdates);
                            // Datos básicos
                            java.util.Map<String, Object> UsuarioMap = new java.util.HashMap<>();
                            UsuarioMap.put("nombreUsuario", nombre);
                            UsuarioMap.put("nombre", nombre);
                            UsuarioMap.put("distrito", "");
                            UsuarioMap.put("correo", email);
                            UsuarioMap.put("Correo", email);
                            UsuarioMap.put("estadoCuenta", "active");
                            UsuarioMap.put("fechaCreacionCuenta", FieldValue.serverTimestamp());
                            UsuarioMap.put("fechaActualizacionCuenta", FieldValue.serverTimestamp());
                            // Objeto telefono (Map anidado vacío por ahora)
                            java.util.Map<String, Object> telefonoMap = new java.util.HashMap<>();
                            telefonoMap.put("codigoPais", "");
                            telefonoMap.put("numero", "");
                            UsuarioMap.put("telefono", telefonoMap);
                            // Objeto preferenciasNotificacion
                            java.util.Map<String, Object> preferenciasNotificacion = new java.util.HashMap<>();
                            preferenciasNotificacion.put("notificacionesPush", true);
                            preferenciasNotificacion.put("vacunas", true);
                            preferenciasNotificacion.put("recordatorios", true);
                            UsuarioMap.put("preferenciasNotificacion", preferenciasNotificacion);
                            // Objeto suscripcion (Plan gratuito por defecto según BDNoSQLZoolopatas.json)
                            java.util.Map<String, Object> suscripcionMap = new java.util.HashMap<>();
                            suscripcionMap.put("nivel", "free");
                            suscripcionMap.put("plan", "Plan Gratuito");
                            suscripcionMap.put("esPremium", false);
                            suscripcionMap.put("periodoFacturacion", null);
                            suscripcionMap.put("estado", "none");
                            suscripcionMap.put("fechaInicio", null);
                            suscripcionMap.put("fechaExpiracion", null);
                            UsuarioMap.put("suscripcion", suscripcionMap);
                            // Objeto estadisticas (Contadores iniciales)
                            java.util.Map<String, Object> estadisticasMap = new java.util.HashMap<>();
                            estadisticasMap.put("cantidadMascotas", 0);
                            estadisticasMap.put("recordatoriosPendientes", 0);
                            UsuarioMap.put("estadisticas", estadisticasMap);
                            // Guardar en Firestore
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("usuarios").document(user.getUid())
                                    .set(UsuarioMap)
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("FIRESTORE", "¡Usuario creado en Firestore con ID: "+user.getUid());
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("FIRESTORE", "Error al guardar usuario: " + e.getMessage());
                                    });

                            // Enviar correo de verificación (Criterio de aceptación HU01)
                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    // Redirigir a la pantalla de confirmación
                                    Intent intent = new Intent(registro_cuenta.this, confirmacion_registro.class);
                                    intent.putExtra("USER_EMAIL", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    btnRegistrar.setEnabled(true);
                                    Toast.makeText(registro_cuenta.this, "Error al enviar correo de verificación.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        // Si el correo ya existe o falla el registro (Criterio de aceptación HU01)
                        btnRegistrar.setEnabled(true);
                        String mensajeError = "Error en el registro. Es posible que el correo ya se encuentre registrado.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            // Opcional: puedes personalizar según la excepción de Firebase
                            mensajeError = "La cuenta ya se encuentra registrada o el correo no es válido.";
                        }
                        Toast.makeText(registro_cuenta.this, mensajeError, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean correoEsValido(String correo) {
        if (correo.isEmpty()) return false;
        return correo.matches(
                "^[a-zA-Z0-9._%+\\-]+@(gmail|hotmail|outlook|yahoo)\\.com$"
        );
    }

    private boolean passwordEsValida(String password) {
        if (password.length() < 8) return false;
        boolean tieneMayuscula = password.matches(".*[A-Z].*");
        boolean tieneNumero = password.matches(".*[0-9].*");
        boolean tieneSimbolo = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        return tieneMayuscula && tieneNumero && tieneSimbolo;
    }
}
