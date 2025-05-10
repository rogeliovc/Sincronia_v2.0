package com.example.sincronia;

public class Song {
    private String title;
    private String artist;
    private String duration;
    private int coverResId = -1; // -1 si no es recurso local
    private String coverUrl;     // null si no es URL
    private String uri; // URI de Spotify o null

    // Constructor para recursos locales
    public Song(String title, String artist, String duration, int coverResId) {
        this(title, artist, duration, coverResId, null, null);
    }

    // Constructor para URLs
    public Song(String title, String artist, String duration, String coverUrl) {
        this(title, artist, duration, -1, coverUrl, null);
    }

    // Constructor extendido para Spotify
    public Song(String title, String artist, String duration, int coverResId, String coverUrl, String uri) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.coverResId = coverResId;
        this.coverUrl = coverUrl;
        this.uri = uri;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public int getCoverResId() { return coverResId; }
    public String getCoverUrl() { return coverUrl; }
    public String getUri() { return uri; }
}
