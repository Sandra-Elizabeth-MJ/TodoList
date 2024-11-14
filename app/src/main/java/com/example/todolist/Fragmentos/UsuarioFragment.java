package com.example.todolist.Fragmentos;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.activities.ActivityLogin;
import com.example.todolist.entities.Tarea;
import com.example.todolist.services.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;


public class UsuarioFragment extends Fragment {
    private static final String TAG = "UsuarioFragment";
    private View rootView;
    private TextView tvCantidadTareasCompletadas;
    private TextView tvCantidadTareasPendientes;
    private FirestoreManager firestoreManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        rootView = inflater.inflate(R.layout.fragment_usuario, container, false);

        // Inicializar el Toolbar
        Toolbar toolbar = rootView.findViewById(R.id.toolbar_usuario);
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        // Inicializar FirestoreManager
        firestoreManager = FirestoreManager.getInstance(requireContext());

        // Inicializar TextViews
        tvCantidadTareasCompletadas = rootView.findViewById(R.id.tv_cantidad_tc);
        tvCantidadTareasPendientes = rootView.findViewById(R.id.tv_cant_tpendientes);

        // Cargar los conteos
        loadTaskCounts();

        // Inflar el menú
        setHasOptionsMenu(true);

        return rootView;
    }

    private void loadTaskCounts() {
        // Obtener conteo de tareas completadas
        firestoreManager.getTaskCount(true, new FirestoreManager.FirestoreCallback<Long>() {
            @Override
            public void onSuccess(Long count) {
                if (isAdded()) {
                    tvCantidadTareasCompletadas.setText(String.valueOf(count));
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error al cargar conteo de tareas completadas", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Obtener conteo de tareas pendientes
        firestoreManager.getTaskCount(false, new FirestoreManager.FirestoreCallback<Long>() {
            @Override
            public void onSuccess(Long count) {
                if (isAdded()) {
                    tvCantidadTareasPendientes.setText(String.valueOf(count));
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error al cargar conteo de tareas pendientes", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Actualizar los conteos cada vez que el fragmento se reanuda
        loadTaskCounts();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_toolbar_usuario, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), ActivityLogin.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar los listeners de Firestore al destruir la vista
        if (firestoreManager != null) {
            firestoreManager.removeListeners();
        }
    }
}