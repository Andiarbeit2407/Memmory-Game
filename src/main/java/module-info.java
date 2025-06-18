module com.example.memmorygame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires javafx.media;

    opens com.example.memmorygame to javafx.fxml;
    exports com.example.memmorygame;

}
