package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Pantalla de Edición de perfil.
 * Se abre desde PerfilUsuario ("Editar perfil").
 * Guarda: nombre (FirebaseAuth) y teléfono / ciudad (Firestore: usuarios/{uid}).
 */
public class EditarPerfil extends AppCompatActivity {

    private static final int MAX_NOMBRE = 50;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etNombreUsuario, etTelefonoUsuario;
    private Spinner spCodigoPais, spCiudadDistrito;
    private TextView tvContadorNombreUsuario, tvNombreFoto;
    private ImageView ivFotoEditar;

    // Cámara (vista previa en miniatura, no necesita permisos) y galería
    private final ActivityResultLauncher<Void> tomarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) mostrarFoto(bitmap);
            });

    private final ActivityResultLauncher<String> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) mostrarFoto(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etNombreUsuario = findViewById(R.id.etNombreUsuario);
        etTelefonoUsuario = findViewById(R.id.etTelefonoUsuario);
        spCodigoPais = findViewById(R.id.spCodigoPais);
        spCiudadDistrito = findViewById(R.id.spCiudadDistrito);
        tvContadorNombreUsuario = findViewById(R.id.tvContadorNombreUsuario);
        tvNombreFoto = findViewById(R.id.tvNombreFoto);
        ivFotoEditar = findViewById(R.id.ivFotoEditar);

        configurarSpinners();
        configurarContador();
        cargarDatos();

        // ── Navegación / acciones ──
        findViewById(R.id.btnBackEditarPerfil).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelarEdicion).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardarTop).setOnClickListener(v -> guardarCambios());
        findViewById(R.id.btnGuardarCambios).setOnClickListener(v -> guardarCambios());

        findViewById(R.id.btnTomarFotoPerfil).setOnClickListener(v -> tomarFotoLauncher.launch(null));
        findViewById(R.id.btnElegirGaleriaPerfil).setOnClickListener(v -> galeriaLauncher.launch("image/*"));
        findViewById(R.id.btnEditarFotoBadge).setOnClickListener(v -> galeriaLauncher.launch("image/*"));

        // ── Barra inferior ──
        findViewById(R.id.btnNavInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnNavPerfil).setOnClickListener(v -> finish()); // vuelve a PerfilUsuario
        findViewById(R.id.btnProfileAvatar).setOnClickListener(v -> finish());
    }

    private void configurarSpinners() {
        spCodigoPais.setAdapter(crearAdapter(R.array.codigos_pais));
        spCiudadDistrito.setAdapter(crearAdapter(R.array.ciudades_distritos));
    }

    // Adapter con letra oscura para que se lea bien sobre el fondo blanco (también en modo oscuro)
    private ArrayAdapter<CharSequence> crearAdapter(int arrayRes) {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                this, android.R.layout.simple_spinner_item,
                getResources().getTextArray(arrayRes)) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(0xFF1F2428);
                tv.setTextSize(15);
                return tv;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(0xFF1F2428);
                tv.setTextSize(15);
                tv.setPadding(32, 32, 32, 32);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void configurarContador() {
        etNombreUsuario.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                tvContadorNombreUsuario.setText(s.length() + "/" + MAX_NOMBRE + " caracteres");
            }
        });
    }

    private void cargarDatos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String nombre = user.getDisplayName();
        if (nombre != null) {
            etNombreUsuario.setText(nombre);
            tvNombreFoto.setText(nombre);
        }

        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            String telefono = doc.getString("telefono");
            if (telefono != null) etTelefonoUsuario.setText(telefono);
            seleccionar(spCodigoPais, doc.getString("codigoPais"));
            seleccionar(spCiudadDistrito, doc.getString("ciudad"));
        });
    }

    private void seleccionar(Spinner spinner, String valor) {
        if (valor == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (valor.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void mostrarFoto(Uri uri) {
        ivFotoEditar.setPadding(0, 0, 0, 0);
        ivFotoEditar.clearColorFilter();
        ivFotoEditar.setImageTintList(null);
        ivFotoEditar.setImageURI(uri);
    }

    private void mostrarFoto(Bitmap bitmap) {
        ivFotoEditar.setPadding(0, 0, 0, 0);
        ivFotoEditar.setImageTintList(null);
        ivFotoEditar.setImageBitmap(bitmap);
    }

    private void guardarCambios() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Tu sesión expiró, vuelve a iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = etNombreUsuario.getText().toString().trim();
        String telefono = etTelefonoUsuario.getText().toString().replaceAll("\\s+", "");

        if (nombre.isEmpty()) {
            etNombreUsuario.setError("Ingresa tu nombre completo");
            etNombreUsuario.requestFocus();
            return;
        }
        if (telefono.length() < 7) {
            etTelefonoUsuario.setError("Ingresa un teléfono válido");
            etTelefonoUsuario.requestFocus();
            return;
        }

        UserProfileChangeRequest cambios = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();

        user.updateProfile(cambios).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "No se pudo actualizar el nombre", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", nombre);
            datos.put("codigoPais", String.valueOf(spCodigoPais.getSelectedItem()));
            datos.put("telefono", etTelefonoUsuario.getText().toString().trim());
            datos.put("ciudad", String.valueOf(spCiudadDistrito.getSelectedItem()));

            db.collection("usuarios").document(user.getUid())
                    .set(datos, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Nombre guardado, pero no se pudo guardar el teléfono/ciudad", Toast.LENGTH_LONG).show());
        });
    }
}
