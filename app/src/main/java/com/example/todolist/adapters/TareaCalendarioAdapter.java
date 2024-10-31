package com.example.todolist.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;

import java.util.List;

public class TareaCalendarioAdapter extends RecyclerView.Adapter<TareaCalendarioAdapter.TareaCalendarioViewHolder>{

    private List<Tarea> tareas;

    private OnTareaCalendarioClickListener listener;

    public interface OnTareaCalendarioClickListener {
        void onTareaClick(Tarea tarea);
    }

    public TareaCalendarioAdapter(List<Tarea> tareas) {
        this.tareas = tareas;
    }

    public void setOnTareaClickListener(OnTareaCalendarioClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TareaCalendarioAdapter.TareaCalendarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_tareas_calendario, parent, false);
        return new TareaCalendarioAdapter.TareaCalendarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaCalendarioAdapter.TareaCalendarioViewHolder holder, int position) {
        Tarea tarea = tareas.get(position);
        holder.bind(tarea);
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
    public void actualizarTareas(List<Tarea> nuevasTareas) {
        this.tareas = nuevasTareas;
        notifyDataSetChanged();
    }

    public class TareaCalendarioViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvHora;
        private final TextView tvCategoria;
        public TareaCalendarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreTarea);
            tvHora = itemView.findViewById(R.id.tvHoraTarea);
            tvCategoria = itemView.findViewById(R.id.tvCategoriaTarea);
        }
        public void bind(Tarea tarea) {
            tvNombre.setText(tarea.getNombre());
            tvHora.setText(tarea.getHora());
            tvCategoria.setText(tarea.getCategoria());
        }
    }
}
