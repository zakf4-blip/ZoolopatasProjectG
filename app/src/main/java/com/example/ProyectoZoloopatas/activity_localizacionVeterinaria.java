package com.example.ProyectoZoloopatas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class activity_localizacionVeterinaria extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, VeterinariaAdapter.OnVeterinariaClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Coordenadas iniciales (Lima, Perú - Miraflores)
    private static final LatLng LIMA_CENTER = new LatLng(-12.1192, -77.0310);

    // Coordenadas canónicas de distritos populares para navegación directa
    private static final Map<String, LatLng> DISTRITO_COORDINATES = new HashMap<>();
    static {
        DISTRITO_COORDINATES.put("miraflores", new LatLng(-12.1192, -77.0310));
        DISTRITO_COORDINATES.put("san isidro", new LatLng(-12.0970, -77.0350));
        DISTRITO_COORDINATES.put("surco", new LatLng(-12.1389, -76.9950));
        DISTRITO_COORDINATES.put("san miguel", new LatLng(-12.0768, -77.0934));
        DISTRITO_COORDINATES.put("los olivos", new LatLng(-11.9922, -77.0700));
    }

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private RecyclerView rvVeterinarias;
    private VeterinariaAdapter adapter;
    private MaterialCardView cardMapContainer;

    private EditText etBuscar;
    private ImageView btnClearSearch;
    private TextView tvContadorClinicas;
    private LinearLayout layoutEmptyState;

    // Botón flotante "Buscar en esta zona"
    private View btnBuscarEstaZona;
    private ProgressBar pbBuscarZona;
    private ImageView ivIconBuscarZona;
    private TextView tvBuscarZonaLabel;

    // Chips tipo
    private TextView chipTipoTodas;
    private TextView chipTipoEspecializada;
    private TextView chipTipoExterna;

    // Chips distrito
    private LinearLayout chipDistritoTodos;
    private TextView tvChipDistritoTodos;
    private ImageView ivIconPinTodos;
    private TextView chipDistritoMiraflores;
    private TextView chipDistritoSanIsidro;
    private TextView chipDistritoSurco;
    private TextView chipDistritoSanMiguel;
    private TextView chipDistritoLosOlivos;

    // Toggle Lista / Mapa
    private LinearLayout btnModoLista;
    private LinearLayout btnModoMapa;
    private ImageView ivIconLista;
    private TextView tvLabelLista;
    private ImageView ivIconMapa;
    private TextView tvLabelMapa;

    // Datos
    private final List<Veterinaria> listaCompleta = new ArrayList<>();
    private final List<Veterinaria> listaFiltrada = new ArrayList<>();
    private final Map<Marker, Veterinaria> markerMap = new HashMap<>();

    // Estados de ubicación y filtros
    private LatLng lastSearchedLatLng = LIMA_CENTER;
    private Location ubicacionActualUsuario;
    private String filtroTexto = "";
    private String filtroTipo = "todas"; // "todas", "especializada", "externa"
    private String filtroDistrito = "todos"; // "todos", "miraflores", "san isidro", etc.
    private boolean filtroSoloAbiertas = false;
    private boolean modoSoloMapa = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_localizacion_veterinaria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        inicializarVistas();
        cargarDatos();
        configurarBuscador();
        configurarChipsTipo();
        configurarChipsDistrito();
        configurarSegmentedToggle();
        configurarMapa();
        configurarBotonesAccion();
        configurarNavegacionInferior();

        aplicarFiltros(false);
    }

    private void inicializarVistas() {
        cardMapContainer = findViewById(R.id.cardMapContainer);
        rvVeterinarias = findViewById(R.id.rvVeterinarias);
        etBuscar = findViewById(R.id.etBuscar);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvContadorClinicas = findViewById(R.id.tvContadorClinicas);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Botón flotante "Buscar en esta zona"
        btnBuscarEstaZona = findViewById(R.id.btnBuscarEstaZona);
        pbBuscarZona = findViewById(R.id.pbBuscarZona);
        ivIconBuscarZona = findViewById(R.id.ivIconBuscarZona);
        tvBuscarZonaLabel = findViewById(R.id.tvBuscarZonaLabel);

        if (btnBuscarEstaZona != null) {
            btnBuscarEstaZona.setOnClickListener(v -> {
                if (mMap == null) return;
                LatLng center = mMap.getCameraPosition().target;
                if (pbBuscarZona != null) pbBuscarZona.setVisibility(View.VISIBLE);
                if (ivIconBuscarZona != null) ivIconBuscarZona.setVisibility(View.GONE);
                if (tvBuscarZonaLabel != null) tvBuscarZonaLabel.setText("Buscando...");

                // Al explorar y buscar manualmente en una nueva zona del mapa, restablecer filtro a "todos los distritos"
                filtroDistrito = "todos";
                actualizarEstiloChipsDistrito();

                consultarGooglePlaces(center.latitude, center.longitude, null, false);
            });
        }

        // Chips tipo
        chipTipoTodas = findViewById(R.id.chipTipoTodas);
        chipTipoEspecializada = findViewById(R.id.chipTipoEspecializada);
        chipTipoExterna = findViewById(R.id.chipTipoExterna);

        // Chips distrito
        chipDistritoTodos = findViewById(R.id.chipDistritoTodos);
        tvChipDistritoTodos = findViewById(R.id.tvChipDistritoTodos);
        ivIconPinTodos = findViewById(R.id.ivIconPinTodos);
        chipDistritoMiraflores = findViewById(R.id.chipDistritoMiraflores);
        chipDistritoSanIsidro = findViewById(R.id.chipDistritoSanIsidro);
        chipDistritoSurco = findViewById(R.id.chipDistritoSurco);
        chipDistritoSanMiguel = findViewById(R.id.chipDistritoSanMiguel);
        chipDistritoLosOlivos = findViewById(R.id.chipDistritoLosOlivos);

        // Toggle Lista / Mapa
        btnModoLista = findViewById(R.id.btnModoLista);
        btnModoMapa = findViewById(R.id.btnModoMapa);
        ivIconLista = findViewById(R.id.ivIconLista);
        tvLabelLista = findViewById(R.id.tvLabelLista);
        ivIconMapa = findViewById(R.id.ivIconMapa);
        tvLabelMapa = findViewById(R.id.tvLabelMapa);

        // Configurar RecyclerView
        rvVeterinarias.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VeterinariaAdapter(this, listaFiltrada, this);
        rvVeterinarias.setAdapter(adapter);

        // Conectar el contenedor táctil del mapa con el ScrollView para evitar que la pantalla se mueva
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollView);
        TouchableFrameLayout touchableMapContainer = findViewById(R.id.touchableMapContainer);
        if (touchableMapContainer != null && scrollView != null) {
            touchableMapContainer.setNestedScrollView(scrollView);
        }
    }

    private void cargarDatos() {
        listaCompleta.clear();
        listaCompleta.addAll(VeterinariaData.getClinicas(this));
    }

    /**
     * Consulta exhaustiva en Google Places (New) según las coordenadas y distrito indicados.
     */
    private void consultarGooglePlaces(double lat, double lng, String distrito, boolean ajustarCamara) {
        lastSearchedLatLng = new LatLng(lat, lng);

        double radiusMeters = 4500.0;
        if (mMap != null) {
            float zoom = mMap.getCameraPosition().zoom;
            if (zoom >= 15.5f) radiusMeters = 2500.0;
            else if (zoom >= 13.5f) radiusMeters = 4500.0;
            else if (zoom >= 11.5f) radiusMeters = 8000.0;
            else radiusMeters = 14000.0;
        }

        GooglePlacesService.buscarVeterinariasEnZona(this, lat, lng, radiusMeters, distrito, new GooglePlacesService.PlacesCallback() {
            @Override
            public void onPlacesLoaded(List<Veterinaria> clinicas) {
                if (btnBuscarEstaZona != null) {
                    btnBuscarEstaZona.setVisibility(View.GONE);
                    if (pbBuscarZona != null) pbBuscarZona.setVisibility(View.GONE);
                    if (ivIconBuscarZona != null) ivIconBuscarZona.setVisibility(View.VISIBLE);
                    if (tvBuscarZonaLabel != null) tvBuscarZonaLabel.setText("Buscar en esta zona");
                }

                if (clinicas != null && !clinicas.isEmpty()) {
                    // Preservar favoritos seleccionados por el usuario
                    for (Veterinaria vet : clinicas) {
                        boolean fav = VeterinariaData.isFavorita(activity_localizacionVeterinaria.this, vet.getId());
                        vet.setEsFavorita(fav);
                    }

                    // Recalcular distancia con respecto al GPS real del usuario si está activo
                    if (ubicacionActualUsuario != null) {
                        for (Veterinaria v : clinicas) {
                            float[] dist = new float[1];
                            Location.distanceBetween(ubicacionActualUsuario.getLatitude(), ubicacionActualUsuario.getLongitude(),
                                    v.getLatitud(), v.getLongitud(), dist);
                            double km = Math.round((dist[0] / 1000.0) * 10.0) / 10.0;
                            v.setDistanciaKm(km);
                        }
                    }

                    listaCompleta.clear();
                    listaCompleta.addAll(clinicas);
                    aplicarFiltros(ajustarCamara);

                    Toast.makeText(activity_localizacionVeterinaria.this,
                            "Google Maps: " + clinicas.size() + " veterinarias cargadas",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String mensajeError) {
                if (btnBuscarEstaZona != null) {
                    btnBuscarEstaZona.setVisibility(View.GONE);
                }
                android.util.Log.d("LocalizacionVet", "Nota: " + mensajeError);
            }
        });
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtroTexto = s.toString().trim();
                btnClearSearch.setVisibility(filtroTexto.isEmpty() ? View.GONE : View.VISIBLE);
                aplicarFiltros(false);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etBuscar.setText("");
            filtroTexto = "";
            aplicarFiltros(false);
        });

        findViewById(R.id.btnFilter).setOnClickListener(v -> mostrarDialogoFiltros());
    }

    private void configurarChipsTipo() {
        chipTipoTodas.setOnClickListener(v -> {
            filtroTipo = "todas";
            actualizarEstiloChipsTipo();
            aplicarFiltros(false);
        });

        chipTipoEspecializada.setOnClickListener(v -> {
            filtroTipo = "especializada";
            actualizarEstiloChipsTipo();
            aplicarFiltros(false);
        });

        chipTipoExterna.setOnClickListener(v -> {
            filtroTipo = "externa";
            actualizarEstiloChipsTipo();
            aplicarFiltros(false);
        });
    }

    private void actualizarEstiloChipsTipo() {
        chipTipoTodas.setBackgroundResource(filtroTipo.equals("todas") ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chipTipoTodas.setTextColor(filtroTipo.equals("todas") ? Color.WHITE : Color.parseColor("#374151"));

        chipTipoEspecializada.setBackgroundResource(filtroTipo.equals("especializada") ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chipTipoEspecializada.setTextColor(filtroTipo.equals("especializada") ? Color.WHITE : Color.parseColor("#374151"));

        chipTipoExterna.setBackgroundResource(filtroTipo.equals("externa") ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chipTipoExterna.setTextColor(filtroTipo.equals("externa") ? Color.WHITE : Color.parseColor("#374151"));
    }

    private void configurarChipsDistrito() {
        chipDistritoTodos.setOnClickListener(v -> seleccionarDistrito("todos"));
        chipDistritoMiraflores.setOnClickListener(v -> seleccionarDistrito("miraflores"));
        chipDistritoSanIsidro.setOnClickListener(v -> seleccionarDistrito("san isidro"));
        chipDistritoSurco.setOnClickListener(v -> seleccionarDistrito("surco"));
        chipDistritoSanMiguel.setOnClickListener(v -> seleccionarDistrito("san miguel"));
        chipDistritoLosOlivos.setOnClickListener(v -> seleccionarDistrito("los olivos"));
    }

    private void seleccionarDistrito(String distrito) {
        filtroDistrito = distrito;
        actualizarEstiloChipsDistrito();

        if (btnBuscarEstaZona != null) {
            btnBuscarEstaZona.setVisibility(View.GONE);
        }

        if (distrito.equals("todos")) {
            LatLng destino = ubicacionActualUsuario != null ?
                    new LatLng(ubicacionActualUsuario.getLatitude(), ubicacionActualUsuario.getLongitude()) : LIMA_CENTER;
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destino, 13.8f));
            }
            consultarGooglePlaces(destino.latitude, destino.longitude, null, false);
        } else {
            LatLng coords = DISTRITO_COORDINATES.get(distrito.toLowerCase());
            if (coords != null) {
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 14.5f));
                }
                consultarGooglePlaces(coords.latitude, coords.longitude, distrito, false);
            } else {
                aplicarFiltros(false);
            }
        }
    }

    private void actualizarEstiloChipsDistrito() {
        boolean esTodos = filtroDistrito.equals("todos");
        chipDistritoTodos.setBackgroundResource(esTodos ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        tvChipDistritoTodos.setTextColor(esTodos ? Color.WHITE : Color.parseColor("#374151"));
        ivIconPinTodos.setColorFilter(esTodos ? Color.WHITE : Color.parseColor("#007A6E"));

        actualizarChipIndividual(chipDistritoMiraflores, filtroDistrito.equals("miraflores"));
        actualizarChipIndividual(chipDistritoSanIsidro, filtroDistrito.equals("san isidro"));
        actualizarChipIndividual(chipDistritoSurco, filtroDistrito.equals("surco"));
        actualizarChipIndividual(chipDistritoSanMiguel, filtroDistrito.equals("san miguel"));
        actualizarChipIndividual(chipDistritoLosOlivos, filtroDistrito.equals("los olivos"));
    }

    private void actualizarChipIndividual(TextView chip, boolean seleccionado) {
        chip.setBackgroundResource(seleccionado ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(seleccionado ? Color.WHITE : Color.parseColor("#374151"));
    }

    private void configurarSegmentedToggle() {
        btnModoLista.setOnClickListener(v -> setModoVisualizacion(false));
        btnModoMapa.setOnClickListener(v -> setModoVisualizacion(true));
    }

    private void setModoVisualizacion(boolean soloMapa) {
        modoSoloMapa = soloMapa;

        if (soloMapa) {
            // Activar botón Mapa
            btnModoMapa.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            ivIconMapa.setColorFilter(Color.WHITE);
            tvLabelMapa.setTextColor(Color.WHITE);

            btnModoLista.setBackgroundResource(android.R.color.transparent);
            ivIconLista.setColorFilter(Color.parseColor("#6B7280"));
            tvLabelLista.setTextColor(Color.parseColor("#6B7280"));

            // En modo Mapa: ocultar la lista larga y banner para explorar el mapa cómodamente
            rvVeterinarias.setVisibility(View.GONE);
            if (findViewById(R.id.cardTriage) != null) {
                findViewById(R.id.cardTriage).setVisibility(View.GONE);
            }

            // Expandir contenedor del mapa
            ViewGroup.LayoutParams params = cardMapContainer.getLayoutParams();
            params.height = (int) (getResources().getDisplayMetrics().density * 520);
            cardMapContainer.setLayoutParams(params);
        } else {
            // Activar botón Lista
            btnModoLista.setBackgroundResource(R.drawable.bg_btn_segmented_selected);
            ivIconLista.setColorFilter(Color.WHITE);
            tvLabelLista.setTextColor(Color.WHITE);

            btnModoMapa.setBackgroundResource(android.R.color.transparent);
            ivIconMapa.setColorFilter(Color.parseColor("#6B7280"));
            tvLabelMapa.setTextColor(Color.parseColor("#6B7280"));

            // En modo Lista: restaurar la lista y banner
            rvVeterinarias.setVisibility(listaFiltrada.isEmpty() ? View.GONE : View.VISIBLE);
            if (findViewById(R.id.cardTriage) != null) {
                findViewById(R.id.cardTriage).setVisibility(View.VISIBLE);
            }

            // Restaurar altura estándar del mapa
            ViewGroup.LayoutParams params = cardMapContainer.getLayoutParams();
            params.height = (int) (getResources().getDisplayMetrics().density * 215);
            cardMapContainer.setLayoutParams(params);
        }
    }

    private void configurarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.layoutGpsActivo).setOnClickListener(v -> centrarEnMiUbicacion());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(marker -> {
            Veterinaria vet = markerMap.get(marker);
            if (vet != null) {
                onVeterinariaClick(vet);
            }
        });

        // Controlar el clic del botón de ubicación de Google Maps
        mMap.setOnMyLocationButtonClickListener(() -> {
            centrarEnMiUbicacion();
            return true;
        });

        // Detector de desplazamiento del mapa para mostrar el botón "Buscar en esta zona"
        mMap.setOnCameraIdleListener(() -> {
            if (mMap == null) return;
            LatLng cameraCenter = mMap.getCameraPosition().target;
            if (lastSearchedLatLng != null) {
                float[] dist = new float[1];
                Location.distanceBetween(lastSearchedLatLng.latitude, lastSearchedLatLng.longitude,
                        cameraCenter.latitude, cameraCenter.longitude, dist);
                if (dist[0] > 1100) { // Si el mapa se desplazó más de 1.1 km de la última búsqueda
                    if (btnBuscarEstaZona != null) {
                        btnBuscarEstaZona.setVisibility(View.VISIBLE);
                        if (pbBuscarZona != null) pbBuscarZona.setVisibility(View.GONE);
                        if (ivIconBuscarZona != null) ivIconBuscarZona.setVisibility(View.VISIBLE);
                        if (tvBuscarZonaLabel != null) tvBuscarZonaLabel.setText("Buscar en esta zona");
                    }
                } else {
                    if (btnBuscarEstaZona != null) {
                        btnBuscarEstaZona.setVisibility(View.GONE);
                    }
                }
            }
        });

        solicitarPermisoUbicacion();
        actualizarMarcadoresEnMapa(false);

        // Mover cámara a Lima por defecto
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LIMA_CENTER, 13.5f));
    }

    private void centrarEnMiUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisoUbicacion();
            return;
        }

        Toast.makeText(this, "Buscando tu ubicación exacta vía GPS...", Toast.LENGTH_SHORT).show();

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        procesarUbicacionExacta(location);
                    } else {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, this::procesarUbicacionExacta);
                    }
                })
                .addOnFailureListener(this, e -> {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, this::procesarUbicacionExacta);
                });
    }

    private void procesarUbicacionExacta(Location location) {
        if (location == null || mMap == null) {
            Toast.makeText(this, "No se pudo obtener la señal GPS. Verifica que la ubicación esté activada.", Toast.LENGTH_SHORT).show();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LIMA_CENTER, 14f));
            return;
        }

        ubicacionActualUsuario = location;
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());

        boolean esEmuladorUSA = Math.abs(location.getLatitude() - 37.42) < 0.8
                && Math.abs(location.getLongitude() - (-122.08)) < 0.8;

        if (esEmuladorUSA) {
            Toast.makeText(this, "Aviso: Tu emulador tiene GPS por defecto en California (USA).", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "¡Ubicación exacta encontrada!", Toast.LENGTH_SHORT).show();
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15.5f));

        // Recalcular distancias dinámicamente desde la ubicación exacta del usuario
        for (Veterinaria vet : listaCompleta) {
            float[] dist = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                    vet.getLatitud(), vet.getLongitud(), dist);
            double km = Math.round((dist[0] / 1000.0) * 10.0) / 10.0;
            vet.setDistanciaKm(km);
        }

        // Obtener nombre del distrito / zona con Geocoder
        try {
            Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String zona = addr.getSubLocality();
                if (zona == null || zona.isEmpty()) zona = addr.getLocality();
                if (zona != null && !zona.isEmpty()) {
                    TextView tvBadge = findViewById(R.id.tvGpsActivoBadge);
                    if (tvBadge != null) {
                        tvBadge.setText("GPS: " + zona);
                    }
                }
            }
        } catch (Exception ignored) {}

        adapter.notifyDataSetChanged();

        // Consultar veterinarias en vivo alrededor de la ubicación exacta
        consultarGooglePlaces(location.getLatitude(), location.getLongitude(), null, false);
    }

    private void solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    centrarEnMiUbicacion();
                }
            } catch (SecurityException ignored) {}
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (mMap != null) {
                        mMap.setMyLocationEnabled(true);
                        centrarEnMiUbicacion();
                    }
                } catch (SecurityException ignored) {}
            }
        }
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String norm = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        return norm.replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private void aplicarFiltros(boolean ajustarCamara) {
        listaFiltrada.clear();
        String query = normalizar(filtroTexto);

        for (Veterinaria vet : listaCompleta) {
            // Filtro por texto
            boolean coincideTexto = query.isEmpty()
                    || normalizar(vet.getNombre()).contains(query)
                    || normalizar(vet.getEspecialidad()).contains(query)
                    || normalizar(vet.getDireccion()).contains(query)
                    || normalizar(vet.getDistrito()).contains(query);

            // Filtro por tipo de clínica (especializada o externa)
            boolean coincideTipo = true;
            if (filtroTipo.equals("especializada")) {
                coincideTipo = vet.isEspecializada();
            } else if (filtroTipo.equals("externa")) {
                coincideTipo = vet.isExterna();
            }

            // Filtro por distrito
            boolean coincideDistrito = true;
            if (!filtroDistrito.equals("todos")) {
                coincideDistrito = normalizar(vet.getDistrito()).contains(normalizar(filtroDistrito))
                        || normalizar(vet.getDireccion()).contains(normalizar(filtroDistrito));
            }

            // Filtro solo abiertas
            boolean coincideHorario = !filtroSoloAbiertas || vet.isAbierto();

            if (coincideTexto && coincideTipo && coincideDistrito && coincideHorario) {
                listaFiltrada.add(vet);
            }
        }

        adapter.actualizarLista(listaFiltrada);
        int total = listaFiltrada.size();
        tvContadorClinicas.setText(total + " centros médicos " + (filtroDistrito.equals("todos") ? "cercanos" : "en " + filtroDistrito));

        if (total == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvVeterinarias.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvVeterinarias.setVisibility(modoSoloMapa ? View.GONE : View.VISIBLE);
        }

        actualizarMarcadoresEnMapa(ajustarCamara);
    }

    private void actualizarMarcadoresEnMapa(boolean ajustarCamara) {
        if (mMap == null) return;

        mMap.clear();
        markerMap.clear();

        if (listaFiltrada.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Veterinaria vet : listaFiltrada) {
            LatLng posicion = new LatLng(vet.getLatitud(), vet.getLongitud());
            builder.include(posicion);

            BitmapDescriptor iconPaw = crearIconoPatita(vet);

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(posicion)
                    .title(vet.getNombre())
                    .snippet(vet.getEspecialidad() + " • " + vet.getHorarioTexto())
                    .icon(iconPaw));

            if (marker != null) {
                markerMap.put(marker, vet);
            }
        }

        // Animar cámara para incluir los marcadores SOLO si se pide explícitamente
        if (ajustarCamara) {
            try {
                if (listaFiltrada.size() == 1) {
                    Veterinaria unica = listaFiltrada.get(0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(unica.getLatitud(), unica.getLongitud()), 15f));
                } else {
                    LatLngBounds bounds = builder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
                }
            } catch (Exception ignored) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LIMA_CENTER, 13f));
            }
        }
    }

    /**
     * Genera un marcador visual de Google Maps con la patita de mascota y el nombre/estado de la veterinaria
     */
    private BitmapDescriptor crearIconoPatita(Veterinaria vet) {
        View markerView = LayoutInflater.from(this).inflate(R.layout.view_paw_marker, null);

        TextView tvLabel = markerView.findViewById(R.id.tvMarkerLabel);
        FrameLayout markerCircle = markerView.findViewById(R.id.markerCirclePaw);

        String textoEtiqueta = vet.getNombre().replace("Veterinaria ", "") + " • " + (vet.isAbierto() ? "Abierto" : vet.getPrecioConsulta());
        tvLabel.setText(textoEtiqueta);

        if (vet.isEspecializada()) {
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
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Veterinaria vet = markerMap.get(marker);
        if (vet != null) {
            marker.showInfoWindow();
            int index = listaFiltrada.indexOf(vet);
            if (index >= 0) {
                rvVeterinarias.smoothScrollToPosition(index);
            }
            Toast.makeText(this, vet.getNombre() + " (" + vet.getTipo() + ")", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onVeterinariaClick(Veterinaria vet) {
        if (vet != null) {
            Intent intent = new Intent(this, activity_contactoVeterinaria.class);
            intent.putExtra("veterinaria", vet);
            startActivity(intent);
        }
    }

    @Override
    public void onFavoritoClick(Veterinaria vet, int position) {
        boolean nuevoEstado = !vet.isEsFavorita();
        vet.setEsFavorita(nuevoEstado);
        VeterinariaData.setFavorita(this, vet.getId(), nuevoEstado);
        adapter.notifyItemChanged(position);

        String msg = nuevoEstado ? "Añadida a tus veterinarias favoritas" : "Eliminada de favoritos";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLlamarClick(Veterinaria vet) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + vet.getTelefono()));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el marcador telefónico", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNavegarClick(Veterinaria vet) {
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + vet.getLatitud() + "," + vet.getLongitud()
                    + "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Uri genericUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                        + vet.getLatitud() + "," + vet.getLongitud());
                startActivity(new Intent(Intent.ACTION_VIEW, genericUri));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Abriendo coordenadas en Maps...", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoFiltros() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filtro_veterinarias, null);
        dialog.setContentView(dialogView);

        RadioGroup rgTipo = dialogView.findViewById(R.id.rgTipoClinica);
        RadioButton rbTodas = dialogView.findViewById(R.id.rbTipoTodas);
        RadioButton rbEspecializada = dialogView.findViewById(R.id.rbTipoEspecializada);
        RadioButton rbExterna = dialogView.findViewById(R.id.rbTipoExterna);
        Spinner spinnerDistrito = dialogView.findViewById(R.id.spinnerDistrito);
        SwitchMaterial switchSoloAbiertas = dialogView.findViewById(R.id.switchSoloAbiertas);
        Button btnRestablecer = dialogView.findViewById(R.id.btnRestablecerFiltros);
        Button btnAplicar = dialogView.findViewById(R.id.btnAplicarFiltros);

        if (filtroTipo.equals("especializada")) {
            rbEspecializada.setChecked(true);
        } else if (filtroTipo.equals("externa")) {
            rbExterna.setChecked(true);
        } else {
            rbTodas.setChecked(true);
        }

        String[] distritos = new String[]{"Todos los distritos", "Miraflores", "San Isidro", "Surco", "San Miguel", "Los Olivos"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, distritos);
        spinnerDistrito.setAdapter(spinnerAdapter);

        for (int i = 0; i < distritos.length; i++) {
            if (distritos[i].equalsIgnoreCase(filtroDistrito) ||
                    (i == 0 && filtroDistrito.equals("todos"))) {
                spinnerDistrito.setSelection(i);
                break;
            }
        }

        switchSoloAbiertas.setChecked(filtroSoloAbiertas);

        btnRestablecer.setOnClickListener(v -> {
            etBuscar.setText("");
            filtroTexto = "";
            rbTodas.setChecked(true);
            spinnerDistrito.setSelection(0);
            switchSoloAbiertas.setChecked(false);
            filtroTipo = "todas";
            filtroDistrito = "todos";
            filtroSoloAbiertas = false;
            actualizarEstiloChipsTipo();
            actualizarEstiloChipsDistrito();
            aplicarFiltros(false);
            dialog.dismiss();
        });

        btnAplicar.setOnClickListener(v -> {
            int selectedId = rgTipo.getCheckedRadioButtonId();
            if (selectedId == R.id.rbTipoEspecializada) {
                filtroTipo = "especializada";
            } else if (selectedId == R.id.rbTipoExterna) {
                filtroTipo = "externa";
            } else {
                filtroTipo = "todas";
            }

            int distPos = spinnerDistrito.getSelectedItemPosition();
            if (distPos == 0) {
                filtroDistrito = "todos";
            } else {
                filtroDistrito = distritos[distPos].toLowerCase();
            }

            filtroSoloAbiertas = switchSoloAbiertas.isChecked();

            actualizarEstiloChipsTipo();
            actualizarEstiloChipsDistrito();

            if (!filtroDistrito.equals("todos")) {
                seleccionarDistrito(filtroDistrito);
            } else {
                aplicarFiltros(false);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void configurarBotonesAccion() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnProfileAvatar).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));

        findViewById(R.id.btnContactarTriage).setOnClickListener(v -> {
            try {
                Intent triageCall = new Intent(Intent.ACTION_DIAL);
                triageCall.setData(Uri.parse("tel:+5112418900"));
                startActivity(triageCall);
            } catch (Exception e) {
                Toast.makeText(this, "Llamando al triage de emergencias...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarNavegacionInferior() {
        findViewById(R.id.btnNavInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones veterinarias activas", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnNavPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));
    }
}
