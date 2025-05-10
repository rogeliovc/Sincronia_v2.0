package com.example.sincronia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

public class MainPlayerActivity extends AppCompatActivity {
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_player);
        // Obtener el access_token del Intent
        accessToken = getIntent().getStringExtra("access_token");
        // Solo para depuración: mostrar el token
        if (accessToken != null) {
            // Obtener perfil del usuario en un hilo separado
            new Thread(() -> {
                try {
                    JSONObject userProfile = SpotifyService.getUserProfile(accessToken);
                    if (userProfile != null && userProfile.has("display_name")) {
                        String displayName = userProfile.optString("display_name", "Usuario");
                        new Handler(Looper.getMainLooper()).post(() ->
                            android.widget.Toast.makeText(MainPlayerActivity.this, "¡Bienvenido, " + displayName + "!", android.widget.Toast.LENGTH_LONG).show()
                        );
                    } else {
                        new Handler(Looper.getMainLooper()).post(() ->
                            android.widget.Toast.makeText(MainPlayerActivity.this, "No se pudo obtener el perfil de usuario", android.widget.Toast.LENGTH_LONG).show()
                        );
                    }
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        android.widget.Toast.makeText(MainPlayerActivity.this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        } else {
            android.widget.Toast.makeText(this, "No se recibió access_token", android.widget.Toast.LENGTH_LONG).show();
        }
        // Aquí irá la lógica del reproductor principal usando el accessToken
    }
}
