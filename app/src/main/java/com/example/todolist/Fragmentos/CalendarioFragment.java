package com.example.todolist.Fragmentos;

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

import com.example.todolist.R;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.ClienteAPI;
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
    private ListView listaTareas;
    private List<Tarea> tareas;
    private HashMap<String, List<Tarea>> tareasPorFecha;
    private SimpleDateFormat formatoAPI = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Calendar calendarInstance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        calendario = view.findViewById(R.id.calendario);
        listaTareas = view.findViewById(R.id.lista_tareas);
        tareas = new ArrayList<>();
        tareasPorFecha = new HashMap<>();
        calendarInstance = Calendar.getInstance(TimeZone.getDefault());

        configurarCalendario();
        obtenerTareas();

        return view;
    }

    private void configurarCalendario() {
        calendario.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            mostrarTareas(fechaSeleccionada);
        });
    }

    private void obtenerTareas() {
        ServicioAPI servicioAPI = ClienteAPI.obtenerInstancia();

        Call<List<Tarea>> llamada = servicioAPI.obtenerTareas();
        llamada.enqueue(new Callback<List<Tarea>>() {
            @Override
            public void onResponse(Call<List<Tarea>> call, Response<List<Tarea>> response) {
                if (response.isSuccessful() && isAdded()) {
                    tareas = response.body();
                    organizarTareasPorFecha();
                    configurarFechasConTareas();

                    // Mostrar tareas para la fecha actual
                    String fechaActual = formatoAPI.format(new Date());
                    mostrarTareas(fechaActual);
                } else if (isAdded()) {
                    Toast.makeText(requireActivity(), "Error al obtener tareas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Tarea>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireActivity(), "Error de red", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void organizarTareasPorFecha() {
        tareasPorFecha.clear();
        for (Tarea tarea : tareas) {
            String fecha = tarea.getFecha();
            if (!tareasPorFecha.containsKey(fecha)) {
                tareasPorFecha.put(fecha, new ArrayList<Tarea>());
            }
            tareasPorFecha.get(fecha).add(tarea);
        }
    }

    private void configurarFechasConTareas() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                // Crear un calendario para la fecha mínima
                Calendar minDate = Calendar.getInstance();
                minDate.add(Calendar.YEAR, -1); // Un año atrás
                calendario.setMinDate(minDate.getTimeInMillis());

                // Crear un calendario para la fecha máxima
                Calendar maxDate = Calendar.getInstance();
                maxDate.add(Calendar.YEAR, 1); // Un año adelante
                calendario.setMaxDate(maxDate.getTimeInMillis());

                // Establecer el color para fechas normales y fechas con tareas
                calendario.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                    String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    mostrarTareas(fechaSeleccionada);
                });

                // Recorrer todas las fechas y marcar las que tienen tareas
                for (String fechaStr : tareasPorFecha.keySet()) {
                    try {
                        Date fecha = formatoAPI.parse(fechaStr);
                        if (fecha != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(fecha);

                            // Establecer la fecha en el calendario
                            // Esto no "selecciona" la fecha, solo la marca visualmente
                            calendario.setDate(cal.getTimeInMillis(), true, true);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void mostrarTareas(String fecha) {
        if (getContext() == null) return;

        List<Tarea> tareasDelDia = tareasPorFecha.get(fecha);
        if (tareasDelDia == null || tareasDelDia.isEmpty()) {
            ArrayList<String> mensajeNoTareas = new ArrayList<String>();
            mensajeNoTareas.add("No hay tareas para esta fecha");
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    android.R.layout.simple_list_item_1, mensajeNoTareas);
            listaTareas.setAdapter(adapter);
        } else {
            ArrayAdapter<Tarea> adapter = new ArrayAdapter<Tarea>(getContext(),
                    android.R.layout.simple_list_item_1, tareasDelDia) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(android.R.id.text1);
                    Tarea tarea = getItem(position);
                    if (tarea != null) {
                        text.setText(String.format("%s - %s - %s",
                                tarea.getNombre(),
                                tarea.getHora(),
                                tarea.getCategoria()));
                    }
                    return view;
                }
            };
            listaTareas.setAdapter(adapter);
        }
    }
}