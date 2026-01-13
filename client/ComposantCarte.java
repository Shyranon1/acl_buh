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

    private java.util.Map<String, java.util.Map<String, String>> routesMap;

    public void setRoutesMap(java.util.Map<String, java.util.Map<String, String>> routesMap) {
        this.routesMap = routesMap;
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

        // 2. Dessin des Routes
        double totalDist = 0;

        for (Camion c : flotte) {
            List<Ville> tournee = c.getTourneeOrdonnee();
            if (tournee == null || tournee.isEmpty()) {
                tournee = c.getVillesALivrer(); // Pas encore calculé
            }

            for (int i = 0; i < tournee.size() - 1; i++) {
                Ville v1 = tournee.get(i);
                Ville v2 = tournee.get(i + 1);

                Point p1 = toScreen(v1, w, h, padding, minLon, maxLon, minLat, maxLat);
                Point p2 = toScreen(v2, w, h, padding, minLon, maxLon, minLat, maxLat);

                // Déterminer le style de route
                String type = "Inconnu";
                if (routesMap != null) {
                    if (routesMap.containsKey(v1.getNom()) && routesMap.get(v1.getNom()).containsKey(v2.getNom())) {
                        type = routesMap.get(v1.getNom()).get(v2.getNom());
                    } else if (routesMap.containsKey(v2.getNom())
                            && routesMap.get(v2.getNom()).containsKey(v1.getNom())) {
                        type = routesMap.get(v2.getNom()).get(v1.getNom());
                    }
                }

                type = type.toLowerCase();
                Stroke stroke;
                Color col = c.getCouleur(); // Couleur de base du camion

                // Styles
                // Styles
                // Styles High Contrast
                if (type.contains("autoroute")) {
                    stroke = new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); // TRES EPAIS
                } else if (type.contains("rapide")) {
                    stroke = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); // EPAIS
                } else if (type.contains("europe")) {
                    float[] dash = { 10.0f, 10.0f }; // TIRETS TRES DISTINCTS (BLOCKY)
                    stroke = new BasicStroke(5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
                } else if (type.contains("national")) {
                    float[] dash = { 5.0f, 5.0f }; // TIRETS COURTS
                    stroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
                } else {
                    stroke = new BasicStroke(1.0f); // FIN
                }

                g2.setStroke(stroke);
                g2.setColor(col);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                // Distance (Km) au milieu
                double distKm = calculDistance(v1, v2);
                totalDist += distKm;

                // Affichage discret de la distance

                // g2.setColor(Color.DARK_GRAY);
                // g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                // g2.drawString(String.format("%.0f", distKm), midX, midY);
            }

            // Villes (Dessin par dessus les routes)
            for (Ville v : tournee) {
                Point p = toScreen(v, w, h, padding, minLon, maxLon, minLat, maxLat);
                g2.setColor(Color.WHITE);
                g2.fillOval(p.x - 4, p.y - 4, 8, 8);
                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(p.x - 4, p.y - 4, 8, 8);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString(v.getNom(), p.x + 6, p.y + 4);
            }
        }

        // Affichage total en bas
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("Longueur totale : " + (int) totalDist + " km", 20, h - 20);

        // LEGENDE
        drawLegend(g2, w, h);
    }

    private void drawLegend(Graphics2D g2, int w, int h) {
        int boxW = 220; // Plus large pour le texte
        int boxH = 130;
        int x = w - boxW - 20;
        int y = h - boxH - 20;

        // Background
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(Color.GRAY);
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);

        // Titre
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString("Type de Route", x + 10, y + 20);

        // Items
        int rowH = 20;
        int currY = y + 40;

        // Autoroute
        g2.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(x + 10, currY, x + 50, currY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Autoroute (Très Rapide)", x + 60, currY + 4);

        currY += rowH;
        // Voie Rapide
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 10, currY, x + 50, currY);
        g2.drawString("Voie Rapide (Rapide)", x + 60, currY + 4);

        currY += rowH;
        // Europeenne
        float[] dashEu = { 10.0f, 10.0f };
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashEu, 0.0f));
        g2.drawLine(x + 10, currY, x + 50, currY);
        g2.drawString("R. Européenne (Rapide)", x + 60, currY + 4);

        currY += rowH;
        // Nationale
        float[] dashNat = { 5.0f, 5.0f };
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashNat, 0.0f));
        g2.drawLine(x + 10, currY, x + 50, currY);
        g2.drawString("Nationale (Rapide)", x + 60, currY + 4);

        currY += rowH;
        // Autre
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(x + 10, currY, x + 50, currY);
        g2.drawString("Dép/Autre (Standard)", x + 60, currY + 4);
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
