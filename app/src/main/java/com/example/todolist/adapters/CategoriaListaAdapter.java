package com.example.todolist.adapters;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.FirestoreManager;

import java.util.List;

public class CategoriaListaAdapter extends RecyclerView.Adapter<CategoriaListaAdapter.ViewHolder> {
    private List<String> categorias;
    private Context context;
    private FirestoreManager firestoreManager;
    private OnCategoriaChangeListener listener;

    public interface OnCategoriaChangeListener {
        void onCategoriaChanged();
    }
    public CategoriaListaAdapter(Context context, List<String> categorias) {
        this.context = context;
        this.categorias = categorias;
        this.firestoreManager = FirestoreManager.getInstance(context);
    }
    public CategoriaListaAdapter(Context context, List<String> categorias, OnCategoriaChangeListener listener) {
        this.context = context;
        this.categorias = categorias;
        this.firestoreManager = FirestoreManager.getInstance(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rv_categorias, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String categoria = categorias.get(position);
        holder.nombreCategoria.setText(categoria);

        // Actualizar el contador de tareas
        firestoreManager.contarTareasPorCategoria(categoria, new FirestoreManager.FirestoreCallback<Long>() {
            @Override
            public void onSuccess(Long count) {
                holder.cantTareas.setText(String.valueOf(count));
            }

            @Override
            public void onError(Exception e) {
                holder.cantTareas.setText("0");
                Log.e("CategoriaAdapter", "Error al contar tareas: " + e.getMessage());
            }
        });

        // Botón editar
        holder.btnEditar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Editar categoría");

            final EditText input = new EditText(context);
            input.setText(categoria);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Guardar", (dialog, which) -> {
                String nuevaCategoria = input.getText().toString().trim();
                if (!nuevaCategoria.isEmpty() && !nuevaCategoria.equals(categoria)) {
                    // Actualizar la categoría
                    firestoreManager.updateCategoria(categoria, nuevaCategoria,
                            new FirestoreManager.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (listener != null) {
                                        listener.onCategoriaChanged();
                                    }
                                    Toast.makeText(context, "Categoría actualizada",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(context,
                                            "Error al actualizar: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        // Botón eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Eliminar categoría")
                    .setMessage("¿Estás seguro de que quieres eliminar esta categoría? " +
                            "Las tareas asociadas se moverán a 'Sin categoría'")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        firestoreManager.deleteCategoria(categoria,
                                new FirestoreManager.FirestoreCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        if (listener != null) {
                                            listener.onCategoriaChanged();
                                        }
                                        Toast.makeText(context, "Categoría eliminada",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(context,
                                                "Error al eliminar: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void actualizarCategoria(String categoriaAntigua, String categoriaNueva, int position) {
        // Primero crear la nueva categoría
        firestoreManager.createCategoria(categoriaNueva, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Actualizar la lista local
                categorias.set(position, categoriaNueva);
                notifyItemChanged(position);
                Toast.makeText(context, "Categoría actualizada", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(context, "Error al actualizar: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarCategoria(String categoria, int position) {
        // Implementar la eliminación en Firestore
        // Nota: Necesitarás agregar un método deleteCategoria en FirestoreManager
        categorias.remove(position);
        notifyItemRemoved(position);
        Toast.makeText(context, "Categoría eliminada", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombreCategoria;
        TextView cantTareas;
        ImageButton btnEditar;
        ImageButton btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreCategoria = itemView.findViewById(R.id.tv_nombreCategoria);
            cantTareas = itemView.findViewById(R.id.tv_cant_tareas_categoria);
            btnEditar = itemView.findViewById(R.id.btn_editar_categoria);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_categoria);
        }
    }

    public void actualizarCategorias(List<String> nuevasCategorias) {
        this.categorias = nuevasCategorias;
        notifyDataSetChanged();
    }
}
