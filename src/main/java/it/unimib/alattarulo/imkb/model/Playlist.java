package it.unimib.alattarulo.imkb.model;

import lombok.Data;
import java.util.List;

@Data
public class Playlist {
    private String id;
    private String name;
    private List<Track> tracks;
}