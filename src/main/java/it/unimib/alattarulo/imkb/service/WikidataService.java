package it.unimib.alattarulo.imkb.service;

import it.unimib.alattarulo.imkb.model.Artist;
import org.apache.jena.query.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.springframework.stereotype.Service;

@Service
public class WikidataService {

    private static final String WIKIDATA_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    public Artist getArtistInfo(String artistName) {
        String queryStr = String.format(
            "SELECT ?artist ?artistLabel ?birthPlaceLabel ?birthDate WHERE { " +
            "  ?artist ?label \"%s\"@en. " +
            "  ?artist wdt:P31 wd:Q5. " +
            "  ?artist wdt:P19 ?birthPlace. " +
            "  ?artist wdt:P569 ?birthDate. " +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\". } " +
            "} LIMIT 1", artistName);

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionHTTP.service(WIKIDATA_SPARQL_ENDPOINT).query(query).build()) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Artist artist = new Artist();
                artist.setName(soln.get("artistLabel").toString());
                artist.setBirthPlace(soln.get("birthPlaceLabel").toString());
                artist.setBirthDate(soln.get("birthDate").toString());
                return artist;
            }
        }
        return null;
    }
}