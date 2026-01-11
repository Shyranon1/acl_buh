package client;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.util.Vector;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.BoxLayout;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.Cursor;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ApplicationLivraison extends JFrame {

    private ComposantCarte carte;
    private ServiceReseau serviceReseau;
    private List<Camion> flotte;
    private JComboBox<String> comboProfil;
    private JSpinner spinnerNbCamions;

    // Palette Premium
    private final Color COLOR_SIDEBAR = new Color(44, 62, 80); // Dark Blue/Grey
    private final Color COLOR_TEXT_LIGHT = new Color(236, 240, 241);
    private final Color COLOR_BTN_ACTION = new Color(52, 152, 219); // Blue
    private final Color COLOR_BTN_LOAD = new Color(39, 174, 96); // Green

    private JLabel statusLabel; // Promoted to field for access in helpers
    // Map<Depart, Map<Arrivee, Type>>
    private java.util.Map<String, java.util.Map<String, String>> routesMap = new java.util.HashMap<>();

    // New members for Directory Selection
    private java.io.File currentDirectory = new java.io.File(".");
    private JList<String> listFichiers;
    private JLabel labelCurrentDir;

    // Store loaded cities globally to allow dynamic reassignment
    private List<Ville> toutesLesVillesGlobal = new ArrayList<>();

    public ApplicationLivraison() {
        super("Logistics Optimizer Pro | ACL 2026");
        this.serviceReseau = new ServiceReseau();
        this.flotte = new ArrayList<>();

        // UI Base
        this.setLayout(new BorderLayout());
        this.setSize(1200, 800);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        // --- SIDEBAR (GAUCHE) ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_SIDEBAR);
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20));
        sidebar.setPreferredSize(new Dimension(300, 800));

        // Titre
        JLabel titleLabel = new JLabel("ACL LOGISTICS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_TEXT_LIGHT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(titleLabel);

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel subtitleLabel = new JLabel("Optimisation de Tournées");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(subtitleLabel);

        sidebar.add(Box.createRigidArea(new Dimension(0, 40)));

        // Section Configuration
        addSectionHeader(sidebar, "CONFIGURATION");

        // Initialisation label status (déplacé ici pour portée)
        statusLabel = new JLabel("Status: Prêt");
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Nombre de camions
        addLabel(sidebar, "Nombre de Camions");
        spinnerNbCamions = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        styleSpinner(spinnerNbCamions);
        spinnerNbCamions.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinnerNbCamions.setMaximumSize(new Dimension(260, 40));
        sidebar.add(spinnerNbCamions);
        spinnerNbCamions.addChangeListener(e -> {
            SwingUtilities.invokeLater(() -> {
                int nb = (Integer) spinnerNbCamions.getValue();
                repartirVilles(nb);
            });
        });

        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. Stratégie
        addLabel(sidebar, "Stratégie de Route");
        comboProfil = new JComboBox<>(new String[] {
                "Standard (Distance Min)", "Rapide (Favoriser Autoroutes)", "Touristique (Routes Secondaires)"
        });
        comboProfil.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboProfil.setMaximumSize(new Dimension(260, 35));
        sidebar.add(comboProfil);

        sidebar.add(Box.createRigidArea(new Dimension(0, 50)));

        addSectionHeader(sidebar, "FICHIERS LOCAUX");

        // Label Dossier
        labelCurrentDir = new JLabel("Dossier: .");
        labelCurrentDir.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        labelCurrentDir.setForeground(Color.GRAY);
        labelCurrentDir.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(labelCurrentDir);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        // Button Change Dir
        JButton btnChangeDir = createStyledButton("Changer Dossier...", new Color(127, 140, 141));
        btnChangeDir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnChangeDir.setPreferredSize(new Dimension(260, 30));
        btnChangeDir.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(currentDirectory);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentDirectory = chooser.getSelectedFile();
                refreshFileList();
            }
        });
        sidebar.add(btnChangeDir);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        // List
        listFichiers = new JList<>();
        listFichiers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listFichiers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshFileList(); // Initial load

        JScrollPane scrollPane = new JScrollPane(listFichiers);
        scrollPane.setMaximumSize(new Dimension(260, 100));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(scrollPane);

        JButton btnChargerList = createStyledButton("Charger Sélection", new Color(46, 204, 113));
        btnChargerList.addActionListener(e -> {
            List<String> selectedNames = listFichiers.getSelectedValuesList();
            if (selectedNames.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sélectionnez au moins un fichier dans la liste.", "Info",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            toutesLesVillesGlobal.clear();
            routesMap.clear();

            for (String name : selectedNames) {
                processFile(new java.io.File(currentDirectory, name), toutesLesVillesGlobal);
            }

            int nb = (Integer) spinnerNbCamions.getValue();
            if (toutesLesVillesGlobal.isEmpty())
                return;

            repartirVilles(nb);
            JOptionPane.showMessageDialog(this, "Chargement terminé : " + selectedNames.size() + " région(s).");
        });
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        sidebar.add(btnChargerList);

        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        // Section Actions
        addSectionHeader(sidebar, "ACTIONS AVANCEES");

        JButton btnCharger = createStyledButton("Parcourir...", COLOR_BTN_LOAD);
        JButton btnCalculer = createStyledButton("Calculer Tournées", COLOR_BTN_ACTION);

        sidebar.add(btnCharger);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnCalculer);

        // Footer info (Spacer pushed to bottom)
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(statusLabel);

        this.add(sidebar, BorderLayout.WEST);

        // --- MAP (CENTRE) ---
        this.carte = new ComposantCarte();
        this.add(carte, BorderLayout.CENTER);

        // --- LOGIC ---
        btnCharger.addActionListener(e -> {
            int nb = (Integer) spinnerNbCamions.getValue();
            chargerDonneesSimulees(nb);
            carte.setFlotte(flotte);
            statusLabel.setText("Status: " + nb + " camions chargés.");
            JOptionPane.showMessageDialog(this, "Données chargées avec succès.\n" + nb + " camions prêts.");
        });

        btnCalculer.addActionListener(e -> {
            if (flotte.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez d'abord charger les données.", "Erreur",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            statusLabel.setText("Status: Calcul en cours...");
            lancerCalculAsynchrone(statusLabel);
        });
    }

    // --- UI HELPERS ---

    private void addSectionHeader(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(149, 165, 166)); // Greyish
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void addLabel(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(COLOR_TEXT_LIGHT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 45));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setHorizontalAlignment(JTextField.LEFT);
        }
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    // --- LOGIQUE METIER ---

    private void chargerDonneesSimulees(int nbCamions) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Ouvrir fichiers de données");
        // Update: Enable multi-selection
        fileChooser.setMultiSelectionEnabled(true);
        // Remove directory selection to avoid confusion (user asked for multi-file
        // selection)
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(new java.io.File("."));

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        java.io.File[] selectedFiles = fileChooser.getSelectedFiles();

        // Fallback for single selection if array is empty but selectedFile is set
        // (legacy swing quirk)
        if (selectedFiles == null || selectedFiles.length == 0) {
            if (fileChooser.getSelectedFile() != null) {
                selectedFiles = new java.io.File[] { fileChooser.getSelectedFile() };
            } else {
                return;
            }
        }

        toutesLesVillesGlobal.clear();
        routesMap.clear();

        // Loop through all selected files and merge data
        for (java.io.File f : selectedFiles) {
            processFile(f, toutesLesVillesGlobal);
        }

        if (toutesLesVillesGlobal.isEmpty())
            return;

        repartirVilles(nbCamions);
    }

    private void processFile(java.io.File f, List<Ville> villes) {
        villes.addAll(LecteurDonnees.lireVillesJSON(f.getAbsolutePath()));

        String jsonPath = f.getAbsolutePath();
        String csvPath = jsonPath.replace(".json", ".csv");

        // Tentative 1 : meme nom
        if (!new java.io.File(csvPath).exists()) {
            // Tentative 2 : dossier parent + nom sans accent
            String dir = f.getParent();
            String name = f.getName().replace(".json", ".csv");
            try {
                String nameNoAccent = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                        .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                csvPath = new java.io.File(dir, nameNoAccent).getAbsolutePath();
            } catch (Exception e) {
                System.err.println("Erreur normalisation nom fichier: " + name);
            }
        }

        java.io.File csvFile = new java.io.File(csvPath);
        if (csvFile.exists()) {
            System.out.println("Chargement routes depuis : " + csvPath);
            java.util.Map<String, java.util.Map<String, String>> newRoutes = LecteurDonnees.lireMatriceRoutes(csvPath);

            // Deep merge to avoid overwriting existing cities' connections
            for (java.util.Map.Entry<String, java.util.Map<String, String>> entry : newRoutes.entrySet()) {
                String source = entry.getKey();
                java.util.Map<String, String> targets = entry.getValue();

                if (routesMap.containsKey(source)) {
                    routesMap.get(source).putAll(targets);
                } else {
                    routesMap.put(source, targets);
                }
            }
        } else {
            System.out.println("Info: Pas de fichier de routes trouve pour " + f.getName());
        }
    }

    private void lancerCalculAsynchrone(JLabel statusLabel) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String profilName = (String) comboProfil.getSelectedItem();
                String profilCode = "STANDARD";
                if (profilName != null) {
                    if (profilName.contains("Rapide"))
                        profilCode = "AUTOROUTE";
                    if (profilName.contains("Secondaires"))
                        profilCode = "SECONDAIRE";
                }

                for (Camion c : flotte) {
                    try {
                        List<Ville> res = serviceReseau.optimiserTournee(c.getVillesALivrer(), routesMap, profilCode);
                        c.setTourneeOrdonnee(res);
                    } catch (Exception ex) {
                        System.err.println("Erreur pour camion " + c.getId() + " : " + ex.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                carte.repaint();
                statusLabel.setText("Status: Terminé.");
                JOptionPane.showMessageDialog(ApplicationLivraison.this,
                        "Optimisation terminée pour " + flotte.size() + " camions.");
            }
        };
        worker.execute();
    }

    private void refreshFileList() {
        if (listFichiers == null)
            return;

        java.io.File[] jsonFiles = currentDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        Vector<String> fileList = new Vector<>();
        if (jsonFiles != null) {
            for (java.io.File f : jsonFiles) {
                fileList.add(f.getName());
            }
        }
        listFichiers.setListData(fileList);

        if (labelCurrentDir != null) {
            String path = currentDirectory.getAbsolutePath();
            // Shorten if too long
            if (path.length() > 30)
                path = "..." + path.substring(path.length() - 27);
            labelCurrentDir.setText("Dossier: " + path);
            labelCurrentDir.setToolTipText(currentDirectory.getAbsolutePath());
        }
    }

    private void repartirVilles(int nbCamions) {
        if (toutesLesVillesGlobal.isEmpty())
            return;

        flotte.clear();
        Color[] couleurs = { Color.RED, Color.BLUE, new Color(0, 128, 0), Color.MAGENTA,
                new Color(255, 128, 0), Color.CYAN, Color.PINK, Color.YELLOW,
                Color.GRAY, new Color(139, 69, 19) };

        for (int i = 0; i < nbCamions; i++) {
            Color c = (i < couleurs.length) ? couleurs[i] : Color.BLACK;
            Camion camion = new Camion("Camion " + (i + 1), c);
            if (!toutesLesVillesGlobal.isEmpty())
                camion.ajouterVille(toutesLesVillesGlobal.get(0)); // Depot
            flotte.add(camion);
        }

        // Toutes les autres villes sauf depot
        for (int i = 1; i < toutesLesVillesGlobal.size(); i++) {
            flotte.get((i - 1) % nbCamions).ajouterVille(toutesLesVillesGlobal.get(i));
        }

        carte.setFlotte(flotte);
        statusLabel.setText("Status: Répartition mise à jour (" + nbCamions + " camions).");
    }

    public static void main(String[] args) {
        // Use Nimbus Look and Feel for Premium look and stability on Windows
        try {
            boolean nimbusFound = false;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    nimbusFound = true;
                    break;
                }
            }
            if (!nimbusFound) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored2) {
            }
        }

        // Custom colors for Nimbus to match our Palette (Optional but nice)
        UIManager.put("control", new Color(236, 240, 241));
        UIManager.put("nimbusBase", new Color(44, 62, 80));
        UIManager.put("nimbusBlueGrey", new Color(44, 62, 80));
        UIManager.put("nimbusFocus", new Color(52, 152, 219));

        SwingUtilities.invokeLater(() -> {
            new ApplicationLivraison().setVisible(true);
        });
    }
}
