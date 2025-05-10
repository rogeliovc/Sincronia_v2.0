package com.example.sincronia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.ImageButton;
import org.json.JSONObject;

public class MainPlayerActivity extends AppCompatActivity {
    private String accessToken;
    private PlayerStateViewModel playerStateViewModel;
    private android.os.Handler playbackHandler = new android.os.Handler();
    private Runnable playbackUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_player);

        // Initialize ViewModel
        playerStateViewModel = new androidx.lifecycle.ViewModelProvider(this).get(PlayerStateViewModel.class);

        // UI references
        ImageView albumArt = findViewById(R.id.player_album_art);
        TextView songTitle = findViewById(R.id.player_song_title);
        TextView songArtist = findViewById(R.id.player_song_artist);
        TextView currentTime = findViewById(R.id.player_current_time);
        TextView totalTime = findViewById(R.id.player_total_time);
        SeekBar seekBar = findViewById(R.id.player_seekbar);
        ImageButton btnPrev = findViewById(R.id.player_btn_prev);
        ImageButton btnPlayPause = findViewById(R.id.player_btn_play_pause);
        ImageButton btnNext = findViewById(R.id.player_btn_next);

        // Observe ViewModel
        playerStateViewModel.getPlaybackPosition().observe(this, positionMs -> {
            seekBar.setProgress(positionMs);
            currentTime.setText(millisToMinSec(positionMs));
        });
        playerStateViewModel.getDuration().observe(this, durationMs -> {
            seekBar.setMax(durationMs);
            totalTime.setText(millisToMinSec(durationMs));
        });
        playerStateViewModel.getIsPlaying().observe(this, playing -> {
            btnPlayPause.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });

        // Controls
        btnPlayPause.setOnClickListener(v -> {
            Boolean isPlaying = playerStateViewModel.getIsPlaying().getValue();
            if (isPlaying != null) playerStateViewModel.setIsPlaying(!isPlaying);
            // TODO: Call SpotifyService to play/pause
        });
        btnPrev.setOnClickListener(v -> {
            // TODO: Implement previous track logic
        });
        btnNext.setOnClickListener(v -> {
            // TODO: Implement next track logic
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) playerStateViewModel.setPlaybackPosition(progress);
                // TODO: Optionally seek Spotify playback
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

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

        // Set up periodic playback state fetch
        playbackUpdater = new Runnable() {
            @Override
            public void run() {
                if (accessToken != null) {
                    new Thread(() -> {
                        try {
                            org.json.JSONObject playback = SpotifyService.getCurrentlyPlaying(accessToken);
                            if (playback != null) {
                                int position = playback.optInt("progress_ms", 0);
                                int duration = playback.optJSONObject("item") != null ? playback.optJSONObject("item").optInt("duration_ms", 0) : 0;
                                boolean isPlaying = playback.optBoolean("is_playing", false);
                                runOnUiThread(() -> {
                                    playerStateViewModel.setPlaybackPosition(position);
                                    playerStateViewModel.setDuration(duration);
                                    playerStateViewModel.setIsPlaying(isPlaying);
                                });
                            }
                        } catch (Exception e) {
                            // Optionally log or handle error
                        }
                    }).start();
                }
                playbackHandler.postDelayed(this, 1000); // Update every second
            }
        };
        playbackHandler.post(playbackUpdater);
    }
    private String millisToMinSec(int millis) {
        int sec = millis / 1000;
        int min = sec / 60;
        sec = sec % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playbackHandler.removeCallbacks(playbackUpdater);
    }
}
