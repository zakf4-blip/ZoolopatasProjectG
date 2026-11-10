package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.net.URLEncoder;
import java.util.Locale;

public class activity_contactoVeterinaria extends AppCompatActivity implements OnMapReadyCallback {

    private Veterinaria vet;
    private GoogleMap mMap;

    // UI Views
    private ImageButton btnBack;
    private FrameLayout btnProfileAvatar;
    private FrameLayout btnFavorito;
    private ImageView ivIconFavorito;
    private FrameLayout btnShare;

    private TextView tvCodigoSenasa;
    private TextView tvNombreVeterinaria;
    private TextView tvEspecialidades;
    private TextView tvRatingValor;
    private TextView tvCantidadResenas;

    private TextView btnTriajeDirecto;
    private LinearLayout btnLlamarUrgencias;
    private LinearLayout btnWhatsappSos;
    private LinearLayout btnComoLlegar;

    private TextView tvDistanciaBadge;
    private TextView tvPinDistritoTexto;
    private FrameLayout btnMiniMapCenterGps;
    private FrameLayout btnMiniMapExpand;

    private TextView tvDireccionDetallada;
    private TextView tvReferencia;
    private TextView tvHorarioDetallado;
    private TextView tvBadgeTurnoNoche;

    private TextView tvDirectorioTelefono;
    private FrameLayout btnDirectorioLlamar;
    private FrameLayout btnDirectorioWhatsapp;

    private LinearLayout btnBottomLlamar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacto_veterinaria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Obtener el objeto Veterinaria del Intent
        vet = (Veterinaria) getIntent().getSerializableExtra("veterinaria");
        if (vet == null) {
            // Datos de respaldo predeterminados si se abre directamente
            vet = new Veterinaria(
                    "vet_default",
                    "Hospital Veterinario Central Miraflores",
                    "Clínica Especializada",
                    "Av. José Pardo 834, Miraflores, Lima",
                    "Miraflores",
                    4.9,
                    248,
                    true,
                    "Abierto 24 Horas",
                    1.4,
                    "Medicina General • Cirugía de Especialidad • Urgencias 24 Horas",
                    "+51 981 234 567",
                    "S/ 70",
                    -12.1220,
                    -77.0320,
                    true,
                    R.drawable.img_clinic_san_martin
            );
        }

