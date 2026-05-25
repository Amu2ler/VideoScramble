/*
 * SimilarityCriterion.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Interface commune aux critères de ressemblance entre lignes.
 */
package fr.adiallo.videoscramble;

import org.opencv.core.Mat;

/**
 * Définit comment évaluer la similarité entre deux lignes d'une image.
 * Un score élevé = lignes qui se ressemblent = image probablement bien déchiffrée.
 */
public interface SimilarityCriterion {
    
    /** Score de ressemblance entre les lignes row1 et row2. */
    double computeSimilarity(Mat frame, int row1, int row2);

    /**
     * Score global : somme la similarité de chaque paire de lignes
     * consécutives (i, i+1) dans l'image.
     */
    default double evaluateImage(Mat frame) {
        if (frame.empty() || frame.rows() < 2) {
            return 0.0;
        }

        double totalScore = 0.0;
        int numPairs = frame.rows() - 1;

        for (int i = 0; i < numPairs; i++) {
            totalScore += computeSimilarity(frame, i, i + 1);
        }

        return totalScore;
    }

    /**
     * Variante optimisée pour le cassage de clé : évalue le score sur l'image
     * DÉCHIFFRÉE sans la matérialiser. Pour chaque paire (i, i+1) de l'image
     * déchiffrée, on lit directement les lignes perm[i] et perm[i+1] de l'image
     * chiffrée fournie. Évite une allocation Mat complète par clé testée.
     */
    default double evaluateImageWithPermutation(Mat scrambledFrame, int[] unscramblePerm) {
        if (scrambledFrame.empty() || unscramblePerm == null || unscramblePerm.length < 2) {
            return 0.0;
        }

        double totalScore = 0.0;
        int numPairs = unscramblePerm.length - 1;

        for (int i = 0; i < numPairs; i++) {
            totalScore += computeSimilarity(scrambledFrame, unscramblePerm[i], unscramblePerm[i + 1]);
        }

        return totalScore;
    }
}
