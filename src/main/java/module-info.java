/*
 * module-info.java
 * Abdoulaye DIALLO & Arthur MULLER
 * BUT Informatique S5 - Alternants
 * Programmation Multimédia - VideoScramble
 *
 * Déclaration du module Java : dépendances JavaFX + OpenCV.
 */
module fr.adiallo.videoscramble {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires opencv;

    opens fr.adiallo.videoscramble to javafx.fxml;
    exports fr.adiallo.videoscramble;
}