package com.example.todolist.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.todolist.R;
import com.example.todolist.adapters.CategoriaAdapter;
import com.example.todolist.entities.Categorias;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.FirestoreManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityDetalleTarea extends AppCompatActivity {
    private EditText tvDetailInfo;
    private TextView fechavencimiento_tv;
    private TextView hora_tv;
    private Spinner spinnerCategories;
    private CategoriaAdapter spinnerAdapter;
    private List<String> categorias = new ArrayList<>();
    private String tareaId;
    private FloatingActionButton fab;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_tarea);
        //
        firestoreManager = FirestoreManager.getInstance(this);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();

        // Initialize views
        tvDetailInfo = findViewById(R.id.tvDetailInfo);
        fechavencimiento_tv = findViewById(R.id.fechavencimiento_tv);
        hora_tv = findViewById(R.id.hora_tv);
        spinnerCategories = findViewById(R.id.spinnerCategories);
        fab = findViewById(R.id.fab);

        // Configure FAB
        fab.setOnClickListener(view -> actualizarTareaEnFirestore());

        // Configure Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_dettalle_tarea);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Get task ID from intent
        tareaId = getIntent().getStringExtra("TAREA_ID");
        cargarDetallesTarea(tareaId);

        // Initialize spinner adapter
        spinnerAdapter = new CategoriaAdapter(this, categorias);
        spinnerCategories.setAdapter(spinnerAdapter);
        categorias.add("Crear nueva categoría");

        // Configure spinner listener
        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == categorias.size() - 1) {
                    mostrarDialogoNuevaCategoria();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load data
        cargarCategorias();
        configurarListeners();

    }

    private void configurarListeners() {
        fechavencimiento_tv.setOnClickListener(v -> mostrarDatePicker());
        hora_tv.setOnClickListener(v -> mostrarTimePicker());
    }

    private void mostrarDatePicker() {
        // Establecer el Locale en español
        Locale.setDefault(new Locale("es", "ES"));
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fecha = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            fechavencimiento_tv.setText(fecha);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            hora_tv.setText(hora);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void cargarCategorias() {
        firestore.collection("user").document(userId)
                .collection("categorias")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categorias.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            categorias.add(document.getString("nombre"));
                        }
                        categorias.add("Crear nueva categoría");
                        spinnerAdapter.notifyDataSetChanged();

                        if (tareaId != null) {
                            cargarDetallesTarea(tareaId);
                        }
                    } else {
                        Toast.makeText(ActivityDetalleTarea.this, "Error al cargar las categorías", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void cargarDetallesTarea(String tareaId) {
        firestoreManager.getTareas(new FirestoreManager.FirestoreCallback<List<Tarea>>() {
            @Override
            public void onSuccess(List<Tarea> result) {
                for (Tarea tarea : result) {
                    if (tarea.getId().equals(tareaId)) {
                        tvDetailInfo.setText(tarea.getNombre());
                        fechavencimiento_tv.setText(tarea.getFecha());
                        hora_tv.setText(tarea.getHora());

                        int posicion = categorias.indexOf(tarea.getCategoria());
                        if (posicion != -1) {
                            spinnerCategories.setSelection(posicion);
                        }
                        break;
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActivityDetalleTarea.this, "Error al cargar la tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarTareaEnFirestore() {
        String categoriaSeleccionada = spinnerCategories.getSelectedItem().toString();
        if (categoriaSeleccionada.equals("Crear nueva categoría")) {
            Toast.makeText(this, "Por favor, selecciona una categoría válida", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = tvDetailInfo.getText().toString();
        String fecha = fechavencimiento_tv.getText().toString();
        String hora = hora_tv.getText().toString();

        if (nombre.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> tareaMap = new HashMap<>();
        tareaMap.put("nombre", nombre);
        tareaMap.put("fecha", fecha);
        tareaMap.put("hora", hora);
        tareaMap.put("categoria", categoriaSeleccionada);
        tareaMap.put("userId", userId);

        DocumentReference tareaRef;
        if (tareaId != null) {
            tareaRef = firestore.collection("user").document(userId)
                    .collection("tareas").document(tareaId);
        } else {
            tareaRef = firestore.collection("user").document(userId)
                    .collection("tareas").document();
        }
        Tarea tareaActualizada = new Tarea(tareaId, nombre, fecha, hora, categoriaSeleccionada, userId);

        firestoreManager.updateTarea(tareaActualizada, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ActivityDetalleTarea.this, "Tarea guardada con éxito", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ActivityDetalleTarea.this, "Error al guardar la tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void mostrarDialogoNuevaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Crear Nueva Categoría");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_nueva_categoria, null);
        final EditText input = viewInflated.findViewById(R.id.etNuevaCategoria);
        builder.setView(viewInflated);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevaCategoria = input.getText().toString();
            if (!nuevaCategoria.isEmpty()) {
                guardarNuevaCategoria(nuevaCategoria);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void guardarNuevaCategoria(String nombreCategoria) {
        Map<String, Object> categoria = new HashMap<>();
        categoria.put("nombre", nombreCategoria);
        categoria.put("userId", userId);

        firestore.collection("user").document(userId)
                .collection("categorias")
                .add(categoria)
                .addOnSuccessListener(documentReference -> {
                    categorias.add(categorias.size() - 1, nombreCategoria);
                    spinnerAdapter.notifyDataSetChanged();
                    int posicion = categorias.indexOf(nombreCategoria);
                    spinnerCategories.setSelection(posicion);
                    Toast.makeText(ActivityDetalleTarea.this, "Categoría creada con éxito", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ActivityDetalleTarea.this, "Error al crear la categoría", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_dt_tarea, menu);
        return true;
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

    @Override
    protected void onPause() {
        super.onPause();
        actualizarTareaEnFirestore();
    }

}