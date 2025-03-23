import java.io.*;
import javax.net.ssl.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

public class Client {
    private SSLSocket socket;
    private PrintWriter sortie;
    private BufferedReader entree;
    private boolean connexionReussie;
    private String hote;
    private int port;
    public InterfaceClient interfaceClient;
    private String serverOsType;

    public Client() {
        this.connexionReussie = false;
        this.hote = "localhost";
        this.port = 8080;
    }

    public Client(InterfaceClient interfaceClient) {
        this.interfaceClient = interfaceClient;
        this.connexionReussie = false;
        this.hote = "localhost";
        this.port = 8080;
    }

    public void seConnecter(String hote, int port) {
        new Thread(() -> {
            try {
                interfaceClient.journaliser("Tentative de connexion à " + hote + ":" + port);

                // Charger le truststore
                KeyStore trustStore = KeyStore.getInstance("JKS");
                try (FileInputStream trustStoreStream = new FileInputStream("client_truststore.jks")) {
                    trustStore.load(trustStoreStream, "passer123".toCharArray());
                }

                // Initialiser le TrustManagerFactory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);

                // Créer un contexte SSL
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

                // Créer un SSLSocket
                SSLSocketFactory factory = sslContext.getSocketFactory();
                socket = (SSLSocket) factory.createSocket(hote, port);

                

                interfaceClient.journaliser("Socket SSL créé avec succès");

                sortie = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                entree = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                interfaceClient.journaliser("Flux d'entrée/sortie initialisés");

                String demandeMotDePasse = entree.readLine();
                interfaceClient.journaliser("Message reçu du serveur : " + demandeMotDePasse);
                String motDePasse = interfaceClient.demanderMotDePasse();
                if (motDePasse == null) {
                    throw new IOException("Authentification annulée par l'utilisateur.");
                }
                sortie.println(motDePasse);
                sortie.flush();

                String reponse = entree.readLine();
                interfaceClient.journaliser("Réponse du serveur après mot de passe : " + reponse);
                if (reponse != null && reponse.contains("Mot de passe incorrect")) {
                    interfaceClient.journaliser("Connexion refusée : " + reponse);
                    socket.close();
                    socket = null;
                    connexionReussie = false;
                } else if (reponse != null && reponse.contains("Connexion établie")) {
                    String osInfo = entree.readLine();
                    interfaceClient.journaliser("Information OS reçue : " + osInfo);
                    if (osInfo != null && osInfo.startsWith("OS:")) {
                        serverOsType = osInfo.substring(3);
                        interfaceClient.journaliser("Système d'exploitation du serveur : " + serverOsType);
                    } else {
                        interfaceClient.journaliser("Erreur : Information OS non reçue ou mal formée.");
                    }
                    connexionReussie = true;
                    interfaceClient.setServerOsType(serverOsType);
                    interfaceClient.activerInterface();
                } else {
                    interfaceClient.journaliser("Réponse inattendue du serveur : " + reponse);
                    connexionReussie = false;
                }
            } catch (NoSuchAlgorithmException e) {
                interfaceClient.journaliser("Erreur SSL : Algorithme TLS non supporté : " + e.getMessage());
                connexionReussie = false;
            } catch (KeyManagementException e) {
                interfaceClient.journaliser("Erreur SSL : Problème d'initialisation SSL : " + e.getMessage());
                connexionReussie = false;
            } catch (IOException e) {
                interfaceClient.journaliser("Erreur lors de la connexion : " + e.getMessage());
                connexionReussie = false;
            } catch (java.security.KeyStoreException e) {
                interfaceClient.journaliser("Erreur lors du chargement du truststore : " + e.getMessage());
                connexionReussie = false;
            } catch (java.security.cert.CertificateException e) {
                interfaceClient.journaliser("Erreur de certificat : " + e.getMessage());
                connexionReussie = false;
            }
        }).start();
    }

    public void deconnecter() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connexionReussie = false;
            interfaceClient.desactiverInterface();
            interfaceClient.journaliser("Déconnecté du serveur.");
        } catch (IOException e) {
            interfaceClient.journaliser("Erreur lors de la déconnexion : " + e.getMessage());
        }
    }

    public void envoyerCommande(String commande) {
        if (!connexionReussie || socket == null || socket.isClosed()) {
            interfaceClient.journaliser("Erreur : Pas de connexion au serveur.");
            return;
        }

        if (sortie != null && entree != null) {
            new Thread(() -> {
                try {
                    sortie.println(commande);
                    sortie.flush();

                    StringBuilder reponse = new StringBuilder();
                    String ligne;
                    while ((ligne = entree.readLine()) != null) {
                        if (ligne.equals("END_OF_RESPONSE")) {
                            break;
                        }
                        reponse.append(ligne).append("\n");
                    }
                    if (reponse.length() == 0) {
                        interfaceClient.journaliser("La commande a été traitée avec succès.");
                    } else {
                        interfaceClient.afficherResultat(reponse.toString());
                    }
                } catch (IOException e) {
                    interfaceClient.journaliser("Erreur lors de l'envoi ou de la réception : " + e.getMessage());
                    connexionReussie = false;
                    interfaceClient.desactiverInterface();
                }
            }).start();
        } else {
            interfaceClient.journaliser("Erreur : Pas de connexion au serveur.");
            connexionReussie = false;
        }
    }

    public boolean isConnexionReussie() {
        return connexionReussie && socket != null && !socket.isClosed();
    }

    public String getServerOsType() {
        return serverOsType;
    }

    public static void main(String[] args) {
        InterfaceClient.launch(InterfaceClient.class);
    }
}