package com.example.sincronia;

import com.bumptech.glide.Glide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<Playlist> playlistList;
    private Context context;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(int position);
    }

    public PlaylistAdapter(Context context, List<Playlist> playlistList, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlistList = playlistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.tvPlaylistName.setText(playlist.getName());
        if (playlist.getImageUrl() != null) {
            Glide.with(context)
                .load(playlist.getImageUrl())
                .placeholder(R.drawable.icon_default_music)
                .into(holder.ivPlaylistCover);
        } else {
            holder.ivPlaylistCover.setImageResource(R.drawable.icon_default_music);
        }
        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(position));
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaylistCover;
        TextView tvPlaylistName;
        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaylistCover = itemView.findViewById(R.id.ivPlaylistCover);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
        }
    }
}
