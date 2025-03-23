import java.io.*;
import java.net.Socket;
import javax.net.ssl.SSLException; // Ajout pour gérer les exceptions SSL
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class GestionClient implements Runnable {
    private Socket socketClient;
    private Serveur serveur;
    private int idClient;
    private PrintWriter sortie;
    private BufferedReader entree;
    private String repertoireCourant;
    private String adresseIP;
    private InterfaceServeur interfaceServeur;

    public GestionClient(Socket socketClient, Serveur serveur, int idClient, InterfaceServeur interfaceServeur) {
        this.socketClient = socketClient;
        this.serveur = serveur;
        this.idClient = idClient;
        this.interfaceServeur = interfaceServeur;
        this.repertoireCourant = System.getProperty("user.dir");
        this.adresseIP = socketClient.getInetAddress().getHostAddress();
        try {
            sortie = new PrintWriter(new OutputStreamWriter(socketClient.getOutputStream(), StandardCharsets.UTF_8), true);
            entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream(), StandardCharsets.UTF_8));
            interfaceServeur.journaliser("Client #" + idClient + " (" + adresseIP + ") initialisé avec succès.");
        } catch (IOException e) {
            interfaceServeur.journaliser("Erreur lors de l'initialisation du client #" + idClient + " : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String commande;
            while ((commande = entree.readLine()) != null) {
                interfaceServeur.journaliser("Commande reçue du client #" + idClient + " (" + adresseIP + ") : " + commande);
                String resultat = exécuterCommande(commande);
                interfaceServeur.journaliser("Résultat envoyé au client #" + idClient + " (" + adresseIP + ") :\n" + resultat);
                BufferedReader lecteurResultat = new BufferedReader(new StringReader(resultat));
                String ligne;
                while ((ligne = lecteurResultat.readLine()) != null) {
                    sortie.println(ligne);
                }
                sortie.println("END_OF_RESPONSE");
                sortie.flush();
            }
        } catch (SSLException e) {
            interfaceServeur.journaliser("Erreur SSL avec le client #" + idClient + " (" + adresseIP + ") : " + e.getMessage());
        } catch (IOException e) {
            interfaceServeur.journaliser("Erreur avec le client #" + idClient + " (" + adresseIP + ") : " + e.getMessage());
        } finally {
            try {
                socketClient.close();
                serveur.getClientsConnectes().remove(this);
                interfaceServeur.mettreAJourClients();
                interfaceServeur.journaliser("Client #" + idClient + " (" + adresseIP + ") déconnecté.");
            } catch (IOException e) {
                interfaceServeur.journaliser("Erreur lors de la fermeture du client #" + idClient + " (" + adresseIP + ") : " + e.getMessage());
            }
        }
    }

    private String exécuterCommande(String commande) {
        try {
            // Détecter le système d’exploitation
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            boolean isLinux = os.contains("nix") || os.contains("nux") || os.contains("mac");

            // Gestion de la commande cd
            if (commande.toLowerCase().startsWith("cd ")) {
                String nouveauRepertoire = commande.substring(3).trim();
                // Gérer les guillemets dans le chemin
                if (nouveauRepertoire.startsWith("\"") && nouveauRepertoire.endsWith("\"")) {
                    nouveauRepertoire = nouveauRepertoire.substring(1, nouveauRepertoire.length() - 1);
                }
                try {
                    // Résoudre le chemin absolu
                    String cheminAbsolu;
                    if (isWindows && nouveauRepertoire.matches("^[A-Za-z]:.*")) {
                        cheminAbsolu = Paths.get(nouveauRepertoire).normalize().toString();
                    } else {
                        cheminAbsolu = Paths.get(repertoireCourant, nouveauRepertoire).normalize().toString();
                    }
                    File dossier = new File(cheminAbsolu);
                    if (dossier.exists() && dossier.isDirectory()) {
                        repertoireCourant = cheminAbsolu;
                        return "Répertoire changé : " + repertoireCourant;
                    } else {
                        return "Erreur : Le répertoire n'existe pas : " + cheminAbsolu;
                    }
                } catch (Exception e) {
                    return "Erreur lors du changement de répertoire : " + e.getMessage();
                }
            } else if (commande.toLowerCase().equals("cd")) {
                return "Répertoire courant : " + repertoireCourant;
            }

            // Ajuster la commande en fonction du système d’exploitation
            String adjustedCommand = commande;
            if (isLinux) {
                if (commande.toLowerCase().startsWith("dir")) {
                    adjustedCommand = commande.replaceFirst("(?i)dir", "ls -l");
                } else if (commande.toLowerCase().startsWith("del ")) {
                    adjustedCommand = commande.replaceFirst("(?i)del", "rm");
                }
                // mkdir et cd sont les mêmes sur Linux
            }

            // Gestion des autres commandes
            ProcessBuilder processBuilder;
            if (isWindows) {
                processBuilder = new ProcessBuilder("cmd", "/c", adjustedCommand);
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", adjustedCommand);
            }
            processBuilder.directory(new File(repertoireCourant));
            Process processus = processBuilder.start();

            BufferedReader lecteurSortie = new BufferedReader(
                new InputStreamReader(processus.getInputStream(), isWindows ? "CP850" : "UTF-8")
            );
            BufferedReader lecteurErreur = new BufferedReader(
                new InputStreamReader(processus.getErrorStream(), isWindows ? "CP850" : "UTF-8")
            );

            StringBuilder resultat = new StringBuilder();
            String ligne;

            while ((ligne = lecteurSortie.readLine()) != null) {
                resultat.append(ligne).append("\n");
            }

            while ((ligne = lecteurErreur.readLine()) != null) {
                resultat.append("Erreur : ").append(ligne).append("\n");
            }

            lecteurSortie.close();
            lecteurErreur.close();

            int codeSortie = processus.waitFor();
            if (codeSortie != 0) {
                resultat.append("La commande a retourné un code d'erreur : ").append(codeSortie).append("\n");
            }

            // Ajouter des messages de confirmation pour mkdir et del/rm
            String commandeLower = adjustedCommand.toLowerCase();
            if (commandeLower.startsWith("mkdir ")) {
                if (codeSortie == 0 && resultat.toString().isEmpty()) {
                    String nomDossier = adjustedCommand.substring(6).trim();
                    return "Répertoire créé avec succès : " + nomDossier;
                } else {
                    return "Erreur lors de la création du répertoire : " + resultat.toString();
                }
            } else if (commandeLower.startsWith("del ") || commandeLower.startsWith("rm ")) {
                if (codeSortie == 0 && resultat.toString().isEmpty()) {
                    String nomFichier = commandeLower.startsWith("del ") ? adjustedCommand.substring(4).trim() : adjustedCommand.substring(3).trim();
                    return "Fichier supprimé avec succès : " + nomFichier;
                } else {
                    return "Erreur lors de la suppression du fichier : " + resultat.toString();
                }
            }

            return resultat.toString();
        } catch (IOException | InterruptedException e) {
            return "Erreur lors de l'exécution de la commande : " + e.getMessage();
        }
    }

    public int getIdClient() {
        return idClient;
    }

    public String getAdresseIP() {
        return adresseIP;
    }

    public void deconnecter() {
        try {
            if (socketClient != null && !socketClient.isClosed()) {
                socketClient.close();
            }
            interfaceServeur.journaliser("Client #" + idClient + " (" + adresseIP + ") déconnecté via la méthode de déconnexion.");
        } catch (IOException e) {
            interfaceServeur.journaliser("Erreur lors de la déconnexion du client #" + idClient + " (" + adresseIP + ") : " + e.getMessage());
        }
    }
}