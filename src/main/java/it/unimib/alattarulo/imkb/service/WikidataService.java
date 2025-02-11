package it.unimib.alattarulo.imkb.service;

import it.unimib.alattarulo.imkb.model.Artist;
import it.unimib.alattarulo.imkb.model.Track;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

@Service
public class WikidataService {

    private static final String WIKIDATA_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    private static final String SPARQL_PREFIXES =
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
        "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
        "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
        "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
        "PREFIX bd: <http://www.bigdata.com/rdf#>\n";

    public Artist getArtistInfo(String artistName, String artistId) {
        String queryStr =
            SPARQL_PREFIXES +
            "SELECT ?artist ?artistLabel ?birthPlaceLabel ?birthDate ?countryLabel ?fundationDate WHERE {\n" +
        "  {\n" +
        "    ?artist wdt:P1902 \"" + artistId + "\".\n" +
        "    OPTIONAL { ?artist wdt:P19 ?birthPlace. }\n" +
        "    OPTIONAL { ?artist wdt:P569 ?birthDate. }\n" +
        "    OPTIONAL { ?artist wdt:P571 ?fundationDate. }\n" +
        "    OPTIONAL { ?artist wdt:P495 ?country. }\n" +
        "  }\n" +
        "  UNION\n" +
        "  {\n" +
        "    ?artist rdfs:label \"" + artistName + "\"@en.\n" +
        "    ?artist wdt:P31 ?type.\n" + 
        "    FILTER(?type = wd:Q5 || ?type = wd:Q215380 || ?type = wd:Q5741069)\n" +
        "    OPTIONAL { ?artist wdt:P19 ?birthPlace. }\n" +
        "    OPTIONAL { ?artist wdt:P569 ?birthDate. }\n" +
        "    OPTIONAL { ?artist wdt:P571 ?fundationDate. }\n" +
        "    OPTIONAL { ?artist wdt:P495 ?country. }\n" +
        "  }\n" +
        "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,it\". }\n" +
        "} LIMIT 1";
    
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionHTTP
                .service(WIKIDATA_SPARQL_ENDPOINT)
                .query(query)
                .build()) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Artist artist = new Artist();
                artist.setName(soln.getLiteral("artistLabel").getString());
    
                // Se umano, luogo di nascita; se gruppo, paese di origine
                String place = "N/D";
                if (soln.contains("birthPlaceLabel")) {
                    place = soln.getLiteral("birthPlaceLabel").getString();
                } else if (soln.contains("countryLabel")) {
                    place = soln.getLiteral("countryLabel").getString();
                }
                artist.setBirthPlace(place);
    
                // Data di nascita o data di fondazione formattata
                String date = "N/D";
                if (soln.contains("birthDate")) {
                    date = convertXsdToDdMmYyyy(soln.getLiteral("birthDate").getLexicalForm());
                } else if (soln.contains("fundationDate")) {
                    date = convertXsdToDdMmYyyy(soln.getLiteral("fundationDate").getLexicalForm());
                }
                artist.setBirthDate(date);
    
