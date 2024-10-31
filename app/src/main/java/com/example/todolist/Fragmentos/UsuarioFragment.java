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
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.activities.ActivityLogin;
import com.google.firebase.auth.FirebaseAuth;


public class UsuarioFragment extends Fragment {

    private View rootView;
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
        // Inflar el menú
        setHasOptionsMenu(true);

        return rootView;

    }
    //metodos para cerrar sesion
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflar el menú
        inflater.inflate(R.menu.menu_toolbar_usuario, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.cerrar_sesion) {
            // Cierra la sesión
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(), "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirige a la pantalla de login
            Intent intent = new Intent(getActivity(), ActivityLogin.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();  // Finaliza la actividad actual
            }  // Finaliza la actividad actual
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}