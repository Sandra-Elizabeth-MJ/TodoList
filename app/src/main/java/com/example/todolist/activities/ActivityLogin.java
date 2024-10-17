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

import com.example.todolist.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;
import com.example.todolist.R;

public class ActivityLogin extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    TextView signupRedirectText;
    Button loginButton;
    FirebaseAuth auth;
    FirebaseFirestore mfirestore;
    ImageView passwordShow;
    boolean isPasswordVisible = false; // Estado de visibilidad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.registrarseRedirectText);
        passwordShow = findViewById(R.id.show_password);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = loginEmail.getText().toString().trim();
                String pass = loginPassword.getText().toString().trim();
                if (email.isEmpty() && pass.isEmpty())
                    Toast.makeText(ActivityLogin.this, "Ingresar los datos", Toast.LENGTH_SHORT).show();
                else{
                    loginUser(email,pass);
                }


            }
        });
        //Ojo para visualizar la contraseña
        passwordShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPasswordVisible) {
                    // Si está visible, oculta la contraseña
                    loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    //passwordShow.setImageResource(R.drawable.ic_eye); // Cambia el ícono
                } else {
                    // Si está oculta, muestra la contraseña
                    loginPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    //passwordShow.setImageResource(R.drawable.ic_eye); // Cambia el ícono
                }
                // Cambia el estado
                isPasswordVisible = !isPasswordVisible;
                // Mueve el cursor al final del texto
                loginPassword.setSelection(loginPassword.getText().length());
            }
        });
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent inten = new Intent(getApplicationContext(),ActivityRegistrarse.class);
                startActivity(inten);
                finish();

            }
        });
    }
    private void loginUser(String emailuser, String contrauser) {
        auth.signInWithEmailAndPassword(emailuser, contrauser).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    finish();
                    startActivity(new Intent(ActivityLogin.this, MainActivity.class));
                    Toast.makeText(ActivityLogin.this,"Bienvenido", Toast.LENGTH_SHORT).show();

                }
                else
                    Toast.makeText(ActivityLogin.this,"Error",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ActivityLogin.this, "Error al inciar sesión", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Verifica si el usuario ya está autenticado
        if (auth.getCurrentUser() != null) {
            // Si el usuario ya está autenticado, redirige a MainActivity
            startActivity(new Intent(ActivityLogin.this, MainActivity.class));
            finish(); // Finaliza esta actividad para que no pueda regresar a la pantalla de login
        }
    }



}