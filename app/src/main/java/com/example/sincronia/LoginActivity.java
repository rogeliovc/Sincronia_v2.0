package com.example.sincronia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    // Reemplaza estos valores por los de tu app registrada en Spotify
    private static final String CLIENT_ID = "4caddaabcd134c6da47d4f7d1c7877ba";
    private static final String REDIRECT_URI = "https://spotify-callback.vercel.app/api/callback";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing playlist-read-private playlist-read-collaborative streaming user-top-read user-read-recently-played user-read-email";
    private String codeVerifier;

    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 1337;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button loginButton = findViewById(R.id.login_button);
        Button manualTokenButton = findViewById(R.id.manual_token_button);
        EditText manualTokenEditText = findViewById(R.id.manual_token_input);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generar code_verifier y code_challenge (PKCE)
                codeVerifier = generateCodeVerifier();
                String codeChallenge = generateCodeChallenge(codeVerifier);
                // Armar URL de autorización
                String url = "https://accounts.spotify.com/authorize" +
                        "?response_type=code" +
                        "&client_id=" + CLIENT_ID +
                        "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                        "&scope=" + Uri.encode(SCOPES) +
                        "&code_challenge_method=S256" +
                        "&code_challenge=" + codeChallenge;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        manualTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = manualTokenEditText.getText().toString().trim();
                if (!token.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Token manual: " + token, Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mainIntent.putExtra("access_token", token);
                    startActivity(mainIntent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Por favor ingresa un access_token válido", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleAuthCallback(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar si la Activity fue abierta por un intent de callback
        Intent intent = getIntent();
        handleAuthCallback(intent);
    }

    private void handleAuthCallback(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            // Manejo del deep link personalizado: tuapp://callback?access_token=...
            if (uri.getScheme() != null && uri.getScheme().equals("tuapp") && "callback".equals(uri.getHost())) {
                String accessToken = uri.getQueryParameter("access_token");
                if (accessToken != null && !accessToken.isEmpty()) {
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mainIntent.putExtra("access_token", accessToken);
                    startActivity(mainIntent);
                    finish();
                    return;
                } else {
                    Toast.makeText(this, "No se recibió access_token en el callback", Toast.LENGTH_LONG).show();
                }
            }
            // Manejo del callback original de Spotify (flujo PKCE puro)
            if (uri.toString().startsWith(REDIRECT_URI)) {
                String code = uri.getQueryParameter("code");
                if (code != null) {
                    // Intercambiar el código por un access_token
                    exchangeCodeForToken(code, codeVerifier);
                } else {
                    Toast.makeText(this, "No se recibió código de autorización", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void exchangeCodeForToken(String code, String codeVerifier) {
        new Thread(() -> {
            try {
                URL url = new URL("https://accounts.spotify.com/api/token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String params = "grant_type=authorization_code" +
                        "&code=" + code +
                        "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                        "&client_id=" + CLIENT_ID +
                        "&code_verifier=" + codeVerifier;
                OutputStream os = conn.getOutputStream();
                os.write(params.getBytes());
                os.flush();
                os.close();
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                String accessToken = json.getString("access_token");
                runOnUiThread(() -> {
                    Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                    mainIntent.putExtra("access_token", accessToken);
                    startActivity(mainIntent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error autenticando: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // --- PKCE utils ---
    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes("US-ASCII");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

