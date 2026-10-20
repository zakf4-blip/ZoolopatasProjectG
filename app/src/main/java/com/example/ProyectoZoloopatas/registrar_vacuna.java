package com.example.ProyectoZoloopatas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class registrar_vacuna extends AppCompatActivity {

    private static final int MAX_COMENTARIOS = 300;

    private Mascota mascota;
    private String uid;

    private ImageView imgFotoMascota;
    private TextView tvNombreMascota, tvInfoMascota;
    private Spinner spinnerTipoRegistro;
    private TextView tvFechaAplicacion, tvFechaVencimiento, tvTipoVacuna;
    private TextView tvValidacionFecha, tvValidacionVencimiento, tvContadorCaracteres;
    private LinearLayout layoutValidacionFecha, layoutValidacionVencimiento;
    private EditText etComentarios;
    private View btnGuardarVacunacion;

    private Calendar fechaAplicacion = hoyCero();
    private Calendar fechaVencimiento;
    private boolean vencimientoManual = false;
    private String tipoVacunaSeleccionada = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrar_vacuna);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        uid = obtenerUidActual();

        // 1. Capturar la mascota seleccionada
        String petId = getIntent().getStringExtra(editar_infoMascota.EXTRA_MASCOTA_ID);
        mascota = MascotasLocalManager.getMascotaById(this, uid, petId);
        if (mascota == null) {
            Toast.makeText(this, "No se encontró la mascota seleccionada.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        enlazarVistas();
        mostrarInfoMascota();
        configurarFormulario();
    }

    private String obtenerUidActual() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : "usuario_default";
    }

    private void enlazarVistas() {
        imgFotoMascota = findViewById(R.id.imgFotoMascota);
        tvNombreMascota = findViewById(R.id.tvNombreMascota);
        tvInfoMascota = findViewById(R.id.tvInfoMascota);
        spinnerTipoRegistro = findViewById(R.id.spinnerTipoRegistro);
        tvFechaAplicacion = findViewById(R.id.tvFechaAplicacion);
        tvFechaVencimiento = findViewById(R.id.tvFechaVencimiento);
        tvTipoVacuna = findViewById(R.id.tvTipoVacuna);
        layoutValidacionFecha = findViewById(R.id.layoutValidacionFecha);
        tvValidacionFecha = findViewById(R.id.tvValidacionFecha);
        layoutValidacionVencimiento = findViewById(R.id.layoutValidacionVencimiento);
        tvValidacionVencimiento = findViewById(R.id.tvValidacionVencimiento);
        etComentarios = findViewById(R.id.etComentarios);
        tvContadorCaracteres = findViewById(R.id.tvContadorCaracteres);
        btnGuardarVacunacion = findViewById(R.id.btnGuardarVacunacion);

        findViewById(R.id.btnBackVacunacion).setOnClickListener(v -> finish());
        findViewById(R.id.btnProfileAvatar).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));
    }

    // ───────────────────────── Información de la mascota ─────────────────────────

    private boolean esGato() {
        String t = mascota.getTipo();
        return t.equalsIgnoreCase("Gato") || t.equalsIgnoreCase("Felino");
    }

    private void mostrarInfoMascota() {
        String nombre = mascota.getNombre().isEmpty() ? "Mascota" : mascota.getNombre();
        tvNombreMascota.setText(nombre);
        imgFotoMascota.setContentDescription("Foto de " + nombre);
        imgFotoMascota.setClipToOutline(true); // recorta la foto al círculo del fondo

        List<String> partes = new ArrayList<>();
        partes.add(mascota.getRaza().isEmpty() ? "Mestizo" : mascota.getRaza());
        String edad = calcularEdad(mascota.getFechaNacimiento());
        if (!edad.isEmpty()) partes.add(edad);
        partes.add(mascota.getSexo());
        tvInfoMascota.setText(android.text.TextUtils.join(" • ", partes));

        boolean fotoCargada = false;
        String fotoUri = mascota.getFotoUri();
        if (fotoUri != null && !fotoUri.trim().isEmpty()) {
            Bitmap bmp = editar_infoMascota.decodeSampledBitmapFromFile(fotoUri, 200, 200);
            if (bmp != null) {
                imgFotoMascota.setImageBitmap(bmp);
                fotoCargada = true;
            }
        }
        if (!fotoCargada) {
            if ("Rocky".equalsIgnoreCase(nombre)) {
                imgFotoMascota.setImageResource(R.drawable.img_pet_rocky);
            } else if ("Luna".equalsIgnoreCase(nombre)) {
                imgFotoMascota.setImageResource(R.drawable.img_pet_luna);
            } else if (esGato()) {
                imgFotoMascota.setImageResource(R.drawable.img_cat_icon_completo);
            } else {
                imgFotoMascota.setImageResource(R.drawable.img_dog_icon_completo);
            }
        }
    }

    private String calcularEdad(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return "";
        try {
            String[] p = fecha.split("/");
            if (p.length != 3) return "";
            Calendar nac = Calendar.getInstance();
            nac.set(Integer.parseInt(p[2].trim()), Integer.parseInt(p[1].trim()) - 1,
                    Integer.parseInt(p[0].trim()));
            Calendar hoy = Calendar.getInstance();
            int anos = hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR);
            if (hoy.get(Calendar.DAY_OF_YEAR) < nac.get(Calendar.DAY_OF_YEAR)) anos--;
            if (anos >= 2) return anos + " años";
            if (anos == 1) return "1 año";
            int meses = (hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR)) * 12
                    + (hoy.get(Calendar.MONTH) - nac.get(Calendar.MONTH));
            if (hoy.get(Calendar.DAY_OF_MONTH) < nac.get(Calendar.DAY_OF_MONTH)) meses--;
            return meses <= 1 ? "1 mes" : meses + " meses";
        } catch (Exception e) {
            return "";
        }
    }

    // ───────────────────────── Formulario ─────────────────────────

    private void configurarFormulario() {
        // Valores iniciales (reemplazan los textos de ejemplo del XML)
        fechaVencimiento = (Calendar) fechaAplicacion.clone();
        fechaVencimiento.add(Calendar.YEAR, 1);
        actualizarFechas();

        tvTipoVacuna.setText("Selecciona el tipo de vacuna");
        tvTipoVacuna.setTextColor(Color.parseColor("#9CA3AF"));

        etComentarios.setText("");
        etComentarios.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_COMENTARIOS)});
        tvContadorCaracteres.setText("0 / " + MAX_COMENTARIOS);
        etComentarios.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                tvContadorCaracteres.setText(s.length() + " / " + MAX_COMENTARIOS);
            }
        });

        findViewById(R.id.layoutFechaAplicacion).setOnClickListener(v -> abrirSelectorAplicacion());
        findViewById(R.id.layoutFechaVencimiento).setOnClickListener(v -> abrirSelectorVencimiento());
        findViewById(R.id.layoutTipoVacuna).setOnClickListener(v -> mostrarDialogoTipoVacuna());
        btnGuardarVacunacion.setOnClickListener(v -> guardarVacunacion());
    }

    private void abrirSelectorAplicacion() {
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            fechaAplicacion.set(y, m, d);
            // Si el usuario no eligió vencimiento, se propone +1 año (esquema anual)
            if (!vencimientoManual) {
                fechaVencimiento = (Calendar) fechaAplicacion.clone();
                fechaVencimiento.add(Calendar.YEAR, 1);
            }
            actualizarFechas();
        }, fechaAplicacion.get(Calendar.YEAR), fechaAplicacion.get(Calendar.MONTH),
                fechaAplicacion.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMaxDate(System.currentTimeMillis()); // no se aplican vacunas en el futuro
        dlg.show();
    }

    private void abrirSelectorVencimiento() {
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            fechaVencimiento.set(y, m, d);
            vencimientoManual = true;
            actualizarFechas();
        }, fechaVencimiento.get(Calendar.YEAR), fechaVencimiento.get(Calendar.MONTH),
                fechaVencimiento.get(Calendar.DAY_OF_MONTH));
        Calendar min = (Calendar) fechaAplicacion.clone();
        min.add(Calendar.DAY_OF_MONTH, 1);
        dlg.getDatePicker().setMinDate(min.getTimeInMillis());
        dlg.show();
    }

    private void mostrarDialogoTipoVacuna() {
        final String[] opciones = opcionesVacuna();
        new AlertDialog.Builder(this)
                .setTitle("Tipo de vacuna")
                .setItems(opciones, (dialog, which) -> {
                    if (which == opciones.length - 1) {
                        pedirVacunaPersonalizada();
                    } else {
                        asignarTipoVacuna(opciones[which]);
                    }
                })
                .show();
    }

    private String[] opcionesVacuna() {
        if (esGato()) {
            return new String[]{
                    "Vacuna Antirrábica Anual",
                    "Triple Felina (panleucopenia, calicivirus, rinotraqueítis)",
                    "Cuádruple Felina",
                    "Leucemia Felina (FeLV)",
                    "Otra…"};
        }
        return new String[]{
                "Vacuna Antirrábica Anual",
                "Parvovirus",
                "Moquillo (Distemper)",
                "Séxtuple (DHPPi + L)",
                "Leptospirosis",
                "Tos de las perreras (Bordetella)",
                "Otra…"};
    }

    private void pedirVacunaPersonalizada() {
        final EditText input = new EditText(this);
        input.setHint("Nombre de la vacuna");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Otra vacuna")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar", (d, w) -> {
                    String txt = input.getText().toString().trim();
                    if (!txt.isEmpty()) asignarTipoVacuna(txt);
                })
                .show();
    }

    private void asignarTipoVacuna(String tipo) {
        tipoVacunaSeleccionada = tipo;
        tvTipoVacuna.setText(tipo);
        tvTipoVacuna.setTextColor(Color.parseColor("#1F2428"));
    }

    // ───────────────────────── Fechas y validación ─────────────────────────

    private static Calendar hoyCero() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static Calendar diaCero(Calendar src) {
        Calendar c = (Calendar) src.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private String formatoPantalla(Calendar c) {
        return String.format(Locale.US, "%02d / %02d / %04d",
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    private String formatoGuardar(Calendar c) {
        return String.format(Locale.US, "%02d/%02d/%04d",
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    private void actualizarFechas() {
        tvFechaAplicacion.setText(formatoPantalla(fechaAplicacion));
        tvFechaVencimiento.setText(formatoPantalla(fechaVencimiento));
        validarFechas();
    }

    private boolean validarFechas() {
        Calendar apl = diaCero(fechaAplicacion);
        Calendar ven = diaCero(fechaVencimiento);
        Calendar hoy = hoyCero();

        boolean aplOk = !apl.after(hoy);
        mostrarValidacion(layoutValidacionFecha, tvValidacionFecha, aplOk,
                aplOk ? "Fecha validada correctamente" : "La fecha de aplicación no puede ser futura");

        boolean venOk = ven.after(apl);
        mostrarValidacion(layoutValidacionVencimiento, tvValidacionVencimiento, venOk,
                venOk ? "Fecha posterior y válida (intervalo seguro)"
                        : "Debe ser posterior a la fecha de aplicación");

        return aplOk && venOk;
    }

    private void mostrarValidacion(LinearLayout contenedor, TextView tv, boolean ok, String mensaje) {
        int color = Color.parseColor(ok ? "#059669" : "#DC2626");
        tv.setText(mensaje);
        tv.setTextColor(color);
        View icono = contenedor.getChildAt(0);
        if (icono instanceof ImageView) {
            ImageView iv = (ImageView) icono;
            iv.setImageResource(ok ? R.drawable.ic_check_circle : R.drawable.ic_warning_circle);
            ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(color));
        }
    }

    // ───────────────────────── Guardado ─────────────────────────

    private void guardarVacunacion() {
        if (tipoVacunaSeleccionada == null || tipoVacunaSeleccionada.trim().isEmpty()) {
            Toast.makeText(this, "Selecciona el tipo de vacuna.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!validarFechas()) {
            Toast.makeText(this, "Revisa las fechas del registro.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardarVacunacion.setEnabled(false); // evita doble toque

        Vacuna v = new Vacuna();
        v.setMascotaId(mascota.getId());
        v.setTipoRegistro(spinnerTipoRegistro.getSelectedItem() != null
                ? spinnerTipoRegistro.getSelectedItem().toString() : "");
        v.setTipoVacuna(tipoVacunaSeleccionada.trim());
        v.setFechaAplicacion(formatoGuardar(fechaAplicacion));
        v.setFechaVencimiento(formatoGuardar(fechaVencimiento));
        v.setComentarios(etComentarios.getText().toString().trim());

        // 1. Guardado local inmediato (genera el id y asigna usuarioId)
        VacunasLocalManager.saveOrUpdateVacuna(this, uid, v);

        // 2. Sincronización en segundo plano con Firestore
        sincronizarConFirestore(v);

        Toast.makeText(this, "Vacunación registrada para " + mascota.getNombre() + ".",
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void sincronizarConFirestore(Vacuna v) {
        VacunasLocalManager.sincronizarConFirestore(uid, v);
    }
}