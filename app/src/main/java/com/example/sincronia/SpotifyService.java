package com.example.sincronia;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpotifyService {
    private static final String TAG = "SpotifyService";
    private static final String BASE_URL = "https://api.spotify.com/v1";

    // Obtener perfil del usuario
    public static JSONObject getUserProfile(String accessToken) {
        String endpoint = BASE_URL + "/me";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return new JSONObject(response.toString());
            } else {
                Log.e(TAG, "Error en getUserProfile: code=" + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Excepción en getUserProfile", e);
        }
        return null;
    }

    // Obtener playlists del usuario
    public static java.util.List<Playlist> getUserPlaylists(String accessToken) {
        java.util.List<Playlist> playlists = new java.util.ArrayList<>();
        String endpoint = BASE_URL + "/me/playlists";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject json = new JSONObject(response.toString());
                org.json.JSONArray items = json.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject playlistJson = items.getJSONObject(i);
                    String id = playlistJson.getString("id");
                    String name = playlistJson.getString("name");
                    String imageUrl = null;
                    org.json.JSONArray images = playlistJson.getJSONArray("images");
                    if (images.length() > 0) {
                        imageUrl = images.getJSONObject(0).getString("url");
                    }
                    playlists.add(new Playlist(id, name, imageUrl));
                }
            } else {
                Log.e(TAG, "Error en getUserPlaylists: code=" + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Excepción en getUserPlaylists", e);
        }
        return playlists;
    }
}
