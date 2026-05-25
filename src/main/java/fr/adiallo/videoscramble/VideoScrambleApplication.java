/*
 * VideoScrambleApplication.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Point d'entrée de l'application.
 * Usage CLI : mode inputVideo outputVideo key [embedKey]
 *   mode = scramble | unscramble | crack
 *   key  = 0-32767 ou -1 pour cassage automatique
 */
package fr.adiallo.videoscramble;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Lance la fenêtre JavaFX et transmet les paramètres CLI au contrôleur. */
public class VideoScrambleApplication extends Application {
    
    static {
        // Charger la bibliothèque native OpenCV
        nu.pattern.OpenCV.loadLocally();
    }
    
    private static String mode = "scramble";
    private static String inputVideo = "";
    private static String outputVideo = "";
    private static int key = 12345;
    private static boolean embedKey = false;
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            VideoScrambleApplication.class.getResource("videoscramble-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1400, 850);
        
        // Passer les paramètres au contrôleur
        VideoScrambleController controller = fxmlLoader.getController();
        controller.setParameters(mode, inputVideo, outputVideo, key, embedKey);
        
        stage.setTitle("VideoScramble - Chiffrement/Déchiffrement Vidéo");
        stage.setScene(scene);
        stage.show();
        
        // Démarrer le traitement si des paramètres ont été fournis
        if (!inputVideo.isEmpty() && !outputVideo.isEmpty()) {
            controller.startProcessing();
        }
    }
    
    /** Parse les arguments CLI puis lance JavaFX. */
    public static void main(String[] args) {
        // Parser les arguments
        if (args.length >= 4) {
            mode = args[0];
            inputVideo = args[1];
            outputVideo = args[2];
            
            try {
                key = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.err.println("Erreur : la clé doit être un nombre entier.");
                key = 0;
            }
            
            if (args.length >= 5) {
                embedKey = Boolean.parseBoolean(args[4]);
            }
        }
        
        // Afficher les paramètres
        if (args.length > 0) {
            System.out.println("=== VideoScramble ===");
            System.out.println("Mode         : " + mode);
            System.out.println("Vidéo entrée : " + inputVideo);
            System.out.println("Vidéo sortie : " + outputVideo);
            System.out.println("Clé          : " + key);
            System.out.println("Embarquer clé: " + embedKey);
            System.out.println("====================");
        }
        
        launch();
    }
}
