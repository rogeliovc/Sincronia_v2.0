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

// Spotify SDK imports para autorización
// --- OAuth PKCE: imports para Custom Tabs y red ---
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
// ...otros imports necesarios para red y almacenamiento...

public class MusicFragment extends Fragment {
    // --- Spotify App Remote ---
    private static final String CLIENT_ID = "4caddaabcd134c6da47d4f7d1c7877ba"; // Reemplaza con tu Client ID
    private static final String REDIRECT_URI = "sincronia://callback"; // Reemplaza con tu Redirect URI
    private com.spotify.android.appremote.api.SpotifyAppRemote mSpotifyAppRemote;
    // PKCE OAuth variables
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token";
    private static final String[] SCOPES = {"app-remote-control", "user-modify-playback-state", "user-read-playback-state"};
    private static final int REQUEST_CODE_SPOTIFY_LOGIN = 1337;
    private String spotifyAccessToken = null;
    private String codeVerifier = null;

    // Required empty public constructor
    public MusicFragment() {}

    private RecyclerView rvPlaylists, rvFavorites, rvRecent;
    private PlaylistAdapter playlistAdapter;
    private SongAdapter favoritesAdapter, recentAdapter;
    private List<Playlist> playlistList;
    private List<Song> favoritesList, recentList;
    private MusicPlayerViewModel musicPlayerViewModel;

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

    // Conexión de botones a los métodos del SDK de Spotify
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    loadSpotifyData();
        super.onViewCreated(view, savedInstanceState);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnNext = view.findViewById(R.id.btnNext);
        btnPrev = view.findViewById(R.id.btnPrev);

        btnPlayPause.setOnClickListener(v -> {
            // Puedes alternar entre play/pause según el estado actual
            if (isPlaying) {
                pauseSpotify();
                isPlaying = false;
            } else {
                // Si tienes el URI actual, úsalo aquí
                Song currentSong = musicPlayerViewModel.getCurrentSong().getValue();
                if (currentSong != null && currentSong.getUri() != null) {
                    playSpotifyUri(currentSong.getUri());
                    isPlaying = true;
                } else {
                    Toast.makeText(requireContext(), "No hay canción seleccionada", Toast.LENGTH_SHORT).show();
                }
            }
            updatePlayPauseIcon();
        });

        btnNext.setOnClickListener(v -> {
            nextSpotify();
            musicPlayerViewModel.next(); // Opcional: sincroniza UI local
        });

