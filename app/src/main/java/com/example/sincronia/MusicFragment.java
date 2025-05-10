package com.example.sincronia;

import com.bumptech.glide.Glide;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.sincronia.R;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {
    private RecyclerView rvPlaylists, rvFavorites, rvRecent;
    private PlaylistAdapter playlistAdapter;
    private SongAdapter favoritesAdapter, recentAdapter;
    private List<Playlist> playlistList;
    private List<Song> favoritesList, recentList;
    private int currentSongIndex = 0;

    private View progressBar;
    private TextView tvPlaylistsEmpty, tvFavoritesEmpty, tvRecentEmpty;
    private TextView tvError;
    private View rootView;

    // --- Web API: No App Remote ---

    // Controles del reproductor
    private ImageView ivSongArt;
    private TextView tvSongTitle, tvSongArtist;
    private ImageButton btnPrev, btnPlayPause, btnNext;
    private ProgressBar playerProgress;
    private boolean isPlaying = false;
    private String lastPlayedUri = null; // Para saber qué canción reanudar

    public MusicFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        rvFavorites = view.findViewById(R.id.rvFavorites);
        rvRecent = view.findViewById(R.id.rvRecent);
        progressBar = view.findViewById(R.id.musicProgressBar);
        tvPlaylistsEmpty = view.findViewById(R.id.tvPlaylistsEmpty);
        tvFavoritesEmpty = view.findViewById(R.id.tvFavoritesEmpty);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);
        rootView = view;

        // Opcional: mensaje de error
        tvError = new TextView(getContext());
        tvError.setText("Error al cargar datos de Spotify. Toca para reintentar.");
        tvError.setTextColor(0xFFFF4444);
        tvError.setTextSize(18);
        tvError.setVisibility(View.GONE);
        ((ViewGroup) view).addView(tvError);
        tvError.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            loadSpotifyData();
        });

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        playlistList = new ArrayList<>();
        favoritesList = new ArrayList<>();
        recentList = new ArrayList<>();

        playlistAdapter = new PlaylistAdapter(getContext(), playlistList, position -> {
            // TODO: acción al seleccionar playlist
        });
        rvPlaylists.setAdapter(playlistAdapter);

        favoritesAdapter = new SongAdapter(getContext(), favoritesList, position -> {
            android.util.Log.d("MusicFragment", "Favorito seleccionado: " + favoritesList.get(position).getTitle());
            currentSongIndex = position;
            updatePlayerUI();
            // Intentar reproducir la canción favorita seleccionada
            playSpotifyUri(favoritesList.get(position).getUri());
        });
        rvFavorites.setAdapter(favoritesAdapter);

        recentAdapter = new SongAdapter(getContext(), recentList, position -> {
            android.util.Log.d("MusicFragment", "Reciente seleccionado: " + recentList.get(position).getTitle());
            // Reproducir desde recientes
            playSpotifyUri(recentList.get(position).getUri());
        });
        rvRecent.setAdapter(recentAdapter);

        // Cargar datos reales desde Spotify
        loadSpotifyData();

        // Inicializar controles del reproductor
        ivSongArt = view.findViewById(R.id.ivSongArt);
        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvSongArtist = view.findViewById(R.id.tvSongArtist);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnNext = view.findViewById(R.id.btnNext);
        playerProgress = view.findViewById(R.id.playerProgress);

        // Validar que todas las vistas existen
        if (ivSongArt == null) android.util.Log.e("MusicFragment", "ivSongArt es NULL");
        if (tvSongTitle == null) android.util.Log.e("MusicFragment", "tvSongTitle es NULL");
        if (tvSongArtist == null) android.util.Log.e("MusicFragment", "tvSongArtist es NULL");
        if (btnPrev == null) android.util.Log.e("MusicFragment", "btnPrev es NULL");
        if (btnPlayPause == null) android.util.Log.e("MusicFragment", "btnPlayPause es NULL");
        if (btnNext == null) android.util.Log.e("MusicFragment", "btnNext es NULL");
        if (playerProgress == null) android.util.Log.e("MusicFragment", "playerProgress es NULL");

        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                isPlaying = !isPlaying;
                updatePlayPauseIcon();
                android.util.Log.d("MusicFragment", isPlaying ? "Play" : "Pause");
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentSongIndex > 0) {
                    currentSongIndex--;
                    updatePlayerUI();
                    android.util.Log.d("MusicFragment", "Anterior: " + favoritesList.get(currentSongIndex).getTitle());
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentSongIndex < favoritesList.size() - 1) {
                    currentSongIndex++;
                    updatePlayerUI();
                    android.util.Log.d("MusicFragment", "Siguiente: " + favoritesList.get(currentSongIndex).getTitle());
                }
            });
        }

        // Inicializar reproductor con la primera canción de favoritos
        updatePlayerUI();

        // Mostrar el reproductor visual al tocar la portada o el área del player
        View playerContainer = view.findViewById(R.id.playerContainer);
        if (playerContainer != null) {
            playerContainer.setOnClickListener(v -> showPlayerBottomSheet());
        } else if (ivSongArt != null) {
            ivSongArt.setOnClickListener(v -> showPlayerBottomSheet());
        }
        return view;
    }

    private PlayerBottomSheetFragment playerSheet;

    private void showPlayerBottomSheet() {
        if (favoritesList == null || favoritesList.isEmpty()) return;
        Song song = favoritesList.get(currentSongIndex);
        if (playerSheet == null) playerSheet = new PlayerBottomSheetFragment();
        playerSheet.setOnPlayerActionListener(new PlayerBottomSheetFragment.OnPlayerActionListener() {
            @Override public void onPrev() {
                if (currentSongIndex > 0) {
                    currentSongIndex--;
                    updatePlayerUI();
                    playSpotifyUri(favoritesList.get(currentSongIndex).getUri());
                    playerSheet.updateSong(
                        favoritesList.get(currentSongIndex).getTitle(),
                        favoritesList.get(currentSongIndex).getArtist(),
                        favoritesList.get(currentSongIndex).getCoverUrl(),
                        parseDurationMs(favoritesList.get(currentSongIndex).getDuration())
                    );
                }
            }
            @Override public void onPlayPause() {
                if (isPlaying) {
                    pauseSpotifyWithCallback(success -> {
                        if (playerSheet != null) playerSheet.setPlaying(!success);
                    });
                } else {
                    resumeSpotifyWithCallback(success -> {
                        if (playerSheet != null) playerSheet.setPlaying(success);
                    });
                }
            }
            @Override public void onNext() {
                if (currentSongIndex < favoritesList.size() - 1) {
                    currentSongIndex++;
                    updatePlayerUI();
                    playSpotifyUri(favoritesList.get(currentSongIndex).getUri());
                    playerSheet.updateSong(
                        favoritesList.get(currentSongIndex).getTitle(),
                        favoritesList.get(currentSongIndex).getArtist(),
                        favoritesList.get(currentSongIndex).getCoverUrl(),
                        parseDurationMs(favoritesList.get(currentSongIndex).getDuration())
                    );
                }
            }
            @Override public void onClose() { playerSheet.dismiss(); }
            @Override public void onSeek(int position) { /* Opcional: implementar seek */ }
            @Override public void onExpandCollapse() {
                if (playerSheet != null) playerSheet.toggleExpandCollapse();
            }
        });
        playerSheet.updateSong(song.getTitle(), song.getArtist(), song.getCoverUrl(), parseDurationMs(song.getDuration()));
        playerSheet.setPlaying(isPlaying);
        playerSheet.show(requireActivity().getSupportFragmentManager(), "PlayerBottomSheet");
    }

    private int parseDurationMs(String duration) {
        // Convierte "3:45" a milisegundos
        if (duration == null || !duration.contains(":")) return 0;
        String[] parts = duration.split(":");
        int min = Integer.parseInt(parts[0]);
        int sec = Integer.parseInt(parts[1]);
        return (min * 60 + sec) * 1000;
    }

    private void updatePlayerUI() {
        if (favoritesList == null || favoritesList.isEmpty()) return;
        Song song = favoritesList.get(currentSongIndex);
        tvSongTitle.setText(song.getTitle());
        tvSongArtist.setText(song.getArtist());
        if (song.getCoverUrl() != null) {
            // Glide para portada
            Glide.with(this).load(song.getCoverUrl()).placeholder(R.drawable.icon_default_music).into(ivSongArt);
        } else {
            ivSongArt.setImageResource(song.getCoverResId());
        }
        // Simular progreso
        playerProgress.setProgress(0);
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (isPlaying) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    // Carga datos reales de Spotify
    private void loadSpotifyData() {
        if (!isAdded() || getActivity() == null) return;
        

requireActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) return;
            progressBar.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.GONE);
            rvRecent.setVisibility(View.GONE);
            tvPlaylistsEmpty.setVisibility(View.GONE);
            tvFavoritesEmpty.setVisibility(View.GONE);
            tvRecentEmpty.setVisibility(View.GONE);
            tvError.setVisibility(View.GONE);
        });
        new Thread(() -> {
            AuthManager authManager = new AuthManager(requireContext());
            String accessToken = authManager.getAccessToken();
            if (accessToken == null) {
                showError();
                return;
            }
            List<Playlist> playlists = null;
            List<Song> favorites = null;
            List<Song> recent = null;
            try {
                playlists = SpotifyService.getUserPlaylists(accessToken);
                favorites = SpotifyService.getUserFavorites(accessToken);
                recent = SpotifyService.getRecentlyPlayed(accessToken);
            } catch (Exception e) {
                showError();
                return;
            }
            List<Playlist> finalPlaylists = playlists;
            List<Song> finalFavorites = favorites;
            List<Song> finalRecent = recent;
            if (!isAdded() || getActivity() == null) return;
            

requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                // Playlists
                playlistList.clear();
                if (finalPlaylists != null && !finalPlaylists.isEmpty()) {
                    playlistList.addAll(finalPlaylists);
                    rvPlaylists.setVisibility(View.VISIBLE);
                    tvPlaylistsEmpty.setVisibility(View.GONE);
                } else {
                    rvPlaylists.setVisibility(View.GONE);
                    tvPlaylistsEmpty.setVisibility(View.VISIBLE);
                }
                playlistAdapter.notifyDataSetChanged();
                // Favoritos
                favoritesList.clear();
                if (finalFavorites != null && !finalFavorites.isEmpty()) {
                    favoritesList.addAll(finalFavorites);
                    rvFavorites.setVisibility(View.VISIBLE);
                    tvFavoritesEmpty.setVisibility(View.GONE);
                } else {
                    rvFavorites.setVisibility(View.GONE);
                    tvFavoritesEmpty.setVisibility(View.VISIBLE);
                }
                favoritesAdapter.notifyDataSetChanged();
                // Recientes
                recentList.clear();
                if (finalRecent != null && !finalRecent.isEmpty()) {
                    recentList.addAll(finalRecent);
                    rvRecent.setVisibility(View.VISIBLE);
                    tvRecentEmpty.setVisibility(View.GONE);
                } else {
                    rvRecent.setVisibility(View.GONE);
                    tvRecentEmpty.setVisibility(View.VISIBLE);
                }
                recentAdapter.notifyDataSetChanged();
                // Actualizar reproductor con la primera canción de favoritos
                currentSongIndex = 0;
                updatePlayerUI();
            });
        }).start();
    }

    private void showError() {
        if (!isAdded() || getActivity() == null) return;
        

requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.GONE);
            rvRecent.setVisibility(View.GONE);
            tvPlaylistsEmpty.setVisibility(View.GONE);
            tvFavoritesEmpty.setVisibility(View.GONE);
            tvRecentEmpty.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
        });
    }

    private void playSpotifyUri(String uri) {
    android.util.Log.d("MusicFragment", "Intentando reproducir URI: " + uri);
    android.util.Log.d("MusicFragment", "Intentando reproducir URI: " + uri);
        if (uri == null || uri.isEmpty()) {
            Toast.makeText(requireContext(), "URI de canción no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                AuthManager authManager = new AuthManager(requireContext());
                String accessToken = authManager.getAccessToken();
                if (accessToken == null) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "No hay sesión de Spotify", Toast.LENGTH_LONG).show());
                    return;
                }
                java.net.URL url = new java.net.URL("https://api.spotify.com/v1/me/player/play");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String body = "{\"uris\":[\"" + uri + "\"]}";
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = conn.getResponseCode();
                android.util.Log.d("MusicFragment", "Código de respuesta de Spotify: " + responseCode);
                String errorMessage = "";
                if (responseCode != 204 && responseCode != 202) {
                    try {
                        java.io.InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                            errorMessage = s.hasNext() ? s.next() : "";
                            android.util.Log.e("MusicFragment", "Respuesta de error de Spotify: " + errorMessage);
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("MusicFragment", "Error leyendo el cuerpo de error", ex);
                    }
                }
                final int finalResponseCode = responseCode;
                final String finalErrorMessage = errorMessage;
                

