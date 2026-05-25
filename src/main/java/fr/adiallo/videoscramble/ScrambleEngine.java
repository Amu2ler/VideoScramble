/*
 * ScrambleEngine.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Moteur de chiffrement/déchiffrement par permutation de lignes.
 */
package fr.adiallo.videoscramble;

import org.opencv.core.Mat;

/**
 * Gère le mélange et le rétablissement des lignes d'une image.
 * La clé (15 bits) se décompose en un offset r (8 bits) et un pas s (7 bits).
 * La permutation est appliquée par blocs de taille puissance de 2 pour
 * s'adapter à n'importe quelle hauteur d'image.
 */
public class ScrambleEngine {
    
    private int offset;  // r : décalage (8 bits)
    private int step;    // s : pas (7 bits)
    
    /** Construit le moteur à partir d'une clé 15 bits (r = bits 14..7, s = bits 6..0). */
    public ScrambleEngine(int key) {
        this.offset = (key >> 7) & 0xFF;  // 8 bits de poids fort
        this.step = key & 0x7F;           // 7 bits de poids faible
    }
    
    /** Construit le moteur avec r et s fournis séparément. */
    public ScrambleEngine(int offset, int step) {
        this.offset = offset & 0xFF;
        this.step = step & 0x7F;
    }
    
    /** Reconstitue la clé 15 bits à partir de r et s. */
    public int getKey() {
        return (offset << 7) | step;
    }
    
    public int getOffset() {
        return offset;
    }

    public int getStep() {
        return step;
    }
    
    /**
     * Chiffre une image : les lignes sont permutées par blocs
     * de taille puissance de 2, du haut vers le bas.
     */
    public Mat scramble(Mat input) {
        Mat output = input.clone();
        int height = input.rows();
        
        // Traitement par itérations sur des puissances de 2
        int startLine = 0;
        while (startLine < height) {
            // Trouver la plus grande puissance de 2 <= lignes restantes
            int remainingLines = height - startLine;
            int blockSize = largestPowerOf2(remainingLines);
            
            // Appliquer la permutation sur ce bloc
            scrambleBlock(input, output, startLine, blockSize);
            
            startLine += blockSize;
        }
        
        return output;
    }
    
    /** Déchiffre une image en inversant la permutation des lignes. */
    public Mat unscramble(Mat input) {
        Mat output = input.clone();
        int height = input.rows();
        
        // Traitement par itérations sur des puissances de 2
        int startLine = 0;
        while (startLine < height) {
            // Trouver la plus grande puissance de 2 <= lignes restantes
            int remainingLines = height - startLine;
            int blockSize = largestPowerOf2(remainingLines);
            
            // Appliquer la permutation inverse sur ce bloc
            unscrambleBlock(input, output, startLine, blockSize);
            
            startLine += blockSize;
        }
        
        return output;
    }
    
    /** Permute les lignes d'un bloc [startLine, startLine+size). */
    private void scrambleBlock(Mat input, Mat output, int startLine, int size) {
        for (int i = 0; i < size; i++) {
            int sourceIdx = startLine + i;
            int destIdx = startLine + ((offset + (2 * step + 1) * i) % size);
            
            // Copier la ligne sourceIdx vers destIdx
            input.row(sourceIdx).copyTo(output.row(destIdx));
        }
    }
    
    /** Permutation inverse pour un bloc [startLine, startLine+size). */
    private void unscrambleBlock(Mat input, Mat output, int startLine, int size) {
        for (int i = 0; i < size; i++) {
            int destIdx = startLine + i;
            int sourceIdx = startLine + ((offset + (2 * step + 1) * i) % size);
            
            // Copier la ligne sourceIdx vers destIdx (inverse du scramble)
            input.row(sourceIdx).copyTo(output.row(destIdx));
        }
    }
    
    /** Plus grande puissance de 2 <= n. */
    private int largestPowerOf2(int n) {
        if (n <= 0) return 0;
        int power = 1;
        while (power * 2 <= n) {
            power *= 2;
        }
        return power;
    }
    
    // --- Nombre de copies pour la redondance (vote majoritaire) ---
    // Avec 5 copies et ~10% de probabilité d'erreur par bit,
    // le vote majoritaire donne ~99,14% de fiabilité par bit.
    private static final int REDUNDANCY = 5;

    /**
     * Place la clé (15 bits) dans le pixel (0,0) de l'image :
     * 5 bits dans B, 5 dans G, 5 dans R (poids faible).
     * La clé est aussi dupliquée sur les pixels suivants de la première
     * ligne (REDUNDANCY copies) pour résister à la compression.
     */
    public void embedKey(Mat frame) {
        if (frame.empty() || frame.cols() < REDUNDANCY) {
            return;
        }

        int key = getKey();

        for (int p = 0; p < REDUNDANCY; p++) {
            double[] pixel = frame.get(0, p);
            if (pixel == null || pixel.length < 3) continue;

            int bBits = key & 0x1F;
            int gBits = (key >> 5) & 0x1F;
            int rBits = (key >> 10) & 0x1F;

            pixel[0] = ((int) pixel[0] & 0xE0) | bBits;
            pixel[1] = ((int) pixel[1] & 0xE0) | gBits;
            pixel[2] = ((int) pixel[2] & 0xE0) | rBits;

            frame.put(0, p, pixel);
        }
    }

    /**
     * Extrait la clé depuis les REDUNDANCY premiers pixels de la ligne 0.
     * Pour chaque bit, un vote majoritaire est effectué sur les copies
     * afin de corriger les éventuelles altérations dues à la compression.
     */
    public static int extractKey(Mat frame) {
        if (frame.empty() || frame.cols() < REDUNDANCY) {
            return 0;
        }

        // Lire les REDUNDANCY copies de la clé
        int[] keys = new int[REDUNDANCY];
        for (int p = 0; p < REDUNDANCY; p++) {
            double[] pixel = frame.get(0, p);
            if (pixel == null || pixel.length < 3) {
                keys[p] = 0;
                continue;
            }
            int bBits = (int) pixel[0] & 0x1F;
            int gBits = (int) pixel[1] & 0x1F;
            int rBits = (int) pixel[2] & 0x1F;
            keys[p] = (rBits << 10) | (gBits << 5) | bBits;
        }

        // Vote majoritaire bit par bit
        int result = 0;
        for (int bit = 0; bit < 15; bit++) {
            int ones = 0;
            for (int p = 0; p < REDUNDANCY; p++) {
                if (((keys[p] >> bit) & 1) == 1) {
                    ones++;
                }
            }
            // Majorité : si plus de la moitié des copies ont ce bit à 1
            if (ones > REDUNDANCY / 2) {
                result |= (1 << bit);
            }
        }

        return result;
    }
}
