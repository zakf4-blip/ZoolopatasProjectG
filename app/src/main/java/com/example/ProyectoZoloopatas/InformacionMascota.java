package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class InformacionMascota extends AppCompatActivity {

    private TextView tvNombreMascotaDetalle;
    private ImageView foto_Mascota;
    private TextView Especie_Mascota, Raza_Mascota, Sexo_Mascota, Castra_afirmacion;
    private TextView Fecha_de_Nacimiento, Anios_cumplidos, Color_Mascota;
    private ImageButton btnBackTop, btnProfileAvatar, btnEditarMascota;

    private String petId;
    private String petNombre;
    private Mascota mascotaActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_informacion_mascota);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupNavigation();

        petId = getIntent().getStringExtra(editar_infoMascota.EXTRA_MASCOTA_ID);
        petNombre = getIntent().getStringExtra(editar_infoMascota.EXTRA_NOMBRE);

        cargarDatosMascota();
    }

    private void initViews() {
        tvNombreMascotaDetalle = findViewById(R.id.tvNombreMascotaDetalle);
        foto_Mascota = findViewById(R.id.foto_Mascota);

        Especie_Mascota = findViewById(R.id.Especie_Mascota);
        Raza_Mascota = findViewById(R.id.Raza_Mascota);
        Sexo_Mascota = findViewById(R.id.Sexo_Mascota);
        Castra_afirmacion = findViewById(R.id.Castra_afirmacion);
        Fecha_de_Nacimiento = findViewById(R.id.Fecha_de_Nacimiento);
        Anios_cumplidos = findViewById(R.id.Anios_cumplidos);
        Color_Mascota = findViewById(R.id.Color_Mascota);

        btnBackTop = findViewById(R.id.btnBackTop);
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        btnEditarMascota = findViewById(R.id.btnEditarMascota);
    }

    private void setupNavigation() {
        if (btnBackTop != null) {
            btnBackTop.setOnClickListener(v -> finish());
        }

        if (btnProfileAvatar != null) {
            btnProfileAvatar.setOnClickListener(v ->
                    startActivity(new Intent(this, PerfilUsuario.class)));
        }

        if (btnEditarMascota != null) {
            btnEditarMascota.setOnClickListener(v -> {
                Intent intent = new Intent(InformacionMascota.this, editar_infoMascota.class);
                if (mascotaActual != null) {
                    intent.putExtra(editar_infoMascota.EXTRA_MASCOTA_ID, mascotaActual.getId());
                    intent.putExtra(editar_infoMascota.EXTRA_NOMBRE, mascotaActual.getNombre());
                    intent.putExtra(editar_infoMascota.EXTRA_TIPO, mascotaActual.getTipo());
                    intent.putExtra(editar_infoMascota.EXTRA_SEXO, mascotaActual.getSexo());
                    intent.putExtra(editar_infoMascota.EXTRA_CASTRADO, mascotaActual.isCastrado());
                    intent.putExtra(editar_infoMascota.EXTRA_RAZA, mascotaActual.getRaza());
                    intent.putExtra(editar_infoMascota.EXTRA_FECHA, mascotaActual.getFechaNacimiento());
                    if (mascotaActual.getColores() != null) {
                        intent.putStringArrayListExtra(editar_infoMascota.EXTRA_COLORES, new ArrayList<>(mascotaActual.getColores()));
                    }
                    if (mascotaActual.getFotoUri() != null) {
                        intent.putExtra(editar_infoMascota.EXTRA_FOTO_URI, mascotaActual.getFotoUri());
                    }
                } else if (getIntent().getExtras() != null) {
                    intent.putExtras(getIntent().getExtras());
                }
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosMascota();
    }

    private void cargarDatosMascota() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "usuario_default";

        // Si fue eliminada, cerrar la pantalla y regresar
        Set<String> eliminadas = MascotasLocalManager.getEliminadas(this, uid);
        if ((petId != null && eliminadas.contains(petId)) ||
                (petNombre != null && (eliminadas.contains("nombre_" + petNombre.trim()) || eliminadas.contains(petNombre.trim().toLowerCase())))) {
            finish();
            return;
        }

        // Buscar mascota actualizada en el gestor local
        List<Mascota> lista = MascotasLocalManager.getMascotas(this, uid);
        Mascota encontrada = null;
        for (Mascota m : lista) {
            boolean mismoId = petId != null && petId.equals(m.getId());
            boolean mismoNombre = petNombre != null && petNombre.equalsIgnoreCase(m.getNombre());
            if (mismoId || mismoNombre) {
                encontrada = m;
                break;
            }
        }

        // Si no se encuentra en el gestor local, intentar construirla desde los extras del intent
        if (encontrada == null && getIntent().getExtras() != null) {
            encontrada = new Mascota();
            encontrada.setId(petId);
            encontrada.setNombre(getIntent().getStringExtra(editar_infoMascota.EXTRA_NOMBRE));
            encontrada.setTipo(getIntent().getStringExtra(editar_infoMascota.EXTRA_TIPO));
            encontrada.setSexo(getIntent().getStringExtra(editar_infoMascota.EXTRA_SEXO));
            encontrada.setCastrado(getIntent().getBooleanExtra(editar_infoMascota.EXTRA_CASTRADO, true));
            encontrada.setRaza(getIntent().getStringExtra(editar_infoMascota.EXTRA_RAZA));
            encontrada.setFechaNacimiento(getIntent().getStringExtra(editar_infoMascota.EXTRA_FECHA));
            ArrayList<String> cols = getIntent().getStringArrayListExtra(editar_infoMascota.EXTRA_COLORES);
            if (cols != null) encontrada.setColores(cols);
            encontrada.setFotoUri(getIntent().getStringExtra(editar_infoMascota.EXTRA_FOTO_URI));
        }

        if (encontrada == null) return;

        mascotaActual = encontrada;
        petId = mascotaActual.getId();
        petNombre = mascotaActual.getNombre();

        // 1. Mostrar inmediatamente los datos locales
        actualizarVistasConMascota();

        // 2. Consultar en segundo plano a Firestore para refrescar con datos de la nube
        if (uid != null && !"usuario_default".equals(uid) && petId != null && !petId.startsWith("demo_")) {
            final String finalPetId = petId;
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios").document(uid)
                    .collection("mascotas").document(finalPetId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Mascota mRemota = Mascota.fromFirestoreDoc(doc);
                            if (mRemota != null) {
                                mRemota.setUsuarioId(uid);
                                mascotaActual = mRemota;
                                MascotasLocalManager.saveOrUpdateMascota(InformacionMascota.this, uid, mRemota);
                                actualizarVistasConMascota();
                            }
                        }
                    });
        }
    }

    private void actualizarVistasConMascota() {
        if (mascotaActual == null) return;

        // ── 1. Nombre ──
        if (tvNombreMascotaDetalle != null) {
            tvNombreMascotaDetalle.setText(mascotaActual.getNombre());
        }

        // ── 2. Especie ──
        boolean esGato = mascotaActual.getTipo() != null &&
                (mascotaActual.getTipo().equalsIgnoreCase("Gato") || mascotaActual.getTipo().equalsIgnoreCase("Felino"));
        if (Especie_Mascota != null) {
            Especie_Mascota.setText(esGato ? "Felino" : "Canino");
        }

        // ── 3. Raza ──
        if (Raza_Mascota != null) {
            String raza = mascotaActual.getRaza();
            Raza_Mascota.setText((raza != null && !raza.trim().isEmpty()) ? raza : "Mestizo");
        }

        // ── 4. Sexo ──
        String sexo = mascotaActual.getSexo() != null ? mascotaActual.getSexo() : "Macho";
        if (Sexo_Mascota != null) {
            Sexo_Mascota.setText(sexo);
        }

        // ── 5. Castrado ──
        if (Castra_afirmacion != null) {
            boolean esHembra = sexo.equalsIgnoreCase("Hembra");
            if (mascotaActual.isCastrado()) {
                Castra_afirmacion.setText(esHembra ? "Sí (Esterilizada)" : "Sí (Castrado)");
            } else {
                Castra_afirmacion.setText("No (Intacto)");
            }
        }

        // ── 6. Fecha de Nacimiento y Años Cumplidos ──
        String fecha = mascotaActual.getFechaNacimiento();
        if (Fecha_de_Nacimiento != null) {
            Fecha_de_Nacimiento.setText((fecha != null && !fecha.trim().isEmpty()) ? fecha : "--/--/----");
        }
        if (Anios_cumplidos != null) {
            Anios_cumplidos.setText(calcularEdad(fecha));
        }

        // ── 7. Coloración ──
        if (Color_Mascota != null) {
            List<String> colores = mascotaActual.getColores();
            if (colores != null && !colores.isEmpty()) {
                Color_Mascota.setText(TextUtils.join(", ", colores));
            } else {
                Color_Mascota.setText("No especificado");
            }
        }

        // ── 8. Foto / Avatar ──
        if (foto_Mascota != null) {
            String fotoUri = mascotaActual.getFotoUri();
            boolean fotoCargada = false;
            if (fotoUri != null && !fotoUri.trim().isEmpty()) {
                Bitmap bmp = editar_infoMascota.decodeSampledBitmapFromFile(fotoUri, 600, 600);
                if (bmp != null) {
                    foto_Mascota.setImageBitmap(bmp);
                    fotoCargada = true;
                }
            }
            if (!fotoCargada) {
                String nombre = mascotaActual.getNombre() != null ? mascotaActual.getNombre() : "";
                if ("Rocky".equalsIgnoreCase(nombre)) {
                    foto_Mascota.setImageResource(R.drawable.img_pet_rocky_clinic);
                } else if ("Luna".equalsIgnoreCase(nombre)) {
                    foto_Mascota.setImageResource(R.drawable.img_pet_luna);
                } else if (esGato) {
                    foto_Mascota.setImageResource(R.drawable.img_cat_icon_completo);
                } else {
                    foto_Mascota.setImageResource(R.drawable.img_dog_icon_completo);
                }
            }
        }
    }

    private String calcularEdad(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return "Edad no registrada";
        try {
            String[] partes = fecha.split("/");
            if (partes.length == 3) {
                int d = Integer.parseInt(partes[0].trim());
                int mth = Integer.parseInt(partes[1].trim()) - 1;
                int y = Integer.parseInt(partes[2].trim());

                Calendar nac = Calendar.getInstance();
                nac.set(y, mth, d);
                Calendar hoy = Calendar.getInstance();

                int anos = hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR);
                if (hoy.get(Calendar.DAY_OF_YEAR) < nac.get(Calendar.DAY_OF_YEAR)) {
                    anos--;
                }

                if (anos > 1) {
                    return anos + " años cumplidos";
                } else if (anos == 1) {
                    return "1 año cumplido";
                } else {
                    int meses = (hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR)) * 12 + (hoy.get(Calendar.MONTH) - nac.get(Calendar.MONTH));
                    if (meses <= 1) return "1 mes cumplido";
                    else return meses + " meses";
                }
            }
        } catch (Exception ignored) {}
        return "Edad no registrada";
    }
}
