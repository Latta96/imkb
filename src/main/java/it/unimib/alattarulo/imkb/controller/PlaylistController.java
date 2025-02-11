package it.unimib.alattarulo.imkb.controller;

import it.unimib.alattarulo.imkb.model.Playlist;
import it.unimib.alattarulo.imkb.model.Track;
import it.unimib.alattarulo.imkb.service.SpotifyService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

@Controller
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private SpotifyService spotifyService;

    @GetMapping("/")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }

    @GetMapping("/import")
    @ResponseBody
    public List<Playlist> importPlaylist(@AuthenticationPrincipal OAuth2User principal) {
        return spotifyService.getPlaylists(principal);
    }

    @GetMapping("/{playlistId}/tracks")
    @ResponseBody
    public List<Track> getTracks(@AuthenticationPrincipal OAuth2User principal,
                                 @PathVariable String playlistId) {
        return spotifyService.getTracksForPlaylist(principal, playlistId);
    }
}