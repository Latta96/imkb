package it.unimib.alattarulo.imkb.model;

import lombok.Data;

@Data
public class Album {
    private String title;
    private Artist artist;
    private String releaseDate;
    private String genre;
}