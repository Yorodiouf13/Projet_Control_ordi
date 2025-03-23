
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class InterfaceServeur extends Application {
    private TextArea journal;
    private ListView<String> listeClients;
    private Serveur serveur = null;
    private Button boutonDemarrer;
    private Button boutonArrêter;
    private static InterfaceServeur instance;

    public static InterfaceServeur getInstance() {
        return instance;
    }

    public InterfaceServeur() {
        instance = this;
    }

    @Override
    public void start(Stage fenetrePrincipale) {
        // Initialiser le serveur
        try {
            serveur = new Serveur(8080);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du serveur dans InterfaceServeur : " + e.getMessage());
            e.printStackTrace();
            journal = new TextArea(); // Initialiser journal pour éviter NullPointerException
            journal.setEditable(false);
            journal.appendText("Erreur lors de l'initialisation du serveur : " + e.getMessage() + "\n");
        }

        // Créer la sidebar
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(150);
        sidebar.setStyle("-fx-background-color: #2C3E50; -fx-padding: 10;");

        Label titreSidebar = new Label("Serveur");
        titreSidebar.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        boutonDemarrer = new Button("Démarrer");
        boutonDemarrer.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;");
        boutonDemarrer.setOnMouseEntered(e -> boutonDemarrer.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonDemarrer.setOnMouseExited(e -> boutonDemarrer.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonDemarrer.setOnAction(e -> {
            if (serveur == null) {
                journaliser("Erreur : Serveur non initialisé.");
                return;
            }
            new Thread(() -> {
                serveur.demarrer();
                Platform.runLater(() -> {
                    boutonDemarrer.setDisable(true);
                    boutonArrêter.setDisable(false);
                });
            }).start();
        });

        boutonArrêter = new Button("Arrêter");
        boutonArrêter.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;");
        boutonArrêter.setOnMouseEntered(e -> boutonArrêter.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonArrêter.setOnMouseExited(e -> boutonArrêter.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120px;"));
        boutonArrêter.setDisable(true);
        boutonArrêter.setOnAction(e -> {
            if (serveur == null) {
                journaliser("Erreur : Serveur non initialisé.");
                return;
            }
            new Thread(() -> {
                serveur.arreter();
                Platform.runLater(() -> {
                    boutonDemarrer.setDisable(false);
                    boutonArrêter.setDisable(true);
                });
            }).start();
        });

        sidebar.getChildren().addAll(titreSidebar, boutonDemarrer, boutonArrêter);
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Zone de journalisation
        journal = new TextArea();
        journal.setEditable(false);
        journal.setStyle("-fx-background-color: #ECF0F1; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");

        // Liste des clients connectés
        listeClients = new ListView<>();
        listeClients.setStyle("-fx-background-color: #ECF0F1; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-border-color: #BDC3C7; -fx-border-width: 1px;");
        Label labelClients = new Label("Clients connectés :");
        labelClients.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Disposition principale
        VBox mainContent = new VBox(10, labelClients, listeClients, new Label("Journal des événements"), journal);
        mainContent.setPadding(new Insets(10));
        mainContent.setStyle("-fx-background-color: #FFFFFF;");

        HBox root = new HBox(sidebar, mainContent);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 600);
        fenetrePrincipale.setTitle("Serveur - Contrôle à Distance");
        fenetrePrincipale.setScene(scene);
        fenetrePrincipale.show();
    }

    public void journaliser(String message) {
        Platform.runLater(() -> {
            if (journal != null) {
                journal.appendText(message + "\n");
            }
        });
    }

    public void mettreAJourClients() {
        Platform.runLater(() -> {
            if (listeClients != null && serveur != null) {
                listeClients.getItems().clear();
                for (GestionClient client : serveur.getClientsConnectes()) {
                    String adresseIP = client.getAdresseIP() != null ? client.getAdresseIP() : "Inconnue";
                    listeClients.getItems().add("Client #" + client.getIdClient() + " (" + adresseIP + ")");
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}