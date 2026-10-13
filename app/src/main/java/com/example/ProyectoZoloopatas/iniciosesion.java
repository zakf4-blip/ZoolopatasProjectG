package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
                                    // Acceso normal a la app (por ejemplo, HomeActivity o MainActivity principal)
                                    Intent intent = new Intent(iniciosesion.this, HomeActivity.class);
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
    }
}