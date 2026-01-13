package client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LecteurDonnees {

    public static List<Ville> lireVillesJSON(String cheminFichier) {
        List<Ville> liste = new ArrayList<>();

        // Regex flexible pour supporter "nom"/"ville", "lat"/"latitude",
        // "lon"/"longitude"
        Pattern patternNom = Pattern.compile("\"(?:nom|ville)\"\\s*:\\s*\"([^\"]+)\"");
        Pattern patternLat = Pattern.compile("\"(?:lat|latitude)\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Pattern patternLon = Pattern.compile("\"(?:lon|longitude)\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)");

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(cheminFichier),
                        java.nio.charset.StandardCharsets.ISO_8859_1))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                // On cherche les motifs dans la ligne
                Matcher mNom = patternNom.matcher(ligne);
                Matcher mLat = patternLat.matcher(ligne);
                Matcher mLon = patternLon.matcher(ligne);

                if (mNom.find() && mLat.find() && mLon.find()) {
                    String nom = mNom.group(1);
                    double lat = Double.parseDouble(mLat.group(1));
                    double lon = Double.parseDouble(mLon.group(1));

                    liste.add(new Ville(nom, lat, lon));
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lecture fichier : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Lit un fichier CSV de matrice de routes.
     * Format attendu : ;Ville1;Ville2...
     * Ville1;Type;Type...
     */
    public static java.util.Map<String, java.util.Map<String, String>> lireMatriceRoutes(String cheminFichier) {
        java.util.Map<String, java.util.Map<String, String>> matrice = new java.util.HashMap<>();

        // Utilisation explicite de UTF-8
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(cheminFichier),
                        java.nio.charset.StandardCharsets.ISO_8859_1))) {
            String ligneHeader = br.readLine();
            if (ligneHeader == null)
                return matrice;

            // Enlever le BOM UTF-8 si present au debut du header
            if (ligneHeader.startsWith("\uFEFF")) {
                ligneHeader = ligneHeader.substring(1);
            }

            String[] villesCols = ligneHeader.split(";");
            // villesCols[0] est vide ou "Villes"

            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(";", -1); // -1 pour garder les chaines vides
                if (parts.length < 1)
                    continue;

                String villeDepart = parts[0];
                if (villeDepart.isEmpty())
                    continue;

                for (int i = 1; i < parts.length && i < villesCols.length; i++) {
                    String villeArrivee = villesCols[i];
                    String typeRoute = parts[i].trim();

                    if (!typeRoute.isEmpty()) {
                        matrice.computeIfAbsent(villeDepart, k -> new java.util.HashMap<>())
                                .put(villeArrivee, typeRoute);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Info: Pas de fichier de routes ou erreur lecture : " + e.getMessage());
        }
        return matrice;
    }
}
