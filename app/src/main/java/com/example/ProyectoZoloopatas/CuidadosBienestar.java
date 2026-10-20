package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Pantalla de Cuidados y Bienestar de la Mascota.
 * Se abre desde HomeActivity (botón "Cuidado & Bienestar").
 *
 * Los tres botones principales llevarán a las pantallas de contenido estático
 * (aún no creadas): advertencias de medicación, guías de cuidados y recetas.
 * Cuando existan esas Activities, reemplaza los Toast por el startActivity indicado.
 */
public class CuidadosBienestar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuidados_bienestar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBackCuidados).setOnClickListener(v -> finish());

        // Botón: Consultar advertencias clínicas -> datos estáticos sobre medicación en animales domésticos
        findViewById(R.id.btnAdvertenciasClinicas).setOnClickListener(v -> {
            // startActivity(new Intent(this, AdvertenciasMedicacion.class));
            Toast.makeText(this, "Advertencias clínicas: próximamente", Toast.LENGTH_SHORT).show();
        });

        // Botón: Ver protocolos de cuidado -> guías de cuidados básicos
        findViewById(R.id.btnProtocolosCuidado).setOnClickListener(v -> {
            // startActivity(new Intent(this, GuiaCuidados.class));
            Toast.makeText(this, "Protocolos de cuidado: próximamente", Toast.LENGTH_SHORT).show();
        });

        // Botón: Explorar recetas y snacks
        findViewById(R.id.btnExplorarRecetas).setOnClickListener(v -> {
            // ✅ Abre el catálogo de recetas
            startActivity(new Intent(CuidadosBienestar.this, catalogo_recetas.class));
        });

        // ── Perfil (avatar y barra inferior) ──
        findViewById(R.id.btnProfileAvatar).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));
        findViewById(R.id.btnNavPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilUsuario.class)));

        // ── Inicio / Notificaciones ──
        findViewById(R.id.btnNavInicio).setOnClickListener(v -> finish()); // vuelve a HomeActivity
        findViewById(R.id.btnNavNotificaciones).setOnClickListener(v ->
                Toast.makeText(this, "Notificaciones: próximamente", Toast.LENGTH_SHORT).show());
    }
}
