module fr.adiallo.videoscramble {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires opencv;

    opens fr.adiallo.videoscramble to javafx.fxml;
    exports fr.adiallo.videoscramble;
}