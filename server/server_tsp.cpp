/**
 * SERVEUR TSP - PROJET ACL 2025-2026
 * Architecture :
 * - GrapheComplet<S, W> : Template (Généricité)
 * - DistanceStrategy : Stratégie (Design Pattern)
 * - ClientHandler : Gestionnaire de session
 */

#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <cmath>
#include <limits>
#include <algorithm>
#include <map>
#include <memory> 
#include <ctime>
#include <cstdlib> 
#include <random> 

// --- ABSTRACTION RESEAU ---
#ifdef _WIN32
    #include <winsock2.h>
    #pragma comment(lib, "ws2_32.lib")
    typedef int socklen_t;
    #define CLOSE_SOCKET closesocket
#else
    #include <netinet/in.h>
    #include <sys/socket.h>
    #include <unistd.h>
    #define CLOSE_SOCKET close
    typedef int SOCKET;
    #define INVALID_SOCKET -1
#endif

using namespace std;

// ============================================================
// 1. MODELE
// ============================================================

struct Ville {
    string nom;
    double lat;
    double lon;
    bool operator==(const Ville& other) const { return nom == other.nom; }
};

// ============================================================
// 2. PATTERN STRATEGIE (Cout)
// ============================================================

// Interface abstraite (conceptuelle via template ou héritage si nécessaire)
// Ici on utilise un Functeurconfigurable pour la performance et compatibilité template

class StrategieRoute {
public:
    enum Mode { STANDARD, AUTO_PRIO, SECONDAIRE_PRIO };
    
    map<pair<string, string>, string> routesConnues;
    Mode modeActuel = STANDARD;

    const double R = 6378.137;
    const double PI = 3.14159265358979323846;

    void setMode(const string& m) {
        if (m == "AUTOROUTE") modeActuel = AUTO_PRIO;
        else if (m == "SECONDAIRE") modeActuel = SECONDAIRE_PRIO;
        else modeActuel = STANDARD;
        cout << "[Strategie] Mode regle sur : " << m << endl;
    }

    void ajouterRoute(string v1, string v2, string type) {
        routesConnues[{v1, v2}] = type;
    }

    double toRad(double degree) const { return degree * (PI / 180.0); }

    double distanceHaversine(const Ville& a, const Ville& b) const {
        double dLat = toRad(b.lat - a.lat);
        double dLon = toRad(b.lon - a.lon);
        double lat1 = toRad(a.lat);
        double lat2 = toRad(b.lat);
        double u = sin(dLat / 2.0);
        double v = sin(dLon / 2.0);
        double c = 2.0 * asin(sqrt(u * u + cos(lat1) * cos(lat2) * v * v));
        return R * c;
    }

    double getPonderation(const string& typeRoute) const {
        string t = typeRoute;
        transform(t.begin(), t.end(), t.begin(), ::tolower);
        
        bool isAutoroute = (t.find("autoroute") != string::npos);
        bool isNationale = (t.find("national") != string::npos || t.find("europe") != string::npos || t.find("rapide") != string::npos);
        
        switch (modeActuel) {
            case AUTO_PRIO:
                if (isAutoroute) return 0.5; // Favorise ++
                if (isNationale) return 1.0;
                return 2.0; // Punalise le reste
            
            case SECONDAIRE_PRIO:
                if (isAutoroute) return 2.0; // Punalise autoroute
                if (isNationale) return 1.5;
                return 0.8; // Favorise petites routes
                
            default: // STANDARD = Optimisation Distance Pure (Resultat Prof)
                return 1.0;
        }
    }

    double operator()(const Ville& a, const Ville& b) const {
        double dist = distanceHaversine(a, b);
        
        string type = "";
        if (routesConnues.count({a.nom, b.nom})) type = routesConnues.at({a.nom, b.nom});
        else if (routesConnues.count({b.nom, a.nom})) type = routesConnues.at({b.nom, a.nom});
        
        return dist * getPonderation(type);
    }
};

// ============================================================
// 3. GRAPH TEMPLATE
// ============================================================

template <typename S, typename W>
class GrapheComplet {
    vector<S> sommets;
public:
    void ajouterSommet(const S& sommet) { sommets.push_back(sommet); }
    void vider() { sommets.clear(); }



private:
    template <typename Foncteur>
    W calculerCoutTotal(const vector<S>& chemin, Foncteur& strategie) {
        W total = 0;
        for (size_t i = 0; i < chemin.size() - 1; ++i) {
            total += strategie(chemin[i], chemin[i+1]);
        }
        return total;
    }

    template <typename Foncteur>
    void optimiser2Opt(vector<S>& chemin, Foncteur& strategie) {
        bool amelioration = true;
        while (amelioration) {
            amelioration = false;
            for (size_t i = 1; i < chemin.size() - 2; ++i) {
                for (size_t j = i + 1; j < chemin.size() - 1; ++j) {
                     W d_current = strategie(chemin[i-1], chemin[i]) + strategie(chemin[j], chemin[j+1]);
                     W d_swap = strategie(chemin[i-1], chemin[j]) + strategie(chemin[i], chemin[j+1]);
                     if (d_swap < d_current) {
                         reverse(chemin.begin() + i, chemin.begin() + j + 1);
                         amelioration = true;
                     }
                }
            }
        }
    }

