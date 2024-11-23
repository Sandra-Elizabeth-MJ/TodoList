package com.example.todolist.Fragmentos;



import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.activities.ActivityAdministarCat;
import com.example.todolist.activities.ActivityDetalleTarea;
import com.example.todolist.activities.ActivityLogin;
import com.example.todolist.activities.ActivityTareaCompletada;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TareaFragment extends Fragment {
    private static final String TAG = "TareaFragment"; // Tag para los logs
    private Spinner spinnerCategories;
    private CategoriaAdapter spinnerAdapter;
    private List<String> categorias = new ArrayList<>();
    List<Tarea> tareaInfoList = new ArrayList<>();
    TareaAdapter adaptar;
    private LinearLayout linearLayoutCategorias;
    private static final int REQUEST_CODE_ACTUALIZAR_TAREA = 1;
    private View rootView;
    private Button botonSeleccionado = null;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private FirestoreManager firestoreManager;

    private boolean isLoading = false;
    private boolean hasMoreTareas = true;
    private ProgressBar progressBar;
    private String currentCategoria; // Categoría por defecto

    private SearchView searchView;
    private String currentSearchQuery = "";

    private MediaPlayer mediaPlayer;
    private String ultimaCategoriaSeleccionada = "Todas"; // Valor por defecto
    private static final String PREF_NAME = "TareaFragmentPrefs";
    private static final String KEY_LAST_CATEGORY = "lastSelectedCategory";
    // Agregar esta variable al inicio de la clase
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_tarea, container, false);
        linearLayoutCategorias = rootView.findViewById(R.id.linearLayoutCategorias);
        progressBar = rootView.findViewById(R.id.progressbar_tareas);
        firestoreManager = FirestoreManager.getInstance(requireContext());

        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();

        // Cargar la última categoría seleccionada desde SharedPreferences
        ultimaCategoriaSeleccionada = getLastSelectedCategory();
        currentCategoria = ultimaCategoriaSeleccionada;

        // Inicializar variables de paginación
        isLoading = false;
        hasMoreTareas = true;
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.task_complete);

        initializeSpinnerAdapter();
        setUpRecyclerView();
        lanzarAddTarea();
        cargarTareas();

        //Inicializar el Toolbar
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }
        setHasOptionsMenu(true);

        return rootView;
    }
    // Método para guardar la última categoría seleccionada
    private void saveLastSelectedCategory(String category) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_CATEGORY, category);
        editor.apply();
    }

    // Método para obtener la última categoría seleccionada
    private String getLastSelectedCategory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_CATEGORY, "Todas");
    }
    private void filtrarTareasPorCategoria(String categoria) {
        currentCategoria = categoria;
        tareaInfoList.clear();
        adaptar.actualizarListaTareas(new ArrayList<>());
        hasMoreTareas = true;
        cargarTareasPaginadas(true); // true para reiniciar paginación
    }
    // Modificar el método cargarTareasPaginadas para incluir la búsqueda
    private void cargarTareasPaginadas(boolean reiniciarPaginacion) {
        if (isLoading || !hasMoreTareas) {
            Log.d(TAG, "Carga de tareas ignorada - isLoading: " + isLoading +
                    ", hasMoreTareas: " + hasMoreTareas);
            return;
        }

        Log.d(TAG, "Iniciando carga paginada - reiniciarPaginacion: " + reiniciarPaginacion +
                ", búsqueda activa: " + !currentSearchQuery.isEmpty());

        showLoading();
        isLoading = true;

        if (!currentSearchQuery.isEmpty()) {
            Log.d(TAG, "Ejecutando búsqueda paginada con query: '" + currentSearchQuery + "'");
            firestoreManager.searchTareasPaginadas(currentSearchQuery, currentCategoria, reiniciarPaginacion,
                    new FirestoreManager.FirestoreCallback<List<Tarea>>() {
                        @Override
                        public void onSuccess(List<Tarea> result) {
                            Log.d(TAG, "Búsqueda paginada exitosa");
                            procesarResultadosPaginacion(result, reiniciarPaginacion);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error en búsqueda paginada: " + e.getMessage(), e);
                            manejarErrorPaginacion(e);
                        }
                    });
        } else {
            Log.d(TAG, "Ejecutando carga normal de tareas");
            firestoreManager.getTareasPaginadas(currentCategoria, reiniciarPaginacion,
                    new FirestoreManager.FirestoreCallback<List<Tarea>>() {
                        @Override
                        public void onSuccess(List<Tarea> result) {
                            Log.d(TAG, "Carga normal de tareas exitosa");
                            procesarResultadosPaginacion(result, reiniciarPaginacion);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error en carga normal: " + e.getMessage(), e);
                            manejarErrorPaginacion(e);
                        }
                    });
        }
    }

    private void procesarResultadosPaginacion(List<Tarea> result, boolean reiniciarPaginacion) {
        try {
            Log.d(TAG, "Procesando resultados de paginación - tamaño: " +
                    (result != null ? result.size() : 0));

            hideLoading();
            if (result.isEmpty()) {
                hasMoreTareas = false;
                if (reiniciarPaginacion && tareaInfoList.isEmpty()) {
                    Log.d(TAG, "No hay tareas en esta categoría");
                    Toast.makeText(requireContext(),
                            "No hay tareas en esta categoría",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                if (reiniciarPaginacion) {
                    Log.d(TAG, "Limpiando lista existente antes de agregar nuevos resultados");
                    tareaInfoList.clear();
                }
                tareaInfoList.addAll(result);
                Log.d(TAG, "Lista actualizada - total de tareas: " + tareaInfoList.size());
                adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
            }
            isLoading = false;

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar resultados de paginación: " + e.getMessage(), e);
            manejarErrorPaginacion(e);
        }
    }

    private void manejarErrorPaginacion(Exception e) {
        Log.e(TAG, "Error en paginación: " + e.getMessage(), e);
        hideLoading();
        isLoading = false;
        Toast.makeText(requireContext(),
                "Error al cargar tareas: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void initializeSpinnerAdapter() {
        categorias = new ArrayList<>();
        spinnerAdapter = new CategoriaAdapter(requireContext(), categorias);
    }
    //Metodo para cargar 300 tareas
    private void createInitialTasks() {
        String[] categorias = {"Cumpleaños", "Trabajo", "Diario"};
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Obtener la fecha actual
        Calendar currentDate = Calendar.getInstance();
        int currentYear = currentDate.get(Calendar.YEAR);

        for (int i = 1; i <= 300; i++) {
            // Seleccionar categoría de forma rotativa
            String categoria = categorias[(i - 1) % 3];

            // Generar fecha futura aleatoria
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate.getTime()); // Comenzar desde la fecha actual

            // Añadir días aleatorios (entre 0 y 365 días)
            int diasAdicionales = (int)(Math.random() * 365);
            cal.add(Calendar.DAY_OF_YEAR, diasAdicionales);

            // Formatear la fecha
            String fecha = String.format(Locale.getDefault(), "%02d/%02d/%d",
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR));

            // Generar hora aleatoria
            int hora = (int)(Math.random() * 24);
            int minuto = (int)(Math.random() * 60);
            String horaStr = String.format(Locale.getDefault(), "%02d:%02d", hora, minuto);

            // Crear nombre de tarea según la categoría y fecha
            String nombreTarea = "";
            switch (categoria) {
                case "Cumpleaños":
                    nombreTarea = "Cumpleaños de Persona " + i + " (" + fecha + ")";
                    break;
                case "Trabajo":
                    nombreTarea = "Tarea laboral #" + i + " para el " + fecha;
                    break;
                case "Diario":
                    nombreTarea = "Actividad diaria " + i + " - " + fecha;
                    break;
            }

            // Crear y guardar la tarea
            final Tarea nuevaTarea = new Tarea(nombreTarea, fecha, horaStr, categoria);
            nuevaTarea.setUserId(userId);

            // Usar un final counter para tracking
            final int taskNumber = i;

            firestoreManager.createTarea(nuevaTarea, new FirestoreManager.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String newTareaId) {
                    if (taskNumber == 300) {
                        // Notificar cuando se complete la última tarea
                        Handler  mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            Toast.makeText(requireContext(),
                                    "Se completó la creación de todas las tareas",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("CreateInitialTasks", "Error al crear tarea " + taskNumber + ": " + e.getMessage());
                }
            });
        }

        Toast.makeText(requireContext(), "Iniciando creación de 300 tareas...", Toast.LENGTH_LONG).show();
    }

    //dialog de tareas
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
    private void guardarNuevaCategoria(String nombreCategoria) {
        firestoreManager.createCategoria(nombreCategoria, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String newCategoriaId) {
                Toast.makeText(requireActivity(), "Categoría creada con éxito", Toast.LENGTH_SHORT).show();
                // La UI se actualizará automáticamente gracias al SnapshotListener en getCategorias
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(requireActivity(), "Error al crear categoría: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void crearTarea(String nombre, String fecha, String hora, String categoriaSeleccionada) {
        Tarea nuevaTarea = new Tarea(nombre, fecha, hora, categoriaSeleccionada);
        nuevaTarea.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        firestoreManager.createTarea(nuevaTarea, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String newTareaId) {
                Toast.makeText(getContext(), "Tarea creada con éxito", Toast.LENGTH_SHORT).show();
                // Aquí puedes actualizar tu lista local si es necesario
                nuevaTarea.setId(newTareaId);
                tareaInfoList.add(nuevaTarea);
                cargarTareas();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireActivity(), "Error al crear tarea: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarCategorias() {
        if (spinnerAdapter == null) {
            initializeSpinnerAdapter();
        }
        firestoreManager.getCategorias(new FirestoreManager.FirestoreCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                categorias.clear();
                categorias.addAll(result);
                categorias.add("Crear nueva categoría");
                spinnerAdapter.notifyDataSetChanged();

                // Limpiar todos los botones existentes
                linearLayoutCategorias.removeAllViews();

                // Agregar el botón "Todas" primero
                agregarBotonTodasLasCategorias();

                // Agregar el resto de categorías
                for (String categoria : categorias) {
                    if (!categoria.equals("Crear nueva categoría")) {
                        agregarBotonCategoria(categoria);
                    }
                }

                // Seleccionar la última categoría guardada
                new Handler().postDelayed(() -> {
                    String lastCategory = getLastSelectedCategory();
                    seleccionarBotonPorCategoria(lastCategory);
                }, 100);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireActivity(), "Error al cargar categorías: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarBotonesCategorias() {
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
            //filtrarTareasPorCategoria(nombreCategoria);
        });

        linearLayoutCategorias.addView(button);
    }

    private void cargarTareas() {
        showLoading();
        isLoading = true;
        //currentCategoria = "Todas";
        tareaInfoList.clear();
        adaptar.actualizarListaTareas(new ArrayList<>());
        hasMoreTareas = true;

        firestoreManager.getTareasPaginadas(currentCategoria, true,
                new FirestoreManager.FirestoreCallback<List<Tarea>>() {
                    @Override
                    public void onSuccess(List<Tarea> result) {
                        hideLoading();
                        if (result.isEmpty()) {
                            hasMoreTareas = false;
                            Toast.makeText(requireContext(),
                                    "No hay tareas pendientes",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            tareaInfoList.addAll(result);
                            adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
                            hasMoreTareas = true;
                        }
                        isLoading = false;
                        cargarCategorias();
                    }

                    @Override
                    public void onError(Exception e) {
                        hideLoading();
                        isLoading = false;
                        Toast.makeText(requireContext(),
                                "Error al cargar tareas: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }



    //boton al seleccionar la categoria
    private void seleccionarBoton(Button botonNuevo) {
        if (botonSeleccionado != null) {
            botonSeleccionado.setSelected(false);
            botonSeleccionado.setTextColor(ContextCompat.getColor(requireContext(), R.color.plomoTex));
            botonSeleccionado.setBackgroundResource(R.drawable.bnt_categoria);
        }

        botonNuevo.setSelected(true);
        botonNuevo.setTextColor(Color.WHITE);
        botonNuevo.setBackgroundResource(R.drawable.boton_categoria_presionado);

        botonSeleccionado = botonNuevo;

        String categoria = botonNuevo.getText().toString();
        ultimaCategoriaSeleccionada = categoria;
        saveLastSelectedCategory(categoria); // Guardar la categoría seleccionada
        filtrarTareasPorCategoria(categoria);
    }

    private void seleccionarBotonPorCategoria(String categoria) {
        for (int i = 0; i < linearLayoutCategorias.getChildCount(); i++) {
            View view = linearLayoutCategorias.getChildAt(i);
            if (view instanceof Button) {
                Button button = (Button) view;
                if (button.getText().toString().equals(categoria)) {
                    seleccionarBoton(button);
                    return;
                }
            }
        }
    }
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void agregarBotonTodasLasCategorias() {
        Button button = new Button(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(30, 4, 13, 4);
        button.setLayoutParams(params);

        button.setText("Todas");
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
            filtrarTareasPorCategoria("Todas");
        });

        linearLayoutCategorias.addView(button);

        // Solo seleccionar el botón "Todas" si es la última categoría seleccionada
        if (ultimaCategoriaSeleccionada.equals("Todas")) {
            seleccionarBoton(button);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String lastCategory = getLastSelectedCategory();
        currentCategoria = lastCategory;
        cargarTareas();
        cargarCategorias();
    }


    private void setUpRecyclerView() {
        RecyclerView rvTareasInfo = rootView.findViewById(R.id.rvTareas);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvTareasInfo.setLayoutManager(layoutManager);

        adaptar = new TareaAdapter(tareaInfoList);
        rvTareasInfo.setAdapter(adaptar);

        rvTareasInfo.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isLoading && hasMoreTareas && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                        cargarTareasPaginadas(false);
                    }
                }
            }
        });

        adaptar.setOnTareaClickListener(tarea -> {
            Intent intent = new Intent(requireActivity(), ActivityDetalleTarea.class);
            intent.putExtra("TAREA_ID", tarea.getId());
            startActivityForResult(intent, REQUEST_CODE_ACTUALIZAR_TAREA);
        });

        adaptar.setOnTareaCompletadaListener(tarea -> {
            firestoreManager.marcarTareaComoCompletada(tarea, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {

                    playCompletionSound();
                    // Eliminar la tarea de la lista local
                    tareaInfoList.remove(tarea);
                    adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
                    Toast.makeText(requireContext(), "Tarea completada exitosamente", Toast.LENGTH_SHORT).show();

                    // Si la lista está vacía después de eliminar, recargar para verificar si hay más tareas
                    if (tareaInfoList.isEmpty()) {
                        cargarTareasPaginadas(true);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(requireContext(), "Error al completar la tarea: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        adaptar.setOnTareaDeleteListener(tarea -> {
            firestoreManager.deleteTarea(tarea.getId(), new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Eliminar la tarea de la lista local
                    tareaInfoList.remove(tarea);
                    adaptar.actualizarListaTareas(new ArrayList<>(tareaInfoList));
                    Toast.makeText(requireContext(), "Tarea eliminada exitosamente", Toast.LENGTH_SHORT).show();

                    // Si la lista está vacía después de eliminar, recargar para verificar si hay más tareas
                    if (tareaInfoList.isEmpty()) {
                        cargarTareasPaginadas(true);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(requireContext(), "Error al eliminar la tarea: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void playCompletionSound() {
        try {
            if (mediaPlayer != null) {
                // Si el MediaPlayer está reproduciendo, detenerlo y reiniciarlo
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                }
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("TareaFragment", "Error al reproducir sonido: " + e.getMessage());
        }
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

    //metodos para menut del toolbar
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflar el menú
//        inflater.inflate(R.menu.menu_toolbar_main, menu);
//        super.onCreateOptionsMenu(menu, inflater);
        // Inflar el menú
        inflater.inflate(R.menu.menu_toolbar_main, menu);

        // Obtener el SearchView correctamente
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();

        // Configurar el SearchView
        searchView.setQueryHint("Buscar tareas...");
        searchView.setIconifiedByDefault(true);

        // Manejar eventos del SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryText) {
                Log.d(TAG, "onQueryTextSubmit: Búsqueda enviada: " + queryText);
                realizarBusqueda(queryText);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String queryText) {
                Log.d(TAG, "onQueryTextChange: Texto cambiado: " + queryText);

                // Usar un Handler para debounce
                if (searchHandler != null) {
                    searchHandler.removeCallbacksAndMessages(null);
                }

                if (queryText.length() >= 3) {
                    searchHandler.postDelayed(() -> {
                        Log.d(TAG, "Realizando búsqueda en tiempo real");
                        realizarBusqueda(queryText);
                    }, 300); // Esperar 300ms antes de realizar la búsqueda
                } else if (queryText.isEmpty()) {
                    Log.d(TAG, "Texto de búsqueda vacío, restaurando vista normal");
                    currentSearchQuery = "";
                    cargarTareas();
                }
                return true;
            }
        });

        // Manejar el cierre del SearchView
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                currentSearchQuery = "";
                cargarTareas();
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();

        if (itemId == R.id.action_admin_cat) {
            // Redirige a la actividad de administración de categorías
            intent = new Intent(getActivity(), ActivityAdministarCat.class);
            startActivity(intent);
            Toast.makeText(getActivity(), "Administrar categorías", Toast.LENGTH_SHORT).show();
            return true;

        }
//        else if (itemId == R.id.action_search) {
//            Toast.makeText(getActivity(), "Buscar", Toast.LENGTH_SHORT).show();
//            return true;
//
//        }
        else if (itemId == R.id.action_tareas_com) {
            // Redirige a la actividad de tareas completadas
            intent = new Intent(getActivity(), ActivityTareaCompletada.class);
            startActivity(intent);
            Toast.makeText(getActivity(), "Tareas completadas", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void realizarBusqueda(String queryText) {
        if (queryText == null) {
            Log.e(TAG, "Error: queryText es null");
            return;
        }

        // Evitar búsquedas duplicadas si el texto no ha cambiado
        if (queryText.equals(currentSearchQuery)) {
            return;
        }

        currentSearchQuery = queryText.trim();
        Log.d(TAG, "Iniciando búsqueda con query: '" + currentSearchQuery + "' en categoría: '" + currentCategoria + "'");

        // Asegurarse de que no haya búsquedas simultáneas
        if (isLoading) {
            return;
        }

        showLoading();
        isLoading = true;

        // Limpiar la lista y el adaptador antes de la nueva búsqueda
        tareaInfoList = new ArrayList<>();
        adaptar.actualizarListaTareas(new ArrayList<>());
        hasMoreTareas = true;

        Log.d(TAG, "Estado antes de la búsqueda - isLoading: " + isLoading + ", hasMoreTareas: " + hasMoreTareas);

        firestoreManager.searchTareasPaginadas(currentSearchQuery, currentCategoria, true,
                new FirestoreManager.FirestoreCallback<List<Tarea>>() {
                    @Override
                    public void onSuccess(List<Tarea> result) {
                        Log.d(TAG, "Búsqueda exitosa. Resultados encontrados: " + (result != null ? result.size() : 0));

                        // Usar un Set para eliminar duplicados basados en el ID de la tarea
                        Set<String> tareaIds = new HashSet<>();
                        List<Tarea> resultadosUnicos = new ArrayList<>();

                        if (result != null) {
                            for (Tarea tarea : result) {
                                if (tareaIds.add(tarea.getId())) {
                                    resultadosUnicos.add(tarea);
                                }
                            }
                        }

                        hideLoading();
                        if (resultadosUnicos.isEmpty()) {
                            hasMoreTareas = false;
                            Log.d(TAG, "No se encontraron resultados para la búsqueda");
                            Toast.makeText(requireContext(),
                                    "No se encontraron tareas que coincidan con la búsqueda",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            tareaInfoList = new ArrayList<>(resultadosUnicos);
                            Log.d(TAG, "Actualizando adapter con " + tareaInfoList.size() + " tareas únicas");
                            adaptar.actualizarListaTareas(tareaInfoList);
                        }
                        isLoading = false;
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error en la búsqueda: " + e.getMessage(), e);
                        hideLoading();
                        isLoading = false;
                        Toast.makeText(requireContext(),
                                "Error al buscar tareas: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        // Liberar recursos del MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        firestoreManager.removeListeners();
    }
}