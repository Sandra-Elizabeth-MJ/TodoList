package com.example.todolist.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;

import java.util.List;

public class TareaCompletadaAdapter extends RecyclerView.Adapter<TareaCompletadaAdapter.ViewHolder>  {

    private List<Tarea> tareasCompletadas;

    public TareaCompletadaAdapter(List<Tarea> tareasCompletadas) {
        this.tareasCompletadas = tareasCompletadas;
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
        holder.radioButton.setEnabled(false);
        holder.nombreTarea.setText(tarea.getNombre());
        holder.nombreTarea.setPaintFlags(holder.nombreTarea.getPaintFlags() |
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        holder.fechaCompletada.setText(tarea.getFechaCompletada());
        holder.fechaInicial.setText(tarea.getFecha());
        holder.horaInical.setText(tarea.getHora());
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
