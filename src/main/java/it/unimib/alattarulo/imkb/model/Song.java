package it.unimib.alattarulo.imkb.model;

import lombok.Data;

@Data
public class Song {
    private String title;
    private Artist artist;
    private Album album;
    private String releaseDate;
}