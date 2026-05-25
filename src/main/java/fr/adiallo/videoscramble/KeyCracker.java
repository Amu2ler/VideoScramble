/*
 * KeyCracker.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Cassage de clé par force brute (2^15 possibilités).
 */
package fr.adiallo.videoscramble;

import org.opencv.core.Mat;

/**
 * Teste les 32768 clés possibles sur une image chiffrée et garde
 * celle qui maximise le critère de similarité entre lignes consécutives.
 */
public class KeyCracker {
    
    private SimilarityCriterion criterion;
    private ProgressCallback progressCallback;
    
    /** Callback pour suivre la progression du cassage. */
    public interface ProgressCallback {
        void onProgress(int current, int total, int bestKey, double bestScore);
    }

    public KeyCracker(SimilarityCriterion criterion) {
        this.criterion = criterion;
    }
    
    /** Enregistre le callback de progression. */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    /** Recherche exhaustive : essaie les 32768 clés et renvoie la meilleure. */
    public int crackKey(Mat scrambledFrame) {
        if (scrambledFrame.empty()) {
            return 0;
        }
        
        int bestKey = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Nombre total de clés possibles : 2^15 = 32768
        int totalKeys = 32768;
        
        // Essayer toutes les clés
        for (int key = 0; key < totalKeys; key++) {
            // Créer un moteur avec cette clé
            ScrambleEngine engine = new ScrambleEngine(key);
            
            // Déchiffrer l'image avec cette clé
            Mat unscrambled = engine.unscramble(scrambledFrame);
            
            // Évaluer la qualité de l'image déchiffrée
            double score = criterion.evaluateImage(unscrambled);
            
            // Mettre à jour la meilleure clé si nécessaire
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
            
            // Libérer la mémoire
            unscrambled.release();
            
            // Notifier la progression (tous les 100 essais)
            if (progressCallback != null && (key % 100 == 0 || key == totalKeys - 1)) {
                progressCallback.onProgress(key + 1, totalKeys, bestKey, bestScore);
            }
        }
        
        return bestKey;
    }
    
    /**
     * Variante optimisée : teste d'abord les steps impairs
     * (ceux-ci génèrent de vraies permutations car 2s+1 est toujours impair).
     */
    public int crackKeyOptimized(Mat scrambledFrame) {
        if (scrambledFrame.empty()) {
            return 0;
        }
        
        int bestKey = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Nombre total de clés possibles
        int totalKeys = 32768;
        int testedKeys = 0;
        
        // Priorité 1 : Tester les clés avec step impair (meilleures permutations)
        for (int offset = 0; offset < 256; offset++) {
            for (int step = 1; step < 128; step += 2) {  // Steps impairs seulement
                int key = (offset << 7) | step;
                
                ScrambleEngine engine = new ScrambleEngine(key);
                Mat unscrambled = engine.unscramble(scrambledFrame);
                double score = criterion.evaluateImage(unscrambled);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestKey = key;
                }
                
                unscrambled.release();
                testedKeys++;
                
                if (progressCallback != null && (testedKeys % 100 == 0)) {
                    progressCallback.onProgress(testedKeys, totalKeys, bestKey, bestScore);
                }
            }
        }
        
        // Priorité 2 : Tester les clés avec step pair (si nécessaire)
        // Pour gagner du temps, on peut arrêter ici si le score est déjà très bon
        
        return bestKey;
    }
    
    /** Teste une clé isolée et renvoie son score. */
    public double testKey(Mat scrambledFrame, int key) {
        if (scrambledFrame.empty()) {
            return Double.NEGATIVE_INFINITY;
        }
        
        ScrambleEngine engine = new ScrambleEngine(key);
        Mat unscrambled = engine.unscramble(scrambledFrame);
        double score = criterion.evaluateImage(unscrambled);
        unscrambled.release();
        
        return score;
    }
}
