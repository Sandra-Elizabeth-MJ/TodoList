package com.example.todolist.Fragmentos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.activities.ActivityDetalleTarea;
import com.example.todolist.adapters.TareaCalendarioAdapter;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.ClienteAPI;
import com.example.todolist.services.FirestoreManager;
import com.example.todolist.services.ServicioAPI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarioFragment extends Fragment {
    private CalendarView calendario;
    private RecyclerView recyclerView;
    private TextView tvNoTareas;
    private TareaCalendarioAdapter adapter;
    private List<Tarea> tareas;
    private HashMap<String, List<Tarea>> tareasPorFecha;
    //private final SimpleDateFormat formatoAPI = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private FirestoreManager firestoreManager;
    private static final int REQUEST_CODE_ACTUALIZAR_TAREA = 1;

    private final SimpleDateFormat formatoAPI = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Date fechaSeleccionadaActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);
        firestoreManager = FirestoreManager.getInstance(requireContext());

        calendario = view.findViewById(R.id.calendario);
        recyclerView = view.findViewById(R.id.rvTareasCalendario);
        tvNoTareas = new TextView(getContext());
        tvNoTareas.setText("No hay tareas en el día seleccionado");
        tvNoTareas.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvNoTareas.setVisibility(View.GONE);

        ((ViewGroup) recyclerView.getParent()).addView(tvNoTareas);

        tareas = new ArrayList<>();
        tareasPorFecha = new HashMap<>();
        fechaSeleccionadaActual = new Date(); // Inicializar con la fecha actual

        configurarRecyclerView();
        configurarCalendario();
        obtenerTareas();

        return view;
    }

    private void configurarCalendario() {
        // Configurar el rango de fechas visible
        Calendar calendarMin = Calendar.getInstance();
        calendarMin.add(Calendar.YEAR, -1);
        calendario.setMinDate(calendarMin.getTimeInMillis());

        Calendar calendarMax = Calendar.getInstance();
        calendarMax.add(Calendar.YEAR, 2); // Aumentamos a 2 años para ver más adelante
        calendario.setMaxDate(calendarMax.getTimeInMillis());

        calendario.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            fechaSeleccionadaActual = selectedCalendar.getTime();

            String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    dayOfMonth, month + 1, year);
            mostrarTareas(fechaSeleccionada);
        });
    }

    private void organizarTareasPorFecha() {
        tareasPorFecha.clear();
        for (Tarea tarea : tareas) {
            try {
                // Parsear la fecha de la tarea
                Date fechaTarea = formatoAPI.parse(tarea.getFecha());
                if (fechaTarea != null) {
                    String fechaFormateada = formatoAPI.format(fechaTarea);
                    if (!tareasPorFecha.containsKey(fechaFormateada)) {
                        tareasPorFecha.put(fechaFormateada, new ArrayList<>());
                    }
                    tareasPorFecha.get(fechaFormateada).add(tarea);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void mostrarTareas(String fecha) {
        List<Tarea> tareasDelDia = tareasPorFecha.get(fecha);

        if (tareasDelDia == null || tareasDelDia.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoTareas.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoTareas.setVisibility(View.GONE);
            // Ordenar las tareas por hora antes de mostrarlas
            tareasDelDia.sort((t1, t2) -> {
                try {
                    SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Date hora1 = formatoHora.parse(t1.getHora());
                    Date hora2 = formatoHora.parse(t2.getHora());
                    return hora1.compareTo(hora2);
                } catch (ParseException e) {
                    return 0;
                }
            });
            adapter.actualizarTareas(tareasDelDia);
        }
    }

    private void obtenerTareas() {
        firestoreManager.getTareas(new FirestoreManager.FirestoreCallback<List<Tarea>>() {
            @Override
            public void onSuccess(List<Tarea> result) {
                if (isAdded()) {
                    tareas = result;
                    organizarTareasPorFecha();

                    // Mostrar las tareas de la fecha seleccionada actual
                    String fechaActual = formatoAPI.format(fechaSeleccionadaActual);
                    mostrarTareas(fechaActual);

                    // Marcar días con tareas en el calendario
                    getActivity().runOnUiThread(() -> configurarFechasConTareas());
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Error al obtener tareas: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void configurarFechasConTareas() {
        if (getActivity() == null) return;

        // Actualizar la UI del calendario para mostrar las fechas con tareas
        calendario.getDisplay().toString(); // Forzar actualización del calendario
    }
    private void configurarRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TareaCalendarioAdapter(new ArrayList<>());

        // Set up click listener
        adapter.setOnTareaClickListener(tarea -> {
            Intent intent = new Intent(requireActivity(), ActivityDetalleTarea.class);
            intent.putExtra("TAREA_ID", tarea.getId());
            startActivityForResult(intent, REQUEST_CODE_ACTUALIZAR_TAREA);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        obtenerTareas();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (firestoreManager != null) {
            firestoreManager.removeListeners();
        }
    }
}