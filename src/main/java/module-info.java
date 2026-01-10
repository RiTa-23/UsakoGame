module com.example.pakupakubird {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pakupakubird to javafx.fxml;
    exports com.example.pakupakubird;
}