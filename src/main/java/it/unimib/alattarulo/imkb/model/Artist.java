package it.unimib.alattarulo.imkb.model;

import lombok.Data;

@Data
public class Artist {
    private String spotifyArtistId;
    private String name;
    private String birthPlace;
    private String birthDate;
}