package com.example.sincronia;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

// Importa Playlist
import com.example.sincronia.Playlist;

public class MusicPlayerViewModel extends ViewModel {
    // Estado del reproductor
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> playlist = new MutableLiveData<>(new ArrayList<>());
    // NUEVO: Lista de playlists
    private final MutableLiveData<List<Playlist>> playlistList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> repeatSong = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> duration = new MutableLiveData<>(0);

    private Handler positionHandler;
    private Runnable positionRunnable;

    public MusicPlayerViewModel() {
        positionHandler = new Handler(Looper.getMainLooper());
    }

    // Getters LiveData
    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<List<Song>> getPlaylist() { return playlist; }
    // NUEVO: Getter para la lista de playlists
    public LiveData<List<Playlist>> getPlaylistList() { return playlistList; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Boolean> getRepeatSong() { return repeatSong; }
    public LiveData<Integer> getPlaybackPosition() { return playbackPosition; }
    public LiveData<Integer> getDuration() { return duration; }

    // Setters
    public void setPlaylistList(List<Playlist> playlists) {
        playlistList.setValue(playlists);
    }
    public void setPlaylist(List<Song> songs) {
        playlist.setValue(songs);
    }
    public void setCurrentIndex(int idx) {
        currentIndex.setValue(idx);
    }
    public void setRepeatSong(boolean repeat) {
        repeatSong.setValue(repeat);
    }
    public void setDuration(int ms) {
        duration.setValue(ms);
    }
    public void setPlaybackPosition(int ms) {
        playbackPosition.setValue(ms);
    }

    // Permite actualizar el estado isPlaying desde la UI
    public void setIsPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }

    // Acciones de reproducción (lógica básica, integración Spotify se hará luego)
    public void playSongFromList(List<Song> songs, int index) {
        setPlaylist(songs);
        setCurrentIndex(index);
        Song song = songs.get(index);
        currentSong.setValue(song);
        setPlaybackPosition(0);
        setDuration(parseDurationMs(song.getDuration()));
        isPlaying.setValue(true);
        startPositionTimer();
    }

    // Eliminada la función play() que usaba la Web API para reproducir. Ahora la reproducción se controla solo por App Remote desde el fragmento.

    public void pause() {
        isPlaying.setValue(false);
        stopPositionTimer();
        // Eliminado: la pausa real se maneja solo vía App Remote desde el fragmento principal.
    }

    public void next() {
        List<Song> songs = playlist.getValue();
        if (songs == null || songs.isEmpty()) return;
        int idx = currentIndex.getValue() != null ? currentIndex.getValue() : 0;
        idx = (idx + 1) % songs.size();
        setCurrentIndex(idx);
        playSongFromList(songs, idx);
    }

    public void previous() {
        List<Song> songs = playlist.getValue();
        if (songs == null || songs.isEmpty()) return;
        int idx = currentIndex.getValue() != null ? currentIndex.getValue() : 0;
        idx = (idx - 1 + songs.size()) % songs.size();
        setCurrentIndex(idx);
        playSongFromList(songs, idx);
    }

    public void seek(int ms) {
        setPlaybackPosition(ms);
    }

    private void startPositionTimer() {
        stopPositionTimer();
        positionRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying.getValue() != null && isPlaying.getValue()) {
                    int pos = playbackPosition.getValue() != null ? playbackPosition.getValue() : 0;
                    int dur = duration.getValue() != null ? duration.getValue() : 0;
                    if (pos < dur) {
                        setPlaybackPosition(pos + 1000);
                        positionHandler.postDelayed(this, 1000);
                    } else if (repeatSong.getValue() != null && repeatSong.getValue()) {
                        setPlaybackPosition(0);
                        positionHandler.postDelayed(this, 1000);
                    } else {
                        isPlaying.setValue(false);
                    }
                }
            }
        };
        positionHandler.post(positionRunnable);
    }

    private void stopPositionTimer() {
        if (positionHandler != null && positionRunnable != null) {
            positionHandler.removeCallbacks(positionRunnable);
        }
    }
    /**
     * Convierte un String de duración ("mm:ss" o "m:ss") a milisegundos
     */
    private int parseDurationMs(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        String[] parts = durationStr.split(":");
        if (parts.length == 2) {
            try {
                int min = Integer.parseInt(parts[0].trim());
                int sec = Integer.parseInt(parts[1].trim());
                return (min * 60 + sec) * 1000;
            } catch (Exception e) { return 0; }
        } else if (parts.length == 1) {
            try {
                return Integer.parseInt(parts[0].trim()) * 1000;
            } catch (Exception e) { return 0; }
        }
        return 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPositionTimer();
    }
}

// NOTA: Se requiere una clase Song con los campos necesarios (id, title, artist, durationMs, etc.) para que esto funcione correctamente.
