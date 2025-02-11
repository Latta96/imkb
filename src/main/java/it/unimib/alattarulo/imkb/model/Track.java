package it.unimib.alattarulo.imkb.model;

import lombok.Data;

@Data
public class Track {
    private String spotifyTrackId;
    private String title;
    private Artist artist;
    private String releaseDate;
}