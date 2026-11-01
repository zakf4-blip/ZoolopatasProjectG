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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Pantalla de Edición de perfil.
 * Se abre desde PerfilUsuario ("Editar perfil").
 * Guarda: nombre (FirebaseAuth) y teléfono / distrito (Firestore: usuarios/{uid}).
 */
public class EditarPerfil extends AppCompatActivity {

    private static final int MAX_NOMBRE = 50;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private File archivoFotoParaSubir = null;
    private EditText etNombreUsuario, etTelefonoUsuario;
    private Spinner spCodigoPais, spCiudadDistrito;
    private TextView tvContadorNombreUsuario, tvNombreFoto;
    private ImageView ivFotoEditar;

    // Cámara (vista previa en miniatura, no necesita permisos) y galería
    private final ActivityResultLauncher<Void> tomarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    mostrarFoto(bitmap);
                    // Guardamos el bitmap temporalmente en un archivo
                    archivoFotoParaSubir = guardarBitmapEnArchivo(bitmap);
                }
            });

    private final ActivityResultLauncher<String> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    mostrarFoto(uri);
                    // Copiamos el archivo de la galería a un archivo temporal
                    archivoFotoParaSubir = copiarUriAFile(uri);
                }
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

        // Cargar el avatar del usuario en la esquina superior
        android.widget.ImageButton btnAvatar = findViewById(R.id.btnProfileAvatar);
        if (btnAvatar != null) {
            PremiumManager.cargarFotoAvatar(this, btnAvatar);

            // Asegurarte de que el botón lleve al perfil
            btnAvatar.setOnClickListener(v ->
                    startActivity(new Intent(this, PerfilUsuario.class)));
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar la foto cada vez que vuelvas a esta pantalla
        cargarDatos();
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

            String nom = doc.getString("nombreUsuario");
            if (nom != null && !nom.trim().isEmpty()) {
                etNombreUsuario.setText(nom);
                tvNombreFoto.setText(nom);
            }

            Object telObj = doc.get("telefono");
            if (telObj instanceof Map) {
                Map<?, ?> telMap = (Map<?, ?>) telObj;
                Object num = telMap.get("numero");
                Object cod = telMap.get("codigoPais");
                if (num != null) etTelefonoUsuario.setText(String.valueOf(num));
                if (cod != null) seleccionar(spCodigoPais, String.valueOf(cod));
            }

            String dist = doc.getString("distrito");
            seleccionar(spCiudadDistrito, dist);

            String urlFoto = doc.getString("urlFoto");
            if (urlFoto != null && !urlFoto.isEmpty()) {
                if (ivFotoEditar != null) {
                    ivFotoEditar.setPadding(0, 0, 0, 0); // Quitamos el padding
                    ivFotoEditar.setImageTintList(null); // Quitamos el color verde
                    com.bumptech.glide.Glide.with(EditarPerfil.this)
                            .load(urlFoto)
                            .placeholder(R.drawable.ic_person)
                            .into(ivFotoEditar);
                }
            }
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

    private void actualizarDatosEnFirebaseYAuth(FirebaseUser user, String nombre, String telefono, String nuevaFotoUrl) {
        UserProfileChangeRequest cambios = new UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build();

        user.updateProfile(cambios).addOnCompleteListener(task -> {
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombreUsuario", nombre);

            String codPais = String.valueOf(spCodigoPais.getSelectedItem());
            Map<String, Object> telefonoMap = new HashMap<>();
            telefonoMap.put("codigoPais", codPais);
            telefonoMap.put("numero", telefono);
            datos.put("telefono", telefonoMap);

            String dist = String.valueOf(spCiudadDistrito.getSelectedItem());
            datos.put("distrito", dist);
            datos.put("fechaActualizacionCuenta", com.google.firebase.firestore.FieldValue.serverTimestamp());

            if (nuevaFotoUrl != null) {
                datos.put("urlFoto", nuevaFotoUrl);
                datos.put("rutaFoto", "avatars/" + nuevaFotoUrl.substring(nuevaFotoUrl.lastIndexOf("/") + 1));
            }

            db.collection("usuarios").document(user.getUid())
                    .set(datos, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        findViewById(R.id.btnGuardarTop).setEnabled(true);
                        findViewById(R.id.btnGuardarCambios).setEnabled(true);
                    });
        });
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

        // Deshabilitar botón para evitar doble clic
        findViewById(R.id.btnGuardarTop).setEnabled(false);
        findViewById(R.id.btnGuardarCambios).setEnabled(false);

        // Si hay una foto nueva seleccionada, primero la subimos a Supabase
        if (archivoFotoParaSubir != null) {
            SupabaseStorageHelper.subirFoto(archivoFotoParaSubir, user.getUid(), new SupabaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String publicUrl) {
                    // Si la foto sube bien, guardamos el resto de datos
                    actualizarDatosEnFirebaseYAuth(user, nombre, telefono, publicUrl);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(EditarPerfil.this, "Error al subir foto: " + error, Toast.LENGTH_LONG).show();
                    findViewById(R.id.btnGuardarTop).setEnabled(true);
                    findViewById(R.id.btnGuardarCambios).setEnabled(true);
                }
            });
        } else {
            // Si no hay foto nueva, guardamos solo los datos de texto
            actualizarDatosEnFirebaseYAuth(user, nombre, telefono, null);
        }
    }

    private File guardarBitmapEnArchivo(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "temp_perfil.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private File copiarUriAFile(Uri uri) {
        try {
            File file = new File(getCacheDir(), "temp_perfil.jpg");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}
