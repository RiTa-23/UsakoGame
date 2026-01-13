module com.example.usakogame {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.usakogame to javafx.fxml;
    exports com.example.usakogame;
    exports com.example.usakogame.manager;
    exports com.example.usakogame.ui;
    exports com.example.usakogame.flappy;
    exports com.example.usakogame.runner;
}