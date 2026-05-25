/*
 * PearsonCriterion.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Critère n°2 : corrélation de Pearson entre lignes.
 */
package fr.adiallo.videoscramble;

import org.opencv.core.Mat;

/**
 * Calcule le coefficient de Pearson entre deux lignes (entre -1 et 1).
 * Moins sensible aux variations d'éclairage que la distance euclidienne
 * car les moyennes sont soustraites avant le calcul.
 */
public class PearsonCriterion implements SimilarityCriterion {

    @Override
    public double computeSimilarity(Mat frame, int row1, int row2) {
        if (frame.empty() || row1 < 0 || row2 < 0 ||
                row1 >= frame.rows() || row2 >= frame.rows()) {
            return Double.NEGATIVE_INFINITY;
        }

        int width = frame.cols();
        int channels = frame.channels();
        int n = width * channels;

        double[] pixels1 = new double[n];
        double[] pixels2 = new double[n];

        if (frame.depth() == org.opencv.core.CvType.CV_8U) {
            byte[] buff1 = new byte[n];
            byte[] buff2 = new byte[n];
            frame.get(row1, 0, buff1);
            frame.get(row2, 0, buff2);
            for (int i = 0; i < n; i++) {
                pixels1[i] = buff1[i] & 0xFF;
                pixels2[i] = buff2[i] & 0xFF;
            }
        } else {
            frame.get(row1, 0, pixels1);
            frame.get(row2, 0, pixels2);
        }

        // Calculer les moyennes
        double mean1 = 0.0;
        double mean2 = 0.0;

        for (int i = 0; i < n; i++) {
            mean1 += pixels1[i];
            mean2 += pixels2[i];
        }

        mean1 /= n;
        mean2 /= n;

        // Calculer les composantes du coefficient de Pearson
        double numerator = 0.0;
        double sumSquaredDiff1 = 0.0;
        double sumSquaredDiff2 = 0.0;

        for (int i = 0; i < n; i++) {
            double diff1 = pixels1[i] - mean1;
            double diff2 = pixels2[i] - mean2;

            numerator += diff1 * diff2;
            sumSquaredDiff1 += diff1 * diff1;
            sumSquaredDiff2 += diff2 * diff2;
        }

        // Éviter la division par zéro
        if (sumSquaredDiff1 == 0.0 || sumSquaredDiff2 == 0.0) {
            return 0.0;
        }

        double denominator = Math.sqrt(sumSquaredDiff1) * Math.sqrt(sumSquaredDiff2);

        return numerator / denominator;
    }
}
