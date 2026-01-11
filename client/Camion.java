package client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Camion {
    private String id;
    private Color couleur; // Pour distinguer les tournées sur la carte
    private List<Ville> villesALivrer; // Affectation (Input)
    private List<Ville> tourneeOrdonnee; // Résultat du TSP (Output)

    public Camion(String id, Color couleur) {
        this.id = id;
        this.couleur = couleur;
        this.villesALivrer = new ArrayList<>();
        this.tourneeOrdonnee = new ArrayList<>();
    }

    public void ajouterVille(Ville v) {
        this.villesALivrer.add(v);
    }

    public List<Ville> getVillesALivrer() { return villesALivrer; }
    
    public void setTourneeOrdonnee(List<Ville> tournee) {
        this.tourneeOrdonnee = tournee;
    }
    
    public List<Ville> getTourneeOrdonnee() { return tourneeOrdonnee; }
    public Color getCouleur() { return couleur; }
    public String getId() { return id; }
}
