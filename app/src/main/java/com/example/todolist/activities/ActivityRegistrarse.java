package com.example.todolist.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.example.todolist.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class ActivityRegistrarse extends AppCompatActivity {

     FirebaseAuth auth;
     EditText signupEmail, signupPassword;
     Button signupButton;
     TextView loginRedirectText;
     ImageView passwordShowRegis;
     boolean passwordVisible = false; // Estado de visibilidad
     FirebaseFirestore mfirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrarse);

        signupEmail = findViewById(R.id.registrarse_email);
        signupPassword = findViewById(R.id.registrarse_password);
        signupButton = findViewById(R.id.registrarse_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        passwordShowRegis = findViewById(R.id.registro_show_pass);

        //Instanciara para ver datos personales
        auth = FirebaseAuth.getInstance();
        mfirestore = FirebaseFirestore.getInstance();

            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String email = signupEmail.getText().toString().trim();
                    String pass = signupPassword.getText().toString().trim();
                    if (email.isEmpty() && pass.isEmpty())
                        Toast.makeText(ActivityRegistrarse.this, "Completar los datos", Toast.LENGTH_SHORT).show();
                    else{
                        registreUser(email,pass);
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
    private void registreUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                String id = auth.getCurrentUser().getUid();
                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("email", email);
                map.put("contraseña", password);

                mfirestore.collection("user").document(id).set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        finish();
                        startActivity(new Intent(ActivityRegistrarse.this,ActivityLogin.class));
                        Toast.makeText(ActivityRegistrarse.this, "Usuario registrado con exito", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ActivityRegistrarse.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ActivityRegistrarse.this, "ERROR AL REGISTRAR", Toast.LENGTH_SHORT).show();
            }
        });
    }

}