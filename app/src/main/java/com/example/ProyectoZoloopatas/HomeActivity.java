package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
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

        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarSaludo();
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
                            String nombreFirestore = documentSnapshot.getString("nombre");
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
