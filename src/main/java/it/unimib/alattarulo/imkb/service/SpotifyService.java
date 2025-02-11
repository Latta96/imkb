package it.unimib.alattarulo.imkb.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimib.alattarulo.imkb.model.Artist;
import it.unimib.alattarulo.imkb.model.Playlist;
import it.unimib.alattarulo.imkb.model.Track;
import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpotifyService {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private WikidataService wikidataService;

    public List<Playlist> getPlaylists(OAuth2User principal) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            "spotify", principal.getName());

        String playlistsJson = webClientBuilder.build()
            .get()
            .uri("https://api.spotify.com/v1/me/playlists")
            .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        List<Playlist> playlists = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            SpotifyPlaylistsResponse resp = mapper.readValue(
                playlistsJson, SpotifyPlaylistsResponse.class);

            if (resp.getItems() != null) {
                for (SpotifyPlaylistItem item : resp.getItems()) {
                    Playlist playlist = new Playlist();
                    playlist.setId(item.getId());
                    playlist.setName(item.getName());
                    playlists.add(playlist);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return playlists;
    }

    public List<Track> getTracksForPlaylist(OAuth2User principal, String playlistId) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            "spotify", principal.getName());

        String tracksJson = webClientBuilder.build()
            .get()
            .uri("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
            .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        List<Track> tracks = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            SpotifyTracksResponse trackResp = mapper.readValue(tracksJson, SpotifyTracksResponse.class);

            for (SpotifyTrackItem item : trackResp.getItems()) {
                if (item != null && item.getTrack() != null) {
                    Track t = new Track();
                    t.setTitle(item.getTrack().getName());
                    t.setSpotifyTrackId(item.getTrack().getId());
                    if (item.getTrack().getArtists() != null && !item.getTrack().getArtists().isEmpty()) {
                        Artist artist = new Artist();
                        artist.setName(item.getTrack().getArtists().get(0).getName());
                        t.setArtist(artist);
                    }
                    tracks.add(t);
                }
            }

            List<Track> wikidataTracks = wikidataService.getTracksAndAuthors(tracks);

            Map<String, Track> wikiTrackMap = wikidataTracks.stream().collect(Collectors.toMap(
                    tk -> (tk.getTitle() + ":" + tk.getArtist().getName()).toLowerCase(),
                    tk -> tk,
                    (existing, replacement) -> existing
            ));

            for (Track t : tracks) {
                if (t.getArtist() != null) {
                    String key = (t.getTitle() + ":" + t.getArtist().getName()).toLowerCase();
                    Track wdTrack = wikiTrackMap.get(key);
                    if (wdTrack != null) {
                        t.setReleaseDate(wdTrack.getReleaseDate());
                    }
                }
            }
            
            Map<String, Artist> artistSummaryMap = new HashMap<>();
            for (Track t : tracks) {
                if (t.getArtist() != null) {
                    String artistName = t.getArtist().getName();
                    if (!artistSummaryMap.containsKey(artistName)) {
                        Artist artistSummary = wikidataService.getArtistInfo(artistName);
                        artistSummaryMap.put(artistName, artistSummary);
                    }
                }
            }

            for (Track t : tracks) {
                if (t.getArtist() != null) {
                    Artist summary = artistSummaryMap.get(t.getArtist().getName());
                    if (summary != null) {
                        t.getArtist().setBirthPlace(summary.getBirthPlace());
                        t.getArtist().setBirthDate(summary.getBirthDate());
                    }
                }
            }
            return tracks;
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tracks;
    }
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyPlaylistsResponse {
    private List<SpotifyPlaylistItem> items;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyPlaylistItem {
    private String id;
    private String name;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyTracksResponse {
    private List<SpotifyTrackItem> items;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyTrackItem {
    private SpotifyTrack track;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyTrack {
    private String id;
    private String name;
    private List<SpotifyArtist> artists;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class SpotifyArtist {
    private String name;
}