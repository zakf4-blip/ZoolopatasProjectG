package com.example.ProyectoZoloopatas;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class editar_infoMascota extends AppCompatActivity {

    public static final String EXTRA_MASCOTA_ID = "EXTRA_MASCOTA_ID";
    public static final String EXTRA_NOMBRE = "EXTRA_NOMBRE";
    public static final String EXTRA_TIPO = "EXTRA_TIPO";
    public static final String EXTRA_SEXO = "EXTRA_SEXO";
    public static final String EXTRA_CASTRADO = "EXTRA_CASTRADO";
    public static final String EXTRA_RAZA = "EXTRA_RAZA";
    public static final String EXTRA_FECHA = "EXTRA_FECHA";
    public static final String EXTRA_COLORES = "EXTRA_COLORES";
    public static final String EXTRA_FOTO_URI = "EXTRA_FOTO_URI";

    private String mascotaId = null;

    private TextView tvTituloEditaMascota, tvContadorNombre, tvRazaSeleccionada, tvFechaNacimiento;
    private EditText etNombreMascota;
    private MaterialButton btnSexoMacho, btnSexoHembra;
    private MaterialButton btnTipoPerro, btnTipoGato;
    private MaterialSwitch switchCastrado;
    private Button btnRazaSelector, btnFechaNacimiento;
    private MaterialButton btnColorDorado, btnColorBlanco, btnColorNegro, btnColorCafe;
    private ImageButton btnBackTop, btnConfirmarTop;
    private MaterialButton btnGuardarMascota, btnEliminarMascota;

    private ImageView ivFotoMascotaPreview;
    private View layoutSinFoto;
    private ImageButton btnCameraBadge;
    private MaterialButton btnTomarFoto, btnElegirGaleria;
    private String fotoActualPath = null;
    private Uri tempCameraUri = null;

    private ActivityResultLauncher<String> galeriaLauncher;
    private ActivityResultLauncher<Uri> camaraLauncher;
    private ActivityResultLauncher<String> camaraPermissionLauncher;

    private String sexoSeleccionado = "Macho";
    private String tipoSeleccionado = "Perro";
    private final List<String> coloresSeleccionados = new ArrayList<>();

    private final String[] razasPerro = new String[]{
            "Golden Retriever", "Labrador Retriever", "Pastor Alemán", "Bulldog Francés",
            "Poodle / Caniche", "Beagle", "Chihuahua", "Pug", "Husky Siberiano", "Mestizo / Criollo"
    };

    private final String[] razasGato = new String[]{
            "Siamés", "Persa", "Maine Coon", "Bengala", "Angora",
            "Ragdoll", "British Shorthair", "Esfinge", "Mestizo / Común Europeo"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_info_mascota);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupActivityResults();
        initViews();
        setupListeners();
        loadIntentData();
    }

    private void setupActivityResults() {
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        guardarImagenDeGaleria(uri);
                    }
                }
        );

        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success)) {
                        mostrarFotoPreview(fotoActualPath);
                        Toast.makeText(this, "Foto capturada con éxito", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        camaraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Se requiere permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initViews() {
        tvTituloEditaMascota = findViewById(R.id.tvTituloEditaMascota);
        tvContadorNombre = findViewById(R.id.tvContadorNombre);
        etNombreMascota = findViewById(R.id.etNombreMascota);

        btnSexoMacho = findViewById(R.id.btnSexoMacho);
        btnSexoHembra = findViewById(R.id.btnSexoHembra);

        btnTipoPerro = findViewById(R.id.btnTipoPerro);
        btnTipoGato = findViewById(R.id.btnTipoGato);

        switchCastrado = findViewById(R.id.switchCastrado);

        tvRazaSeleccionada = findViewById(R.id.tvRazaSeleccionada);
        btnRazaSelector = findViewById(R.id.btnRazaSelector);

        tvFechaNacimiento = findViewById(R.id.tvFechaNacimiento);
        btnFechaNacimiento = findViewById(R.id.btnFechaNacimiento);

        btnColorDorado = findViewById(R.id.btnColorDorado);
        btnColorBlanco = findViewById(R.id.btnColorBlanco);
        btnColorNegro = findViewById(R.id.btnColorNegro);
        btnColorCafe = findViewById(R.id.btnColorCafe);

        btnBackTop = findViewById(R.id.btnBackTop);
        btnConfirmarTop = findViewById(R.id.btnConfirmarTop);
        btnGuardarMascota = findViewById(R.id.btnGuardarMascota);
        btnEliminarMascota = findViewById(R.id.btnEliminarMascota);

        ivFotoMascotaPreview = findViewById(R.id.ivFotoMascotaPreview);
        layoutSinFoto = findViewById(R.id.layoutSinFoto);
        btnCameraBadge = findViewById(R.id.btnCameraBadge);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnElegirGaleria = findViewById(R.id.btnElegirGaleria);
    }

    private void setupListeners() {
        btnBackTop.setOnClickListener(v -> finish());

        // Contador de caracteres para el nombre
        etNombreMascota.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tvContadorNombre.setText(s.length() + "/64");
            }
        });

        // Selección de Sexo
        btnSexoMacho.setOnClickListener(v -> seleccionarSexo("Macho"));
        btnSexoHembra.setOnClickListener(v -> seleccionarSexo("Hembra"));

        // Selección de Tipo
        btnTipoPerro.setOnClickListener(v -> {
            seleccionarTipo("Perro");
            if (!esRazaDe(tvRazaSeleccionada.getText().toString(), razasPerro)) {
                tvRazaSeleccionada.setText(razasPerro[0]);
            }
        });
        btnTipoGato.setOnClickListener(v -> {
            seleccionarTipo("Gato");
            if (!esRazaDe(tvRazaSeleccionada.getText().toString(), razasGato)) {
                tvRazaSeleccionada.setText(razasGato[0]);
            }
        });

        // Selector de Raza
        btnRazaSelector.setOnClickListener(v -> mostrarDialogoRaza());

        // Selector de Fecha de Nacimiento (con DatePicker)
        btnFechaNacimiento.setOnClickListener(v -> mostrarDatePicker());

        // Selección de Colores
        btnColorDorado.setOnClickListener(v -> toggleColor("Dorado", btnColorDorado));
        btnColorBlanco.setOnClickListener(v -> toggleColor("Blanco", btnColorBlanco));
        btnColorNegro.setOnClickListener(v -> toggleColor("Negro", btnColorNegro));
        btnColorCafe.setOnClickListener(v -> toggleColor("Café", btnColorCafe));

        // Cámara y Galería
        if (btnTomarFoto != null) {
            btnTomarFoto.setOnClickListener(v -> verificarPermisoYAbrirCamara());
        }
        if (btnCameraBadge != null) {
            btnCameraBadge.setOnClickListener(v -> verificarPermisoYAbrirCamara());
        }
        if (btnElegirGaleria != null) {
            btnElegirGaleria.setOnClickListener(v -> {
                try {
                    galeriaLauncher.launch("image/*");
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Guardar
        btnConfirmarTop.setOnClickListener(v -> guardarMascotaEnFirebase());
        btnGuardarMascota.setOnClickListener(v -> guardarMascotaEnFirebase());
    }

    private boolean esRazaDe(String raza, String[] razas) {
        for (String r : razas) {
            if (r.equalsIgnoreCase(raza)) return true;
        }
        return false;
    }

    private void loadIntentData() {
        if (getIntent().hasExtra(EXTRA_MASCOTA_ID)) {
            mascotaId = getIntent().getStringExtra(EXTRA_MASCOTA_ID);
            tvTituloEditaMascota.setText("Edita tu mascota");
            btnGuardarMascota.setText("Guardar Cambios");

            String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
            if (nombre != null) {
                etNombreMascota.setText(nombre);
                tvContadorNombre.setText(nombre.length() + "/64");
            }

            String tipo = getIntent().getStringExtra(EXTRA_TIPO);
            if (tipo != null && (tipo.equalsIgnoreCase("Gato") || tipo.equalsIgnoreCase("Felino"))) {
                seleccionarTipo("Gato");
            } else {
                seleccionarTipo("Perro");
            }

            String sexo = getIntent().getStringExtra(EXTRA_SEXO);
            if (sexo != null && sexo.equalsIgnoreCase("Hembra")) {
                seleccionarSexo("Hembra");
            } else {
                seleccionarSexo("Macho");
            }

            boolean castrado = getIntent().getBooleanExtra(EXTRA_CASTRADO, true);
            switchCastrado.setChecked(castrado);

            String raza = getIntent().getStringExtra(EXTRA_RAZA);
            if (raza != null && !raza.trim().isEmpty()) {
                tvRazaSeleccionada.setText(raza);
            }

            String fecha = getIntent().getStringExtra(EXTRA_FECHA);
            if (fecha != null && !fecha.trim().isEmpty()) {
                tvFechaNacimiento.setText(fecha);
            }

            ArrayList<String> colores = getIntent().getStringArrayListExtra(EXTRA_COLORES);
            if (colores != null) {
                coloresSeleccionados.clear();
                for (String c : colores) {
                    if (c.equalsIgnoreCase("Dorado")) toggleColor("Dorado", btnColorDorado);
                    else if (c.equalsIgnoreCase("Blanco")) toggleColor("Blanco", btnColorBlanco);
                    else if (c.equalsIgnoreCase("Negro")) toggleColor("Negro", btnColorNegro);
                    else if (c.equalsIgnoreCase("Café") || c.equalsIgnoreCase("Cafe")) toggleColor("Café", btnColorCafe);
                }
            }

            if (btnEliminarMascota != null) {
                btnEliminarMascota.setVisibility(android.view.View.VISIBLE);
                btnEliminarMascota.setOnClickListener(v -> confirmarEliminarMascotaActual());
            }
        } else {
            // Modo: Registrar nueva mascota
            tvTituloEditaMascota.setText("Registra tu mascota");
            btnGuardarMascota.setText("Guardar Mascota");
            seleccionarSexo("Macho");
            seleccionarTipo("Perro");
            switchCastrado.setChecked(true);
            tvRazaSeleccionada.setText(razasPerro[0]);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -1);
            tvFechaNacimiento.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));

            toggleColor("Dorado", btnColorDorado);

            if (btnEliminarMascota != null) {
                btnEliminarMascota.setVisibility(android.view.View.GONE);
            }
        }

        String fotoExtra = getIntent().getStringExtra(EXTRA_FOTO_URI);
        if (fotoExtra != null && !fotoExtra.trim().isEmpty()) {
            fotoActualPath = fotoExtra;
        } else if (mascotaId != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (user != null) ? user.getUid() : "usuario_default";
            Mascota mLocal = MascotasLocalManager.getMascotaById(this, uid, mascotaId);
            if (mLocal != null && mLocal.getFotoUri() != null && !mLocal.getFotoUri().trim().isEmpty()) {
                fotoActualPath = mLocal.getFotoUri();
            }
        }
        mostrarFotoPreview(fotoActualPath);
    }

    private void confirmarEliminarMascotaActual() {
        String nombre = etNombreMascota.getText().toString().trim();
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Mascota")
                .setMessage("¿Estás seguro de que deseas eliminar a \"" + (nombre.isEmpty() ? "esta mascota" : nombre) + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String uid = (user != null) ? user.getUid() : "usuario_default";

                    MascotasLocalManager.deleteMascota(this, uid, mascotaId, nombre);

                    if (mascotaId != null && !mascotaId.startsWith("demo_")) {
                        try {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            // 1. Eliminar subcolección vacunas de la mascota en Firestore
                            db.collection("usuarios").document(uid)
                                    .collection("mascotas").document(mascotaId)
                                    .collection("vacunas").get()
                                    .addOnSuccessListener(vacunasSnapshots -> {
                                        for (com.google.firebase.firestore.DocumentSnapshot vDoc : vacunasSnapshots) {
                                            vDoc.getReference().delete();
                                        }
                                    });

                            // 2. Eliminar mascota únicamente en usuarios/{uid}/mascotas/{id}
                            db.collection("usuarios").document(uid).collection("mascotas").document(mascotaId).delete();

                            // 3. Decrementar contador de mascotas en usuarios/{uid}
                            db.collection("usuarios").document(uid)
                                    .update("estadisticas.cantidadMascotas", FieldValue.increment(-1));
                        } catch (Exception ignored) {}
                    }

                    Toast.makeText(this, "\"" + (nombre.isEmpty() ? "Mascota" : nombre) + "\" ha sido eliminada.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void seleccionarSexo(String sexo) {
        sexoSeleccionado = sexo;
        if ("Macho".equalsIgnoreCase(sexo)) {
            btnSexoMacho.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            btnSexoMacho.setTextColor(Color.WHITE);
            btnSexoMacho.setIconTint(ColorStateList.valueOf(Color.WHITE));

            btnSexoHembra.setBackgroundResource(R.drawable.bg_btn_segmented_unselected);
            btnSexoHembra.setTextColor(Color.parseColor("#4A5568"));
            btnSexoHembra.setIconTint(ColorStateList.valueOf(Color.parseColor("#718096")));
        } else {
            btnSexoHembra.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            btnSexoHembra.setTextColor(Color.WHITE);
            btnSexoHembra.setIconTint(ColorStateList.valueOf(Color.WHITE));

            btnSexoMacho.setBackgroundResource(R.drawable.bg_btn_segmented_unselected);
            btnSexoMacho.setTextColor(Color.parseColor("#4A5568"));
            btnSexoMacho.setIconTint(ColorStateList.valueOf(Color.parseColor("#718096")));
        }
    }

    private void seleccionarTipo(String tipo) {
        tipoSeleccionado = tipo;
        if ("Perro".equalsIgnoreCase(tipo)) {
            btnTipoPerro.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            btnTipoPerro.setTextColor(Color.WHITE);
            btnTipoPerro.setIconTint(ColorStateList.valueOf(Color.WHITE));

            btnTipoGato.setBackgroundResource(R.drawable.bg_btn_segmented_unselected);
            btnTipoGato.setTextColor(Color.parseColor("#2D3748"));
            btnTipoGato.setIconTint(ColorStateList.valueOf(Color.parseColor("#2D3748")));
        } else {
            btnTipoGato.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            btnTipoGato.setTextColor(Color.WHITE);
            btnTipoGato.setIconTint(ColorStateList.valueOf(Color.WHITE));

            btnTipoPerro.setBackgroundResource(R.drawable.bg_btn_segmented_unselected);
            btnTipoPerro.setTextColor(Color.parseColor("#2D3748"));
            btnTipoPerro.setIconTint(ColorStateList.valueOf(Color.parseColor("#2D3748")));
        }
    }

    private void toggleColor(String color, MaterialButton btn) {
        if (coloresSeleccionados.contains(color)) {
            coloresSeleccionados.remove(color);
            btn.setText(color);
            btn.setBackgroundResource(R.drawable.bg_chip_light);
            btn.setTextColor(Color.parseColor("#2D3748"));
        } else {
            coloresSeleccionados.add(color);
            btn.setText(color + " ✓");
            if ("Dorado".equalsIgnoreCase(color)) {
                btn.setBackgroundResource(R.drawable.bg_chip_gold);
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
                btn.setTextColor(Color.WHITE);
            }
        }
    }

    private void mostrarDialogoRaza() {
        String[] razas = "Gato".equalsIgnoreCase(tipoSeleccionado) ? razasGato : razasPerro;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona la raza");
        builder.setItems(razas, (dialog, which) -> tvRazaSeleccionada.setText(razas[which]));
        builder.show();
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            String[] partes = tvFechaNacimiento.getText().toString().split("/");
            if (partes.length == 3) {
                int d = Integer.parseInt(partes[0].trim());
                int m = Integer.parseInt(partes[1].trim()) - 1;
                int y = Integer.parseInt(partes[2].trim());
                cal.set(y, m, d);
            }
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String fechaFormato = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    tvFechaNacimiento.setText(fechaFormato);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        // Validación: no puede ser fecha futura
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void guardarMascotaEnFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "usuario_default";

        String nombre = etNombreMascota.getText().toString().trim();
        if (nombre.isEmpty()) {
            etNombreMascota.setError("Ingresa el nombre de la mascota");
            etNombreMascota.requestFocus();
            return;
        }

        btnGuardarMascota.setEnabled(false);
        btnConfirmarTop.setEnabled(false);

        boolean esNueva = (mascotaId == null || mascotaId.trim().isEmpty() || mascotaId.startsWith("demo_"));
        String idFinal = !esNueva
                ? mascotaId
                : ("pet_" + System.currentTimeMillis());

        Mascota m = new Mascota();
        m.setId(idFinal);
        m.setNombre(nombre);
        m.setTipo(tipoSeleccionado);
        m.setSexo(sexoSeleccionado);
        m.setCastrado(switchCastrado.isChecked());
        m.setRaza(tvRazaSeleccionada.getText().toString().trim());
        m.setFechaNacimiento(tvFechaNacimiento.getText().toString().trim());
        m.setColores(new ArrayList<>(coloresSeleccionados));
        m.setUsuarioId(uid);
        m.setFotoUri(fotoActualPath != null ? fotoActualPath : "");

        // 1. Guardar de forma local y persistente (Garantiza éxito inmediato e ininterrumpido)
        MascotasLocalManager.saveOrUpdateMascota(this, uid, m);

        // Si venía de una demo, registrar la demo anterior como eliminada para no duplicar
        if (mascotaId != null && mascotaId.startsWith("demo_")) {
            SharedPreferences prefs = getSharedPreferences("zoloopatas_prefs", Context.MODE_PRIVATE);
            Set<String> eliminadas = new HashSet<>(prefs.getStringSet("eliminadas_" + uid, new HashSet<>()));
            eliminadas.add(mascotaId);
            prefs.edit().putStringSet("eliminadas_" + uid, eliminadas).apply();
        }

        // 2. Preparar datos completos para Firestore según BDNoSQLZoolopatas.json
        Map<String, Object> data = m.toFirestoreMap();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 3. Sincronizar con Firebase Firestore: primero en usuarios/{uid}/mascotas/{id}
        db.collection("usuarios").document(uid)
                .collection("mascotas").document(idFinal)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Si es nueva mascota, incrementar estadísticas del usuario
                    if (esNueva) {
                        try {
                            db.collection("usuarios").document(uid)
                                    .update("estadisticas.cantidadMascotas", FieldValue.increment(1));
                        } catch (Exception ignored) {}
                    }

                    Toast.makeText(this, "Mascota guardada con éxito en Firestore", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Si falla la red, la mascota ya quedó asegurada localmente
                    Toast.makeText(this, "Mascota guardada localmente", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            camaraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File dir = new File(getFilesDir(), "fotos_mascotas");
            if (!dir.exists()) dir.mkdirs();
            File archivoFoto = new File(dir, "foto_pet_" + System.currentTimeMillis() + ".jpg");
            fotoActualPath = archivoFoto.getAbsolutePath();

            tempCameraUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    archivoFoto
            );

            camaraLauncher.launch(tempCameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarImagenDeGaleria(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "fotos_mascotas");
            if (!dir.exists()) dir.mkdirs();
            File archivoDestino = new File(dir, "foto_pet_" + System.currentTimeMillis() + ".jpg");

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(archivoDestino)) {
                if (in == null) return;
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            fotoActualPath = archivoDestino.getAbsolutePath();
            mostrarFotoPreview(fotoActualPath);
            Toast.makeText(this, "Foto seleccionada de galería", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al procesar la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarFotoPreview(String path) {
        if (path == null || path.trim().isEmpty()) {
            if (ivFotoMascotaPreview != null) ivFotoMascotaPreview.setVisibility(View.GONE);
            if (layoutSinFoto != null) layoutSinFoto.setVisibility(View.VISIBLE);
            return;
        }

        File f = new File(path);
        if (f.exists()) {
            Bitmap bmp = decodeSampledBitmapFromFile(f.getAbsolutePath(), 400, 400);
            if (bmp != null) {
                if (ivFotoMascotaPreview != null) {
                    ivFotoMascotaPreview.setImageBitmap(bmp);
                    ivFotoMascotaPreview.setVisibility(View.VISIBLE);
                }
                if (layoutSinFoto != null) layoutSinFoto.setVisibility(View.GONE);
                return;
            }
        }
        if (ivFotoMascotaPreview != null) ivFotoMascotaPreview.setVisibility(View.GONE);
        if (layoutSinFoto != null) layoutSinFoto.setVisibility(View.VISIBLE);
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        if (path == null || path.trim().isEmpty()) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
