package it.unimib.alattarulo.imkb.controller;

import it.unimib.alattarulo.imkb.model.Artist;
import it.unimib.alattarulo.imkb.model.Playlist;
import it.unimib.alattarulo.imkb.service.WikidataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private WikidataService wikidataService;

    @PostMapping("/embed")
    public Playlist embedPlaylist(@RequestBody Playlist playlist) {
        //TODO: logica embedding playlist
        return playlist;
    }

    @GetMapping("/artist-info/{artistName}")
    public Artist getArtistInfo(@PathVariable String artistName) {
        return wikidataService.getArtistInfo(artistName);
    }
}