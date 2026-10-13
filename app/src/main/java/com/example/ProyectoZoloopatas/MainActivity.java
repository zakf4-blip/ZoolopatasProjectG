package com.example.ProyectoZoloopatas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button btnCrearCuenta;
    private Button btnIniciarSesionOnly;

    private FirebaseAnalytics analytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        analytics = FirebaseAnalytics.getInstance(this);

        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnIniciarSesionOnly = findViewById(R.id.btnIniciarSesionOnly);


        btnCrearCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, registro_cuenta.class);
                startActivity(intent);
            }
        });

        btnIniciarSesionOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, iniciosesion.class);
                startActivity(intent);
            }
        });


        // Evento de pantalla (recomendado por Firebase)
        logScreenView("MainActivity", "MainActivity");

        // Evento de inicialización de la app
        Bundle initBundle = new Bundle();
        initBundle.putString("app_name", "Zoolopatas");
        initBundle.putString("module", "main");
        logEvent(AnalyticsEvents.APP_INIT, initBundle);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(MainActivity.this, confirmacion_registro.class);
                startActivity(intent);
                finish();
            }
        }
    }

    //Helpers de analítica

    private void logScreenView(String screenName, String screenClass) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    private void logEvent(String eventName, Bundle params) {
        if (analytics != null) {
            analytics.logEvent(eventName, params);
        }
    }

    //Constantes de eventos para Firebase Analytics

    public static final class AnalyticsEvents {
        public static final String APP_INIT = "app_init";
        public static final String PET_ADDED = "pet_added";
        public static final String PET_HISTORY_VIEW = "pet_history_view";

        private AnalyticsEvents() {} // Evitar hacer instanciación ()

        //En los metódos o funciones de botones por ejemplo se hace lo siguiente:
        //Bundle params = new Bundle();
        //params.putString("pet_id", petId);
        //analytics.logEvent(AnalyticsEvents.PET_HISTORY_VIEW, params);
    }
}