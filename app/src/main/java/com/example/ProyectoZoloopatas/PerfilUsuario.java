package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Pantalla de Perfil del Usuario.
 * Conexiones:
 *  - "Editar perfil"        -> EditarPerfil
 *  - "Cerrar sesión"        -> iniciosesion (cierra la sesión de Firebase)
 *  - "Eliminar cuenta"      -> confirma y elimina la cuenta -> iniciosesion
 *  - Barra inferior         -> Inicio (HomeActivity)
 */
public class PerfilUsuario extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvNombreUsuario, tvCorreoUsuario, rowCorreoSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_usuario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvCorreoUsuario = findViewById(R.id.tvCorreoUsuario);
        rowCorreoSub = findViewById(R.id.rowCorreoSub);

        // ── Acciones de la lista ──
        findViewById(R.id.rowEditarPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfil.class)));

        findViewById(R.id.rowSeguridad).setOnClickListener(v -> confirmarCambioContrasena());

        findViewById(R.id.rowCorreo).setOnClickListener(v ->
                Toast.makeText(this, "El correo está vinculado a tu cuenta y no puede cambiarse aquí", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones y alertas: próximamente", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowEliminarCuenta).setOnClickListener(v -> confirmarEliminarCuenta());

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> cerrarSesion());

        findViewById(R.id.btnMejorarPlanPerfil).setOnClickListener(v ->
                Toast.makeText(this, "Pantalla de suscripción: próximamente", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnCamaraPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfil.class)));

        // ── Barra inferior ──
        findViewById(R.id.btnNavInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnProfileAvatar).setOnClickListener(v ->
                Toast.makeText(this, "Ya estás en tu perfil", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuario(); // refresca el nombre si se editó en EditarPerfil
    }

    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String nombre = user.getDisplayName();
        if (nombre != null && !nombre.trim().isEmpty()) tvNombreUsuario.setText(nombre);
        String correo = user.getEmail();
        if (correo != null) {
            tvCorreoUsuario.setText(correo);
            rowCorreoSub.setText(correo);
        }
    }

    private void confirmarCambioContrasena() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setMessage("Te enviaremos un enlace a " + user.getEmail() + " para restablecer tu contraseña.")
                .setPositiveButton("Enviar enlace", (d, w) ->
                        mAuth.sendPasswordResetEmail(user.getEmail()).addOnCompleteListener(task ->
                                Toast.makeText(this,
                                        task.isSuccessful() ? "Enlace enviado a tu correo" : "No se pudo enviar el enlace",
                                        Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        mAuth.signOut();
        irAInicioSesion();
    }

    private void confirmarEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("Esta acción eliminará tu cuenta y tus datos de forma permanente. ¿Deseas continuar?")
                .setPositiveButton("Eliminar", (d, w) -> eliminarCuenta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuenta() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            irAInicioSesion();
            return;
        }
        String uid = user.getUid();
        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Limpia también los datos del perfil guardados en Firestore
                FirebaseFirestore.getInstance().collection("usuarios").document(uid).delete();
                Toast.makeText(this, "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                irAInicioSesion();
            } else {
                // Firebase exige un inicio de sesión reciente para esta operación
                Toast.makeText(this,
                        "Por seguridad, cierra sesión, vuelve a ingresar e inténtalo de nuevo",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void irAInicioSesion() {
        Intent intent = new Intent(this, iniciosesion.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
