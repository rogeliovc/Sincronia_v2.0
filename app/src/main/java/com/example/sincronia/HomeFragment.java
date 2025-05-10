package com.example.sincronia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        new Thread(() -> {
            AuthManager authManager = new AuthManager(requireContext());
            String token = authManager.getAccessToken();
            boolean valid = authManager.isTokenValid();
            if (!valid) {
                boolean refreshed = authManager.refreshAccessToken();
                if (refreshed) {
                    token = authManager.getAccessToken();
                } else {
                    String finalToken = token;
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "Token inválido o expirado. Por favor inicia sesión de nuevo.", android.widget.Toast.LENGTH_LONG).show();
                    });
                    return;
                }
            }
            // Petición a Spotify API /me
            try {
                java.net.URL url = new java.net.URL("https://api.spotify.com/v1/me");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.io.InputStream is = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    String displayName = json.optString("display_name", "?");
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "¡Bienvenido, " + displayName + "!", android.widget.Toast.LENGTH_LONG).show();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "No se pudo obtener usuario de Spotify", android.widget.Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(requireContext(), "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }).start();

        return view;
    }
}
