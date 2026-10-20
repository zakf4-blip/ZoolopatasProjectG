package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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

        // Cambio de correo (tanto el contenedor principal como el subtítulo)
        findViewById(R.id.rowCorreo).setOnClickListener(v -> confirmarCambioCorreo());
        if (rowCorreoSub != null) rowCorreoSub.setOnClickListener(v -> confirmarCambioCorreo());

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

        // Recargamos el usuario para sincronizar los cambios recientes desde Firebase
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser refreshedUser = mAuth.getCurrentUser();
                if (refreshedUser == null) return;

                String nombre = refreshedUser.getDisplayName();
                if (nombre != null && !nombre.trim().isEmpty()) {
                    tvNombreUsuario.setText(nombre);
                }

                String correo = refreshedUser.getEmail();
                if (correo != null) {
                    tvCorreoUsuario.setText(correo);
                    rowCorreoSub.setText(correo);
                }
            }
        });
    }

    private void confirmarCambioCorreo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar correo electrónico");
        builder.setMessage("Ingresa tu nuevo correo y tu contraseña actual por seguridad:");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = 50;
        layout.setPadding(padding, 20, padding, 20);

        final android.widget.EditText inputNuevoCorreo = new android.widget.EditText(this);
        inputNuevoCorreo.setHint("Nuevo correo electrónico");
        inputNuevoCorreo.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(inputNuevoCorreo);

        final android.widget.EditText inputPassword = new android.widget.EditText(this);
        inputPassword.setHint("Contraseña actual");
        inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputPassword);

        builder.setView(layout);

        builder.setPositiveButton("Actualizar", (dialog, which) -> {
            String nuevoCorreo = inputNuevoCorreo.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (nuevoCorreo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            actualizarCorreoEnFirebase(nuevoCorreo, password);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarCorreoEnFirebase(String nuevoCorreo, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 1. Reautenticar al usuario con su contraseña actual
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                // 2. Solicitar actualización segura de correo (envía verificación al nuevo correo)
                user.verifyBeforeUpdateEmail(nuevoCorreo).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Correo de verificación enviado al nuevo buzón. Confírmalo para completar el cambio.", Toast.LENGTH_LONG).show();

                        // Opcional: Actualizar también en Firestore si almacenas el correo allí
                        String uid = user.getUid();
                        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                                .update("correo", nuevoCorreo);

                        cargarDatosUsuario();
                    } else {
                        Toast.makeText(this, "Error al actualizar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Contraseña incorrecta. No se pudo verificar la identidad.", Toast.LENGTH_LONG).show();
            }
        });
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar cuenta");
        builder.setMessage("Esta acción eliminará tu cuenta y tus datos de forma permanente. Por seguridad, ingresa tu contraseña para continuar:");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = 50;
        layout.setPadding(padding, 20, padding, 20);

        final android.widget.EditText inputPassword = new android.widget.EditText(this);
        inputPassword.setHint("Contraseña actual");
        inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputPassword);

        // ✅ ¡ESTA ES LA LÍNEA QUE FALTABA!
        builder.setView(layout);

        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            String password = inputPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "La contraseña es obligatoria", Toast.LENGTH_SHORT).show();
                return;
            }
            eliminarCuentaConReautenticacion(password);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void eliminarCuentaConReautenticacion(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            irAInicioSesion();
            return;
        }

        String email = user.getEmail();
        String uid = user.getUid();

        // 1. Reautenticar al usuario con sus credenciales actuales
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                // 2. Si la reautenticación es exitosa, procedemos a borrar de Firebase Auth
                user.delete().addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        // 3. Eliminar los datos del usuario en Firestore
                        FirebaseFirestore.getInstance().collection("usuarios").document(uid).delete();
                        Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                        irAInicioSesion();
                    } else {
                        Toast.makeText(this, "Error al eliminar la cuenta de Firebase Auth", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Contraseña incorrecta. No se pudo verificar la identidad.", Toast.LENGTH_LONG).show();
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
