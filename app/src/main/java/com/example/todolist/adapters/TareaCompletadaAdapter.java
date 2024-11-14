package com.example.todolist.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.activities.ActivityTareaCompletada;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class TareaCompletadaAdapter extends RecyclerView.Adapter<TareaCompletadaAdapter.ViewHolder>  {

    private List<Tarea> tareasCompletadas;
    private FirestoreManager firestoreManager;
    private ActivityTareaCompletada activity; // Cambiado a tipo específico

    // Constructor actualizado
    public TareaCompletadaAdapter(List<Tarea> tareasCompletadas, FirestoreManager firestoreManager, ActivityTareaCompletada activity) {
        this.tareasCompletadas = new ArrayList<>(tareasCompletadas);
        this.firestoreManager = firestoreManager;
        this.activity = activity;
    }

    @NonNull
    @Override
    public TareaCompletadaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea_completada, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaCompletadaAdapter.ViewHolder holder, int position) {
        Tarea tarea = tareasCompletadas.get(position);
        holder.radioButton.setChecked(true);
        holder.radioButton.setEnabled(true);
        holder.nombreTarea.setText(tarea.getNombre());
        holder.nombreTarea.setPaintFlags(holder.nombreTarea.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        holder.fechaCompletada.setText(tarea.getFechaCompletada());
        holder.fechaInicial.setText(tarea.getFecha());
        holder.horaInical.setText(tarea.getHora());

        holder.radioButton.setOnClickListener(v -> {
            // Obtener la posición actual antes de cualquier operación
            final Tarea tareaToUpdate = tareasCompletadas.get(holder.getAdapterPosition());

            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Confirmación")
                    .setMessage("¿Desea marcar como tarea no completada?")
                    .setPositiveButton("Cambiar", (dialog, which) -> {
                        tareaToUpdate.setCompletada(false);
                        tareaToUpdate.setFechaCompletada(null);

                        firestoreManager.updateTarea(tareaToUpdate, new FirestoreManager.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // Ejecutar en el hilo principal
                                activity.runOnUiThread(() -> {
                                    // Encontrar la posición actual de la tarea
                                    int positionToRemove = tareasCompletadas.indexOf(tareaToUpdate);
                                    if (positionToRemove != -1) {
                                        tareasCompletadas.remove(positionToRemove);
                                        notifyItemRemoved(positionToRemove);

                                        // Notificar cambios en el rango para actualizar las posiciones
                                        if (positionToRemove < tareasCompletadas.size()) {
                                            notifyItemRangeChanged(positionToRemove, tareasCompletadas.size());
                                        }
                                    }

                                    Toast.makeText(holder.itemView.getContext(),
                                            "Tarea marcada como no completada",
                                            Toast.LENGTH_SHORT).show();
                                });

                                // Notificar a la actividad para actualizar
                                if (activity != null) {
                                    activity.cargarTareasCompletadas();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                activity.runOnUiThread(() -> {
                                    Log.e("TareaCompletadaAdapter",
                                            "Error al actualizar la tarea: " + e.getMessage(), e);
                                    Toast.makeText(holder.itemView.getContext(),
                                            "Error al actualizar la tarea: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }
    public void updateTareas(List<Tarea> newTareas) {
        this.tareasCompletadas.clear();
        this.tareasCompletadas.addAll(newTareas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return tareasCompletadas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView nombreTarea;
        TextView fechaCompletada;
        TextView fechaInicial;
        TextView horaInical;

        ViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.rbtn_tarea_completada);
            nombreTarea = itemView.findViewById(R.id.tv_tarea_completada);
            fechaCompletada = itemView.findViewById(R.id.tv_fecha_completada);
            fechaInicial = itemView.findViewById(R.id.tvFechaInicial_tc);
            horaInical = itemView.findViewById(R.id.tvHoraInical_tc);
        }
    }
}
