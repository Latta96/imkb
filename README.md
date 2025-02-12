# Integrated Music Knowledge Base (IMKB)

Questo progetto è stato sviluppato per il corso di Introduzione all'Intelligenza Artificiale presso l'Università degli Studi di Milano-Bicocca. Utilizza Spring Boot per creare un'applicazione web che implementa vari algoritmi di intelligenza artificiale.

## Descrizione del Progetto

### Obiettivo
L'obiettivo principale è familiarizzare con il problema di costruire basi di conoscenza a partire da sorgenti diverse, usando i knowledge graph (KG) sia come sorgente sia come modello integrato della conoscenza nel dominio della musica.

### Idea di Fondo
Arricchire dati musicali estratti da sorgenti non semantiche (playlist, ed eventualmente canzoni) con dati provenienti da KG relativi soprattutto agli artisti; usare i dati arricchiti per elaborazioni di vario tipo non possibili sui dati di partenza.

### Knowledge Graph di Riferimento
- Wikidata 

## Struttura del Progetto

- **src/main/java/it/unimib/alattarulo/imkb**: Contiene il codice sorgente dell'applicazione.
- **src/main/resources**: Contiene i file di configurazione e le risorse statiche.

## Requisiti

- Java 17
- Utenza di Spotify

## Configurazione

1. Clona il repository:
    ```sh
    git clone https://github.com/Latta96/imkb
    cd imkb
    ```

2. Costruisci il progetto con Maven:
    ```sh
    ./mvnw clean install
    ```

3. Avvia l'applicazione:
    ```sh
    ./mvnw spring-boot:run
    ```

## Autori

- [Andrea Lattarulo](https://github.com/Latta96)
