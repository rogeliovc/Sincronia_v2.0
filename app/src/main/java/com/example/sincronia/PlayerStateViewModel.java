package com.example.sincronia;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerStateViewModel extends ViewModel {
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> duration = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);

    public LiveData<Integer> getPlaybackPosition() {
        return playbackPosition;
    }
    public void setPlaybackPosition(int position) {
        playbackPosition.setValue(position);
    }

    public LiveData<Integer> getDuration() {
        return duration;
    }
    public void setDuration(int durationMs) {
        duration.setValue(durationMs);
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }
    public void setIsPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }
}
