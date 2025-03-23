import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InterfaceClient extends Application {
    private TextField champCommande;
    private TextArea zoneResultat;
    private Client client;
    private static InterfaceClient instance;
    private Button boutonEnvoyer;
    private Button boutonDir;
    private Button boutonMkdir;
    private Button boutonDel;
    private Button boutonCd;
    private ComboBox<String> historiqueCommandes;
    private List<String> commandesHistorique;
    private TextField champHote;
    private TextField champPort;
    private Button boutonConnecter;
    private Button boutonDeconnecter;
    private String serverOsType; // Nouvelle variable pour stocker le type de système d'exploitation

    public InterfaceClient() {
        instance = this;
        commandesHistorique = new ArrayList<>();
        client = new Client(this);
    }

    public static InterfaceClient getInstance() {
        return instance;
    }

    @Override
    public void start(Stage fenetrePrincipale) {
        // Créer la sidebar
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(150);
        sidebar.setStyle("-fx-background-color: #2C3E50; -fx-padding: 10;");

        Label titreSidebar = new Label("Client");
        titreSidebar.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        boutonConnecter = new Button("Se connecter");
        boutonConnecter.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;");
        boutonConnecter.setOnMouseEntered(e -> boutonConnecter.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonConnecter.setOnMouseExited(e -> boutonConnecter.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonConnecter.setOnAction(e -> {
            String hote = champHote.getText();
            String portStr = champPort.getText();
            try {
                int port = Integer.parseInt(portStr);
                new Thread(() -> client.seConnecter(hote, port)).start();
            } catch (NumberFormatException ex) {
                journaliser("Erreur : Le port doit être un nombre valide.");
            }
        });

        boutonDeconnecter = new Button("Se déconnecter");
        boutonDeconnecter.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;");
        boutonDeconnecter.setOnMouseEntered(e -> boutonDeconnecter.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonDeconnecter.setOnMouseExited(e -> boutonDeconnecter.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonDeconnecter.setDisable(true);
        boutonDeconnecter.setOnAction(e -> new Thread(() -> client.deconnecter()).start());

        // Boutons pour les commandes de base
        boutonDir = createCommandButton("dir", "ls", "dir");
        boutonMkdir = createCommandButton("mkdir", "mkdir", "mkdir ");
        boutonDel = createCommandButton("del", "rm", "del ");
        boutonCd = createCommandButton("cd", "cd", "cd ");

        sidebar.getChildren().addAll(titreSidebar, boutonConnecter, boutonDeconnecter, boutonDir, boutonMkdir, boutonDel, boutonCd);
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Zone principale
        champHote = new TextField("localhost");
        champHote.setPromptText("Adresse IP du serveur");
        champHote.setStyle("-fx-background-color: #ECF0F1; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");

        champPort = new TextField("8080");
        champPort.setPromptText("Port du serveur");
        champPort.setStyle("-fx-background-color: #ECF0F1; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");

        HBox connexionBox = new HBox(10, new Label("Hôte :"), champHote, new Label("Port :"), champPort);
        connexionBox.setAlignment(Pos.CENTER_LEFT);

        champCommande = new TextField();
        champCommande.setPromptText("Entrez une commande...");
        champCommande.setDisable(true);
        champCommande.setStyle("-fx-background-color: #ECF0F1; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");

        boutonEnvoyer = new Button("Envoyer");
        boutonEnvoyer.setDisable(true);
        boutonEnvoyer.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-size: 14px;");
        boutonEnvoyer.setOnMouseEntered(e -> boutonEnvoyer.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 14px;"));
        boutonEnvoyer.setOnMouseExited(e -> boutonEnvoyer.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-size: 14px;"));
        boutonEnvoyer.setOnAction(e -> {
            String commande = champCommande.getText();
            if (!commande.isEmpty()) {
                if (client != null && client.isConnexionReussie()) {
                    new Thread(() -> {
                        client.envoyerCommande(commande);
                        Platform.runLater(() -> {
                            ajouterAHistorique(commande);
                            champCommande.clear();
                        });
                    }).start();
                } else {
                    journaliser("Erreur : Pas de connexion au serveur.");
                }
            }
        });

        HBox commandeBox = new HBox(10, champCommande, boutonEnvoyer);
        commandeBox.setAlignment(Pos.CENTER_LEFT);

        historiqueCommandes = new ComboBox<>();
        historiqueCommandes.setPromptText("Historique des commandes");
        historiqueCommandes.setPrefWidth(400);
        historiqueCommandes.setDisable(true);
        historiqueCommandes.setStyle("-fx-background-color: #ECF0F1; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");
        historiqueCommandes.setOnAction(e -> {
            String commandeSelectionne = historiqueCommandes.getSelectionModel().getSelectedItem();
            if (commandeSelectionne != null) {
                champCommande.setText(commandeSelectionne);
            }
        });

        zoneResultat = new TextArea();
        zoneResultat.setEditable(false);
        zoneResultat.setStyle("-fx-background-color: #ECF0F1; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");

        VBox mainContent = new VBox(10, connexionBox, new Label("Commande"), commandeBox, new Label("Historique"), historiqueCommandes, new Label("Résultat"), zoneResultat);
        mainContent.setPadding(new Insets(10));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        HBox root = new HBox(sidebar, mainContent);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 600);
        fenetrePrincipale.setTitle("Client - Contrôle à Distance");
        fenetrePrincipale.setScene(scene);
        fenetrePrincipale.show();
    }

    private Button createCommandButton(String windowsLabel, String linuxLabel, String command) {
        Button button = new Button(windowsLabel);
        button.setDisable(true);
        button.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #E67E22; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #F39C12; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        button.setOnAction(e -> {
            if (client != null && client.isConnexionReussie()) {
                String fullCommand = command;
                if (!command.endsWith(" ")) {
                    fullCommand = command;
                } else {
                    String argument = champCommande.getText();
                    if (argument.isEmpty()) {
                        journaliser("Erreur : Veuillez entrer un argument pour " + button.getText() + ".");
                        return;
                    }
                    fullCommand = command + argument;
                }
                String finalCommand = fullCommand;
                new Thread(() -> {
                    client.envoyerCommande(finalCommand);
                    Platform.runLater(() -> {
                        ajouterAHistorique(finalCommand);
                        champCommande.clear();
                    });
                }).start();
            } else {
                journaliser("Erreur : Pas de connexion au serveur.");
            }
        });
        // Ajouter des propriétés personnalisées pour stocker les labels Windows et Linux
        button.getProperties().put("windowsLabel", windowsLabel);
        button.getProperties().put("linuxLabel", linuxLabel);
        return button;
    }

    public void activerInterface() {
        Platform.runLater(() -> {
            champCommande.setDisable(false);
            historiqueCommandes.setDisable(false);
            boutonEnvoyer.setDisable(false);
            boutonDir.setDisable(false);
            boutonMkdir.setDisable(false);
            boutonDel.setDisable(false);
            boutonCd.setDisable(false);
            boutonConnecter.setDisable(true);
            boutonDeconnecter.setDisable(false);
            journaliser("Connecté au serveur " + champHote.getText() + ":" + champPort.getText());
        });
    }

    public void desactiverInterface() {
        Platform.runLater(() -> {
            champCommande.setDisable(true);
            historiqueCommandes.setDisable(true);
            boutonEnvoyer.setDisable(true);
            boutonDir.setDisable(true);
            boutonMkdir.setDisable(true);
            boutonDel.setDisable(true);
            boutonCd.setDisable(true);
            boutonConnecter.setDisable(false);
            boutonDeconnecter.setDisable(true);
            journaliser("Déconnecté du serveur.");
        });
    }

    private void ajouterAHistorique(String commande) {
        if (!commandesHistorique.contains(commande)) {
            commandesHistorique.add(commande);
            historiqueCommandes.getItems().add(commande);
        }
    }

    public void journaliser(String message) {
        Platform.runLater(() -> {
            if (zoneResultat != null) {
                zoneResultat.appendText(message + "\n");
            }
        });
    }

    public void afficherResultat(String resultat) {
        Platform.runLater(() -> {
            if (zoneResultat != null) {
                zoneResultat.appendText("Résultat :\n" + resultat + "\n");
            }
        });
    }

    public String demanderMotDePasse() {
        final String[] motDePasse = {null};
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Authentification");
            dialog.setHeaderText("Connexion au serveur");
            dialog.setContentText("Veuillez entrer le mot de passe :");

            Optional<String> resultat = dialog.showAndWait();
            if (resultat.isPresent()) {
                motDePasse[0] = resultat.get();
            } else {
                journaliser("Saisie du mot de passe annulée.");
            }
        });

        // Attendre que l'utilisateur entre le mot de passe
        while (motDePasse[0] == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                journaliser("Erreur lors de l'attente du mot de passe : " + e.getMessage());
            }
        }
        return motDePasse[0];
    }

    public void setServerOsType(String osType) {
        this.serverOsType = osType;
        journaliser("Mise à jour du type de système d'exploitation : " + osType); // Log pour déboguer
        Platform.runLater(() -> {
            if ("Linux".equals(osType)) {
                journaliser("Mise à jour des labels pour Linux");
                boutonDir.setText((String) boutonDir.getProperties().get("linuxLabel"));
                boutonMkdir.setText((String) boutonMkdir.getProperties().get("linuxLabel"));
                boutonDel.setText((String) boutonDel.getProperties().get("linuxLabel"));
                boutonCd.setText((String) boutonCd.getProperties().get("linuxLabel"));
            } else {
                journaliser("Mise à jour des labels pour Windows");
                boutonDir.setText((String) boutonDir.getProperties().get("windowsLabel"));
                boutonMkdir.setText((String) boutonMkdir.getProperties().get("windowsLabel"));
                boutonDel.setText((String) boutonDel.getProperties().get("windowsLabel"));
                boutonCd.setText((String) boutonCd.getProperties().get("windowsLabel"));
            }
        });
    }    public static void main(String[] args) {
        launch(args);
    }
}