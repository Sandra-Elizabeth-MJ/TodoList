package com.example.todolist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.todolist.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ActivityRegistrarse extends AppCompatActivity {

     FirebaseAuth auth;
     EditText signupNombre,signupApellidos,signupEmail, signupPassword;
     Button signupButton;
     TextView loginRedirectText;
     ImageView passwordShowRegis;
     boolean passwordVisible = false; // Estado de visibilidad
     FirebaseFirestore mfirestore;// Instancia para interactuar con la base de datos en Firebase Firestore.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        signupNombre = findViewById(R.id.registrarse_nombre);
        signupApellidos=findViewById(R.id.registrarse_apellidos);
        signupEmail = findViewById(R.id.registrarse_email);
        signupPassword = findViewById(R.id.registrarse_password);
        signupButton = findViewById(R.id.registrarse_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        passwordShowRegis = findViewById(R.id.registro_show_pass);

        //Inicialisamos servicios
        auth = FirebaseAuth.getInstance();
        mfirestore = FirebaseFirestore.getInstance();

            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Recupera los valores ingresados por el usuario en los campos de texto y los almacena en variables.
                    String nombre =signupNombre.getText().toString().trim();
                    String apellidos = signupApellidos.getText().toString().trim();
                    String email = signupEmail.getText().toString().trim();
                    String pass = signupPassword.getText().toString().trim();
                    if (email.isEmpty() && pass.isEmpty() && nombre.isEmpty() && apellidos.isEmpty())
                        Toast.makeText(ActivityRegistrarse.this, "Completar los datos", Toast.LENGTH_SHORT).show();
                    else{
                        registreUser(email,pass,nombre,apellidos);
                    }

                }
            });
        //Ojo para visualizar la contraseña
        passwordShowRegis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (passwordVisible) {
                    // Si está visible, oculta la contraseña
                    signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    //passwordShow.setImageResource(R.drawable.ic_eye); // Cambia el ícono
                } else {
                    // Si está oculta, muestra la contraseña
                    signupPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    //passwordShow.setImageResource(R.drawable.ic_eye); // Cambia el ícono
                }
                // Cambia el estado
                passwordVisible = !passwordVisible;
                // Mueve el cursor al final del texto
                signupPassword.setSelection(signupPassword.getText().length());
            }
        });

            loginRedirectText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent inten = new Intent(getApplicationContext(),ActivityLogin.class);
                    startActivity(inten);
                    finish();
                }
            });




    }
    private void registreUser(String email, String password, String nombre, String apellidos) {
        //Llama a createUserWithEmailAndPassword de Firebase para crear un nuevo usuario con el correo y contraseña.
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {  // Verificar si la creación del usuario fue exitosa
                    String id = auth.getCurrentUser().getUid();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", id);
                    map.put("email", email);
                    map.put("contraseña", password);
                    map.put("nombre", nombre);
                    map.put("apellidos", apellidos);

                    mfirestore.collection("user").document(id).set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d("ActivityRegistrarse", "Datos del usuario guardados en Firestore");
                            Toast.makeText(ActivityRegistrarse.this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();

                            // Asegurarnos de que el usuario esté completamente registrado antes de redirigir
                            auth.signOut(); // Cerramos la sesión del registro

                            Intent loginIntent = new Intent(ActivityRegistrarse.this, ActivityLogin.class);
                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(loginIntent);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("ActivityRegistrarse", "Error al guardar en Firestore: " + e.getMessage());
                            Toast.makeText(ActivityRegistrarse.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Si la creación del usuario falla
                    Toast.makeText(ActivityRegistrarse.this, "Error al registrar: " + Objects.requireNonNull(task.getException()).getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}