package com.example.sincronia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.content.DialogInterface;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PlayerBottomSheetFragment extends BottomSheetDialogFragment {
    private PlayerStateViewModel playerStateViewModel;
    private ImageView albumArt;
    private TextView songTitle, songArtist, currentTime, totalTime;
    private SeekBar seekBar;
    private ImageButton btnPrev, btnPlayPause, btnNext, btnClose;
    private boolean isPlaying = false;
    private OnPlayerActionListener actionListener;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private boolean isExpanded = false; // Estado actual


    // Métodos públicos para expandir/colapsar desde el padre
    public void expandSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            isExpanded = true;
        }
    }
    public void collapseSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            isExpanded = false;
        }
    }
    public void toggleExpandCollapse() {
        if (bottomSheetBehavior != null) {
            if (isExpanded) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            isExpanded = !isExpanded;
        }
    }

    public interface OnPlayerActionListener {
        void onPrev();
        void onPlayPause();
        void onNext();
        void onClose();
        void onSeek(int position);
        void onExpandCollapse();
    }

    public void setOnPlayerActionListener(OnPlayerActionListener listener) {
        this.actionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player, container, false);
        albumArt = v.findViewById(R.id.player_album_art);
        songTitle = v.findViewById(R.id.player_song_title);
        songArtist = v.findViewById(R.id.player_song_artist);
        currentTime = v.findViewById(R.id.player_current_time);
        totalTime = v.findViewById(R.id.player_total_time);
        seekBar = v.findViewById(R.id.player_seekbar);
        btnPrev = v.findViewById(R.id.player_btn_prev);
        btnPlayPause = v.findViewById(R.id.player_btn_play_pause);
        btnNext = v.findViewById(R.id.player_btn_next);
        btnClose = v.findViewById(R.id.btnClosePlayer);

        // ViewModel setup
        playerStateViewModel = new ViewModelProvider(requireActivity()).get(PlayerStateViewModel.class);

        // Observe playback position
        playerStateViewModel.getPlaybackPosition().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer positionMs) {
                seekBar.setProgress(positionMs);
                currentTime.setText(millisToMinSec(positionMs));
            }
        });
        // Observe duration
        playerStateViewModel.getDuration().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer durationMs) {
                seekBar.setMax(durationMs);
                totalTime.setText(millisToMinSec(durationMs));
            }
        });
        // Observe isPlaying
        playerStateViewModel.getIsPlaying().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean playing) {
                isPlaying = playing;
                btnPlayPause.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });

        btnPrev.setOnClickListener(view -> { if (actionListener != null) actionListener.onPrev(); });
        btnPlayPause.setOnClickListener(view -> { if (actionListener != null) actionListener.onPlayPause(); });
        btnNext.setOnClickListener(view -> { if (actionListener != null) actionListener.onNext(); });
        btnClose.setOnClickListener(view -> { if (actionListener != null) actionListener.onClose(); });
        v.findViewById(R.id.handleBar).setOnClickListener(view -> {
            toggleExpandCollapse();
            if (actionListener != null) actionListener.onExpandCollapse();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && actionListener != null) {
                    actionListener.onSeek(progress);
                    playerStateViewModel.setPlaybackPosition(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(sheet);
                bottomSheetBehavior.setState(isExpanded ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetBehavior.setSkipCollapsed(false);
                bottomSheetBehavior.setHideable(true);
            }
        }
    }

    public void updateSong(String title, String artist, String coverUrl, int durationMs) {
        songTitle.setText(title);
        songArtist.setText(artist);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(requireContext()).load(coverUrl).placeholder(R.drawable.icon_default_music).into(albumArt);
        } else {
            albumArt.setImageResource(R.drawable.icon_default_music);
        }
        playerStateViewModel.setDuration(durationMs);
        playerStateViewModel.setPlaybackPosition(0);
    }

    public void updateProgress(int positionMs) {
        playerStateViewModel.setPlaybackPosition(positionMs);
    }

    public void setPlaying(boolean playing) {
        playerStateViewModel.setIsPlaying(playing);
    }

    private String millisToMinSec(int millis) {
        int sec = millis / 1000;
        int min = sec / 60;
        sec = sec % 60;
        return String.format("%d:%02d", min, sec);
    }
}
