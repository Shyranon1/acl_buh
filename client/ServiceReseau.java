package client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServiceReseau {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    /**
     * Envoie une liste de villes et les types de routes au serveur C++.
     */
    public List<Ville> optimiserTournee(List<Ville> input, java.util.Map<String, java.util.Map<String, String>> routes,
            String profil) throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            StringBuilder sb = new StringBuilder();

            // SECTION PROFILE
            sb.append("PROFILE:").append(profil).append("\n");

            // SECTION VILLES
            sb.append("VILLES\n");
            for (int i = 0; i < input.size(); i++) {
                Ville v = input.get(i);
                sb.append(v.getNom()).append(",")
                        .append(v.getLat()).append(",")
                        .append(v.getLon());
                if (i < input.size() - 1)
                    sb.append(";");
            }
            sb.append("\n"); // Fin section VILLES

            // SECTION ROUTES (optimisée : envoyer seulement celles concernant les villes
            // input)
            sb.append("ROUTES\n");
            boolean first = true;
            for (Ville v1 : input) {
                if (routes.containsKey(v1.getNom())) {
                    java.util.Map<String, String> dests = routes.get(v1.getNom());
                    for (Ville v2 : input) {
                        if (dests.containsKey(v2.getNom())) {
                            if (!first)
                                sb.append(";");
                            sb.append(v1.getNom()).append(",")
                                    .append(v2.getNom()).append(",")
                                    .append(dests.get(v2.getNom()));
                            first = false;
                        }
                    }
                }
            }
            sb.append("\n"); // Fin section ROUTES (ou fin message)

            out.print(sb.toString());
            out.flush();

            // 2. Réception
            String reponse = in.readLine();
            if (reponse == null || reponse.isEmpty()) {
                throw new IOException("Réponse vide du serveur");
            }

            // 3. Désérialisation
            List<Ville> output = new ArrayList<>();
            String[] segments = reponse.split(";");
            for (String s : segments) {
                String[] parts = s.split(",");
                if (parts.length == 3) {
                    output.add(new Ville(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                }
            }
            return output;
        }
    }
}
