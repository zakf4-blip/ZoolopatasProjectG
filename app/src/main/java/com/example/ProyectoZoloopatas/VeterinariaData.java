package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class VeterinariaData {

    private static final String PREF_FAVS = "zoolopatas_vet_favs";

    public static List<Veterinaria> getClinicas(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FAVS, Context.MODE_PRIVATE);
        List<Veterinaria> lista = new ArrayList<>();

        // 1. Veterinaria San Martín (Miraflores)
        lista.add(new Veterinaria(
                "vet_01",
                "Veterinaria San Martín",
                "Clínica Especializada",
                "Av. Alfredo Benavides 1234, Miraflores",
                "Miraflores",
                4.9,
                120,
                true,
                "Abierto ahora",
                1.0,
                "Medicina Felina & Cirugía",
                "+51981445220",
                "S/ 70",
                -12.1264,
                -77.0253,
                prefs.getBoolean("vet_01", true),
                R.drawable.img_clinic_san_martin,
                "https://images.unsplash.com/photo-1584467735871-8e85353a8413?w=600&q=80"
        ));

        // 2. Clínica Veterinaria Rondón (San Isidro)
        lista.add(new Veterinaria(
                "vet_02",
                "Veterinaria Rondón",
                "Clínica Especializada",
                "Av. Dos de Mayo 890, San Isidro",
                "San Isidro",
                4.8,
                98,
                false,
                "Cerrado • Abre 8:00 AM",
                2.5,
                "Ecografía Doppler & UCI",
                "+5114402345",
                "S/ 95",
                -12.0965,
                -77.0384,
                prefs.getBoolean("vet_02", false),
                R.drawable.img_clinic_rondon,
                "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=800&q=80"
        ));

        // 3. Clínica Veterinaria Miraflores (Surco / Miraflores)
        lista.add(new Veterinaria(
                "vet_03",
                "Veterinaria Miraflores",
                "Clínica Especializada",
                "Av. Primavera 650, Chacarilla, Surco",
                "Surco",
                4.7,
                210,
                true,
                "Abierto 24 Horas",
                3.8,
                "Guardia Médica Continua",
                "+5113726161",
                "S/ 85",
                -12.1120,
                -76.9850,
                prefs.getBoolean("vet_03", false),
                R.drawable.img_clinic_miraflores,
                "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=800&q=80"
        ));

        // 4. PetCenter 24H (San Miguel)
        lista.add(new Veterinaria(
                "vet_04",
                "PetCenter 24H",
                "Clínica Externa",
                "Av. La Marina 2150, San Miguel",
                "San Miguel",
                4.6,
                185,
                true,
                "Abierto 24 Horas",
                4.2,
                "Urgencias & Rayos X Digital",
                "+5115664321",
                "S/ 80",
                -12.0792,
                -77.0864,
                prefs.getBoolean("vet_04", false),
                R.drawable.img_clinic_petcenter,
                "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?w=800&q=80"
        ));

        // 5. Clínica Veterinaria Pancho Cavero (Surquillo)
        lista.add(new Veterinaria(
                "vet_05",
                "Veterinaria Pancho Cavero",
                "Clínica Especializada",
                "Av. República de Panamá 4560, Surquillo",
                "Surquillo",
                4.8,
                340,
                true,
                "Abierto ahora",
                2.1,
                "Traumatología, UCI & Cirugía Avanzada",
                "+5114457890",
                "S/ 90",
                -12.1128,
                -77.0245,
                prefs.getBoolean("vet_05", false),
                R.drawable.img_clinic_san_martin,
                "https://images.unsplash.com/photo-1538108149393-fbbd81895907?w=800&q=80"
        ));

        // 6. Centro Veterinario San Borja (San Borja)
        lista.add(new Veterinaria(
                "vet_06",
                "Centro Veterinario San Borja",
                "Clínica Externa",
                "Av. Aviación 2890, San Borja",
                "San Borja",
                4.9,
                140,
                true,
                "Abierto ahora",
                3.1,
                "Tomografía & Cirugía Ambulatoria",
                "+5112259911",
                "S/ 65",
                -12.0950,
                -77.0010,
                prefs.getBoolean("vet_06", true),
                R.drawable.img_clinic_rondon,
                "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800&q=80"
        ));

        // 7. Hospital Veterinario Oasis (Los Olivos)
        lista.add(new Veterinaria(
                "vet_07",
                "Hospital Veterinario Oasis",
                "Clínica Externa",
                "Av. Carlos Izaguirre 820, Los Olivos",
                "Los Olivos",
                4.5,
                82,
                true,
                "Abierto ahora",
                8.5,
                "Consultas Externas & Vacunación",
                "+5115227890",
                "S/ 50",
                -11.9930,
                -77.0700,
                prefs.getBoolean("vet_07", false),
                R.drawable.img_clinic_san_martin,
                "https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=800&q=80"
        ));

        // 8. Clínica Veterinaria SOS Animal (Surco)
        lista.add(new Veterinaria(
                "vet_08",
                "Veterinaria SOS Animal",
                "Clínica Especializada",
                "Av. El Polo 405, Santiago de Surco",
                "Surco",
                4.8,
                115,
                true,
                "Abierto 24 Horas",
                4.5,
                "Oftalmología & Hospitalización",
                "+5113441234",
                "S/ 75",
                -12.1075,
                -76.9740,
                prefs.getBoolean("vet_08", false),
                R.drawable.img_clinic_miraflores,
                "https://images.unsplash.com/photo-1551076805-e1869033e561?w=800&q=80"
        ));

        return lista;
    }

    public static boolean isFavorita(Context context, String vetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FAVS, Context.MODE_PRIVATE);
        return prefs.getBoolean(vetId, false);
    }

    public static void setFavorita(Context context, String vetId, boolean favorita) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FAVS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(vetId, favorita).apply();
    }
}
