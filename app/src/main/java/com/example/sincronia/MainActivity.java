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

// Asegúrate de tener este intent-filter en tu AndroidManifest.xml:
// <activity android:name=".MainActivity">
//     <intent-filter>
//         <action android:name="android.intent.action.VIEW"/>
//         <category android:name="android.intent.category.DEFAULT"/>
//         <category android:name="android.intent.category.BROWSABLE"/>
//         <data android:scheme="sincronia" android:host="callback"/>
//     </intent-filter>
// </activity>

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializa AuthManager para acceso global
        com.example.sincronia.AuthManager.init(getApplicationContext());
    com.example.sincronia.AuthManager.init(getApplicationContext());
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

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Configurar Toolbar

        // Mostrar fragmento raíz (MainShellFragment)
        if (savedInstanceState == null) {
            showMainFragment(new MainShellFragment());
        }

        // Mostrar MiniPlayer siempre
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.mini_player_container, new MiniPlayerFragment())
            .commit();


    }

    private void showMainFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_content_container, fragment)
            .commit();
    }
}