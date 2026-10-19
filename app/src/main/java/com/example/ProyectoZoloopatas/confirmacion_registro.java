package com.example.ProyectoZoloopatas;

import android.os.Bundle;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
public class confirmacion_registro extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private TextView tvCorreoUsuario;
    private Button btnIrInicioSesion, btnReenviarEnlaceVerifi, btnCambiarCorreoReturn;
    private boolean verificado = false;

    // Para el polling de verificación
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final long INTERVALO_POLLING_MS = 5000; // cada 5 segundos consultas a Firebase
    private boolean pollingActivo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmacion_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        mAuth.signOut();
                        Intent intent = new Intent(confirmacion_registro.this,
                                iniciosesion.class
                        );
                        // Evita regresar a la pantalla anterior
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );
                        startActivity(intent);
                        finish();
                    }
                }
        );

        user = mAuth.getCurrentUser();

        tvCorreoUsuario = findViewById(R.id.tvCorreoUsuario);
        btnReenviarEnlaceVerifi = findViewById(R.id.btnReenviarEnlaceVerifi);
        btnCambiarCorreoReturn = findViewById(R.id.btnCambiarCorreoReturn);
        btnIrInicioSesion = findViewById(R.id.btnIrInicioSesion);
        btnIrInicioSesion.setEnabled(false);

        // Rellenar el correo del usuario actual
        if (user != null && user.getEmail() != null) {
            tvCorreoUsuario.setText("📨 " + user.getEmail());
        } else {
            // Intentar leer por extra si viniera desde el intent
            String emailExtra = getIntent().getStringExtra("USER_EMAIL");
            if (emailExtra != null) {
                tvCorreoUsuario.setText("📨 " + emailExtra);
            }
        }

        // Listener: Reenviar correo de verificación
        btnReenviarEnlaceVerifi.setOnClickListener(v -> {
            if (user != null) {
                user.sendEmailVerification().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(confirmacion_registro.this, "Correo reenviado con éxito.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(confirmacion_registro.this, "Error al reenviar el correo.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(confirmacion_registro.this, "No hay sesión activa para reenviar el correo.", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener: Cambiar correo (regresa al registro)
        btnCambiarCorreoReturn.setOnClickListener(v -> {
            Intent intent = new Intent(confirmacion_registro.this, registro_cuenta.class);
            startActivity(intent);
            finish();
        });

        // Listener: Ir al inicio de sesión (solo si el correo está verificado)
        btnIrInicioSesion.setOnClickListener(v -> {
            if (verificado) {
                String correo = (user != null && user.getEmail() != null)
                        ? user.getEmail()
                        : getIntent().getStringExtra("USER_EMAIL");
                mAuth.signOut();
                Intent intent = new Intent(confirmacion_registro.this, iniciosesion.class);
                intent.putExtra("USER_EMAIL", correo);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(confirmacion_registro.this, "Primero verifica tu correo.", Toast.LENGTH_SHORT).show();
            }
        });
        // Iniciar polling de verificación
        iniciarPolling();
    }

    private void iniciarPolling() {
        if (pollingActivo) return;
        pollingActivo = true;

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!pollingActivo) return;
                verificarEstadoCorreo();
                if (pollingActivo && !verificado) {
                    handler.postDelayed(this, INTERVALO_POLLING_MS);
                }
            }
        };
        handler.post(pollingRunnable);
    }

    private void detenerPolling() {
        pollingActivo = false;
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver al primer plano, comprobar inmediatamente
        verificarEstadoCorreo();
        // Y reanudar el polling si aún no está verificado
        if (!verificado) {
            iniciarPolling();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar el polling para no gastar recursos en segundo plano
        detenerPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerPolling();
    }

    private void verificarEstadoCorreo() {
        if (user == null) return;

        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    verificado = true;
                    btnIrInicioSesion.setEnabled(true);
                    // Opcional: avisar al usuario
                    Toast.makeText(confirmacion_registro.this,
                            "¡Correo verificado! Ya puedes iniciar sesión.", Toast.LENGTH_SHORT).show();
                } else {
                    verificado = false;
                    btnIrInicioSesion.setEnabled(false);
                }
            }
            // Si falla el reload (sin internet), dejamos el estado como estaba
        });
    }
}