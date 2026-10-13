package com.example.ProyectoZoloopatas;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class editar_infoMascota extends AppCompatActivity {

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

        android.widget.ImageButton btnBackTop = findViewById(R.id.btnBackTop);
        if (btnBackTop != null) {
            btnBackTop.setOnClickListener(v -> finish());
        }

        android.widget.ImageButton btnConfirmarTop = findViewById(R.id.btnConfirmarTop);
        if (btnConfirmarTop != null) {
            btnConfirmarTop.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Información guardada exitosamente", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        com.google.android.material.button.MaterialButton btnGuardarMascota = findViewById(R.id.btnGuardarMascota);
        if (btnGuardarMascota != null) {
            btnGuardarMascota.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Mascota actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