requireActivity().runOnUiThread(() -> {
                    if (finalResponseCode == 204) {
                        Toast.makeText(requireContext(), "Reproduciendo", Toast.LENGTH_SHORT).show();
                        isPlaying = true;
                        updatePlayPauseIcon();
                    } else if (finalResponseCode == 403 && finalErrorMessage.contains("PREMIUM_REQUIRED")) {
                        Toast.makeText(requireContext(), "Se requiere Premium para controlar la reproducción. Abriendo en Spotify...", Toast.LENGTH_LONG).show();
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(uri));
                            intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + requireContext().getPackageName()));
                            startActivity(intent);
                        } catch (Exception ex) {
                            Toast.makeText(requireContext(), "No se pudo abrir Spotify", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se pudo reproducir", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error al reproducir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Callback para saber si la pausa fue exitosa
    private interface PauseCallback { void onResult(boolean success); }
    private void pauseSpotifyWithCallback(PauseCallback callback) {
        new Thread(() -> {
            try {
                AuthManager authManager = new AuthManager(requireContext());
                String accessToken = authManager.getAccessToken();
                if (accessToken == null) {
                    

requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "No hay sesión de Spotify", Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onResult(false);
                    });
                    return;
                }
                java.net.URL url = new java.net.URL("https://api.spotify.com/v1/me/player/pause");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                int responseCode = conn.getResponseCode();
                android.util.Log.d("MusicFragment", "Código de respuesta de Spotify: " + responseCode);
                String errorMessage = "";
                if (responseCode != 204 && responseCode != 202) {
                    try {
                        java.io.InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                            errorMessage = s.hasNext() ? s.next() : "";
                            android.util.Log.e("MusicFragment", "Respuesta de error de Spotify: " + errorMessage);
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("MusicFragment", "Error leyendo el cuerpo de error", ex);
                    }
                }
                final int respCode = responseCode;
                final String errMsg = errorMessage;


requireActivity().runOnUiThread(() -> {
                    if (respCode == 204) {
                        Toast.makeText(requireContext(), "Pausado", Toast.LENGTH_SHORT).show();
                        isPlaying = false;
                        updatePlayPauseIcon();
                        if (callback != null) callback.onResult(true);
                    } else {
                        String msg = "No se pudo pausar";
                        if (respCode == 403 && errMsg.contains("PREMIUM_REQUIRED")) {
                            msg = "Spotify Premium requerido para pausar";
                        } else if (respCode == 404) {
                            msg = "No hay dispositivo activo para pausar";
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        updatePlayPauseIcon();
                        if (callback != null) callback.onResult(false);
                    }
                });
            } catch (Exception e) {
                

requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error al pausar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updatePlayPauseIcon();
                    if (callback != null) callback.onResult(false);
                });
            }
        }).start();
    }

    // Callback para saber si el play fue exitoso
    private interface ResumeCallback { void onResult(boolean success); }
    private void resumeSpotifyWithCallback(ResumeCallback callback) {
        if (lastPlayedUri == null) {
            Toast.makeText(requireContext(), "No hay canción para reanudar", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onResult(false);
            return;
        }
        playSpotifyUriWithCallback(lastPlayedUri, callback);
    }

    private void pauseSpotify() {
        new Thread(() -> {
            try {
                AuthManager authManager = new AuthManager(requireContext());
                String accessToken = authManager.getAccessToken();
                if (accessToken == null) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "No hay sesión de Spotify", Toast.LENGTH_LONG).show());
                    return;
                }
                java.net.URL url = new java.net.URL("https://api.spotify.com/v1/me/player/pause");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                int responseCode = conn.getResponseCode();
                android.util.Log.d("MusicFragment", "Código de respuesta de Spotify: " + responseCode);
                String errorMessage = "";
                if (responseCode != 204 && responseCode != 202) {
                    try {
                        java.io.InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                            errorMessage = s.hasNext() ? s.next() : "";
                            android.util.Log.e("MusicFragment", "Respuesta de error de Spotify: " + errorMessage);
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("MusicFragment", "Error leyendo el cuerpo de error", ex);
                    }
                }
                final int respCode = responseCode;
                final String errMsg = errorMessage;


requireActivity().runOnUiThread(() -> {
                    if (respCode == 204) {
                        Toast.makeText(requireContext(), "Pausado", Toast.LENGTH_SHORT).show();
                        isPlaying = false;
                        updatePlayPauseIcon();
                    } else {
                        Toast.makeText(requireContext(), "No se pudo pausar", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error al pausar: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Versión con callback para play
    private void playSpotifyUriWithCallback(String uri, ResumeCallback callback) {
        new Thread(() -> {
            try {
                AuthManager authManager = new AuthManager(requireContext());
                String accessToken = authManager.getAccessToken();
                if (accessToken == null) {
                    

requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "No hay sesión de Spotify", Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onResult(false);
                    });
                    return;
                }
                java.net.URL url = new java.net.URL("https://api.spotify.com/v1/me/player/play");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String jsonBody = "{\"uris\":[\"" + uri + "\"]}";
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = conn.getResponseCode();
                android.util.Log.d("MusicFragment", "Código de respuesta de Spotify (play): " + responseCode);
                String errorMessage = "";
                if (responseCode != 204 && responseCode != 202) {
                    try {
                        java.io.InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                            errorMessage = s.hasNext() ? s.next() : "";
                            android.util.Log.e("MusicFragment", "Respuesta de error de Spotify (play): " + errorMessage);
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("MusicFragment", "Error leyendo el cuerpo de error (play)", ex);
                    }
                }
                final int respCode2 = responseCode;
                final String errMsg2 = errorMessage;
                

requireActivity().runOnUiThread(() -> {
                    if (respCode2 == 204 || respCode2 == 202) {
                        Toast.makeText(requireContext(), "Reproduciendo", Toast.LENGTH_SHORT).show();
                        isPlaying = true;
                        updatePlayPauseIcon();
                        if (callback != null) callback.onResult(true);
                    } else {
                        String msg = "No se pudo reproducir";
                        if (respCode2 == 403 && errMsg2.contains("PREMIUM_REQUIRED")) {
                            msg = "Spotify Premium requerido para reproducir";
                        } else if (respCode2 == 404) {
                            msg = "No hay dispositivo activo para reproducir";
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                        updatePlayPauseIcon();
                        if (callback != null) callback.onResult(false);
                    }
                });
            } catch (Exception e) {
                

requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error al reproducir: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updatePlayPauseIcon();
                    if (callback != null) callback.onResult(false);
                });
            }
        }).start();
    }
    private void resumeSpotify() {
        if (lastPlayedUri == null) {
            Toast.makeText(requireContext(), "No hay canción para reanudar", Toast.LENGTH_SHORT).show();
            return;
        }
        playSpotifyUri(lastPlayedUri);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                if (isPlaying) {
                    pauseSpotify();
                } else {
                    resumeSpotify();
                }
            });
        }
    }
}
