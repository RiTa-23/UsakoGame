module com.example.usakogame {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.usakogame to javafx.fxml;
    exports com.example.usakogame;
}