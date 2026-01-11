package client;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ComposantCarte extends JPanel {
    private List<Camion> flotte;

    public void setFlotte(List<Camion> flotte) {
        this.flotte = flotte;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (flotte == null || flotte.isEmpty()) {
            g2.drawString("Aucune donnée chargée.", 20, 20);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        int padding = 50;

        // 1. Bornes
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        boolean hasData = false;
        for (Camion c : flotte) {
            List<Ville> pts = (c.getTourneeOrdonnee().isEmpty()) ? c.getVillesALivrer() : c.getTourneeOrdonnee();
            for (Ville v : pts) {
                if (v.getLat() < minLat)
                    minLat = v.getLat();
                if (v.getLat() > maxLat)
                    maxLat = v.getLat();
                if (v.getLon() < minLon)
                    minLon = v.getLon();
                if (v.getLon() > maxLon)
                    maxLon = v.getLon();
                hasData = true;
            }
        }
        if (!hasData)
            return;

        // 2. Dessin
        double totalDist = 0;

        for (Camion c : flotte) {
            List<Ville> tournee = c.getTourneeOrdonnee();
            if (tournee == null || tournee.isEmpty()) {
                tournee = c.getVillesALivrer(); // Pas encore calculé
                g2.setColor(Color.LIGHT_GRAY);
            } else {
                g2.setColor(c.getCouleur()); // Rouge ou Bleu
            }

            g2.setStroke(new BasicStroke(2));

            for (int i = 0; i < tournee.size() - 1; i++) {
                Ville v1 = tournee.get(i);
                Ville v2 = tournee.get(i + 1);

                Point p1 = toScreen(v1, w, h, padding, minLon, maxLon, minLat, maxLat);
                Point p2 = toScreen(v2, w, h, padding, minLon, maxLon, minLat, maxLat);

                // Ligne
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                // Distance (Km) au milieu
                double distKm = calculDistance(v1, v2);
                totalDist += distKm;

                int midX = (p1.x + p2.x) / 2;
                int midY = (p1.y + p2.y) / 2;

                g2.setColor(Color.BLUE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString(String.format("%.0f", distKm), midX, midY);
                g2.setColor(c.getCouleur()); // Restore couleur camion
            }

            // Villes
            for (Ville v : tournee) {
                Point p = toScreen(v, w, h, padding, minLon, maxLon, minLat, maxLat);
                g2.setColor(Color.RED);
                g2.fillOval(p.x - 5, p.y - 5, 10, 10);

                g2.setColor(Color.BLUE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(v.getNom(), p.x + 8, p.y - 5);
            }
        }

        // Affichage total en bas
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("Longueur chemin trouvé (en km) = " + (int) totalDist, 20, h - 20);
    }

    private double calculDistance(Ville a, Ville b) {
        double R = 6378.137;
        double dLat = Math.toRadians(b.getLat() - a.getLat());
        double dLon = Math.toRadians(b.getLon() - a.getLon());
        double lat1 = Math.toRadians(a.getLat());
        double lat2 = Math.toRadians(b.getLat());

        double u = Math.sin(dLat / 2);
        double v = Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(u * u + Math.cos(lat1) * Math.cos(lat2) * v * v));
        return R * c;
    }

    // Transformation Affine : GPS -> Ecran
    private Point toScreen(Ville v, int w, int h, int pad, double minLon, double maxLon, double minLat, double maxLat) {
        // X correspond à la Longitude
        int x = (int) (pad + (v.getLon() - minLon) / (maxLon - minLon) * (w - 2 * pad));
        // Y correspond à la Latitude (Inversé car l'écran Y descend)
        int y = (int) ((h - pad) - (v.getLat() - minLat) / (maxLat - minLat) * (h - 2 * pad));
        return new Point(x, y);
    }
}
