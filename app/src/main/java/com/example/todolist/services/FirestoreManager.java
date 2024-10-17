package com.example.todolist.services;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.example.todolist.entities.Tarea;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    private static FirestoreManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final Context context;
    private boolean isOnline = false;
    private List<ListenerRegistration> listeners = new ArrayList<>();

    private FirestoreManager(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        setupNetworkCallback();
    }

    public static synchronized FirestoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreManager(context);
        }
        return instance;
    }

    private void setupNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isOnline = true;
                // Aquí puedes implementar lógica adicional cuando la conexión se restablece
            }

            @Override
            public void onLost(@NonNull Network network) {
                isOnline = false;
                // Aquí puedes implementar lógica adicional cuando se pierde la conexión
            }
        });
    }

    public void getTareas(FirestoreCallback<List<Tarea>> callback) {
        String userId = auth.getCurrentUser().getUid();
        ListenerRegistration listener = db.collection("user").document(userId)
                .collection("tareas")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }
                    List<Tarea> tareas = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Tarea tarea = document.toObject(Tarea.class);
                        tarea.setId(document.getId());
                        tareas.add(tarea);
                    }
                    callback.onSuccess(tareas);
                });
        listeners.add(listener);
    }

    public void createTarea(Tarea tarea, FirestoreCallback<String> callback) {
        String userId = auth.getCurrentUser().getUid();
        DocumentReference newTareaRef = db.collection("user").document(userId)
                .collection("tareas")
                .document();  // Esto genera un nuevo ID inmediatamente

        // Asigna el ID generado a la tarea
        String newTareaId = newTareaRef.getId();
        tarea.setId(newTareaId);

        newTareaRef.set(tarea)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newTareaId))
                .addOnFailureListener(callback::onError);
    }

    public void updateTarea(Tarea tarea, FirestoreCallback<Void> callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user").document(userId)
                .collection("tareas")
                .document(tarea.getId())
                .set(tarea)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteTarea(String tareaId, FirestoreCallback<Void> callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user").document(userId)
                .collection("tareas")
                .document(tareaId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void getCategorias(FirestoreCallback<List<String>> callback) {
        String userId = auth.getCurrentUser().getUid();
        ListenerRegistration listener = db.collection("user").document(userId)
                .collection("categorias")
                .whereEqualTo("userId", userId)  // Asegura que solo obtenemos las categorías del usuario actual
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }
                    List<String> categorias = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String nombre = document.getString("nombre");
                        if (nombre != null && !categorias.contains(nombre)) {
                            categorias.add(nombre);
                        }
                    }
                    callback.onSuccess(categorias);
                });
        listeners.add(listener);
    }

    public void createCategoria(String categoria, FirestoreCallback<String> callback) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> categoriaMap = new HashMap<>();
        categoriaMap.put("nombre", categoria);
        categoriaMap.put("userId", userId);

        DocumentReference newCategoriaRef = db.collection("user").document(userId)
                .collection("categorias")
                .document();

        String newCategoriaId = newCategoriaRef.getId();

        newCategoriaRef.set(categoriaMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newCategoriaId))
                .addOnFailureListener(callback::onError);
    }

    public void removeListeners() {
        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }
        listeners.clear();
    }

    public boolean isOnline() {
        return isOnline;
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
