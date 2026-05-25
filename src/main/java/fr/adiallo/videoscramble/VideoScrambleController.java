/*
 * VideoScrambleController.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Contrôleur JavaFX : gère l'IHM, le lancement du traitement
 * et l'affichage côte à côte des vidéos.
 */
package fr.adiallo.videoscramble;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoScrambleController {

    @FXML
    private ImageView inputImageView;

    @FXML
    private ImageView outputImageView;

    @FXML
    private Label keyLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label fpsLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button startButton;

    @FXML
    private Button reassembleButton;

    @FXML
    private Button selectInputButton;

    @FXML
    private Button selectOutputButton;

    @FXML
    private TextField keyInputField;

    @FXML
    private ChoiceBox<String> criterionChoice;

    @FXML
    private CheckBox embedKeyCheckBox;

    @FXML
    private VBox selectionScreen;

    @FXML
    private BorderPane processingScreen;

    @FXML
    private Label titleLabel;

    private String mode = "scramble";
    private String inputVideoPath = "";
    private String outputVideoPath = "";
    private int key = 12345;
    private boolean embedKey = false;
    private boolean isProcessing = false;
    /** Clé retrouvée par le dernier crack, utilisée par "Réassembler". -1 si aucun. */
    private int lastCrackedKey = -1;
    /** Récap textuel du dernier crack (temps précalcul + brute force) pour réaffichage. */
    private String lastCrackSummary = "";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        progressBar.setProgress(0.0);
        statusLabel.setText("Prêt");
    }

    /** Reçoit les paramètres CLI transmis par l'Application. */
    public void setParameters(String mode, String inputVideo, String outputVideo, int key, boolean embedKey) {
        this.mode = mode;
        this.inputVideoPath = inputVideo;
        this.outputVideoPath = outputVideo;
        this.key = key;
        this.embedKey = embedKey;
        updateKeyLabel();
    }

    @FXML
    private void selectScrambleMode() {
        if (!validateAndSetKey())
            return;
        mode = "scramble";
        embedKey = readEmbedKeyFromUI();
        titleLabel.setText("Mode: Chiffrement 🔒" + (embedKey ? "  +  Clé embarquée 🔑" : ""));
        showProcessingScreen();
        updateKeyLabel();
    }

    @FXML
    private void selectUnscrambleMode() {
        if (!validateAndSetKey())
            return;
        mode = "unscramble";
        embedKey = readEmbedKeyFromUI();
        titleLabel.setText("Mode: Déchiffrement 🔓" + (embedKey ? "  +  Extraction clé 🔑" : ""));
        showProcessingScreen();
        updateKeyLabel();
    }

    @FXML
    private void selectCrackMode() {
        mode = "crack";
        key = -1; // Indiquer le mode crack
        embedKey = false; // Le cassage n'a pas de sens combiné à l'extraction
        titleLabel.setText("Mode: Cassage de clé 🔨");
        showProcessingScreen();
        keyLabel.setText("Clé : Inconnue (sera trouvée)");
    }

    /** Lit l'état de la checkbox "Embarquer la clé" (étape 3 du sujet). */
    private boolean readEmbedKeyFromUI() {
        return embedKeyCheckBox != null && embedKeyCheckBox.isSelected();
    }

    private boolean validateAndSetKey() {
        try {
            String text = keyInputField.getText();
            if (text == null || text.trim().isEmpty()) {
                statusLabel.setText("Veuillez entrer une clé valide !");
                return false;
            }
            int newKey = Integer.parseInt(text.trim());
            if (newKey < 0 || newKey > 32767) {
                statusLabel.setText("La clé doit être entre 0 et 32767 !");
                return false;
            }
            this.key = newKey;
            return true;
        } catch (NumberFormatException e) {
            statusLabel.setText("La clé doit être un nombre entier !");
            return false;
        }
    }

    @FXML
    private void goBack() {
        processingScreen.setVisible(false);
        selectionScreen.setVisible(true);
        // Réinitialise l'état du dernier crack pour ne pas proposer "Réassembler"
        // sur une session sans rapport.
        resetCrackState();
    }

    /** Cache le bouton Réassembler et restaure l'état initial (avant crack). */
    private void resetCrackState() {
        lastCrackedKey = -1;
        lastCrackSummary = "";
        if (reassembleButton != null) {
            reassembleButton.setVisible(false);
            reassembleButton.setManaged(false);
        }
        if (startButton != null) {
            startButton.setVisible(true);
            startButton.setManaged(true);
        }
    }

    /**
     * Action déclenchée par le bouton "🎬 Réassembler" affiché après un crack.
     * Bascule le mode en "unscramble" avec la clé retrouvée et relance le
     * pipeline standard — sans refaire la force brute.
     */
    @FXML
    public void startReassemble() {
        if (lastCrackedKey < 0) {
            statusLabel.setText("Aucune clé à réutiliser. Lancez d'abord un cassage.");
            return;
        }
        // On garde le récap du crack visible en début de phase de déchiffrement
        // pour que l'utilisateur ait toujours sous les yeux le résultat obtenu.
        mode = "unscramble";
        key = lastCrackedKey;
        // Le bouton Réassembler disparaît, on remet "Démarrer" prêt pour une autre run.
        reassembleButton.setVisible(false);
        reassembleButton.setManaged(false);
        startButton.setVisible(true);
        startButton.setManaged(true);
        updateKeyLabel();
        startProcessing();
    }

    private void showProcessingScreen() {
        selectionScreen.setVisible(false);
        processingScreen.setVisible(true);
        // Toute nouvelle entrée sur l'écran traitement repart d'un état "pas de crack en attente"
        resetCrackState();
    }

    @FXML
    private void selectInputVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner la vidéo d'entrée");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov", "*.mkv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File file = fileChooser.showOpenDialog(selectInputButton.getScene().getWindow());
        if (file != null) {
            inputVideoPath = file.getAbsolutePath();
            statusLabel.setText("Vidéo d'entrée : " + file.getName());
        }
    }

    @FXML
    private void selectOutputVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner la vidéo de sortie");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mov", "*.mkv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File file = fileChooser.showSaveDialog(selectOutputButton.getScene().getWindow());
        if (file != null) {
            outputVideoPath = file.getAbsolutePath();
            statusLabel.setText("Vidéo de sortie : " + file.getName());
        }
    }

    /** Lance le traitement dans un thread séparé pour ne pas bloquer l'IHM. */
    @FXML
    public void startProcessing() {
        if (isProcessing) {
            statusLabel.setText("Un traitement est déjà en cours...");
            return;
        }

        if (inputVideoPath.isEmpty() || outputVideoPath.isEmpty()) {
            statusLabel.setText("Veuillez sélectionner les fichiers d'entrée et de sortie.");
            return;
        }

        isProcessing = true;
        startButton.setDisable(true);
        progressBar.setProgress(0.0);

        executor.submit(() -> {
            try {
                processVideo();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur : " + e.getMessage());
                });
            } finally {
                isProcessing = false;
                Platform.runLater(() -> {
                    startButton.setDisable(false);
                });
            }
        });
    }

    /** Boucle principale : lit chaque frame, applique scramble/unscramble, écrit le résultat. */
    private void processVideo() {
        VideoCapture capture = new VideoCapture(inputVideoPath);

        if (!capture.isOpened()) {
            Platform.runLater(() -> {
                statusLabel.setText("Erreur : impossible d'ouvrir la vidéo d'entrée.");
            });
            return;
        }

        // Obtenir les propriétés de la vidéo
        int frameWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);

        final double sourceFps = fps;
        final int totalFramesFinal = totalFrames;
        Platform.runLater(() -> {
            fpsLabel.setText(String.format("FPS source : %.1f  ·  Frames : %d  ·  Résolution : %d×%d",
                    sourceFps, totalFramesFinal, frameWidth, frameHeight));
        });

        // Supprimer le fichier de sortie s'il existe (nécessaire sur macOS/AVFoundation)
        File outputFile = new File(outputVideoPath);
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                System.err.println("Attention : Impossible de supprimer " + outputVideoPath);
            }
        }

        // Choix du codec : si embedKey est actif, on privilégie FFV1 (sans perte)
        // pour que les bits de la clé ne soient pas altérés par la compression.
        // Sinon, on essaie les codecs classiques avec perte.
        int[] codecs;
        String[] codecNames;
        String actualOutputPath = outputVideoPath;

        if (embedKey) {
            // FFV1 nécessite un conteneur .avi ou .mkv
            if (!actualOutputPath.endsWith(".avi") && !actualOutputPath.endsWith(".mkv")) {
                actualOutputPath = actualOutputPath.replaceAll("\\.[^.]+$", ".avi");
                System.out.println("embedKey actif : sortie redirigée vers " + actualOutputPath);
            }
            codecs = new int[]{
                    VideoWriter.fourcc('F', 'F', 'V', '1'),  // Sans perte (prioritaire)
                    VideoWriter.fourcc('H', 'F', 'Y', 'U'),  // HuffYUV (sans perte, fallback)
                    VideoWriter.fourcc('M', 'J', 'P', 'G')   // Dernier recours (avec perte légère)
            };
            codecNames = new String[]{"FFV1", "HFYU", "MJPG"};
        } else {
            codecs = new int[]{
                    VideoWriter.fourcc('m', 'p', '4', 'v'),
                    VideoWriter.fourcc('a', 'v', 'c', '1'),
                    VideoWriter.fourcc('H', '2', '6', '4'),
                    VideoWriter.fourcc('M', 'J', 'P', 'G')
            };
            codecNames = new String[]{"mp4v", "avc1", "H264", "MJPG"};
        }

        VideoWriter writer = new VideoWriter();

        for (int i = 0; i < codecs.length; i++) {
            writer = new VideoWriter(
                    actualOutputPath,
                    codecs[i],
                    fps,
                    new org.opencv.core.Size(frameWidth, frameHeight),
                    true);

            if (writer.isOpened()) {
                System.out.println("Codec utilisé : " + codecNames[i]);
                break;
            } else {
                System.out.println("Echec avec " + codecNames[i]);
                writer.release();
            }
        }

        if (!writer.isOpened()) {
            Platform.runLater(() -> {
                statusLabel.setText("Erreur : impossible de créer la vidéo de sortie (tous les codecs ont échoué).");
            });
            capture.release();
            return;
        }

        // Créer le moteur de scrambling
        ScrambleEngine engine = null;
        if (key >= 0) {
            engine = new ScrambleEngine(key);
        }

        // Cassage de clé si demandé (utilise la première frame non noire)
        if ("crack".equals(mode) || key < 0) {
            Mat firstFrame = findNonBlackFrame(capture);
            if (firstFrame != null && !firstFrame.empty()) {
                SimilarityCriterion criterion = buildCriterionFromUI();
                final String criterionName = criterion.getClass().getSimpleName().replace("Criterion", "");
                Platform.runLater(() ->
                    statusLabel.setText("🔨 Pré-calcul de la matrice de similarité (" + criterionName + ")…"));

                // Suivi du temps : on distingue le pré-calcul de la phase brute-force
                // pour pouvoir afficher un récap à l'utilisateur à la fin.
                final long[] times = new long[3]; // 0=start, 1=fin_précalcul, 2=fin_total
                times[0] = System.currentTimeMillis();

                KeyCracker cracker = new KeyCracker(criterion);
                cracker.setProgressCallback((current, total, bestKey, bestScore) -> {
                    // Premier callback avec current>=1 → pré-calcul terminé,
                    // la phase brute-force commence vraiment ici.
                    if (current >= 1 && times[1] == 0) {
                        times[1] = System.currentTimeMillis();
                    }
                    Platform.runLater(() -> {
                        double progress = (double) current / total;
                        progressBar.setProgress(progress);
                        if (current == 0) {
                            // Pendant le pré-calcul, on n'a pas encore de meilleure clé
                            statusLabel.setText("🔨 Pré-calcul de la matrice de similarité (" + criterionName + ")…");
                        } else {
                            long elapsedMs = System.currentTimeMillis() - times[0];
                            statusLabel.setText(String.format(
                                    "🔨 Cassage : %d / %d (%.1f%%)  ·  Meilleure clé : %d  ·  Score : %.3f  ·  ⏱ %s",
                                    current, total, progress * 100, bestKey, bestScore, formatDuration(elapsedMs)));
                        }
                    });
                });

                key = cracker.crackKey(firstFrame);
                times[2] = System.currentTimeMillis();
                engine = new ScrambleEngine(key);

                final long precomputeMs = Math.max(0, times[1] - times[0]);
                final long bruteForceMs = Math.max(0, times[2] - times[1]);
                final long totalCrackMs = times[2] - times[0];
                final int foundKey = key;

                firstFrame.release();

                // En mode "crack" pur, on s'arrête ici : l'utilisateur regarde
                // les résultats puis lance le réassemblage via le bouton dédié.
                if ("crack".equals(mode)) {
                    lastCrackedKey = foundKey;
                    lastCrackSummary = String.format(
                            "✓ Clé trouvée : %d  ·  Pré-calcul %.2fs + Force brute %.2fs = Total %.2fs",
                            foundKey,
                            precomputeMs / 1000.0,
                            bruteForceMs / 1000.0,
                            totalCrackMs / 1000.0);

                    // Libérer les ressources : le réassemblage rouvrira tout.
                    writer.release();
                    capture.release();

                    Platform.runLater(() -> {
                        updateKeyLabel();
                        progressBar.setProgress(1.0);
                        statusLabel.setText(lastCrackSummary +
                                "  ·  Cliquez sur « 🎬 Réassembler » pour déchiffrer la vidéo");
                        // Afficher le bouton de réassemblage, masquer le bouton de démarrage
                        reassembleButton.setVisible(true);
                        reassembleButton.setManaged(true);
                        startButton.setVisible(false);
                        startButton.setManaged(false);
                    });
                    return;
                }

                // Mode hors "crack" (ex : key < 0 par CLI) : on continue automatiquement.
                Platform.runLater(() -> {
                    updateKeyLabel();
                    statusLabel.setText(String.format(
                            "✓ Clé trouvée : %d  ·  Total %.2fs  ·  Déchiffrement en cours…",
                            foundKey, totalCrackMs / 1000.0));
                });
            }

            // Réinitialiser la capture
            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
        }

        // Traiter chaque frame
        Mat frame = new Mat();
        int frameCount = 0;
        long startTime = System.currentTimeMillis();

        while (capture.read(frame) && !frame.empty()) {
            Mat processedFrame;

            // Extraire la clé si elle est embarquée
            if (embedKey && "unscramble".equals(mode)) {
                int extractedKey = ScrambleEngine.extractKey(frame);
                if (extractedKey != key) {
                    key = extractedKey;
                    engine = new ScrambleEngine(key);
                    Platform.runLater(() -> updateKeyLabel());
                }
            }

            // Appliquer le traitement
            if ("scramble".equals(mode)) {
                processedFrame = engine.scramble(frame);
                if (embedKey) {
                    engine.embedKey(processedFrame);
                }
            } else {
                processedFrame = engine.unscramble(frame);
            }

            // Écrire la frame traitée
            writer.write(processedFrame);

            // Afficher les frames
            final Mat inputCopy = frame.clone();
            final Mat outputCopy = processedFrame.clone();
            Platform.runLater(() -> {
                inputImageView.setImage(mat2Image(inputCopy));
                outputImageView.setImage(mat2Image(outputCopy));
                inputCopy.release();
                outputCopy.release();
            });

            // Mettre à jour la progression
            frameCount++;
            final int currentFrame = frameCount;
            Platform.runLater(() -> {
                double progress = (double) currentFrame / totalFrames;
                progressBar.setProgress(progress);

                long elapsed = System.currentTimeMillis() - startTime;
                double avgFps = currentFrame / (elapsed / 1000.0);
                long etaMs = currentFrame < totalFrames
                        ? (long) ((elapsed / (double) currentFrame) * (totalFrames - currentFrame))
                        : 0;

                statusLabel.setText(String.format(
                        "⏱ %s  (ETA %s)  ·  Frames %d / %d (%.1f%%)  ·  %.1f fps moy.",
                        formatDuration(elapsed), formatDuration(etaMs),
                        currentFrame, totalFrames, progress * 100, avgFps));
            });

            processedFrame.release();
        }

        // Libérer les ressources
        frame.release();
        writer.release();
        capture.release();

        long totalTime = System.currentTimeMillis() - startTime;
        final int totalFramesProcessed = frameCount;
        final double avgFpsFinal = totalFramesProcessed / (totalTime / 1000.0);

        // Taille du fichier de sortie effectivement écrit (peut être en .avi si embedKey)
        File written = new File(actualOutputPath);
        final long outputSize = written.exists() ? written.length() : 0;
        final String outputName = written.getName();

        Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            statusLabel.setText(String.format(
                    "✅ Terminé  ·  %d frames en %s  ·  %.1f fps moy.  ·  Sortie : %s (%s)",
                    totalFramesProcessed,
                    formatDuration(totalTime),
                    avgFpsFinal,
                    outputName,
                    formatSize(outputSize)));
        });
    }

    /** Formate une durée en ms vers "m:ss" (ou "m:ss.S" sous la seconde). */
    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return String.format("%.2fs", millis / 1000.0);
        }
        long s = millis / 1000;
        long ms = millis % 1000;
        if (s < 60) {
            return String.format("%d.%ds", s, ms / 100);
        }
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** Formate une taille en octets vers une représentation lisible (Ko / Mo / Go). */
    private static String formatSize(long bytes) {
        if (bytes <= 0) return "—";
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024L * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f Mo", bytes / (1024.0 * 1024));
        return String.format("%.2f Go", bytes / (1024.0 * 1024 * 1024));
    }

    /** Saute les éventuelles frames noires du début (intro, fondu). */
    private Mat findNonBlackFrame(VideoCapture capture) {
        Mat frame = new Mat();
        int maxFramesToCheck = 30; // Vérifier les 30 premières frames

        for (int i = 0; i < maxFramesToCheck; i++) {
            if (!capture.read(frame) || frame.empty()) {
                break;
            }

            // Vérifier si la frame n'est pas noire (moyenne des pixels > seuil)
            double[] meanValues = org.opencv.core.Core.mean(frame).val;
            double meanBrightness = (meanValues[0] + meanValues[1] + meanValues[2]) / 3.0;

            if (meanBrightness > 10.0) { // Seuil arbitraire
                return frame.clone();
            }
        }

        frame.release();
        return null;
    }

    /**
     * Construit le critère de similarité choisi dans l'IHM.
     * Par défaut (ou si la ChoiceBox n'est pas accessible depuis un thread non-UI),
     * on retombe sur Pearson, plus robuste aux variations d'éclairage.
     */
    private SimilarityCriterion buildCriterionFromUI() {
        String selected = null;
        try {
            selected = criterionChoice == null ? null : criterionChoice.getValue();
        } catch (Exception ignored) {
            // Lecture depuis un thread non-UI : on garde le défaut
        }
        if (selected != null && selected.toLowerCase().contains("euclid")) {
            return new EuclideanCriterion();
        }
        return new PearsonCriterion();
    }

    /** Rafraîchit le label de clé dans l'IHM. */
    private void updateKeyLabel() {
        int offset = (key >> 7) & 0xFF;
        int step = key & 0x7F;
        Platform.runLater(() -> {
            keyLabel.setText(String.format("Clé: %d (offset=%d, step=%d)", key, offset, step));
        });
    }

    /** Conversion Mat OpenCV -> Image JavaFX (via BufferedImage). */
    private Image mat2Image(Mat mat) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(mat), null);
        } catch (Exception e) {
            System.err.println("Erreur de conversion Mat vers Image : " + e.getMessage());
            return null;
        }
    }

    /** Mat -> BufferedImage en copiant directement les octets du raster. */
    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image;
        if (channels > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }

        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    /** Arrête proprement le thread de traitement. */
    public void shutdown() {
        executor.shutdown();
    }
}
