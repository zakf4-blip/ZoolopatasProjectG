package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class historial_clinico_mascotas extends AppCompatActivity {

    private static final int TAB_REVISION = 0;
    private static final int TAB_VACUNAS = 1;
    private static final int TAB_DESPARASITACION = 2;

    private LinearLayout layoutMascotasHistorial, layoutRegistrosVacunas;
    private View cardRevisionGeneral;
    private TextView tvEstadoVacio;

    private final LinearLayout[] tabs = new LinearLayout[3];
    private final TextView[] tabTitulos = new TextView[3];
    private final TextView[] tabBadges = new TextView[3];

    private final List<Mascota> listaMascotas = new ArrayList<>();
    private String mascotaSeleccionadaId = null;
    private int tabActual = TAB_REVISION;

    // Al volver de registrar una vacuna con éxito, se abre el tab "Vacunas"
    private final ActivityResultLauncher<Intent> registrarVacunaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    tabActual = TAB_VACUNAS;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_clinico_mascotas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutMascotasHistorial = findViewById(R.id.layoutMascotasHistorial);
        layoutRegistrosVacunas = findViewById(R.id.layoutRegistrosVacunas);
        cardRevisionGeneral = findViewById(R.id.cardRevisionGeneral);
        tvEstadoVacio = findViewById(R.id.tvEstadoVacio);

        tabs[TAB_REVISION] = findViewById(R.id.tabRevision);
        tabs[TAB_VACUNAS] = findViewById(R.id.tabVacunas);
        tabs[TAB_DESPARASITACION] = findViewById(R.id.tabDesparasitacion);
        tabTitulos[TAB_REVISION] = findViewById(R.id.tvTabRevision);
        tabTitulos[TAB_VACUNAS] = findViewById(R.id.tvTabVacunas);
        tabTitulos[TAB_DESPARASITACION] = findViewById(R.id.tvTabDesparasitacion);
        tabBadges[TAB_REVISION] = findViewById(R.id.tvBadgeRevision);
        tabBadges[TAB_VACUNAS] = findViewById(R.id.tvBadgeVacunas);
        tabBadges[TAB_DESPARASITACION] = findViewById(R.id.tvBadgeDesparasitacion);

        for (int i = 0; i < tabs.length; i++) {
            final int indice = i;
            tabs[i].setOnClickListener(v -> {
                tabActual = indice;
                actualizarTabs();
                mostrarRegistros();
            });
        }

        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        findViewById(R.id.btnPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));

        findViewById(R.id.fabAgregarRegistro).setOnClickListener(v -> {
            if (mascotaSeleccionadaId == null) {
                Toast.makeText(this, "Primero registra o selecciona una mascota.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, registrar_vacuna.class);
            intent.putExtra(editar_infoMascota.EXTRA_MASCOTA_ID, mascotaSeleccionadaId);
            registrarVacunaLauncher.launch(intent);
        });

        findViewById(R.id.btnAgregarCupo).setOnClickListener(v -> {
            int limite = PremiumManager.getLimiteMascotas(this);
            if (listaMascotas.size() >= limite) {
                Toast.makeText(this, "Has alcanzado el límite de " + limite + " mascotas en tu " + PremiumManager.getPlan(this) + ".",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, editar_infoMascota.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMascotas();

        String uid = obtenerUidActual();
        if (uid != null && !"usuario_default".equals(uid)) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios").document(uid).collection("mascotas").get()
                    .addOnSuccessListener(subSnapshots -> {
                        List<Mascota> remotas = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : subSnapshots) {
                            try {
                                Mascota m = Mascota.fromFirestoreDoc(doc);
                                if (m != null) {
                                    m.setUsuarioId(uid);
                                    remotas.add(m);
                                }
                            } catch (Exception ignored) {}
                        }
                        MascotasLocalManager.setMascotasFromFirestore(this, uid, remotas);
                        cargarMascotas();
                    });
        }
    }

    private String obtenerUidActual() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : "usuario_default";
    }

    // ───────────────────────── Mascotas ─────────────────────────

    private void cargarMascotas() {
        List<Mascota> locales = MascotasLocalManager.getMascotas(this, obtenerUidActual());
        listaMascotas.clear();
        listaMascotas.addAll(locales);

        boolean seleccionValida = false;
        for (Mascota m : listaMascotas) {
            if (Objects.equals(m.getId(), mascotaSeleccionadaId)) {
                seleccionValida = true;
                break;
            }
        }
        if (!seleccionValida) {
            mascotaSeleccionadaId = listaMascotas.isEmpty() ? null : listaMascotas.get(0).getId();
        }

        mostrarMascotas();
        actualizarTabs();
        mostrarRegistros();

        if (mascotaSeleccionadaId != null) {
            VacunasLocalManager.cargarVacunasDeFirestore(this, obtenerUidActual(), mascotaSeleccionadaId, this::mostrarRegistros);
        }
    }

    private void mostrarMascotas() {
        layoutMascotasHistorial.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Mascota m : listaMascotas) {
            View item = inflater.inflate(R.layout.item_tarjeta_vacuna_mascota, layoutMascotasHistorial, false);
            ImageView ivAvatar = item.findViewById(R.id.ivAvatarMascota);

            String nombre = m.getNombre().isEmpty() ? "Mascota" : m.getNombre();
            ivAvatar.setContentDescription(nombre);

            boolean esGato = m.getTipo().equalsIgnoreCase("Gato") || m.getTipo().equalsIgnoreCase("Felino");

            boolean fotoCargada = false;
            String fotoUri = m.getFotoUri();
            if (fotoUri != null && !fotoUri.trim().isEmpty()) {
                Bitmap bmp = editar_infoMascota.decodeSampledBitmapFromFile(fotoUri, 200, 200);
                if (bmp != null) {
                    ivAvatar.setImageBitmap(bmp);
                    fotoCargada = true;
                }
            }
            if (!fotoCargada) {
                if ("Rocky".equalsIgnoreCase(nombre)) {
                    ivAvatar.setImageResource(R.drawable.img_pet_rocky);
                } else if ("Luna".equalsIgnoreCase(nombre)) {
                    ivAvatar.setImageResource(R.drawable.img_pet_luna);
                } else if (esGato) {
                    ivAvatar.setImageResource(R.drawable.img_cat_icon_completo);
                } else {
                    ivAvatar.setImageResource(R.drawable.img_dog_icon_completo);
                }
            }

            item.setAlpha(Objects.equals(m.getId(), mascotaSeleccionadaId) ? 1f : 0.45f);

            item.setOnClickListener(v -> {
                mascotaSeleccionadaId = m.getId();
                mostrarMascotas();
                mostrarRegistros(); // historial individual de la mascota tocada
                if (mascotaSeleccionadaId != null) {
                    VacunasLocalManager.cargarVacunasDeFirestore(this, obtenerUidActual(), mascotaSeleccionadaId, this::mostrarRegistros);
                }
            });

            layoutMascotasHistorial.addView(item);
        }
    }

    private String nombreMascotaSeleccionada() {
        for (Mascota m : listaMascotas) {
            if (Objects.equals(m.getId(), mascotaSeleccionadaId)) {
                return m.getNombre().isEmpty() ? "tu mascota" : m.getNombre();
            }
        }
        return "tu mascota";
    }

    // ───────────────────────── Tabs ─────────────────────────

    private void actualizarTabs() {
        for (int i = 0; i < tabs.length; i++) {
            boolean sel = (i == tabActual);
            setFondo(tabs[i], sel ? R.drawable.bg_btn_segmented_selected : R.drawable.bg_btn_segmented_unselected);
            tabTitulos[i].setTextColor(sel ? Color.WHITE : ContextCompat.getColor(this, R.color.gray_subtitle));
            tabTitulos[i].setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);
            setFondo(tabBadges[i], sel ? R.drawable.bg_icon_teal : R.drawable.bg_edit_card_gris);
            tabBadges[i].setTextColor(sel ? Color.WHITE : ContextCompat.getColor(this, R.color.gray_subtitle));
        }
    }

    // Cambia el fondo conservando el padding original de la vista
    private void setFondo(View v, int drawableRes) {
        int l = v.getPaddingLeft(), t = v.getPaddingTop(), r = v.getPaddingRight(), b = v.getPaddingBottom();
        v.setBackgroundResource(drawableRes);
        v.setPadding(l, t, r, b);
    }

    // ───────────────────────── Registros ─────────────────────────

    private void mostrarRegistros() {
        layoutRegistrosVacunas.removeAllViews();

        List<Vacuna> vacunas = new ArrayList<>();
        if (mascotaSeleccionadaId != null) {
            vacunas.addAll(VacunasLocalManager.getVacunas(this, obtenerUidActual(), mascotaSeleccionadaId));
            // Más recientes primero
            Collections.sort(vacunas, (a, b) ->
                    Long.compare(millis(b.getFechaAplicacion()), millis(a.getFechaAplicacion())));
        }
        tabBadges[TAB_VACUNAS].setText(String.valueOf(vacunas.size()));

        cardRevisionGeneral.setVisibility(tabActual == TAB_REVISION ? View.VISIBLE : View.GONE);
        layoutRegistrosVacunas.setVisibility(tabActual == TAB_VACUNAS ? View.VISIBLE : View.GONE);
        tvEstadoVacio.setVisibility(View.GONE);

        if (mascotaSeleccionadaId == null) {
            cardRevisionGeneral.setVisibility(View.GONE);
            layoutRegistrosVacunas.setVisibility(View.GONE);
            tvEstadoVacio.setText("Registra una mascota para ver su historial clínico.");
            tvEstadoVacio.setVisibility(View.VISIBLE);
            return;
        }

        if (tabActual == TAB_VACUNAS) {
            if (vacunas.isEmpty()) {
                tvEstadoVacio.setText("Aún no hay vacunas registradas para " + nombreMascotaSeleccionada()
                        + ".\nToca el botón + para agregar una.");
                tvEstadoVacio.setVisibility(View.VISIBLE);
            } else {
                for (Vacuna v : vacunas) agregarTarjetaVacuna(v);
            }
        } else if (tabActual == TAB_DESPARASITACION) {
            tvEstadoVacio.setText("Aún no hay registros de desparasitación para "
                    + nombreMascotaSeleccionada() + ".");
            tvEstadoVacio.setVisibility(View.VISIBLE);
        }
    }

    private void agregarTarjetaVacuna(Vacuna v) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_tarjeta_vacunas, layoutRegistrosVacunas, false);

        ((TextView) card.findViewById(R.id.tvTipoVacunaItem)).setText(v.getTipoVacuna());
        ((TextView) card.findViewById(R.id.tvFechaAplicacionItem)).setText("• " + v.getFechaAplicacion());
        ((TextView) card.findViewById(R.id.tvRefuerzoItem))
                .setText("Próximo refuerzo: " + v.getFechaVencimiento());

        // Estado según la fecha de vencimiento
        TextView tvEstado = card.findViewById(R.id.tvEstadoVacuna);
        long ven = millis(v.getFechaVencimiento());
        if (ven == 0L) {
            tvEstado.setVisibility(View.GONE);
        } else {
            long dias = TimeUnit.MILLISECONDS.toDays(ven - hoyMillis());
            if (dias < 0) {
                tvEstado.setText("Vencida");
                tvEstado.setTextColor(Color.parseColor("#DC2626"));
            } else if (dias <= 30) {
                tvEstado.setText("Vence pronto");
                tvEstado.setTextColor(Color.parseColor("#B7791F"));
            } else {
                tvEstado.setText("Vigente");
                tvEstado.setTextColor(Color.parseColor("#059669"));
            }
        }

        TextView tvComentarios = card.findViewById(R.id.tvComentariosItem);
        if (v.getComentarios().isEmpty()) {
            tvComentarios.setVisibility(View.GONE);
        } else {
            tvComentarios.setText(v.getComentarios());
        }

        card.findViewById(R.id.btnEliminarVacuna).setOnClickListener(x -> confirmarEliminarVacuna(v));

        layoutRegistrosVacunas.addView(card);
    }

    private void confirmarEliminarVacuna(Vacuna v) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar vacuna")
                .setMessage("¿Eliminar \"" + v.getTipoVacuna() + "\" del historial? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    String uid = obtenerUidActual();
                    VacunasLocalManager.deleteVacuna(this, uid, v.getMascotaId(), v.getId());
                    VacunasLocalManager.eliminarEnFirestore(uid, v.getMascotaId(), v.getId());
                    mostrarRegistros();
                    Toast.makeText(this, "Vacuna eliminada.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ───────────────────────── Fechas (dd/MM/yyyy) ─────────────────────────

    private long millis(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return 0L;
        try {
            String[] p = fecha.split("/");
            if (p.length != 3) return 0L;
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(p[2].trim()), Integer.parseInt(p[1].trim()) - 1,
                    Integer.parseInt(p[0].trim()));
            return c.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private long hoyMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}