    template <typename Foncteur>
    vector<S> optimiserIndividu(const vector<S>& inputSommets, vector<bool> visite, Foncteur& strategie) {
         vector<S> chemin;
         // Nearest Neighbor Logic
         int current = 0;
         visite[0] = true;
         chemin.push_back(inputSommets[0]);

         for (size_t i = 0; i < inputSommets.size() - 1; ++i) {
             int next = -1;
             W minCost = numeric_limits<W>::max();
             for (size_t j = 0; j < inputSommets.size(); ++j) {
                 if (!visite[j]) {
                     W cost = strategie(inputSommets[current], inputSommets[j]);
                     if (cost < minCost) { minCost = cost; next = j; }
                 }
             }
             if (next != -1) {
                 visite[next] = true;
                 chemin.push_back(inputSommets[next]);
                 current = next;
             }
         }
         chemin.push_back(inputSommets[0]);
         optimiser2Opt(chemin, strategie);
         return chemin;
    }

public:
    template <typename Foncteur>
    vector<S> resoudreTSP(Foncteur& strategie) {
        if (sommets.empty()) return {};
        
        // 1. Initialisation gloutonne (Nearest Neighbor) + 2-OPT (Baseline)
        vector<bool> visite(sommets.size(), false);
        vector<S> meilleurChemin = optimiserIndividu(sommets, visite, strategie);
        W meilleurCout = calculerCoutTotal(meilleurChemin, strategie);

        // 2. Multi-Start (Restarts Aléatoires)
        int nbRestarts = 50; 
        srand(time(0));

        for (int k = 0; k < nbRestarts; ++k) {
            vector<S> candidat = sommets;
            if (candidat.size() > 2) {
                std::random_device rd;
                std::mt19937 g(rd());
                std::shuffle(candidat.begin() + 1, candidat.end(), g);
            }
            candidat.push_back(candidat[0]); // Cycle

            optimiser2Opt(candidat, strategie);
            
            W coutCandidat = calculerCoutTotal(candidat, strategie);
            if (coutCandidat < meilleurCout) {
                meilleurCout = coutCandidat;
                meilleurChemin = candidat;
            }
        }

        return meilleurChemin;
    }
};

// ============================================================
// 4. MAIN & RESEAU
// ============================================================

void parseVilles(const string& line, GrapheComplet<Ville, double>& graphe);
void parseRoutes(const string& line, StrategieRoute& strategie);

void handleClient(SOCKET clientSocket) {
    char buffer[65536] = {0}; // Increased buffer
    int valread = recv(clientSocket, buffer, 65536, 0);
    if (valread <= 0) return;

    string data(buffer);
    stringstream ss(data);
    string line;

    GrapheComplet<Ville, double> graphe;
    StrategieRoute strategie; 
    
    string section = "";

    while(getline(ss, line)) {
        if (line.substr(0, 8) == "PROFILE:") {
            string p = line.substr(8);
            // Suppression cr/lf potentiels
            p.erase(remove(p.begin(), p.end(), '\r'), p.end());
            p.erase(remove(p.begin(), p.end(), '\n'), p.end());
            strategie.setMode(p);
            continue;
        }
        if (line == "VILLES") { section = "VILLES"; continue; }
        if (line == "ROUTES") { section = "ROUTES"; continue; }
        if (line.empty()) continue;

        if (section == "VILLES") {
            parseVilles(line, graphe);
        }
        else if (section == "ROUTES") {
            parseRoutes(line, strategie);
        }
    }

    cout << "[Serveur] Calcul TSP en cours (Optimise)..." << endl;
    vector<Ville> path = graphe.resoudreTSP<StrategieRoute>(strategie);
    
    // Serialisation reponse
    stringstream ssOut;
    for(size_t i=0; i<path.size(); ++i) {
        ssOut << path[i].nom << "," << path[i].lat << "," << path[i].lon;
        if(i < path.size()-1) ssOut << ";";
    }
    string resp = ssOut.str() + "\n";
    send(clientSocket, resp.c_str(), resp.length(), 0);
    cout << "[Serveur] Reponse envoyee." << endl;
}

void parseVilles(const string& line, GrapheComplet<Ville, double>& graphe) {
    stringstream ssRow(line);
    string segment;
    while(getline(ssRow, segment, ';')) {
            stringstream ssCell(segment);
            string nom, lat, lon;
            getline(ssCell, nom, ',');
            getline(ssCell, lat, ',');
            getline(ssCell, lon, ',');
            if(!nom.empty()) graphe.ajouterSommet({nom, stod(lat), stod(lon)});
    }
}

void parseRoutes(const string& line, StrategieRoute& strategie) {
    stringstream ssRow(line);
    string segment;
    while(getline(ssRow, segment, ';')) {
            stringstream ssCell(segment);
            string v1, v2, type;
            getline(ssCell, v1, ',');
            getline(ssCell, v2, ',');
            getline(ssCell, type, ',');
            if(!v1.empty() && !v2.empty()) strategie.ajouterRoute(v1, v2, type);
    }
}

int main() {
    #ifdef _WIN32
        WSADATA wsaData;
        WSAStartup(MAKEWORD(2, 2), &wsaData);
    #endif

    SOCKET server_fd = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in address;
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(8080);

    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        cerr << "Echec bind port 8080" << endl;
        return 1;
    }
    listen(server_fd, 5);

    cout << "=== SERVEUR TSP (Design Patterns & Clean Code) ===" << endl;
    cout << "Pret a recevoir des requetes..." << endl;

    while (true) {
        socklen_t addrlen = sizeof(address);
        SOCKET osocket = accept(server_fd, (struct sockaddr *)&address, &addrlen);
        if (osocket != INVALID_SOCKET) {
            handleClient(osocket);
            CLOSE_SOCKET(osocket);
        }
    }

    #ifdef _WIN32
        WSACleanup();
    #endif
    return 0;
}
