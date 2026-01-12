# Rapport Technique - Projet ACL Logistique (2026)

## 1. Introduction
Ce projet vise à résoudre le problème de la tournée de livraison (proche du Traveling Salesman Problem ou TSP) pour une flotte de camions. L'objectif est d'optimiser les trajets pour minimiser la distance totale ou le coût, en fonction de différentes contraintes (types de routes, modes de déplacement).

L'application se compose d'une architecture Client-Serveur robuste, séparant l'interface graphique (Java Swing) de la logique de calcul intensive (C++).

## 2. Architecture Logicielle
### 2.1 Organisation du Projet
Le projet a été restructuré pour garantir portabilité et propreté :
*   **`bin/`** : Contient tous les fichiers compilés (`.class`), gardant les sources propres.
*   **`client/`** : Code source Java.
*   **`server/`** : Code source C++ et exécutable du serveur.
*   **`docs/`** : Documentation technique et utilisateur.
*   **`routes/`** : Base de données par défaut des fichiers JSON/CSV.

### 2.2 Le Client (Java)
Le client est une application lourde développée en Java (Swing) qui assure les responsabilités suivantes :

*   **Interface Graphique (GUI)** : Visualisation interactive des villes, des tournées et de l'état de la flotte.
*   **Gestion des Données (E/S)** : Lecture des fichiers JSON (Villes) et CSV (Matrice des distances/routes).
*   **Logique Métier Légère** : Gestion de la flotte (attributions initiales), sélection multi-régions.
*   **Communication Réseau** : Envoi des requêtes au serveur C++ et parsing des réponses via Sockets TCP.

**Points forts de l'implémentation Client :**
*   **Expérience Utilisateur Fluidifiée** : Détection et ouverture automatique du dossier `./routes` au démarrage.
*   **Chargement Dynamique** : Capacité de charger plusieurs fichiers JSON simultanément pour fusionner des régions.
*   **Réactivité** : Utilisation de `SwingWorker` pour les calculs réseau et `SwingUtilities.invokeLater` pour garantir la fluidité de l'interface.
*   **Compatibilité** : Compilation automatique ciblée pour Java 8 (via flags `--release 8`) assurant le fonctionnement sur la plupart des environnements académiques, quel que soit le JDK installé.

### 2.3 Le Serveur (C++)
Le serveur est un moteur de calcul haute performance conçu pour résoudre le TSP.

*   **Modèle de Données** : Graphe complet générique `GrapheComplet<S, W>` supportant n'importe quel type de sommet et de poids.
*   **Multi-Threading** : Architecture capable de gérer plusieurs clients (le calcul TSP reste séquentiel par requête pour la cohérence).
*   **Flexibilité** : Supporte plusieurs stratégies de calcul de poids via le pattern Strategy.

## 3. Design Patterns Appliqués
Le projet fait un usage intensif des patrons de conception pour assurer modularité et maintenabilité :

*   **MVC (Modèle-Vue-Contrôleur)** : Séparation claire entre les données (`Ville`, `Camion`), l'affichage (`ComposantCarte`), et la logique (`ApplicationLivraison`).
*   **Facade** : La classe `ServiceReseau` masque la complexité des `Socket`, `PrintWriter` et `BufferedReader`.
*   **Strategy (C++)** : Le calcul du coût est encapsulé dans des foncteurs, permettant de changer d'algorithme (Distance vs Temps) sans toucher au cœur du TSP.
*   **Observer / Event Listener (Java)** : Utilisé pour réagir aux interactions utilisateur (boutons, spinners).

## 4. Analyse Algorithmique
### 4.1 Heuristiques TSP
Nous avons exclu les solutions exactes (Brute Force) pour les grands ensembles au profit d'une approche hybride :
1.  **Construction Initiale (Nearest Neighbor)** : Complexité $O(N^2)$. Génère une solution de base.
2.  **Optimisation Locale (2-Opt)** : Algorithme de "Hill Climbing" qui décroise les routes en échangeant les arêtes tant qu'une amélioration est possible.

### 4.2 Stratégie Multi-Start (Méta-heuristique)
Pour éviter les minimums locaux, nous utilisons une stratégie Multi-Start :
*   L'algorithme est relancé **50 fois** pour chaque requête.
*   À chaque itération, le graphe est mélangé aléatoirement (shuffle) avant d'appliquer NN + 2-Opt.
*   Seule la meilleure solution est conservée.

## 5. Performances et Résultats
### 5.1 Benchmark
Sur le jeu de données de référence, notre moteur atteint une tournée optimale de **1087 km**.
Ce résultat valide l'efficacité du Multi-Start face à un simple Nearest Neighbor (>1300km) ou un 2-Opt simple (~1150km).

### 5.2 Fusion Multi-Régions
Le système gère la fusion de graphes (ex: chargement simultané de `grandest.json` et `idf.json`), traités comme un unique graphe connexe par le serveur.

## 6. Guide Utilisateur Rapide
### Lancement Automatisé (Recommandé)
Le script `launch.ps1` a été développé pour automatiser tout le processus de build et de lancement :
1.  Double-cliquez sur **`launch.ps1`** (ou exécutez `.\launch.ps1` dans un terminal PowerShell).
2.  Le script va automatiquement :
    *   Nettoyer les anciens fichiers de build.
    *   Compiler le code Java (compatibilité Java 8 garantie).
    *   Lancer le serveur C++ (port 8080).
    *   Lancer le client Java.

### Utilisation
1.  **Chargement** : L'app s'ouvre directement sur le dossier `routes`. Sélectionnez vos fichiers (Ctrl+Clic) et cliquez sur **"Charger Sélection"**.
2.  **Configuration** : Ajustez le nombre de camions via le sélecteur.
3.  **Calcul** : Cliquez sur **"Calculer Tournées"**. Le client envoie les données au serveur C++, qui retourne le chemin optimal (affiché en < 1 seconde grâce au C++).
