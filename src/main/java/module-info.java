module com.javafx.nutrimaker {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.base;

    requires mysql.connector.j;
    requires com.google.gson;

    requires kernel;
    requires layout;
    requires io;
    requires java.desktop;
    
    opens com.javafx.nutrimaker.models to javafx.base, com.google.gson;

    requires java.sql;
    requires jbcrypt;
    opens com.javafx.nutrimaker to javafx.fxml;
    exports com.javafx.nutrimaker;
}
