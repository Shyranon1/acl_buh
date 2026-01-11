package client;

import java.io.Serializable;

public class Ville implements Serializable {
    private String nom;
    private double latitude;
    private double longitude;

    public Ville(String nom, double latitude, double longitude) {
        this.nom = nom;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters pour JSON et Affichage
    public String getNom() { return nom; }
    public double getLat() { return latitude; }
    public double getLon() { return longitude; }
    
    @Override
    public String toString() { return nom; }
}
