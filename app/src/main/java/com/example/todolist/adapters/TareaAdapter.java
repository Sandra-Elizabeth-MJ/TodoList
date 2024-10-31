package com.example.todolist.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {
    private List<Tarea> tareas;
    private OnTareaClickListener listener;
    private OnTareaCompletadaListener completadaListener;
    private Map<String, Boolean> radioButtonStates = new HashMap<>();
    private OnTareaDeleteListener deleteListener;

//On
    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }
    public interface OnTareaCompletadaListener {
        void onTareaCompletada(Tarea tarea);
    }
    public interface OnTareaDeleteListener {
        void onTareaDelete(Tarea tarea);
    }

    public TareaAdapter(List<Tarea> tareas) {
        this.tareas = new ArrayList<>(tareas);
        initializeRadioButtonStates();
    }
    private void initializeRadioButtonStates() {
        for (Tarea tarea : tareas) {
            radioButtonStates.put(tarea.getId(), tarea.isCompletada());
        }
    }
//set
    public void setOnTareaClickListener(OnTareaClickListener listener) {
        this.listener = listener;
    }
    public void setOnTareaCompletadaListener(OnTareaCompletadaListener listener) {
        this.completadaListener = listener;
    }
    public void setOnTareaDeleteListener(OnTareaDeleteListener listener) {
        this.deleteListener = listener;
    }


    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rv_tareas, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        Tarea tarea = tareas.get(position);
        Log.d("TareaAdapter", "Tarea: " + tarea.getNombre());
        holder.bind(tarea);

        // Establecer el estado del RadioButton
        holder.radioButton.setChecked(radioButtonStates.getOrDefault(tarea.getId(), false));

        // Listener para el RadioButton
        holder.radioButton.setOnClickListener(v -> {
            if (completadaListener != null && !radioButtonStates.getOrDefault(tarea.getId(), false)) {
                radioButtonStates.put(tarea.getId(), true);
                completadaListener.onTareaCompletada(tarea);
            }
        });

        // Listener para el click en el item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTareaClick(tarea);
            }
        });

        // Listener para el botón de eliminar
        holder.imageButton.setOnClickListener(v -> {
            mostrarDialogoConfirmacion(holder.itemView.getContext(), tarea);
        });
    }
    private void mostrarDialogoConfirmacion(Context context, Tarea tarea) {
        new AlertDialog.Builder(context)
                .setTitle("Eliminar Tarea")
                .setMessage("¿Quieres eliminar la tarea?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (deleteListener != null) {
                        deleteListener.onTareaDelete(tarea);
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }


    @Override
    public int getItemCount() {
        return tareas.size();
    }


    public void actualizarListaTareas(List<Tarea> nuevasTareas) {
        this.tareas = new ArrayList<>(nuevasTareas);
        // Actualizar el estado de los RadioButtons para las nuevas tareas
        for (Tarea tarea : nuevasTareas) {
            if (!radioButtonStates.containsKey(tarea.getId())) {
                radioButtonStates.put(tarea.getId(), false);
            }
        }
        // Limpiar estados antiguos que ya no existen en la nueva lista
        radioButtonStates.keySet().removeIf(tareaId ->
                nuevasTareas.stream().noneMatch(t -> t.getId().equals(tareaId))
        );
        notifyDataSetChanged();
    }



    static class TareaViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        ImageButton imageButton;
        TextView nombreTarea;
        TextView fechaTarea;
        TextView horaTarea;

        TareaViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.rbtn_tarea);
            nombreTarea = itemView.findViewById(R.id.tvTarea_rv);
            fechaTarea = itemView.findViewById(R.id.tvFecha_rv);
            horaTarea = itemView.findViewById(R.id.tvHora_rv);
            imageButton=itemView.findViewById(R.id.btn_eliminar_tarea);
        }
        void bind(Tarea tarea) {
            nombreTarea.setText(tarea.getNombre());
            fechaTarea.setText(tarea.getFecha());
            horaTarea.setText(tarea.getHora());
        }
    }
    @Override
    public long getItemId(int position) {
        return tareas.get(position).getId().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}