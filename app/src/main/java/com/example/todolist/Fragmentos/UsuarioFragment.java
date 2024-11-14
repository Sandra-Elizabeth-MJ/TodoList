package com.example.todolist.Fragmentos;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.activities.ActivityLogin;
import com.example.todolist.entities.Tarea;
import com.example.todolist.entities.Usuario;
import com.example.todolist.services.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsuarioFragment extends Fragment {
    private static final String TAG = "UsuarioFragment";
    private View rootView;
    private TextView tvCantidadTareasCompletadas;
    private TextView tvCantidadTareasPendientes;
    private FirestoreManager firestoreManager;
    private TextView tvNombre, tvApellidos, tvEmail;
    private ImageButton btnEditProfile;

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
        tvNombre = rootView.findViewById(R.id.userName);
        tvApellidos = rootView.findViewById(R.id.userapellidos);
        tvEmail = rootView.findViewById(R.id.email);
        btnEditProfile = rootView.findViewById(R.id.editProfileButton);

        // Cargar los conteos
        loadTaskCounts();

        // Cargar datos del usuario
        loadUserData();

        // Configurar el botón de editar
        setupEditButton();

        // Inflar el menú
        setHasOptionsMenu(true);

        return rootView;
    }
    //Metodo conteo de tareas
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

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Obtener datos del usuario desde Firestore
            FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario usuario = documentSnapshot.toObject(Usuario.class);
                            if (usuario != null) {
                                tvNombre.setText(usuario.getNombre());
                                tvApellidos.setText(usuario.getApellidos());
                                tvEmail.setText(usuario.getEmail());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading user data", e);
                    });
        }
    }

    private void setupEditButton() {
        btnEditProfile.setOnClickListener(v -> showEditDialog());
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText etNombre = dialogView.findViewById(R.id.et_edit_nombre);
        EditText etApellidos = dialogView.findViewById(R.id.et_edit_apellidos);

        // Establecer valores actuales
        etNombre.setText(tvNombre.getText());
        etApellidos.setText(tvApellidos.getText());

        builder.setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String newNombre = etNombre.getText().toString().trim();
                    String newApellidos = etApellidos.getText().toString().trim();

                    if (!newNombre.isEmpty() && !newApellidos.isEmpty()) {
                        updateUserProfile(newNombre, newApellidos);
                    } else {
                        Toast.makeText(getContext(), "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUserProfile(String nombre, String apellidos) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre", nombre);
            updates.put("apellidos", apellidos);

            FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        tvNombre.setText(nombre);
                        tvApellidos.setText(apellidos);
                        Toast.makeText(getContext(), "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al actualizar el perfil", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating profile", e);
                    });
        }
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