        inicializarVistas();
        configurarDatosVeterinaria();
        configurarMiniMapa();
        configurarListeners();
    }

    private void inicializarVistas() {
        btnBack = findViewById(R.id.btnBack);
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);
        btnFavorito = findViewById(R.id.btnFavorito);
        ivIconFavorito = findViewById(R.id.ivIconFavorito);
        btnShare = findViewById(R.id.btnShare);

        tvCodigoSenasa = findViewById(R.id.tvCodigoSenasa);
        tvNombreVeterinaria = findViewById(R.id.tvNombreVeterinaria);
        tvEspecialidades = findViewById(R.id.tvEspecialidades);
        tvRatingValor = findViewById(R.id.tvRatingValor);
        tvCantidadResenas = findViewById(R.id.tvCantidadResenas);

        btnTriajeDirecto = findViewById(R.id.btnTriajeDirecto);
        btnLlamarUrgencias = findViewById(R.id.btnLlamarUrgencias);
        btnWhatsappSos = findViewById(R.id.btnWhatsappSos);
        btnComoLlegar = findViewById(R.id.btnComoLlegar);

        tvDistanciaBadge = findViewById(R.id.tvDistanciaBadge);
        tvPinDistritoTexto = findViewById(R.id.tvPinDistritoTexto);
        btnMiniMapCenterGps = findViewById(R.id.btnMiniMapCenterGps);
        btnMiniMapExpand = findViewById(R.id.btnMiniMapExpand);

        tvDireccionDetallada = findViewById(R.id.tvDireccionDetallada);
        tvReferencia = findViewById(R.id.tvReferencia);
        tvHorarioDetallado = findViewById(R.id.tvHorarioDetallado);
        tvBadgeTurnoNoche = findViewById(R.id.tvBadgeTurnoNoche);

        tvDirectorioTelefono = findViewById(R.id.tvDirectorioTelefono);
        btnDirectorioLlamar = findViewById(R.id.btnDirectorioLlamar);
        btnDirectorioWhatsapp = findViewById(R.id.btnDirectorioWhatsapp);

        btnBottomLlamar = findViewById(R.id.btnBottomLlamar);
    }

    private void configurarDatosVeterinaria() {
        // Código SENASA y Sede
        String distrito = (vet.getDistrito() != null && !vet.getDistrito().isEmpty())
                ? vet.getDistrito()
                : "Miraflores";
        int codSenasa = Math.abs((vet.getNombre() != null ? vet.getNombre().hashCode() : 24099) % 900) + 100;
        tvCodigoSenasa.setText("SEDE PRINCIPAL • Código Reg. SENASA 24-" + (codSenasa % 100 < 10 ? "0" : "") + (codSenasa % 100));

        // Nombre
        tvNombreVeterinaria.setText(vet.getNombre());

        // Especialidades
        String esp = vet.getEspecialidad();
        if (esp == null || esp.trim().isEmpty()) {
            esp = "Medicina General • Cirugía de Especialidad • Urgencias 24 Horas";
        } else if (!esp.toLowerCase().contains("urgencias") && !esp.toLowerCase().contains("24")) {
            esp = esp + " • Urgencias 24 Horas";
        }
        tvEspecialidades.setText(esp);

        // Calificación y Reseñas
        tvRatingValor.setText(String.format(Locale.US, "%.1f", vet.getCalificacion() > 0 ? vet.getCalificacion() : 4.9));
        int resenas = vet.getCantidadResenas() > 0 ? vet.getCantidadResenas() : 248;
        tvCantidadResenas.setText("(" + resenas + " valoraciones)");

        // Estado Favorito
        boolean esFav = VeterinariaData.isFavorita(this, vet.getId());
        actualizarIconoFavorito(esFav);

        // Distancia
        double dist = vet.getDistanciaKm() > 0 ? vet.getDistanciaKm() : 1.4;
        tvDistanciaBadge.setText(String.format(Locale.US, "A %.1f km de ti", dist));

        // Distrito en badge flotante del mapa
        tvPinDistritoTexto.setText(distrito + ", Lima");

        // Dirección completa
        String dir = vet.getDireccion();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "Av. José Pardo 834, " + distrito + ", Lima";
        }
        tvDireccionDetallada.setText(dir);

        // Referencia
        String ref = "Referencia: A media cuadra del Óvalo Julio Ramón Ribeyro";
        if (!distrito.equalsIgnoreCase("Miraflores")) {
            ref = "Referencia: Zona céntrica de " + distrito + " con acceso directo a avenidas";
        }
        tvReferencia.setText(ref);

        // Horario y Turno Noche
        if (vet.isAbierto()) {
            tvHorarioDetallado.setText(vet.getHorarioTexto() != null && !vet.getHorarioTexto().isEmpty()
                    ? vet.getHorarioTexto() : "Abierto 24 Horas");
            tvBadgeTurnoNoche.setText("TURNO NOCHE ACTIVO");
            tvBadgeTurnoNoche.setVisibility(View.VISIBLE);
        } else {
            tvHorarioDetallado.setText(vet.getHorarioTexto() != null && !vet.getHorarioTexto().isEmpty()
                    ? vet.getHorarioTexto() : "Cerrado temporalmente");
            tvBadgeTurnoNoche.setText("URGENCIAS ACTIVAS");
            tvBadgeTurnoNoche.setVisibility(View.VISIBLE);
        }

        // Directorio telefónico
        String tel = vet.getTelefono();
        if (tel == null || tel.trim().isEmpty()) {
            tel = "+51 981 234 567";
        }
        tvDirectorioTelefono.setText(tel);
    }

    private void configurarMiniMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragmentMini);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;

        // Limpiar controles innecesarios para mini-mapa
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false); // Evita interferencia con el scrollview

        centrarEnClinica();

        // Al tocar el mapa, abrir navegación en pantalla completa
        mMap.setOnMapClickListener(latLng -> abrirNavegacionGps());
    }

    private void centrarEnClinica() {
        if (mMap == null || vet == null) return;

        LatLng posicion = new LatLng(vet.getLatitud(), vet.getLongitud());
        mMap.clear();

        BitmapDescriptor iconoMarcador = crearIconoPatitaMarcador();

        mMap.addMarker(new MarkerOptions()
                .position(posicion)
                .title(vet.getNombre())
                .snippet(vet.getDireccion())
                .icon(iconoMarcador));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15.5f));
    }

    private BitmapDescriptor crearIconoPatitaMarcador() {
        try {
            View markerView = LayoutInflater.from(this).inflate(R.layout.view_paw_pin_mini, null);
            FrameLayout markerCircle = markerView.findViewById(R.id.markerCirclePawMini);

            if (vet != null && vet.isEspecializada()) {
                markerCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#007A6E")));
            } else {
                markerCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F766E")));
            }

            markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            markerView.draw(canvas);

            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
    }

    private void configurarListeners() {
        // Volver atrás
        btnBack.setOnClickListener(v -> finish());

        // Perfil de usuario
        btnProfileAvatar.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, PerfilUsuario.class);
                startActivity(intent);
            } catch (Exception ignored) {}
        });

        // Alternar favorito
        btnFavorito.setOnClickListener(v -> {
            boolean nuevoEstado = !VeterinariaData.isFavorita(this, vet.getId());
            VeterinariaData.setFavorita(this, vet.getId(), nuevoEstado);
            vet.setEsFavorita(nuevoEstado);
            actualizarIconoFavorito(nuevoEstado);
            Toast.makeText(this, nuevoEstado ? "Añadida a tus centros favoritos" : "Eliminada de tus favoritos", Toast.LENGTH_SHORT).show();
        });

        // Compartir contacto
        btnShare.setOnClickListener(v -> compartirContacto());

        // Llamadas de emergencia
        View.OnClickListener llamadaListener = v -> realizarLlamada(vet.getTelefono());
        btnTriajeDirecto.setOnClickListener(llamadaListener);
        btnLlamarUrgencias.setOnClickListener(llamadaListener);
        btnDirectorioLlamar.setOnClickListener(llamadaListener);
        btnBottomLlamar.setOnClickListener(llamadaListener);

        // WhatsApp SOS
        btnWhatsappSos.setOnClickListener(v -> {
            String mensaje = "🚨 *URGENCIA VETERINARIA - APP ZOOLOPATAS*\n"
                    + "Hola, requiero atención médica urgente para mi mascota en *" + vet.getNombre() + "*.\n"
                    + "Por favor indíquenme si cuentan con especialista de turno y quirófano disponible de inmediato.";
            abrirWhatsApp(vet.getTelefono(), mensaje);
        });

        // WhatsApp Directorio
        btnDirectorioWhatsapp.setOnClickListener(v -> {
            String mensaje = "Hola, me comunico a través de la app *Zoolopatas* para realizar una consulta médica en *" + vet.getNombre() + "*.";
            abrirWhatsApp(vet.getTelefono(), mensaje);
        });

        // Cómo llegar y controles de mapa
        btnComoLlegar.setOnClickListener(v -> abrirNavegacionGps());
        btnMiniMapExpand.setOnClickListener(v -> abrirNavegacionGps());
        btnMiniMapCenterGps.setOnClickListener(v -> centrarEnClinica());
    }

    private void actualizarIconoFavorito(boolean esFavorita) {
        if (esFavorita) {
            ivIconFavorito.setImageResource(R.drawable.ic_heart_filled);
            ivIconFavorito.setColorFilter(Color.parseColor("#EF4444"));
        } else {
            ivIconFavorito.setImageResource(R.drawable.ic_heart_border);
            ivIconFavorito.setColorFilter(Color.parseColor("#374151"));
        }
    }

    private void realizarLlamada(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            telefono = "981234567";
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + telefono.trim()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo iniciar la llamada: " + telefono, Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirWhatsApp(String telefono, String mensaje) {
        if (telefono == null || telefono.trim().isEmpty()) {
            telefono = "981234567";
        }
        String cleanPhone = telefono.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 9 && cleanPhone.startsWith("9")) {
            cleanPhone = "51" + cleanPhone;
        }

        try {
            String encodedMsg = URLEncoder.encode(mensaje, "UTF-8");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedMsg;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo conectar con WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirNavegacionGps() {
        if (vet == null) return;
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + vet.getLatitud() + "," + vet.getLongitud() + "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Uri fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                        + vet.getLatitud() + "," + vet.getLongitud());
                startActivity(new Intent(Intent.ACTION_VIEW, fallbackUri));
            }
        } catch (Exception e) {
            Uri fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + vet.getLatitud() + "," + vet.getLongitud());
            startActivity(new Intent(Intent.ACTION_VIEW, fallbackUri));
        }
    }

    private void compartirContacto() {
        if (vet == null) return;
        String texto = "🐾 *Contacto Veterinario - Zoolopatas*\n\n"
                + "🏥 *" + vet.getNombre() + "*\n"
                + "📍 Dirección: " + vet.getDireccion() + "\n"
                + "⭐ Calificación: " + vet.getCalificacion() + " (" + vet.getCantidadResenas() + " reseñas)\n"
                + "🕒 Horario: " + vet.getHorarioTexto() + "\n"
                + "📞 Teléfono: " + vet.getTelefono() + "\n"
                + "🗺️ Ubicación: https://maps.google.com/?q=" + vet.getLatitud() + "," + vet.getLongitud();

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, texto);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Compartir contacto de " + vet.getNombre()));
    }
}
