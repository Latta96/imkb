package it.unimib.alattarulo.imkb.model;

import lombok.Data;
import java.util.List;

@Data
public class Playlist {
    private String name;
    private List<Song> songs;
}