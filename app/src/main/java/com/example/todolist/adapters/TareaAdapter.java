package com.example.todolist.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {
    private List<Tarea> tareas;
    private OnTareaClickListener listener;
    private static final String TAG = "TareaAdapter";

    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }

    public TareaAdapter(List<Tarea> tareas) {
        this.tareas = tareas != null ? new ArrayList<>(tareas) : new ArrayList<>();
    }

    public void setOnTareaClickListener(OnTareaClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rv_tareas, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        try {
            if (position < 0 || position >= tareas.size()) {
                Log.e(TAG, "Invalid position in onBindViewHolder: " + position);
                return;
            }

            Tarea tarea = tareas.get(position);
            if (tarea == null) {
                Log.e(TAG, "Null tarea object at position: " + position);
                return;
            }

            Log.d(TAG, "Binding tarea at position " + position + ": " + tarea.getNombre());
            holder.nombreTarea.setText(tarea.getNombre());
            holder.fechaTarea.setText(tarea.getFecha());
            holder.horaTarea.setText(tarea.getHora());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTareaClick(tarea);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return tareas != null ? tareas.size() : 0;
    }

    public void actualizarListaTareas(List<Tarea> nuevasTareas) {
        if (nuevasTareas == null) {
            Log.w(TAG, "Attempting to update with null list");
            this.tareas = new ArrayList<>();
        } else {
            this.tareas = new ArrayList<>(nuevasTareas);
        }
        notifyDataSetChanged();
    }

    public Tarea getTareaAt(int position) {
        if (tareas == null) {
            Log.e(TAG, "Tareas list is null");
            return null;
        }

        if (position < 0 || position >= tareas.size()) {
            Log.e(TAG, "Invalid position in getTareaAt: " + position);
            return null;
        }

        try {
            return tareas.get(position);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Index out of bounds in getTareaAt: " + position, e);
            return null;
        }
    }

    public boolean removeTarea(int position) {
        if (tareas == null) {
            Log.e(TAG, "Tareas list is null");
            return false;
        }

        if (position < 0 || position >= tareas.size()) {
            Log.e(TAG, "Invalid position in removeTarea: " + position);
            return false;
        }

        try {
            tareas.remove(position);
            notifyItemRemoved(position);
            return true;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Error removing tarea at position " + position, e);
            return false;
        }
    }

    static class TareaViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView nombreTarea;
        TextView fechaTarea;
        TextView horaTarea;

        TareaViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.rbtn_tarea);
            nombreTarea = itemView.findViewById(R.id.tvTarea_rv);
            fechaTarea = itemView.findViewById(R.id.tvFecha_rv);
            horaTarea = itemView.findViewById(R.id.tvHora_rv);
        }
    }
}