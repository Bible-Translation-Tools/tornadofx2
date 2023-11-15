module tornadofxbuilders {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.media;

    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires transitive java.json;
    requires transitive java.prefs;
    requires transitive java.logging;

    opens tornadofxbuilders to javafx.fxml;

    exports tornadofxbuilders;
}