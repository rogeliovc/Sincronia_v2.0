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
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return null;
            accessToken = auth.getAccessToken();
        }
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
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return new java.util.ArrayList<>();
            accessToken = auth.getAccessToken();
        }
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
                    if (playlistJson.has("images") && !playlistJson.isNull("images")) {
                        Object imagesObj = playlistJson.get("images");
                        if (imagesObj instanceof org.json.JSONArray) {
                            org.json.JSONArray images = (org.json.JSONArray) imagesObj;
                            if (images.length() > 0) {
                                imageUrl = images.getJSONObject(0).getString("url");
                            }
                        }
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

    // Obtener canciones favoritas (Liked Songs)
    public static java.util.List<Song> getUserFavorites(String accessToken) {
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return new java.util.ArrayList<>();
            accessToken = auth.getAccessToken();
        }
        java.util.List<Song> songs = new java.util.ArrayList<>();
        String endpoint = BASE_URL + "/me/tracks";
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
                org.json.JSONObject json = new org.json.JSONObject(response.toString());
                org.json.JSONArray items = json.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    org.json.JSONObject trackObj = items.getJSONObject(i).getJSONObject("track");
                    String title = trackObj.getString("name");
                    String artist = trackObj.getJSONArray("artists").getJSONObject(0).getString("name");
                    String duration = millisToMinSec(trackObj.getInt("duration_ms"));
                    String coverUrl = null;
                    org.json.JSONArray images = trackObj.getJSONObject("album").getJSONArray("images");
                    if (images.length() > 0) {
                        coverUrl = images.getJSONObject(0).getString("url");
                    }
                    String uri = trackObj.getString("uri");
songs.add(new Song(title, artist, duration, -1, coverUrl, uri));
                }
            } else {
                Log.e(TAG, "Error en getUserFavorites: code=" + responseCode);
            }
        } catch (IOException | org.json.JSONException e) {
            Log.e(TAG, "Excepción en getUserFavorites", e);
        }
        return songs;
    }

    // Obtener canciones reproducidas recientemente
    public static java.util.List<Song> getRecentlyPlayed(String accessToken) {
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return new java.util.ArrayList<>();
            accessToken = auth.getAccessToken();
        }
        java.util.List<Song> songs = new java.util.ArrayList<>();
        String endpoint = BASE_URL + "/me/player/recently-played";
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
                org.json.JSONObject json = new org.json.JSONObject(response.toString());
                org.json.JSONArray items = json.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    org.json.JSONObject trackObj = items.getJSONObject(i).getJSONObject("track");
                    String title = trackObj.getString("name");
                    String artist = trackObj.getJSONArray("artists").getJSONObject(0).getString("name");
                    String duration = millisToMinSec(trackObj.getInt("duration_ms"));
                    String coverUrl = null;
                    org.json.JSONArray images = trackObj.getJSONObject("album").getJSONArray("images");
                    if (images.length() > 0) {
                        coverUrl = images.getJSONObject(0).getString("url");
                    }
                    String uri = trackObj.getString("uri");
songs.add(new Song(title, artist, duration, -1, coverUrl, uri));
                }
            } else {
                Log.e(TAG, "Error en getRecentlyPlayed: code=" + responseCode);
            }
        } catch (IOException | org.json.JSONException e) {
            Log.e(TAG, "Excepción en getRecentlyPlayed", e);
        }
        return songs;
    }

    // Utilidad para convertir ms a mm:ss
    private static String millisToMinSec(int millis) {
        int seconds = millis / 1000;
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    // Reproducir una canción por URI (Spotify Web API)
    public static boolean playTrack(String accessToken, String trackUri) {
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return false;
            accessToken = auth.getAccessToken();
        }
        String endpoint = BASE_URL + "/me/player/play";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String jsonBody = "{\"uris\":[\"" + trackUri + "\"]}";
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes());
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == 204) return true;
            Log.e(TAG, "Error al reproducir canción: code=" + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en playTrack", e);
        }
        return false;
    }

    // Pausar la reproducción actual
    public static boolean pause(String accessToken) {
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return false;
            accessToken = auth.getAccessToken();
        }
        String endpoint = BASE_URL + "/me/player/pause";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            int responseCode = conn.getResponseCode();
            if (responseCode == 204) return true;
            Log.e(TAG, "Error al pausar: code=" + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "Excepción en pause", e);
        }
        return false;
    }

    // Obtener información de la canción actualmente en reproducción
    public static JSONObject getCurrentlyPlaying(String accessToken) {
        // Manejo automático de token
        AuthManager auth = AuthManager.getInstance();
        if (!auth.isTokenValid()) {
            boolean refreshed = auth.refreshAccessToken();
            if (!refreshed) return null;
            accessToken = auth.getAccessToken();
        }
        String endpoint = BASE_URL + "/me/player/currently-playing";
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
            } else if (responseCode == 204) {
                // No content: no track is currently playing
                Log.d(TAG, "No hay canción en reproducción (204)");
                return null;
            } else {
                Log.e(TAG, "Error en getCurrentlyPlaying: code=" + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Excepción en getCurrentlyPlaying", e);
        }
        return null;
    }
}
