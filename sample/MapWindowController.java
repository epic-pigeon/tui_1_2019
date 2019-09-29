package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class MapWindowController implements Initializable {
    @FXML MapView mapView;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public MapView getMapView() {
        return mapView;
    }
}
