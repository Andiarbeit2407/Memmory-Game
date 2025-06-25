module com.example.memorygame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires javafx.media;

    opens com.example.memorygame to javafx.fxml;
    exports com.example.memorygame;

}
