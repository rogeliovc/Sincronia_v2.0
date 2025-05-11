package com.example.sincronia;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class AuthManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private SharedPreferences prefs;

    // --- Singleton para acceso global ---
    private static AuthManager instance;
    private static Context appContext;
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        instance = new AuthManager(appContext);
    }
    public static AuthManager getInstance() {
        if (instance == null && appContext != null) {
            instance = new AuthManager(appContext);
        }
        return instance;
    }
    // Método estático para obtener el accessToken global
    public static String getGlobalAccessToken() {
        AuthManager mgr = getInstance();
        return mgr != null ? mgr.getAccessToken() : null;
    }

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken, long expiresInSeconds) {
        long expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000);
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getTokenExpiry() {
        return prefs.getLong(KEY_TOKEN_EXPIRY, 0);
    }

    public boolean isTokenValid() {
        long expiry = getTokenExpiry();
        return getAccessToken() != null && System.currentTimeMillis() < expiry - 5 * 60 * 1000; // 5 min margen
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // Refresca el access_token usando el refresh_token
    public boolean refreshAccessToken() {
        String refreshToken = getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) return false;
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            String params = "grant_type=refresh_token" +
                    "&refresh_token=" + refreshToken +
                    "&client_id=4caddaabcd134c6da47d4f7d1c7877ba";
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
            String newAccessToken = json.getString("access_token");
            long expiresIn = json.has("expires_in") ? json.getLong("expires_in") : 3600;
            String newRefreshToken = json.has("refresh_token") ? json.getString("refresh_token") : refreshToken;
            saveTokens(newAccessToken, newRefreshToken, expiresIn);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
