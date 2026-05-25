/*
 * EuclideanCriterion.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Critère n°1 : distance euclidienne entre lignes.
 */
package fr.adiallo.videoscramble;

import org.opencv.core.Mat;

/**
 * Retourne -sqrt(somme des carrés des différences pixel à pixel).
 * Le signe négatif permet de garder la convention "plus c'est grand, mieux c'est".
 */
public class EuclideanCriterion implements SimilarityCriterion {

    @Override
    public double computeSimilarity(Mat frame, int row1, int row2) {
        if (frame.empty() || row1 < 0 || row2 < 0 ||
                row1 >= frame.rows() || row2 >= frame.rows()) {
            return Double.NEGATIVE_INFINITY;
        }

        int width = frame.cols();
        int channels = frame.channels();

        double[] pixels1 = new double[width * channels];
        double[] pixels2 = new double[width * channels];

        if (frame.depth() == org.opencv.core.CvType.CV_8U) {
            byte[] buff1 = new byte[width * channels];
            byte[] buff2 = new byte[width * channels];
            frame.get(row1, 0, buff1);
            frame.get(row2, 0, buff2);
            for (int i = 0; i < pixels1.length; i++) {
                pixels1[i] = buff1[i] & 0xFF;
                pixels2[i] = buff2[i] & 0xFF;
            }
        } else {
            frame.get(row1, 0, pixels1);
            frame.get(row2, 0, pixels2);
        }

        // Calculer la distance euclidienne au carré (évite la racine carrée coûteuse)
        double sumSquaredDiff = 0.0;

        for (int i = 0; i < pixels1.length; i++) {
            double diff = pixels1[i] - pixels2[i];
            sumSquaredDiff += diff * diff;
        }

        // Retourner la négation pour que "plus élevé = meilleur"
        return -Math.sqrt(sumSquaredDiff);
    }
}
