# Contrôle à Distance d'Ordinateur

![Java](https://img.shields.io/badge/Java-17-blue) ![JavaFX](https://img.shields.io/badge/JavaFX-17.0.14-green) ![OS](https://img.shields.io/badge/OS-Windows%20%7C%20Linux-orange)

Ce projet est une application Java qui permet de contrôler à distance un ordinateur via une interface graphique. L'application utilise une architecture client-serveur pour exécuter des commandes sur une machine distante, avec un support pour les systèmes d'exploitation Windows et Linux. Elle est développée avec Java 17 et JavaFX pour l'interface utilisateur.

## Fonctionnalités

- **Connexion client-serveur** : Connexion avec authentification par mot de passe.
- **Commandes prédéfinies** :
  - Windows : "dir", "mkdir", "del", "cd"
  - Linux : "ls", "mkdir", "rm", "cd"
- **Support multiplateforme** : Détection automatique du système d’exploitation (Windows ou Linux) pour exécuter les commandes appropriées.
- **Interface graphique moderne** : Interface utilisateur intuitive avec une sidebar pour les commandes prédéfinies, un historique des commandes, et une zone de résultats.
- **Historique des commandes** : Enregistrement des commandes exécutées pour une réutilisation facile.

## Prérequis

Pour exécuter ce projet, vous devez avoir les éléments suivants installés sur votre machine :

- **Java 17** : Le projet est développé avec Java 17. Assurez-vous d’avoir le JDK 17 installé.
  - Téléchargez-le depuis [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) ou installez-le via un gestionnaire de paquets :
    - Sur Windows : Téléchargez et installez depuis le site d’Oracle.
    - Sur Ubuntu/Linux : `sudo apt update && sudo apt install openjdk-17-jdk`
- **JavaFX 17.0.14** : L’interface graphique utilise JavaFX.
  - Téléchargez JavaFX depuis [GluonHQ](https://gluonhq.com/products/javafx/).
  - Décompressez le SDK dans un dossier (par exemple, `C:\javafx-sdk-17.0.14` sur Windows ou `/opt/javafx-sdk-17.0.14` sur Linux).
- **Un environnement réseau** : Si vous testez sur deux machines (par exemple, une VM Ubuntu et une machine Windows), assurez-vous qu’elles sont sur le même réseau et que les ports (par défaut `8080`) sont ouverts.

## Installation

1. **Clonez le dépôt GitHub** :
   git clone https://github.com/Yorodiouf13/Projet_Control_ordi.git

2. **Configurez JavaFX** :
Assurez-vous que le chemin vers le dossier lib de JavaFX est correct dans les commandes de compilation et d’exécution
(par exemple, C:\javafx-sdk-17.0.14\lib sur Windows ou /opt/javafx-sdk-17.0.14/lib sur Linux).

Compilez le projet :
Sur Windows powershell : javac -encoding UTF-8 -cp ".;C:\javafx-sdk-17.0.14\lib\*" *.java
Sur Linux bash : javac -encoding UTF-8 -cp "/home/$user/javafx-sdk-17.0.14/lib/*" *.java

## Utilisation
1. **Lancer le serveur**
Le serveur doit être exécuté sur la machine que vous souhaitez contrôler à distance.

Sur Windows powershell : java -Dfile.encoding=UTF-8 --module-path "C:\javafx-sdk-17.0.14\lib" --add-modules javafx.controls -cp . Serveur
Sur Linux bash : java -Dfile.encoding=UTF-8 --module-path "/opt/javafx-sdk-17.0.14/lib" --add-modules javafx.controls -cp . Serveur

Cliquez sur "Démarrer" dans l’interface serveur pour lancer le serveur sur le port 8080.

2. **Lancer le client**
Le client peut être exécuté sur une autre machine pour se connecter au serveur.

Sur Windows powershell : java -Dfile.encoding=UTF-8 --module-path "C:\javafx-sdk-17.0.14\lib" --add-modules javafx.controls -cp . Client
Sur Linux bash : java -Dfile.encoding=UTF-8 --module-path "/opt/javafx-sdk-17.0.14/lib" --add-modules javafx.controls -cp . Client

Dans l’interface client :
Entrez l’adresse IP du serveur (par exemple, 192.168.1.x pour une VM Ubuntu ou localhost si le serveur est sur la même machine).
Entrez le port (8080 par défaut).
Cliquez sur "Se connecter" et entrez le mot de passe par défaut (admin123).
Une fois connecté, utilisez les boutons de la sidebar (dir/ls, mkdir, del/rm, cd) ou entrez des commandes manuellement dans le champ de texte.

3. **Tester sur une machine virtuelle Ubuntu**
Pour tester le support Linux :

Configurez une VM Ubuntu avec un réseau en mode "Bridge" pour qu’elle soit accessible depuis votre machine hôte.
Trouvez l’adresse IP de la VM Ubuntu avec ip addr show.
Lancez le serveur sur la VM Ubuntu (voir étape 1).
Lancez le client sur votre machine Windows et connectez-vous à l’adresse IP de la VM Ubuntu.
Les commandes prédéfinies s’adapteront automatiquement (par exemple, dir deviendra ls).

# Structure du projet
  Serveur.java : Point d’entrée du serveur, gère les connexions des clients.
  
  Client.java : Point d’entrée du client, gère la connexion au serveur et l’envoi des commandes.
  
  InterfaceServeur.java : Interface graphique du serveur.
  
  InterfaceClient.java : Interface graphique du client.
  
  GestionClient.java : Gère l’exécution des commandes sur le serveur, avec support pour Windows et Linux.

## Interface Serveur
![image](https://github.com/user-attachments/assets/c161986a-a163-47f2-ba80-3f032ea265ec)

## Interface Client
![image](https://github.com/user-attachments/assets/b013c468-1ad2-45f5-8c96-8a2dc390f2e5)

# Auteurs
Oumar Yoro Diouf & Maman Nafy Ndiaye - Développeur principaux - Profil GitHub : Yorodiouf13
