package com.example.todolist.services;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.todolist.entities.Tarea;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
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
    private static final int TAREAS_POR_PAGINA = 7;
    private DocumentSnapshot lastVisible = null;
    private String currentCategoria = null;

    //Para los Log
    private static final String TAG = "FirestoreManager";


    // Para controlar la categoría actual
    public void getTareas(FirestoreCallback<List<Tarea>> callback) {
        String userId = auth.getCurrentUser().getUid();

        ListenerRegistration listener = db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", false) // Añadir este filtro
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
    // Nuevo método para cargar tareas progresivamente
    public void getTareasPaginadas(String categoria, boolean reiniciarPaginacion, FirestoreCallback<List<Tarea>> callback) {
        String userId = auth.getCurrentUser().getUid();

        // Reiniciar paginación si es una nueva categoría o se solicita explícitamente
        if (reiniciarPaginacion || !categoria.equals(currentCategoria)) {
            lastVisible = null;
            currentCategoria = categoria;
        }

        // Construir la consulta base
        Query query = db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", false)
                .orderBy("fecha") // Asegúrate de tener un índice para esta consulta
                .limit(TAREAS_POR_PAGINA);

        // Agregar filtro por categoría si no es "Todas"
        if (!categoria.equals("Todas")) {
            query = query.whereEqualTo("categoria", categoria);
        }

        // Agregar punto de inicio si no es la primera página
        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Tarea> tareas = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Tarea tarea = document.toObject(Tarea.class);
                tarea.setId(document.getId());
                tareas.add(tarea);
            }

            // Actualizar el último documento visible
            if (!queryDocumentSnapshots.isEmpty()) {
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
            }

            callback.onSuccess(tareas);
        }).addOnFailureListener(callback::onError);
    }
    public void getNextTareas(FirestoreCallback<List<Tarea>> callback) {
        if (lastVisible == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Query query = db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", false)
                .orderBy("fecha")
                .startAfter(lastVisible)
                .limit(TAREAS_POR_PAGINA);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Tarea> tareas = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Tarea tarea = document.toObject(Tarea.class);
                tarea.setId(document.getId());
                tareas.add(tarea);
            }

            if (!queryDocumentSnapshots.isEmpty()) {
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
            }

            callback.onSuccess(tareas);
        }).addOnFailureListener(callback::onError);
    }

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
                Log.d(TAG, "Conexión de red disponible.");
                // Aquí puedes implementar lógica adicional cuando la conexión se restablece
            }

            @Override
            public void onLost(@NonNull Network network) {
                isOnline = false;
                Log.d(TAG, "Conexión de red perdida.");
                // Aquí puedes implementar lógica adicional cuando se pierde la conexión
            }
        });
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

    public void marcarTareaComoCompletada(Tarea tarea, FirestoreCallback<Void> callback) {
        String userId = auth.getCurrentUser().getUid();
        tarea.setCompletada(true);
        tarea.setFechaCompletada(new java.text.SimpleDateFormat("yyyy/MM/dd",
                java.util.Locale.getDefault()).format(new java.util.Date()));

        db.collection("user").document(userId)
                .collection("tareas")
                .document(tarea.getId())
                .set(tarea)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void getTareasCompletadas(FirestoreCallback<List<Tarea>> callback) {
        String userId = auth.getCurrentUser().getUid();
        ListenerRegistration listener = db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", true)
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
    public void deleteAllCompletedTasks(FirestoreCallback<Void> callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Create a batch operation to delete all documents at once
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        batch.delete(document.getReference());
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
    //Metodo conteo tareas completadas y pendientes
    public void getTaskCount(boolean isCompleted, FirestoreCallback<Long> callback) {
        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Obteniendo el conteo de tareas para completadas=" + isCompleted + ", userId=" + userId);

        db.collection("user").document(userId)
                .collection("tareas")
                .whereEqualTo("completada", isCompleted)
                .count()
                .get(AggregateSource.SERVER)  // Especificamos la fuente para la consulta de agregación
                .addOnSuccessListener(snapshot -> {
                    long count = snapshot.getCount();
                    Log.d(TAG, "Conteo obtenido con éxito: " + count + " para completadas=" + isCompleted);
                    callback.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al obtener el conteo para completadas=" + isCompleted, e);
                    callback.onError(e);
                });
    }

    public boolean isOnline() {
        return isOnline;
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
