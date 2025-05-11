package com.example.sincronia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import com.bumptech.glide.Glide;
import androidx.lifecycle.ViewModelProvider;

public class MiniPlayerFragment extends Fragment {
    private MusicPlayerViewModel musicPlayerViewModel;
    private ImageView ivMiniArt;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private View miniPlayerRoot;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mini_player, container, false);
        ivMiniArt = v.findViewById(R.id.ivMiniArt);
        tvMiniTitle = v.findViewById(R.id.tvMiniTitle);
        tvMiniArtist = v.findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = v.findViewById(R.id.btnMiniPlayPause);
        miniPlayerRoot = v;
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        musicPlayerViewModel = new ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);

        // Observa la canción actual
        musicPlayerViewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                tvMiniTitle.setText(song.getTitle());
                tvMiniArtist.setText(song.getArtist());
                if (song.getCoverUrl() != null && !song.getCoverUrl().isEmpty()) {
                    Glide.with(this).load(song.getCoverUrl()).placeholder(R.drawable.icon_default_music).into(ivMiniArt);
                } else {
                    ivMiniArt.setImageResource(song.getCoverResId());
                }
            }
        });
        // Observa el estado de reproducción
        musicPlayerViewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                btnMiniPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });
        btnMiniPlayPause.setOnClickListener(v -> {
            Boolean isPlaying = musicPlayerViewModel.getIsPlaying().getValue();
            if (isPlaying != null && isPlaying) {
                musicPlayerViewModel.pause();
            } else {
                musicPlayerViewModel.play();
            }
        });
        // Al tocar el minireproductor, expande el reproductor completo
        miniPlayerRoot.setOnClickListener(v -> {
            PlayerBottomSheetFragment playerSheet = new PlayerBottomSheetFragment();
            playerSheet.show(requireActivity().getSupportFragmentManager(), "PlayerBottomSheet");
        });
    }
}

