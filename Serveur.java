import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.*;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.application.Platform;

import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

public class Serveur {
    private SSLServerSocket socketServeur;
    private List<GestionClient> clientsConnectes;
    private int compteurClients;
    private static final String MOT_DE_PASSE = "admin123";
    private Thread threadServeur;
    private boolean serveurEnCours;
    private int port;

    public Serveur(int port) {
        this.port = port; // Stocker le port pour l'utiliser dans demarrer()
        clientsConnectes = new ArrayList<>();
        compteurClients = 0;
        serveurEnCours = false;
    }

    public void demarrer() {
        if (serveurEnCours) {
            InterfaceServeur.getInstance().journaliser("Le serveur est déjà en cours d'exécution.");
            return;
        }

        try {
            // Charger le keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream keyStoreStream = new FileInputStream("serveur.jks")) {
                keyStore.load(keyStoreStream, "passer123".toCharArray());
            }

            // Initialiser le KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "passer123".toCharArray());

            // Créer un contexte SSL
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            // Créer un SSLServerSocket
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            socketServeur = (SSLServerSocket) factory.createServerSocket(port);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur SSL : Algorithme TLS non supporté : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation du serveur SSL", e);
        } catch (KeyManagementException e) {
            System.err.println("Erreur SSL : Problème d'initialisation SSL : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation du serveur SSL", e);
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de la création du socket serveur", e);
        } catch (java.security.KeyStoreException e) {
            System.err.println("Erreur lors du chargement du keystore : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation du keystore", e);
        } catch (java.security.cert.CertificateException e) {
            System.err.println("Erreur de certificat : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation du certificat", e);
        } catch (java.security.UnrecoverableKeyException e) {
            System.err.println("Erreur lors de la récupération de la clé : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation de la clé", e);
        }

        String os = System.getProperty("os.name").toLowerCase();
        String osType = os.contains("win") ? "Windows" : (os.contains("nix") || os.contains("nux") || os.contains("mac") ? "Linux" : "Unknown");

        serveurEnCours = true;
        threadServeur = new Thread(() -> {
            try {
                InterfaceServeur.getInstance().journaliser("Serveur démarré sur le port " + socketServeur.getLocalPort());
                while (serveurEnCours) {
                    Socket socketClient = socketServeur.accept();
                    InterfaceServeur.getInstance().journaliser("Client connecté : " + socketClient.getInetAddress().getHostAddress());
                    BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream(), "UTF-8"));
                    PrintWriter sortie = new PrintWriter(new OutputStreamWriter(socketClient.getOutputStream(), "UTF-8"), true);

                    sortie.println("Veuillez entrer le mot de passe :");
                    sortie.flush();
                    String motDePasse = entree.readLine();
                    InterfaceServeur.getInstance().journaliser("Mot de passe reçu : " + motDePasse);

                    if (motDePasse != null && motDePasse.equals(MOT_DE_PASSE)) {
                        sortie.println("Connexion établie");
                        sortie.flush();
                        sortie.println("OS:" + osType);
                        sortie.flush();
                        InterfaceServeur.getInstance().journaliser("Connexion acceptée pour le client");
                        compteurClients++;
                        GestionClient gestionClient = new GestionClient(socketClient, this, compteurClients, InterfaceServeur.getInstance());
                        clientsConnectes.add(gestionClient);
                        InterfaceServeur.getInstance().mettreAJourClients();
                        new Thread(gestionClient).start();
                    } else {
                        sortie.println("Mot de passe incorrect. Connexion refusée.");
                        sortie.flush();
                        socketClient.close();
                        InterfaceServeur.getInstance().journaliser("Connexion refusée pour un client (mot de passe incorrect).");
                    }
                }
            } catch (IOException e) {
                if (serveurEnCours) {
                    InterfaceServeur.getInstance().journaliser("Erreur lors de la connexion d’un client : " + e.getMessage());
                } 
            }
        });
        threadServeur.start();
    }

    public void arreter() {
        try {
            serveurEnCours = false;
            if (socketServeur != null && !socketServeur.isClosed()) {
                socketServeur.close();
            }
            if (threadServeur != null) {
                threadServeur.interrupt(); // Interrompre le thread
                threadServeur.join(); // Attendre que le thread se termine
                threadServeur = null; // Réinitialiser le thread
            }
            for (GestionClient client : new ArrayList<>(clientsConnectes)) {
                client.deconnecter();
            }
            clientsConnectes.clear();
            InterfaceServeur.getInstance().mettreAJourClients();
            InterfaceServeur.getInstance().journaliser("Serveur arrêté.");
        } catch (IOException e) {
            InterfaceServeur.getInstance().journaliser("Erreur lors de l'arrêt du serveur : " + e.getMessage());
        } catch (InterruptedException e) {
            InterfaceServeur.getInstance().journaliser("Erreur lors de l'interruption du thread serveur : " + e.getMessage());
        }
    }

    public List<GestionClient> getClientsConnectes() {
        return clientsConnectes;
    }

    public static void main(String[] args) {
        try {
            InterfaceServeur.main(args);
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de l'application : " + e.getMessage());
            e.printStackTrace();
        }
    }
}