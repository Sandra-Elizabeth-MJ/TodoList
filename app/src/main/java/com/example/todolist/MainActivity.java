package com.example.todolist;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todolist.Fragmentos.CalendarioFragment;
import androidx.fragment.app.Fragment;
import com.example.todolist.Fragmentos.TareaFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TareaFragment())
                    .commit();
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_tareas) {
                selectedFragment = new TareaFragment();
            }
            else if (itemId == R.id.navigation_calendario) {
                selectedFragment = new CalendarioFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}