        btnPrev.setOnClickListener(v -> {
            previousSpotify();
            musicPlayerViewModel.previous(); // Opcional: sincroniza UI local
        });
        // Puedes conectar más controles aquí si lo deseas
    }
    // Fin de conexión de botones


    // Inicia el flujo OAuth PKCE usando Custom Tabs
    private void launchSpotifyLoginPKCE() {
        codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String scopes = String.join(" ", SCOPES);
        Uri authUri = Uri.parse(AUTHORIZATION_ENDPOINT)
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("scope", scopes)
                .build();
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(requireContext(), authUri);
    }

    // Genera un code verifier PKCE
    private String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Genera un code challenge PKCE
    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes("US-ASCII");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(bytes, 0, bytes.length);
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Error generando code_challenge", e);
        }
    }

    // Debes capturar el intent de redirección en tu Activity (onNewIntent o similar) y extraer el 'code' para intercambiarlo por el access_token.
    // Luego, llama a un método como 'exchangeCodeForToken(code)' para obtener el token de acceso y refresco.
    // Guarda el token y úsalo para conectar App Remote y la Web API.

    // Conecta con Spotify App Remote usando el token recibido
    private void connectToSpotifyRemote() {
        com.spotify.android.appremote.api.ConnectionParams connectionParams =
                new com.spotify.android.appremote.api.ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();
        com.spotify.android.appremote.api.SpotifyAppRemote.connect(requireContext(), connectionParams,
                new com.spotify.android.appremote.api.Connector.ConnectionListener() {
                    @Override
                    public void onConnected(com.spotify.android.appremote.api.SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Toast.makeText(requireContext(), "Conectado a Spotify", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        Toast.makeText(requireContext(), "No se pudo conectar a Spotify: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Chequeo de instalación de la app oficial de Spotify
        boolean spotifyInstalled = false;
        try {
            android.content.pm.PackageManager pm = requireContext().getPackageManager();
            android.content.pm.PackageInfo info = pm.getPackageInfo("com.spotify.music", 0);
            spotifyInstalled = true;
            // Usa AuthManager para obtener y validar el token
            String token = com.example.sincronia.AuthManager.getGlobalAccessToken();
            if (token == null || !com.example.sincronia.AuthManager.getInstance().isTokenValid()) {
                launchSpotifyLoginPKCE();
                return;
            }
            spotifyAccessToken = token;
            connectToSpotifyRemote();
        } catch (Exception e) {
            spotifyInstalled = false;
        }
        if (!spotifyInstalled) {
            Toast.makeText(requireContext(), "Debes instalar la app oficial de Spotify", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSpotifyAppRemote != null) {
            com.spotify.android.appremote.api.SpotifyAppRemote.disconnect(mSpotifyAppRemote);
            mSpotifyAppRemote = null;
        }
    }

    @Nullable
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

        playlistAdapter = new PlaylistAdapter(getContext(), playlistList, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(int position) {
                Playlist selectedPlaylist = playlistList.get(position);
                // Aquí puedes cargar las canciones de la playlist seleccionada usando la Web API
                // Por ejemplo: mostrar un nuevo fragmento o actualizar un RecyclerView de canciones
                // (Implementación detallada en el siguiente paso)
                Toast.makeText(getContext(), "Seleccionaste: " + selectedPlaylist.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        rvPlaylists.setAdapter(playlistAdapter);

        favoritesAdapter = new SongAdapter(getContext(), favoritesList, position -> {
            android.util.Log.d("MusicFragment", "Favorito seleccionado: " + favoritesList.get(position).getTitle());
            if (!favoritesList.isEmpty()) {
                // Lógica de UI local:
                musicPlayerViewModel.playSongFromList(new ArrayList<>(favoritesList), position);
                // Reproducción real en Spotify:
                playSpotifyUri(favoritesList.get(position).getUri());
            }
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

        // Inicializar ViewModel
        musicPlayerViewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(MusicPlayerViewModel.class);

        // Observadores para actualizar la UI
        musicPlayerViewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                tvSongTitle.setText(song.getTitle());
                tvSongArtist.setText(song.getArtist());
                if (song.getCoverUrl() != null) {
                    Glide.with(this).load(song.getCoverUrl()).placeholder(R.drawable.icon_default_music).into(ivSongArt);
                } else {
                    ivSongArt.setImageResource(song.getCoverResId());
                }
            }
        });
        musicPlayerViewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            isPlaying = playing != null && playing;
            updatePlayPauseIcon();
        });
        musicPlayerViewModel.getPlaybackPosition().observe(getViewLifecycleOwner(), pos -> {
            if (playerProgress != null && pos != null) playerProgress.setProgress(pos);
        });
        musicPlayerViewModel.getDuration().observe(getViewLifecycleOwner(), dur -> {
            if (playerProgress != null && dur != null) playerProgress.setMax(dur);
        });

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
                if (isPlaying) {
                    musicPlayerViewModel.pause();
                } else {
                    musicPlayerViewModel.play();
                }
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> musicPlayerViewModel.previous());
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> musicPlayerViewModel.next());
        }

        // Inicializar reproductor con la primera canción de favoritos
        if (!favoritesList.isEmpty()) {
            musicPlayerViewModel.playSongFromList(new ArrayList<>(favoritesList), 0);
        }

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
        List<Song> songs = musicPlayerViewModel.getPlaylist().getValue();
        Integer idx = musicPlayerViewModel.getCurrentIndex().getValue();
        if (songs == null || songs.isEmpty() || idx == null || idx < 0 || idx >= songs.size()) return;
        Song song = songs.get(idx);
        if (playerSheet == null) playerSheet = new PlayerBottomSheetFragment();
        playerSheet.setOnPlayerActionListener(new PlayerBottomSheetFragment.OnPlayerActionListener() {
            @Override public void onPrev() {
                musicPlayerViewModel.previous();
                // Actualiza UI del sheet
                Integer newIdx = musicPlayerViewModel.getCurrentIndex().getValue();
                if (newIdx != null && newIdx >= 0 && newIdx < songs.size()) {
                    Song newSong = songs.get(newIdx);
                    playerSheet.updateSong(newSong.getTitle(), newSong.getArtist(), newSong.getCoverUrl(), parseDurationMs(newSong.getDuration()));
                }
            }
            @Override public void onPlayPause() {
                if (isPlaying) {
                    musicPlayerViewModel.pause();
                    playerSheet.setPlaying(false);
                } else {
                    musicPlayerViewModel.play();
                    playerSheet.setPlaying(true);
                }
            }
            @Override public void onNext() {
                musicPlayerViewModel.next();
                Integer newIdx = musicPlayerViewModel.getCurrentIndex().getValue();
                if (newIdx != null && newIdx >= 0 && newIdx < songs.size()) {
                    Song newSong = songs.get(newIdx);
                    playerSheet.updateSong(newSong.getTitle(), newSong.getArtist(), newSong.getCoverUrl(), parseDurationMs(newSong.getDuration()));
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
        Integer idx = musicPlayerViewModel.getCurrentIndex().getValue();
        List<Song> songs = musicPlayerViewModel.getPlaylist().getValue();
        if (songs == null || songs.isEmpty() || idx == null || idx < 0 || idx >= songs.size()) return;
        Song song = songs.get(idx);
        tvSongTitle.setText(song.getTitle());
        tvSongArtist.setText(song.getArtist());
        if (song.getCoverUrl() != null) {
            Glide.with(this).load(song.getCoverUrl()).placeholder(R.drawable.icon_default_music).into(ivSongArt);
        } else {
            ivSongArt.setImageResource(song.getCoverResId());
        }
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
        String accessToken = AuthManager.getGlobalAccessToken();
        if (accessToken == null) {
            requireActivity().runOnUiThread(this::showError);
            return;
        }
        List<Playlist> realPlaylists = SpotifyService.getUserPlaylists(accessToken);
        List<Song> realFavorites = SpotifyService.getUserFavorites(accessToken);
        List<Song> realRecent = SpotifyService.getRecentlyPlayed(accessToken);
        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            // Playlists
            playlistList.clear();
            if (realPlaylists != null && !realPlaylists.isEmpty()) {
                playlistList.addAll(realPlaylists);
                rvPlaylists.setVisibility(View.VISIBLE);
                tvPlaylistsEmpty.setVisibility(View.GONE);
            } else {
                rvPlaylists.setVisibility(View.GONE);
                tvPlaylistsEmpty.setVisibility(View.VISIBLE);
            }
            playlistAdapter.notifyDataSetChanged();
            // Favoritos
            favoritesList.clear();
            if (realFavorites != null && !realFavorites.isEmpty()) {
                favoritesList.addAll(realFavorites);
                rvFavorites.setVisibility(View.VISIBLE);
                tvFavoritesEmpty.setVisibility(View.GONE);
            } else {
                rvFavorites.setVisibility(View.GONE);
                tvFavoritesEmpty.setVisibility(View.VISIBLE);
            }
            favoritesAdapter.notifyDataSetChanged();
            // Recientes
            recentList.clear();
            if (realRecent != null && !realRecent.isEmpty()) {
                recentList.addAll(realRecent);
                rvRecent.setVisibility(View.VISIBLE);
                tvRecentEmpty.setVisibility(View.GONE);
            } else {
                rvRecent.setVisibility(View.GONE);
                tvRecentEmpty.setVisibility(View.VISIBLE);
            }
            recentAdapter.notifyDataSetChanged();
        });
    }).start();
}

    // (Método duplicado eliminado para evitar error de sintaxis)
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

    // --- Métodos de control de reproducción usando Spotify App Remote ---
    private void playSpotifyUri(String uri) {
        if (mSpotifyAppRemote != null && uri != null && !uri.isEmpty()) {
            mSpotifyAppRemote.getPlayerApi().play(uri);
        } else {
            Toast.makeText(requireContext(), "No conectado a Spotify o URI inválida", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseSpotify() {
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi().pause();
        }
    }

    private void resumeSpotify() {
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi().resume();
        }
    }

    private void nextSpotify() {
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi().skipNext();
        }
    }

    private void previousSpotify() {
        if (mSpotifyAppRemote != null) {
            mSpotifyAppRemote.getPlayerApi().skipPrevious();
        }
    }


    // Callback para saber si la pausa fue exitosa
    private interface PauseCallback { void onResult(boolean success); }
    private void pauseSpotifyWithCallback(PauseCallback callback) {
        musicPlayerViewModel.pause();
        updatePlayPauseIcon();
        if (callback != null) callback.onResult(true);
    }
    // Eliminado: método duplicado resumeSpotify() innecesario porque ahora usamos el SDK directamente.

}


