package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PantallaMascotas extends AppCompatActivity {

    private LinearLayout layoutListaMascotas;
    private TextView tvCapacidadPacientes, tvCapacidadSubtitulo;
    private View progressSegment1, progressSegment2, progressSegment3;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<Mascota> listaActualMascotas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pantalla_mascotas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        layoutListaMascotas = findViewById(R.id.layoutListaMascotas);
        tvCapacidadPacientes = findViewById(R.id.tvCapacidadPacientes);
        tvCapacidadSubtitulo = findViewById(R.id.tvCapacidadSubtitulo);
        progressSegment1 = findViewById(R.id.progressSegment1);
        progressSegment2 = findViewById(R.id.progressSegment2);
        progressSegment3 = findViewById(R.id.progressSegment3);

        setupNavigation();
        actualizarBadgePlan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBadgePlan();
        PremiumManager.sincronizarConFirestore(this, this::actualizarBadgePlan);
        cargarMascotasDeFirebase();
    }

    private void actualizarBadgePlan() {
        View badgeLayout = findViewById(R.id.badgePlanGratuito);
        ImageView ivBadge = findViewById(R.id.ivBadgePlan);
        TextView tvBadge = findViewById(R.id.tvBadgePlan);
        TextView tvTituloCupo = findViewById(R.id.tvTituloCupoDisponible);

        boolean esPremium = PremiumManager.isPremium(this);
        String plan = PremiumManager.getPlan(this);

        if (badgeLayout != null && tvBadge != null) {
            if (esPremium) {
                badgeLayout.setBackgroundResource(R.drawable.bg_badge_plan_premium);
                if (ivBadge != null) {
                    ivBadge.setImageResource(R.drawable.ic_award_vital);
                }
                int dias = PremiumManager.getDiasRestantes(this);
                tvBadge.setText("Plan Premium (" + dias + (dias == 1 ? " día)" : " días)"));
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.dashboard_premium_gold));
            } else {
                badgeLayout.setBackgroundResource(R.drawable.bg_badge_plan_gratuito);
                if (ivBadge != null) {
                    ivBadge.setImageResource(R.drawable.ic_badge_check);
                }
                tvBadge.setText("Plan Gratuito");
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.dashboard_teal_text));
            }

            badgeLayout.setOnClickListener(v ->
                    startActivity(new Intent(this, SuscripcionPremium.class)));
        }

        if (tvTituloCupo != null) {
            int limite = PremiumManager.getLimiteMascotas(this);
            int disponibles = Math.max(0, limite - listaActualMascotas.size());
            if (esPremium) {
                tvTituloCupo.setText(disponibles + (disponibles == 1 ? " cupo disponible en Plan Premium" : " cupos disponibles en Plan Premium"));
            } else {
                tvTituloCupo.setText(disponibles + (disponibles == 1 ? " cupo disponible en plan gratuito" : " cupos disponibles en plan gratuito"));
            }
        }
    }

    private void setupNavigation() {
        // Redirigir al registro de nueva mascota mediante btnAgregarCupo, cardCupo y FAB
        View.OnClickListener irAAgregar = v -> {
            int limite = PremiumManager.getLimiteMascotas(PantallaMascotas.this);
            if (listaActualMascotas.size() >= limite) {
                Toast.makeText(PantallaMascotas.this, "Has alcanzado el límite de " + limite + " mascotas en tu " + PremiumManager.getPlan(PantallaMascotas.this) + ".", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent intent = new Intent(PantallaMascotas.this, editar_infoMascota.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(PantallaMascotas.this, "Error al abrir registro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (findViewById(R.id.btnAgregarCupo) != null) {
            findViewById(R.id.btnAgregarCupo).setOnClickListener(irAAgregar);
        }
        if (findViewById(R.id.btnCardCupoDisponible) != null) {
            findViewById(R.id.btnCardCupoDisponible).setOnClickListener(irAAgregar);
        }
        if (findViewById(R.id.cardCupoDisponible) != null) {
            findViewById(R.id.cardCupoDisponible).setOnClickListener(irAAgregar);
        }
        if (findViewById(R.id.fabAgregarMascota) != null) {
            findViewById(R.id.fabAgregarMascota).setOnClickListener(irAAgregar);
        }

        // Navegación inferior
        if (findViewById(R.id.btnNavInicio) != null) {
            findViewById(R.id.btnNavInicio).setOnClickListener(v -> finish());
        }

        View.OnClickListener abrirPerfil = v ->
                startActivity(new Intent(this, PerfilUsuario.class));

        if (findViewById(R.id.btnProfileAvatar) != null) {
            findViewById(R.id.btnProfileAvatar).setOnClickListener(abrirPerfil);
        }
        if (findViewById(R.id.btnNavPerfil) != null) {
            findViewById(R.id.btnNavPerfil).setOnClickListener(abrirPerfil);
        }

        if (findViewById(R.id.btnNavNotificaciones) != null) {
            findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                    Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
        }
    }

    private String obtenerUidActual() {
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null) ? user.getUid() : "usuario_default";
    }

    private void cargarMascotasDeFirebase() {
        String uid = obtenerUidActual();

        // 1. Cargar y mostrar inmediatamente desde el almacenamiento local persistente del usuario
        List<Mascota> locales = MascotasLocalManager.getMascotas(this, uid);
        mostrarMascotas(locales);

        // 2. Intentar sincronizar en segundo plano con Firebase Firestore para este usuario
        if (uid == null || "usuario_default".equals(uid)) return;

        final String finalUid = uid;
        db.collection("usuarios").document(finalUid).collection("mascotas").get()
                .addOnSuccessListener(subSnapshots -> {
                    List<Mascota> remotas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : subSnapshots) {
                        try {
                            Mascota m = Mascota.fromFirestoreDoc(doc);
                            if (m != null) {
                                m.setUsuarioId(finalUid);
                                remotas.add(m);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Buscar también en colección raíz donde usuarioId == finalUid (por si se crearon ahí)
                    db.collection("mascotas").whereEqualTo("usuarioId", finalUid).get()
                            .addOnSuccessListener(rootSnapshots -> {
                                for (QueryDocumentSnapshot doc : rootSnapshots) {
                                    try {
                                        String docId = doc.getId();
                                        boolean existe = false;
                                        for (Mascota r : remotas) {
                                            if (docId.equals(r.getId())) {
                                                existe = true;
                                                break;
                                            }
                                        }
                                        if (!existe) {
                                            Mascota m = Mascota.fromFirestoreDoc(doc);
                                            if (m != null) {
                                                m.setUsuarioId(finalUid);
                                                remotas.add(m);
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                }

                                MascotasLocalManager.setMascotasFromFirestore(PantallaMascotas.this, finalUid, remotas);
                                mostrarMascotas(MascotasLocalManager.getMascotas(PantallaMascotas.this, finalUid));
                            })
                            .addOnFailureListener(e -> {
                                MascotasLocalManager.setMascotasFromFirestore(PantallaMascotas.this, finalUid, remotas);
                                mostrarMascotas(MascotasLocalManager.getMascotas(PantallaMascotas.this, finalUid));
                            });
                })
                .addOnFailureListener(e -> {
                    db.collection("mascotas").whereEqualTo("usuarioId", finalUid).get()
                            .addOnSuccessListener(rootSnapshots -> {
                                List<Mascota> remotas = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : rootSnapshots) {
                                    try {
                                        Mascota m = Mascota.fromFirestoreDoc(doc);
                                        if (m != null) {
                                            m.setUsuarioId(finalUid);
                                            remotas.add(m);
                                        }
                                    } catch (Exception ignored) {}
                                }
                                MascotasLocalManager.setMascotasFromFirestore(PantallaMascotas.this, finalUid, remotas);
                                mostrarMascotas(MascotasLocalManager.getMascotas(PantallaMascotas.this, finalUid));
                            });
                });
    }

    private void mostrarMascotas(List<Mascota> mascotas) {
        listaActualMascotas.clear();
        listaActualMascotas.addAll(mascotas);

        layoutListaMascotas.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Mascota m : mascotas) {
            View cardView = inflater.inflate(R.layout.item_tarjeta_mascota, layoutListaMascotas, false);

            ImageView ivAvatar = cardView.findViewById(R.id.ivAvatarMascota);
            TextView tvNombre = cardView.findViewById(R.id.tvNombreMascota);
            TextView tvEspecie = cardView.findViewById(R.id.tvEspecieMascota);
            TextView tvRaza = cardView.findViewById(R.id.tvRazaMascota);
            TextView tvDetalle = cardView.findViewById(R.id.tvDetalleMascota);
            Button btnTarjeta = cardView.findViewById(R.id.btnTarjetaMascota);
            ImageButton btnOpciones = cardView.findViewById(R.id.btnOpciones);

            String nombre = m.getNombre() != null ? m.getNombre() : "Mascota";
            tvNombre.setText(nombre);

            boolean esGato = m.getTipo() != null && (m.getTipo().equalsIgnoreCase("Gato") || m.getTipo().equalsIgnoreCase("Felino"));
            tvEspecie.setText(esGato ? "Felino" : "Canino");
            tvRaza.setText(m.getRaza() != null ? m.getRaza() : "Mestizo");
            tvDetalle.setText(calcularDetalle(m));

            // Asignación de avatar
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

            // Click en la tarjeta -> Detalle de Mascota
            btnTarjeta.setOnClickListener(v -> {
                Intent intent = new Intent(PantallaMascotas.this, InformacionMascota.class);
                intent.putExtra(editar_infoMascota.EXTRA_MASCOTA_ID, m.getId());
                intent.putExtra(editar_infoMascota.EXTRA_NOMBRE, m.getNombre());
                intent.putExtra(editar_infoMascota.EXTRA_TIPO, m.getTipo());
                intent.putExtra(editar_infoMascota.EXTRA_SEXO, m.getSexo());
                intent.putExtra(editar_infoMascota.EXTRA_CASTRADO, m.isCastrado());
                intent.putExtra(editar_infoMascota.EXTRA_RAZA, m.getRaza());
                intent.putExtra(editar_infoMascota.EXTRA_FECHA, m.getFechaNacimiento());
                if (m.getColores() != null) {
                    intent.putStringArrayListExtra(editar_infoMascota.EXTRA_COLORES, new ArrayList<>(m.getColores()));
                }
                if (m.getFotoUri() != null) {
                    intent.putExtra(editar_infoMascota.EXTRA_FOTO_URI, m.getFotoUri());
                }
                startActivity(intent);
            });

            // Click en botón de opciones (3 puntos: Editar o Eliminar)
            btnOpciones.setOnClickListener(v -> mostrarMenuOpciones(btnOpciones, m));

            layoutListaMascotas.addView(cardView);
        }

        actualizarCapacidad(mascotas.size());
    }

    private void mostrarMenuOpciones(View anchor, Mascota m) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Editar");
        popup.getMenu().add(0, 2, 1, "Eliminar");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // Opción Editar
                try {
                    Intent intent = new Intent(PantallaMascotas.this, editar_infoMascota.class);
                    intent.putExtra(editar_infoMascota.EXTRA_MASCOTA_ID, m.getId());
                    intent.putExtra(editar_infoMascota.EXTRA_NOMBRE, m.getNombre());
                    intent.putExtra(editar_infoMascota.EXTRA_TIPO, m.getTipo());
                    intent.putExtra(editar_infoMascota.EXTRA_SEXO, m.getSexo());
                    intent.putExtra(editar_infoMascota.EXTRA_CASTRADO, m.isCastrado());
                    intent.putExtra(editar_infoMascota.EXTRA_RAZA, m.getRaza());
                    intent.putExtra(editar_infoMascota.EXTRA_FECHA, m.getFechaNacimiento());
                    if (m.getColores() != null) {
                        intent.putStringArrayListExtra(editar_infoMascota.EXTRA_COLORES, new ArrayList<>(m.getColores()));
                    }
                    if (m.getFotoUri() != null) {
                        intent.putExtra(editar_infoMascota.EXTRA_FOTO_URI, m.getFotoUri());
                    }
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(PantallaMascotas.this, "Error al abrir edición: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (item.getItemId() == 2) {
                // Opción Eliminar
                confirmarEliminarMascota(m);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmarEliminarMascota(Mascota m) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Mascota")
                .setMessage("¿Estás seguro de que deseas eliminar a \"" + m.getNombre() + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    String uid = obtenerUidActual();
                    String petId = m.getId();
                    String petNombre = m.getNombre();

                    // 1. Eliminar del almacenamiento local persistente
                    MascotasLocalManager.deleteMascota(this, uid, petId, petNombre);

                    // 2. Refrescar la pantalla de inmediato
                    List<Mascota> actualizadas = MascotasLocalManager.getMascotas(this, uid);
                    mostrarMascotas(actualizadas);
                    Toast.makeText(this, "\"" + petNombre + "\" ha sido eliminada.", Toast.LENGTH_SHORT).show();

                    // 3. Eliminar de Firebase Firestore en segundo plano si está disponible
                    if (petId != null && !petId.startsWith("demo_")) {
                        try {
                            // Limpiar subcolección de vacunas
                            db.collection("usuarios").document(uid)
                                    .collection("mascotas").document(petId)
                                    .collection("vacunas").get()
                                    .addOnSuccessListener(vacSnapshots -> {
                                        for (com.google.firebase.firestore.DocumentSnapshot vDoc : vacSnapshots) {
                                            vDoc.getReference().delete();
                                        }
                                    });

                            db.collection("usuarios").document(uid).collection("mascotas").document(petId).delete();
                            db.collection("mascotas").document(petId).delete();

                            // Decrementar contador en estadísticas
                            db.collection("usuarios").document(uid)
                                    .update("estadisticas.cantidadMascotas", com.google.firebase.firestore.FieldValue.increment(-1));
                        } catch (Exception ignored) {}
                    }
                })
                .show();
    }

    private void actualizarCapacidad(int count) {
        int limite = PremiumManager.getLimiteMascotas(this);
        boolean esPremium = PremiumManager.isPremium(this);

        if (tvCapacidadPacientes != null) {
            tvCapacidadPacientes.setText(count + " de " + limite + " registradas");
        }

        if (progressSegment1 != null) {
            progressSegment1.setBackgroundResource(count >= 1 ?
                    R.drawable.bg_progress_bar_segment_filled : R.drawable.bg_progress_bar_segment_empty);
        }
        if (progressSegment2 != null) {
            progressSegment2.setBackgroundResource(count >= 2 ?
                    R.drawable.bg_progress_bar_segment_filled : R.drawable.bg_progress_bar_segment_empty);
        }
        if (progressSegment3 != null) {
            progressSegment3.setBackgroundResource(count >= 3 ?
                    R.drawable.bg_progress_bar_segment_filled : R.drawable.bg_progress_bar_segment_empty);
        }

        if (tvCapacidadSubtitulo != null) {
            if (esPremium) {
                int restantes = Math.max(0, limite - count);
                tvCapacidadSubtitulo.setText("Plan Premium activo: te quedan " + restantes + " espacios libres para registrar.");
            } else {
                if (count == 0) {
                    tvCapacidadSubtitulo.setText("Tienes 3 espacios libres para registrar a tus compañeros.");
                } else if (count == 1) {
                    tvCapacidadSubtitulo.setText("Te quedan 2 espacios libres para vincular historial clínico o telemetría.");
                } else if (count == 2) {
                    tvCapacidadSubtitulo.setText("Te queda 1 espacio libre para vincular historial clínico o telemetría.");
                } else {
                    tvCapacidadSubtitulo.setText("Has alcanzado la capacidad de 3 mascotas en tu Plan Gratuito.");
                }
            }
        }

        View cardCupo = findViewById(R.id.cardCupoDisponible);
        if (cardCupo != null) {
            cardCupo.setVisibility(count >= limite ? View.GONE : View.VISIBLE);
        }

        actualizarBadgePlan();
    }

    private String calcularDetalle(Mascota m) {
        StringBuilder sb = new StringBuilder();
        String fecha = m.getFechaNacimiento();
        if (fecha != null && !fecha.trim().isEmpty()) {
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
                        sb.append(anos).append(" años • ");
                    } else if (anos == 1) {
                        sb.append("1 año • ");
                    } else {
                        int meses = (hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR)) * 12 + (hoy.get(Calendar.MONTH) - nac.get(Calendar.MONTH));
                        if (meses <= 1) sb.append("1 mes • ");
                        else sb.append(meses).append(" meses • ");
                    }
                }
            } catch (Exception ignored) {}
        }

        sb.append(m.getSexo());
        if (m.isCastrado()) {
            sb.append(m.getSexo().equalsIgnoreCase("Hembra") ? " esterilizada" : " castrado");
        }
        return sb.toString();
    }
}
