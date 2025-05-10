package com.example.sincronia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

public class TaskManagerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);
        // Obtener el access_token desde el Intent
        String accessToken = getIntent().getStringExtra("access_token");
        final ListView[] listViewHolder = new ListView[1];
        listViewHolder[0] = findViewById(android.R.id.list);
        if (listViewHolder[0] == null) {
            listViewHolder[0] = new ListView(this);
            listViewHolder[0].setId(android.R.id.list);
            ((android.widget.LinearLayout) findViewById(android.R.id.content)).addView(listViewHolder[0]);
        }
        if (accessToken != null) {
            new Thread(() -> {
                List<Playlist> playlists = SpotifyService.getUserPlaylists(accessToken);
                String[] playlistNames = new String[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) {
                    playlistNames[i] = playlists.get(i).getName();
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(TaskManagerActivity.this, android.R.layout.simple_list_item_1, playlistNames);
                    listViewHolder[0].setAdapter(adapter);
                });
            }).start();
        }
    }
}
