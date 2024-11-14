package com.example.todolist.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapters.TareaCompletadaAdapter;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class ActivityTareaCompletada extends AppCompatActivity {

    private FirestoreManager firestoreManager;
    private TareaCompletadaAdapter adapter;
    private List<Tarea> tareasCompletadas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tarea_completada);
        // Inicializar FirestoreManager
        firestoreManager = FirestoreManager.getInstance(this);
        // Configure Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_tarea_completada);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // Configurar RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rvTareasCompletadas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TareaCompletadaAdapter(tareasCompletadas, firestoreManager, this);
        recyclerView.setAdapter(adapter);



        // Cargar tareas completadas
        cargarTareasCompletadas();
    }
    public void cargarTareasCompletadas() { // Cambiado a público
        firestoreManager.getTareasCompletadas(new FirestoreManager.FirestoreCallback<List<Tarea>>() {
            @Override
            public void onSuccess(List<Tarea> result) {
                tareasCompletadas.clear();
                tareasCompletadas.addAll(result);
                adapter.updateTareas(new ArrayList<>(result));
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActivityTareaCompletada.this,
                        "Error al cargar tareas completadas: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú con la opción de cerrar sesión
        getMenuInflater().inflate(R.menu.toolbar_tareas_completadas, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_OK);
            finish();
            return true;
        }else if (item.getItemId() == R.id.delete_tareas_com) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CONFIRMAR ELIMINACIÓN")
                .setMessage("¿Quieres eliminar las tareas??")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    deleteAllCompletedTasks();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void deleteAllCompletedTasks() {
        firestoreManager.deleteAllCompletedTasks(new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ActivityTareaCompletada.this,
                        "Tareas completadas eliminadas exitosamente",
                        Toast.LENGTH_SHORT).show();
                cargarTareasCompletadas(); // Reload the list
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActivityTareaCompletada.this,
                        "Error al eliminar las tareas: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}