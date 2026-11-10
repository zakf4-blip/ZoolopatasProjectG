package com.example.ProyectoZoloopatas;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class VeterinariaAdapter extends RecyclerView.Adapter<VeterinariaAdapter.VeterinariaViewHolder> {

    public interface OnVeterinariaClickListener {
        void onVeterinariaClick(Veterinaria vet);
        void onFavoritoClick(Veterinaria vet, int position);
        void onLlamarClick(Veterinaria vet);
        void onNavegarClick(Veterinaria vet);
    }

    private final Context context;
    private final List<Veterinaria> listaVeterinarias = new ArrayList<>();
    private final OnVeterinariaClickListener listener;

    public VeterinariaAdapter(Context context, List<Veterinaria> lista, OnVeterinariaClickListener listener) {
        this.context = context;
        if (lista != null) {
            this.listaVeterinarias.addAll(lista);
        }
        this.listener = listener;
    }

    public void actualizarLista(List<Veterinaria> nuevaLista) {
        listaVeterinarias.clear();
        if (nuevaLista != null) {
            listaVeterinarias.addAll(nuevaLista);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VeterinariaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_veterinaria_card, parent, false);
        return new VeterinariaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VeterinariaViewHolder holder, int position) {
        Veterinaria vet = listaVeterinarias.get(position);

        holder.tvNombre.setText(vet.getNombre());
        holder.tvRating.setText(String.format(java.util.Locale.US, "%.1f", vet.getCalificacion()));
        holder.tvDireccion.setText(vet.getDireccion());
        holder.tvDistanciaResenas.setText(String.format(java.util.Locale.US, "A %.1f km • (%d reseñas)",
                vet.getDistanciaKm(), vet.getCantidadResenas()));
        holder.tvEspecialidad.setText(vet.getEspecialidad());

        // Imagen de la clínica (Foto original o recurso local)
        if (vet.hasUrlImagen()) {
            com.bumptech.glide.Glide.with(context)
                    .load(vet.getUrlImagen())
                    .centerCrop()
                    .placeholder(R.drawable.bg_badge_gris)
                    .error(vet.getImagenResId() != 0 ? vet.getImagenResId() : R.drawable.img_clinic_san_martin)
                    .into(holder.ivClinicImage);
        } else if (vet.getImagenResId() != 0) {
            holder.ivClinicImage.setImageResource(vet.getImagenResId());
        }

        // Estado horario (Abierto / Cerrado)
        if (vet.isAbierto()) {
            holder.tvEstadoHorario.setText("• " + vet.getHorarioTexto());
            holder.tvEstadoHorario.setBackgroundResource(R.drawable.bg_status_open);
            holder.tvEstadoHorario.setTextColor(Color.parseColor("#007A6E"));
        } else {
            holder.tvEstadoHorario.setText("• " + vet.getHorarioTexto());
            holder.tvEstadoHorario.setBackgroundResource(R.drawable.bg_status_closed);
            holder.tvEstadoHorario.setTextColor(Color.parseColor("#6B7280"));
        }

        // Ícono de favorito
        if (vet.isEsFavorita()) {
            holder.btnFavorito.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.btnFavorito.setImageResource(R.drawable.ic_heart_border);
        }

        // Eventos
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onVeterinariaClick(vet);
        });

        holder.btnFavorito.setOnClickListener(v -> {
            if (listener != null) listener.onFavoritoClick(vet, holder.getAdapterPosition());
        });

        holder.btnLlamar.setOnClickListener(v -> {
            if (listener != null) listener.onLlamarClick(vet);
        });

        holder.btnNavegar.setOnClickListener(v -> {
            if (listener != null) listener.onNavegarClick(vet);
        });
    }

    @Override
    public int getItemCount() {
        return listaVeterinarias.size();
    }

    public static class VeterinariaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivClinicImage;
        TextView tvRating;
        TextView tvNombre;
        ImageView btnFavorito;
        TextView tvDireccion;
        TextView tvEstadoHorario;
        TextView tvDistanciaResenas;
        TextView tvEspecialidad;
        FrameLayout btnLlamar;
        FrameLayout btnNavegar;

        public VeterinariaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivClinicImage = itemView.findViewById(R.id.ivClinicImage);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvNombre = itemView.findViewById(R.id.tvNombreVeterinaria);
            btnFavorito = itemView.findViewById(R.id.btnFavorito);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvEstadoHorario = itemView.findViewById(R.id.tvEstadoHorario);
            tvDistanciaResenas = itemView.findViewById(R.id.tvDistanciaResenas);
            tvEspecialidad = itemView.findViewById(R.id.tvEspecialidad);
            btnLlamar = itemView.findViewById(R.id.btnLlamar);
            btnNavegar = itemView.findViewById(R.id.btnNavegar);
        }
    }
}