                return artist;
            }
        }
        return null;
    }

    public List<Track> getTracksAndAuthors(List<Track> spotifyTracks) {
        List<Track> results = new ArrayList<>();
        List<Pair<String, String>> pairsForQuery = new ArrayList<>();
    
        List<Track> matchedBySpotify = getTracksAndAuthorsBySpotifyIds(spotifyTracks);
        results.addAll(matchedBySpotify);

        for (Track st : spotifyTracks) {
            boolean found = false;
            for (Track m : matchedBySpotify) {
                if (st.getTitle().equalsIgnoreCase(m.getTitle()) &&
                    st.getArtist().getName().equalsIgnoreCase(m.getArtist().getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                pairsForQuery.add(Pair.of(st.getTitle(), st.getArtist().getName()));
            }
        }

        if (!pairsForQuery.isEmpty()) {
            results.addAll(getTracksAndAuthorsInternal(pairsForQuery));
        }
    
        return results;
    }

    public List<Track> getTracksAndAuthorsBySpotifyIds(List<Track> spotifyTracks) {
        List<Track> tracks = new ArrayList<>();
        if (spotifyTracks.isEmpty()) {
            return tracks;
        }
    
        StringBuilder valuesBuilder = new StringBuilder();
        for (Track track : spotifyTracks) {
            String sanitizedId = track.getSpotifyTrackId().replace("\"", "\\\"");
            valuesBuilder.append("(\"").append(sanitizedId).append("\") ");
        }
    
        String queryStr = SPARQL_PREFIXES +
            "SELECT DISTINCT ?song ?songLabel ?artist ?artistLabel ?releaseDate WHERE {\n" +
            "  VALUES (?spotifyId) {\n" +
            "    " + valuesBuilder.toString() + "\n" +
            "  }\n" +
            "  ?song wdt:P2207 ?spotifyId .\n" +
            "  ?song wdt:P175 ?artist .\n" +
            "  OPTIONAL { ?song wdt:P577 ?releaseDate } .\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,it\". }\n" +
            "}";
        
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionHTTP
                 .service(WIKIDATA_SPARQL_ENDPOINT)
                 .query(query)
                 .build()) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Track track = new Track();
                // Titolo della canzone
                track.setTitle(soln.getLiteral("songLabel").getString());
        
                // Artista
                Artist artist = new Artist();
                artist.setName(soln.getLiteral("artistLabel").getString());
                track.setArtist(artist);
        
                // Data di rilascio formattata
                try{
                    String lexicalDate = soln.getLiteral("releaseDate").getLexicalForm();
                    String formattedDate = convertXsdToDdMmYyyy(lexicalDate);
                    track.setReleaseDate(formattedDate);
                } catch (Exception e) {
                    track.setReleaseDate("N/D");
                }
        
                tracks.add(track);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return tracks;
    }

    public List<Track> getTracksAndAuthorsInternal(Collection<Pair<String, String>> titleArtistPairs) {
        List<Track> tracks = new ArrayList<>();
    
        StringBuilder valuesBuilder = new StringBuilder();
        for (Pair<String, String> pair : titleArtistPairs) {
            String track = pair.getLeft().replace("\"", "\\\"");
            String artist = pair.getRight().replace("\"", "\\\"");
            valuesBuilder.append("(\"").append(track).append("\"@en \"").append(artist).append("\"@en) ");
        }
    
        String queryStr = SPARQL_PREFIXES + 
            "SELECT ?song ?songLabel ?artist ?artistLabel ?releaseDate WHERE {\n" +
            "  VALUES (?songLabel ?artistLabel) {\n" +
            "    " + valuesBuilder.toString() + "\n" +
            "  }\n" +
            "  ?song rdfs:label ?songLabel.\n" +
            "  ?song wdt:P175 ?artist.\n" +
            "  ?artist rdfs:label ?artistLabel.\n" +
            "  ?song wdt:P31 ?type.\n" +
            "  FILTER(?type = wd:Q2188189 || ?type = wd:Q134556)\n" +
            "  ?song wdt:P577 ?releaseDate.\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,it\". }\n" +
            "}";
    
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionHTTP
                 .service(WIKIDATA_SPARQL_ENDPOINT)
                 .query(query)
                 .build()) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
    
                Track track = new Track();
                track.setTitle(soln.getLiteral("songLabel").getString());
                Artist artist = new Artist();
                artist.setName(soln.getLiteral("artistLabel").getString());
                track.setArtist(artist);
                String lexicalDate = soln.getLiteral("releaseDate").getLexicalForm();
                String formattedDate = convertXsdToDdMmYyyy(lexicalDate);
                track.setReleaseDate(formattedDate);
    
                tracks.add(track);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return tracks;
    }
    
    public List<Track> getTracksAndAuthors(Collection<Pair<String, String>> titleArtistPairs) {
        List<Track> tracks = new ArrayList<>();
    
        StringBuilder valuesBuilder = new StringBuilder();
        for (Pair<String, String> pair : titleArtistPairs) {
            String track = pair.getLeft().replace("\"", "\\\"");
            String artist = pair.getRight().replace("\"", "\\\"");
            valuesBuilder.append("(\"").append(track).append("\"@en \"").append(artist).append("\"@en) ");
        }
    
        String queryStr = SPARQL_PREFIXES + 
            "SELECT ?song ?songLabel ?artist ?artistLabel ?releaseDate WHERE {\n" +
            "  VALUES (?songLabel ?artistLabel) {\n" +
            "    " + valuesBuilder.toString() + "\n" +
            "  }\n" +
            "  ?song rdfs:label ?songLabel.\n" +
            "  ?song wdt:P175 ?artist.\n" +
            "  ?artist rdfs:label ?artistLabel.\n" +
            "  ?song wdt:P31 ?type.\n" +
            "  FILTER(?type = wd:Q2188189 || ?type = wd:Q134556)\n" +
            "  ?song wdt:P577 ?releaseDate.\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en,it\". }\n" +
            "}";
    
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionHTTP
                 .service(WIKIDATA_SPARQL_ENDPOINT)
                 .query(query)
                 .build()) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
    
                Track track = new Track();
                track.setTitle(soln.getLiteral("songLabel").getString());
                Artist artist = new Artist();
                artist.setName(soln.getLiteral("artistLabel").getString());
                track.setArtist(artist);
                String lexicalDate = soln.getLiteral("releaseDate").getLexicalForm();
                String formattedDate = convertXsdToDdMmYyyy(lexicalDate);
                track.setReleaseDate(formattedDate);
    
                tracks.add(track);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return tracks;
    }

    /**
     * Converte una stringa in formato xsd:dateTime (es: 2021-01-01T00:00:00Z o 2021-01-01)
     * in formato dd/MM/yyyy (es: 01/01/2021).
     */
    private String convertXsdToDdMmYyyy(String xsdDateStr) {
        /*  Molti valori di P577 possono apparire come:
        /   YYYY-MM-DD
        /   YYYY-MM-DDT00:00:00Z
        /   YYYY
        */
        try {
            if (xsdDateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // Formato YYYY-MM-DD
                LocalDate date = LocalDate.parse(xsdDateStr);
                return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (xsdDateStr.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
                // Formato con T e forse Z (es. 2021-01-01T00:00:00Z)
                OffsetDateTime odt = OffsetDateTime.parse(xsdDateStr);
                return odt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else if (xsdDateStr.matches("\\d{4}")) {
                // Solo l'anno
                return "01/01/" + xsdDateStr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Se il parsing fallisce o non corrisponde ai pattern, restituiamo la stringa originale
        return xsdDateStr;
    }
}