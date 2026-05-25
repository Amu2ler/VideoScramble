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
    
    /**
     * Recherche exhaustive : essaie les 32768 clés et renvoie la meilleure.
     *
     * Optimisations cumulées :
     *   1. Table de permutation int[] au lieu de matérialiser la Mat déchiffrée.
     *   2. Matrice de similarité PRÉ-CALCULÉE entre toutes les paires de lignes.
     *      Au lieu de recalculer la similarité (ligne_i, ligne_j) pour chacune
     *      des 32768 clés, on la calcule une seule fois (H*(H-1)/2 paires) et
     *      chaque clé devient une simple boucle d'additions sur ces valeurs.
     *      Gain : passage de O(K*H*W) à O(H²*W) pré-calcul + O(K*H) lookups,
     *      soit un facteur ~150 supplémentaire (cf. crack ~50s → <1s sur 640).
     */
    public int crackKey(Mat scrambledFrame) {
        if (scrambledFrame.empty()) {
            return 0;
        }

        int totalKeys = 32768;        // 2^15
        int height = scrambledFrame.rows();

        // ---- Phase 1 : pré-calcul de la matrice de similarité ----
        // Notification progression : pré-calcul affiché comme une étape "0".
        if (progressCallback != null) {
            progressCallback.onProgress(0, totalKeys, 0, Double.NEGATIVE_INFINITY);
        }
        double[][] sim = precomputeSimilarityMatrix(scrambledFrame);

        // ---- Phase 2 : test des 32768 clés (simples additions) ----
        int bestKey = 0;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int key = 0; key < totalKeys; key++) {
            ScrambleEngine engine = new ScrambleEngine(key);
            int[] perm = engine.getUnscramblePermutation(height);

            double score = 0.0;
            for (int i = 0; i < height - 1; i++) {
                score += sim[perm[i]][perm[i + 1]];
            }

            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }

            // On notifie moins souvent (256) puisque chaque clé est désormais
            // quasi-instantanée, sinon la mise à jour UI devient le goulot.
            if (progressCallback != null && (key % 256 == 0 || key == totalKeys - 1)) {
                progressCallback.onProgress(key + 1, totalKeys, bestKey, bestScore);
            }
        }

        return bestKey;
    }

    /**
     * Calcule la matrice symétrique des similarités entre toutes les paires de
     * lignes. Seule la moitié supérieure est calculée (les critères Euclidienne
     * et Pearson sont symétriques) puis recopiée pour éviter de gérer i/j à
     * l'exécution.
     */
    private double[][] precomputeSimilarityMatrix(Mat frame) {
        int height = frame.rows();
        double[][] sim = new double[height][height];
        for (int i = 0; i < height; i++) {
            sim[i][i] = 0.0;  // self-similarité non utilisée (paires (i,i+1) seulement)
            for (int j = i + 1; j < height; j++) {
                double s = criterion.computeSimilarity(frame, i, j);
                sim[i][j] = s;
                sim[j][i] = s;
            }
        }
        return sim;
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
                int[] perm = engine.getUnscramblePermutation(scrambledFrame.rows());
                double score = criterion.evaluateImageWithPermutation(scrambledFrame, perm);

                if (score > bestScore) {
                    bestScore = score;
                    bestKey = key;
                }

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
        int[] perm = engine.getUnscramblePermutation(scrambledFrame.rows());
        return criterion.evaluateImageWithPermutation(scrambledFrame, perm);
    }
}
