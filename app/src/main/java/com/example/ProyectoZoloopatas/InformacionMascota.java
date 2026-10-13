package com.example.ProyectoZoloopatas;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InformacionMascota extends AppCompatActivity {

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

        android.widget.ImageButton btnEditarMascota = findViewById(R.id.btnEditarMascota);
        if (btnEditarMascota != null) {
            btnEditarMascota.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(InformacionMascota.this, editar_infoMascota.class);
                startActivity(intent);
            });
        }
    }
}
