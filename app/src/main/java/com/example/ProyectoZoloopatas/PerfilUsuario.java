package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
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

        // Inicializar inmediatamente con los datos del usuario autenticado para evitar cualquier parpadeo de datos por defecto
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual != null) {
            String nom = usuarioActual.getDisplayName();
            if (nom != null && !nom.trim().isEmpty()) {
                tvNombreUsuario.setText(nom.trim());
            } else {
                tvNombreUsuario.setText("Usuario");
            }
            String mail = usuarioActual.getEmail();
            if (mail != null && !mail.trim().isEmpty()) {
                tvCorreoUsuario.setText(mail.trim());
                if (rowCorreoSub != null) rowCorreoSub.setText(mail.trim());
            }
        }

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

        // ── Ir a Suscripción Premium (activity_suscripcion_premium.xml) ──
        android.view.View.OnClickListener abrirSuscripcion = v ->
                startActivity(new Intent(this, SuscripcionPremium.class));

        if (findViewById(R.id.btnMejorarPlanPerfil) != null) {
            findViewById(R.id.btnMejorarPlanPerfil).setOnClickListener(abrirSuscripcion);
        }
        if (findViewById(R.id.cardPremiumPerfil) != null) {
            findViewById(R.id.cardPremiumPerfil).setOnClickListener(abrirSuscripcion);
        }

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
        TextView tvPlanUsuario = findViewById(R.id.tvPlanUsuario);
        View layoutAnuncioHeader = findViewById(R.id.layoutAnuncioHeaderPerfil);
        View tvPrecioAnuncio = findViewById(R.id.tvPrecioAnuncioPerfil);
        View layoutFilaBoton = findViewById(R.id.layoutFilaBotonPerfil);
        android.widget.Button btnMejorarPlanPerfil = findViewById(R.id.btnMejorarPlanPerfil);

        Runnable aplicarEstadoPlan = () -> {
            boolean esPremium = PremiumManager.isPremium(this);
            if (esPremium) {
                if (tvPlanUsuario != null) {
                    int dias = PremiumManager.getDiasRestantes(this);
                    tvPlanUsuario.setText("PLAN PREMIUM (" + dias + (dias == 1 ? " DÍA RESTANTE)" : " DÍAS RESTANTES)"));
                }
                // Ocultar anuncios de activar plan de la pantalla
                if (layoutAnuncioHeader != null) layoutAnuncioHeader.setVisibility(View.GONE);
                if (tvPrecioAnuncio != null) tvPrecioAnuncio.setVisibility(View.GONE);

                // Solo queda el botón indicando que el plan está activado con los días restantes y al presionarlo lleva a SuscripcionPremium
                if (btnMejorarPlanPerfil != null) {
                    btnMejorarPlanPerfil.setText(PremiumManager.getTextoBotonPremium(this));
                    btnMejorarPlanPerfil.setEnabled(true);
                    btnMejorarPlanPerfil.setOnClickListener(v -> startActivity(new Intent(this, SuscripcionPremium.class)));
                    ViewGroup.LayoutParams lp = btnMejorarPlanPerfil.getLayoutParams();
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        btnMejorarPlanPerfil.setLayoutParams(lp);
                    }
                }
                if (layoutFilaBoton != null) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutFilaBoton.getLayoutParams();
                    if (mlp != null) {
                        mlp.topMargin = 0;
                        layoutFilaBoton.setLayoutParams(mlp);
                    }
                }
            } else {
                int petCount = MascotasLocalManager.getMascotas(this, PremiumManager.getUidActual()).size();
                if (tvPlanUsuario != null) {
                    tvPlanUsuario.setText("PLAN GRATUITO (" + petCount + "/3 MASCOTAS)");
                }
                // Sincronizar conteo real desde Firestore
                FirebaseFirestore.getInstance().collection("usuarios").document(PremiumManager.getUidActual())
                        .collection("mascotas").get()
                        .addOnSuccessListener(snap -> {
                            if (snap != null && tvPlanUsuario != null && !PremiumManager.isPremium(this)) {
                                tvPlanUsuario.setText("PLAN GRATUITO (" + snap.size() + "/3 MASCOTAS)");
                            }
                        });

                // Mostrar anuncios de activación
                if (layoutAnuncioHeader != null) layoutAnuncioHeader.setVisibility(View.VISIBLE);
                if (tvPrecioAnuncio != null) tvPrecioAnuncio.setVisibility(View.VISIBLE);

                if (btnMejorarPlanPerfil != null) {
                    btnMejorarPlanPerfil.setText("Mejorar plan  →");
                    btnMejorarPlanPerfil.setEnabled(true);
                    btnMejorarPlanPerfil.setOnClickListener(v -> startActivity(new Intent(this, SuscripcionPremium.class)));
                    ViewGroup.LayoutParams lp = btnMejorarPlanPerfil.getLayoutParams();
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        btnMejorarPlanPerfil.setLayoutParams(lp);
                    }
                }
                if (layoutFilaBoton != null) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutFilaBoton.getLayoutParams();
                    if (mlp != null) {
                        mlp.topMargin = (int) (22 * getResources().getDisplayMetrics().density);
                        layoutFilaBoton.setLayoutParams(mlp);
                    }
                }
            }
        };

        aplicarEstadoPlan.run();
        PremiumManager.sincronizarConFirestore(this, aplicarEstadoPlan);

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

        // Consultar nombre directamente desde Firestore
        FirebaseFirestore.getInstance().collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        // 1. Leer y asignar el nombre
                        String nom = doc.getString("nombreUsuario");
                        if (nom != null && !nom.trim().isEmpty()) {
                            tvNombreUsuario.setText(nom);
                        }

                        // 2. Leer y asignar la foto de perfil (Central y Avatar Superior)
                        String urlFoto = doc.getString("urlFoto");
                        if (urlFoto != null && !urlFoto.isEmpty()) {

                            // Foto central
                            ImageView ivPerfil = findViewById(R.id.ivFotoPerfil);
                            if (ivPerfil != null) {
                                ivPerfil.setPadding(0, 0, 0, 0); // Quitar el padding
                                ivPerfil.setImageTintList(null); // Quitar el tinte verde
                                Glide.with(this)
                                        .load(urlFoto)
                                        .placeholder(R.drawable.ic_person) // Mientras carga
                                        .into(ivPerfil);
                            }

                            // Avatar de la esquina superior derecha
                            android.widget.ImageButton btnAvatar = findViewById(R.id.btnProfileAvatar);
                            if (btnAvatar != null) {
                                btnAvatar.setPadding(0, 0, 0, 0);
                                Glide.with(this)
                                        .load(urlFoto)
                                        .placeholder(R.drawable.ic_person)
                                        .into(btnAvatar);
                            }
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

                        // Actualizar también en Firestore según BDNoSQLZoolopatas.json
                        String uid = user.getUid();
                        java.util.Map<String, Object> updateMap = new java.util.HashMap<>();
                        updateMap.put("correo", nuevoCorreo);
                        updateMap.put("fechaActualizacionCuenta", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                                .set(updateMap, com.google.firebase.firestore.SetOptions.merge());

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
        PremiumManager.limpiarSesionUsuario(this);
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
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 2. Limpiar subcolecciones de Firestore antes de eliminar el usuario
                db.collection("usuarios").document(uid).collection("mascotas").get()
                        .addOnSuccessListener(mascotasSnapshots -> {
                            for (com.google.firebase.firestore.DocumentSnapshot petDoc : mascotasSnapshots) {
                                petDoc.getReference().collection("vacunas").get()
                                        .addOnSuccessListener(vacSnapshots -> {
                                            for (com.google.firebase.firestore.DocumentSnapshot vDoc : vacSnapshots) {
                                                vDoc.getReference().delete();
                                            }
                                        });
                                petDoc.getReference().delete();
                            }

                            // Limpiar recordatorios
                            db.collection("usuarios").document(uid).collection("recordatorios").get()
                                    .addOnSuccessListener(recSnapshots -> {
                                        for (com.google.firebase.firestore.DocumentSnapshot rDoc : recSnapshots) {
                                            rDoc.getReference().delete();
                                        }
                                    });

                            // Eliminar documento del usuario en Firestore
                            db.collection("usuarios").document(uid).delete().addOnCompleteListener(firestoreTask -> {
                                // 3. Proceder a borrar de Firebase Auth
                                user.delete().addOnCompleteListener(deleteTask -> {
                                    PremiumManager.limpiarSesionUsuario(PerfilUsuario.this);
                                    if (deleteTask.isSuccessful()) {
                                        Toast.makeText(PerfilUsuario.this, "Cuenta eliminada permanentemente.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(PerfilUsuario.this, "Datos de usuario eliminados.", Toast.LENGTH_SHORT).show();
                                    }
                                    irAInicioSesion();
                                });
                            });
                        })
                        .addOnFailureListener(e -> {
                            db.collection("usuarios").document(uid).delete();
                            user.delete();
                            PremiumManager.limpiarSesionUsuario(PerfilUsuario.this);
                            irAInicioSesion();
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
