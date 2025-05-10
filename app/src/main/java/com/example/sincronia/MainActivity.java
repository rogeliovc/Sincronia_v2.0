package com.example.sincronia;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Verificación de sesión
        AuthManager authManager = new AuthManager(this);
        boolean valid = authManager.isTokenValid();
        if (!valid) {
            boolean refreshed = authManager.refreshAccessToken();
            if (!refreshed || !authManager.isTokenValid()) {
                // Redirigir a LoginActivity
                startActivity(new android.content.Intent(this, LoginActivity.class));
                finish();
                return;
            }
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sincronía");
        }

        // Mostrar fragmento por defecto (Home)
        if (savedInstanceState == null) {
            showMainFragment(new HomeFragment());
        }

        // Mostrar MiniPlayer siempre
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.mini_player_container, new MiniPlayerFragment())
            .commit();

        // Configurar navegación de tabs
        ImageButton tabPlayer = findViewById(R.id.tab_player);
        ImageButton tabHome = findViewById(R.id.tab_home);
        ImageButton tabTasks = findViewById(R.id.tab_tasks);

        tabPlayer.setOnClickListener(v -> showMainFragment(new PlayerFragment()));
        tabHome.setOnClickListener(v -> showMainFragment(new HomeFragment()));
        tabTasks.setOnClickListener(v -> showMainFragment(new TasksFragment()));
    }

    private void showMainFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_content_container, fragment)
            .commit();
    }
}