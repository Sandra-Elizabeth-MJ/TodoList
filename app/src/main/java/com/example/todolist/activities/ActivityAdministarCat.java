package com.example.todolist.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapters.CategoriaAdapter;
import com.example.todolist.adapters.CategoriaListaAdapter;
import com.example.todolist.services.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class ActivityAdministarCat extends AppCompatActivity implements CategoriaListaAdapter.OnCategoriaChangeListener {
    private RecyclerView recyclerView;
    private CategoriaListaAdapter adapter;
    private FirestoreManager firestoreManager;
    private List<String> categorias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administar_cat);

        // Configure Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_admin_cat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        // Configurar Toolbar
        setupToolbar();

        // Configurar RecyclerView
        setupRecyclerView();

        // Inicializar FirestoreManager
        firestoreManager = FirestoreManager.getInstance(this);

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.rvCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoriaListaAdapter(this, categorias);
        recyclerView.setAdapter(adapter);

        // Configurar el botón de agregar
        findViewById(R.id.iconAddSubtask).setOnClickListener(v -> mostrarDialogoNuevaCategoria());

        // Cargar categorías
        cargarCategorias();
    }
    @Override
    public void onCategoriaChanged() {
        cargarCategorias();
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_admin_cat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.rvCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoriaListaAdapter(this, categorias, this);
        recyclerView.setAdapter(adapter);
    }


    private void cargarCategorias() {
        firestoreManager.getCategorias(new FirestoreManager.FirestoreCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                categorias.clear();
                categorias.addAll(result);
                adapter.actualizarCategorias(categorias);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActivityAdministarCat.this,
                        "Error al cargar categorías: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoNuevaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nueva categoría");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String nuevaCategoria = input.getText().toString();
            if (!nuevaCategoria.isEmpty()) {
                firestoreManager.createCategoria(nuevaCategoria,
                        new FirestoreManager.FirestoreCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                cargarCategorias(); // Recargar la lista
                                Toast.makeText(ActivityAdministarCat.this,
                                        "Categoría creada", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(ActivityAdministarCat.this,
                                        "Error al crear categoría: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}