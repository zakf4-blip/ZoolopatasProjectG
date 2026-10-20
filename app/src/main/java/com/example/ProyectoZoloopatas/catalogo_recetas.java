package com.example.ProyectoZoloopatas;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class catalogo_recetas extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_catalogo_recetas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // ── Botón Atrás (Buena práctica para no dejar al usuario atrapado) ──
        findViewById(R.id.btnBackRecetas).setOnClickListener(v -> finish());

        // ── Abrir Detalle de la Receta 1 ──
        findViewById(R.id.cardReceta1).setOnClickListener(v -> {
            // ✅ Abre el detalle de la receta del perro
            Intent intent = new Intent(catalogo_recetas.this, detalle_receta_dog_one.class);
            startActivity(intent);
        });
    }
}