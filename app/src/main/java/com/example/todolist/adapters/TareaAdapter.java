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

    public interface OnTareaClickListener {
        void onTareaClick(Tarea tarea);
    }

    public TareaAdapter(List<Tarea> tareas) {
        this.tareas = new ArrayList<>(tareas);
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
        Tarea tarea = tareas.get(position);
        Log.d("TareaAdapter", "Tarea: " + tarea.getNombre());
        holder.nombreTarea.setText(tarea.getNombre());
        holder.fechaTarea.setText(tarea.getFecha());
        holder.horaTarea.setText(tarea.getHora());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTareaClick(tarea);
            }
        });
    }


    @Override
    public int getItemCount() {
        return tareas.size();
    }


    public void actualizarListaTareas(List<Tarea> nuevasTareas) {
        this.tareas = new ArrayList<>(nuevasTareas);
        notifyDataSetChanged();
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