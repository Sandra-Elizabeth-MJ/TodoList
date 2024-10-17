package com.example.todolist.Fragmentos;



import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.activities.ActivityDetalleTarea;
import com.example.todolist.activities.ActivityLogin;
import com.example.todolist.adapters.CategoriaAdapter;
import com.example.todolist.adapters.TareaAdapter;
import com.example.todolist.entities.Categorias;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.ClienteAPI;
import com.example.todolist.services.FirestoreManager;
import com.example.todolist.services.ServicioAPI;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TareaFragment extends Fragment {
    private Spinner spinnerCategories;
    private CategoriaAdapter spinnerAdapter;
    private List<String> categorias = new ArrayList<>();
    List<Tarea> tareaInfoList = new ArrayList<>();
    TareaAdapter adaptar;
    private LinearLayout linearLayoutCategorias;
    private static final int REQUEST_CODE_ACTUALIZAR_TAREA = 1;
    private View rootView;
    private Button botonSeleccionado = null;
    private Toolbar toolbar;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private boolean isInitialLoad = true;

    private FirestoreManager firestoreManager;

    //constante con las lista de categorias predefinidas

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tarea, container, false);
        linearLayoutCategorias = rootView.findViewById(R.id.linearLayoutCategorias);
        firestoreManager = FirestoreManager.getInstance(requireContext());
        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        initializeSpinnerAdapter();
        setUpRecyclerView();
        lanzarAddTarea();
        // Solo cargar tareas en la creación inicial
       cargarTareas();


        // Inicializar el Toolbar
        toolbar = rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        // Inflar el menú
        setHasOptionsMenu(true);
        // inicializar la clase de sincronizacion


        return rootView;
    }
    private void initializeSpinnerAdapter() {
        categorias = new ArrayList<>();
        spinnerAdapter = new CategoriaAdapter(requireContext(), categorias);
    }

    private void lanzarAddTarea() {
        FloatingActionButton btnCreateActividad = rootView.findViewById(R.id.fbtn_detalleTarea);
        btnCreateActividad.setOnClickListener(view -> {
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.alert_add_tarea, null);
            spinnerCategories = dialogView.findViewById(R.id.spinnerCategoriesAlert);
            EditText etNombre = dialogView.findViewById(R.id.etnuevaTarea);
            EditText etFecha = dialogView.findViewById(R.id.etfecha);
            EditText etHora = dialogView.findViewById(R.id.ethora);
            // Asegurarse de que el adapter esté inicializado
            if (spinnerAdapter == null) {
                initializeSpinnerAdapter();
            }
            spinnerAdapter = new CategoriaAdapter(requireContext(), categorias);
            spinnerCategories.setAdapter(spinnerAdapter);

            cargarCategorias();

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

            etFecha.setInputType(InputType.TYPE_NULL);
            etHora.setInputType(InputType.TYPE_NULL);

            etFecha.setOnClickListener(v -> mostrarDatePicker(etFecha));
            etHora.setOnClickListener(v -> mostrarTimePicker(etHora));

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton("Añadir", (dialogInterface, i) -> {
                        String nombre = etNombre.getText().toString();
                        String fecha = etFecha.getText().toString();
                        String hora = etHora.getText().toString();
                        Object selectedItem = spinnerCategories.getSelectedItem();
                        if (selectedItem == null || selectedItem.toString().equals("Crear nueva categoría")) {
                            Toast.makeText(requireContext(), "Por favor, selecciona una categoría válida", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String categoriaSeleccionada = selectedItem.toString();
                        crearTarea(nombre, fecha, hora, categoriaSeleccionada);
                    })
                    .setNegativeButton("Cancelar", (dialogInterface, i) -> dialogInterface.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void mostrarDialogoNuevaCategoria() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Crear Nueva Categoría");

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_nueva_categoria, null);
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
    //guarda las tareas en el firestore
    /*
    private void crearTarea(String nombre, String fecha, String hora, String categoriaSeleccionada) {
        Map<String, Object> tarea = new HashMap<>();
        tarea.put("nombre", nombre);
        tarea.put("fecha", fecha);
        tarea.put("hora", hora);
        tarea.put("categoria", categoriaSeleccionada);
        tarea.put("userId", userId);

        firestore.collection("user").document(userId)
                .collection("tareas")
                .add(tarea)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Tarea creada con éxito", Toast.LENGTH_SHORT).show();
                    cargarTareas(); // Recargar todas las tareas después de crear una nueva
                })
                .addOnFailureListener(e -> Toast.makeText(requireActivity(), "Error al crear tarea", Toast.LENGTH_SHORT).show());
    }*/
    private void crearTarea(String nombre, String fecha, String hora, String categoriaSeleccionada) {
        Tarea nuevaTarea = new Tarea(nombre, fecha, hora, categoriaSeleccionada);
        nuevaTarea.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        firestoreManager.createTarea(nuevaTarea, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getContext(), "Tarea creada con éxito", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireActivity(), "Error al crear tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarNuevaCategoria(String nombreCategoria) {
        Map<String, Object> categoria = new HashMap<>();
        categoria.put("nombre", nombreCategoria);
        categoria.put("userId", userId);

        firestore.collection("user").document(userId)
                .collection("categorias")
                .add(categoria)
                .addOnSuccessListener(documentReference -> {
                    categorias.add(nombreCategoria);
                    spinnerAdapter.notifyDataSetChanged();
                    Toast.makeText(requireActivity(), "Categoría creada con éxito", Toast.LENGTH_SHORT).show();
                    cargarCategorias();
                })
                .addOnFailureListener(e -> Toast.makeText(requireActivity(), "Error al crear categoría", Toast.LENGTH_SHORT).show());
    }

    private void cargarCategorias() {
        if (spinnerAdapter == null) {
            initializeSpinnerAdapter();
        }
        firestore.collection("user").document(userId)
                .collection("categorias")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categorias.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String nombreCategoria = document.getString("nombre");
                        if (nombreCategoria != null) {
                            categorias.add(nombreCategoria);
                        }
                    }
                    categorias.add("Crear nueva categoría");
                    spinnerAdapter.notifyDataSetChanged();
                    agregarBotonesCategorias(categorias);
                })
                .addOnFailureListener(e -> Toast.makeText(requireActivity(), "Error al cargar categorías", Toast.LENGTH_SHORT).show());
    }
    private void agregarBotonesCategorias(List<String> categorias) {
        linearLayoutCategorias.removeAllViews();
        agregarBotonTodasLasCategorias();
        for (String categoria : categorias) {
            if (!categoria.equals("Crear nueva categoría")) {
                agregarBotonCategoria(categoria);
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private void agregarBotonCategoria(String nombreCategoria) {
        Button button = new Button(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(30, 4, 13, 4);
        button.setLayoutParams(params);

        button.setText(nombreCategoria);
        button.setBackgroundResource(R.drawable.bnt_categoria);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.plomoTex));
        button.setAllCaps(false);

        int paddingHorizontal = dpToPx(14);
        int paddingVertical = dpToPx(6);
        button.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        button.setMinWidth(dpToPx(80));
        button.setMaxWidth(dpToPx(200));

        button.setOnClickListener(v -> {
            seleccionarBoton(button);
            filtrarTareasPorCategoria(nombreCategoria);
        });

        linearLayoutCategorias.addView(button);
    }
    //Recupera todas las tareas de Firestore y las guarda en la lista tareaInfoList. Luego, actualiza la vista del RecyclerView.
    /*
    private void cargarTareas() {
        firestore.collection("user").document(userId)
                .collection("tareas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tareaInfoList.clear(); // Limpiar la lista maestra
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Tarea tarea = new Tarea(
                                document.getString("nombre"),
                                document.getId(),
                                document.getString("fecha"),
                                document.getString("hora"),
                                document.getString("categoria")
                        );
                        tareaInfoList.add(tarea);
                    }
                    adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
                    cargarCategorias(); // Cargar categorías después de tener las tareas
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error al cargar tareas", e));
    }*/
    private void cargarTareas() {
        firestoreManager.getTareas(new FirestoreManager.FirestoreCallback<List<Tarea>>() {
            @Override
            public void onSuccess(List<Tarea> result) {
                tareaInfoList.clear();
                tareaInfoList.addAll(result);
                adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
                cargarCategorias();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error al cargar tareas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void seleccionarBoton(Button botonNuevo) {
        if (botonSeleccionado != null) {
            botonSeleccionado.setSelected(false);
            botonSeleccionado.setTextColor(ContextCompat.getColor(requireContext(), R.color.plomoTex));
            botonSeleccionado.setBackgroundResource(R.drawable.bnt_categoria);
        }

        botonNuevo.setSelected(true);
        botonNuevo.setTextColor(Color.WHITE);
        botonNuevo.setBackgroundResource(R.drawable.bnt_categoria);

        botonSeleccionado = botonNuevo;
    }

    private void filtrarTareasPorCategoria(String categoria) {
        List<Tarea> tareasFiltradas;
        if (categoria.equalsIgnoreCase("Todas")) {
            tareasFiltradas = new ArrayList<>(tareaInfoList);
        } else {
            tareasFiltradas = tareaInfoList.stream()
                    .filter(tarea -> tarea.getCategoria().equalsIgnoreCase(categoria))
                    .collect(Collectors.toList());
        }

        if (tareasFiltradas.isEmpty() && !categoria.equalsIgnoreCase("Todas")) {
            Toast.makeText(getContext(), "No hay tareas en esta categoría", Toast.LENGTH_SHORT).show();
        }

        adaptar.actualizarListaTareas(tareasFiltradas);
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void agregarBotonTodasLasCategorias() {
        Button button = new Button(requireContext());
        button.setText("Todas");
        agregarBotonCategoria("Todas");
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarTareas(); // Recargar datos solo cuando se regrese al fragmento
    }

    private void setUpRecyclerView() {
        RecyclerView rvTareasInfo = rootView.findViewById(R.id.rvTareas);
        rvTareasInfo.setLayoutManager(new LinearLayoutManager(requireContext()));

        adaptar = new TareaAdapter(tareaInfoList);
        rvTareasInfo.setAdapter(adaptar);

        adaptar.setOnTareaClickListener(tarea -> {
            Intent intent = new Intent(requireActivity(), ActivityDetalleTarea.class);
            intent.putExtra("TAREA_ID", tarea.getId());
            startActivityForResult(intent, REQUEST_CODE_ACTUALIZAR_TAREA);
        });
    }

    private void mostrarDatePicker(EditText etFecha) {
        // Configura el idioma a español
        Locale locale = new Locale("es", "ES");
        Locale.setDefault(locale);
        final Calendar calendar = Calendar.getInstance(locale);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String fecha = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    etFecha.setText(fecha);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void mostrarTimePicker(EditText etHora) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> {
                    String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    etHora.setText(hora);
                }, hour, minute, true);
        timePickerDialog.show();
    }
    //metodos para cerrar sesion
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflar el menú
        inflater.inflate(R.menu.menu_toolbar_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.cerrar_sesion) {
            // Cierra la sesión
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirige a la pantalla de login
            Intent intent = new Intent(getActivity(), ActivityLogin.class);
            startActivity(intent);
            getActivity().finish();  // Finaliza la actividad actual
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        firestoreManager.removeListeners();
    }
}