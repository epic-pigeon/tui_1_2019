package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void createMapWindow(
        double focusDistance,
        double photoCensorHeight,
        double photoCensorWidth,
        double height,
        double fieldHeight,
        double fieldWidth
    ) {
        double[] a = calculate(focusDistance, photoCensorHeight, photoCensorWidth, height);
        invokeMapWindow(
                calculateRoute(a[0], a[1], fieldHeight, fieldWidth), fieldHeight, fieldWidth
        );
    }


    private static double[] calculate(double focusDistance, double photoCensorHeight, double photoCensorWidth, double height) {
        double photoCensorDiameter = Math.sqrt(photoCensorHeight * photoCensorHeight + photoCensorWidth * photoCensorWidth);
        double groundDiameter = photoCensorDiameter * height / focusDistance;
        double widthHeightRatio = photoCensorWidth / photoCensorHeight;
        double groundHeight = Math.sqrt(groundDiameter * groundDiameter / (widthHeightRatio * widthHeightRatio + 1));
        double groundWidth = Math.sqrt(groundDiameter * groundDiameter / (1 / (widthHeightRatio * widthHeightRatio) + 1));
        return new double[] {groundHeight, groundWidth};
    }

    private static double[][] calculateRoute(double photoHeight, double photoWidth, double fieldHeight, double fieldWidth) {
        RouteCalculator routeCalculator = new RouteCalculator(photoHeight, photoWidth, fieldHeight, fieldWidth);
        int h = routeCalculator.getHeight();
        int w = routeCalculator.getWidth();

        boolean invert = false;

        if (h % 2 == 0 || w % 2 == 0) {
            if (h % 2 == 0) {
                routeCalculator = new RouteCalculator(photoWidth, photoHeight, fieldWidth, fieldHeight);
                h = routeCalculator.getHeight();
                w = routeCalculator.getWidth();
                invert = true;
            }

            for (int i = 0; i < w - 1; i++) routeCalculator.moveRight();
            if (h == 1) {
                routeCalculator.end();
            } else {
                routeCalculator.moveUp();
                for (int i = 0; i < w / 2; i++) {
                    if (i != 0) routeCalculator.moveLeft();
                    for (int j = 0; j < h - 2; j++) {
                        routeCalculator.moveUp();
                    }
                    routeCalculator.moveLeft();
                    for (int j = 0; j < h - 2; j++) {
                        routeCalculator.moveDown();
                    }
                }
                routeCalculator.end();
            }
        } else {
            for (int i = 0; i < w - 1; i++) {
                routeCalculator.moveRight();
            }
            if (h == 1) {
                routeCalculator.end();
            } else {
                routeCalculator.moveUp();
                for (int i = 0; i < (w - 3) / 2; i++) {
                    if (i != 0) routeCalculator.moveLeft();
                    for (int j = 0; j < h - 2; j++) {
                        routeCalculator.moveUp();
                    }
                    routeCalculator.moveLeft();
                    for (int j = 0; j < h - 2; j++) {
                        routeCalculator.moveDown();
                    }
                }
                routeCalculator.moveLeft();
                for (int j = 0; j < h - 2; j++) {
                    routeCalculator.moveUp();
                }
                routeCalculator.moveLeft();
                for (int i = 0; i < (h - 1) / 2; i++) {
                    if (i != 0) routeCalculator.moveDown();
                    routeCalculator.moveLeft();
                    routeCalculator.moveDown();
                    routeCalculator.moveRight();
                }
                routeCalculator.end();
            }
        }

        return invert ? invertCoords(routeCalculator.getRoute()) : routeCalculator.getRoute();
    }

    private static double[][] invertCoords(double[][] route) {
        double[][] result = new double[route.length][];
        for (int i = 0; i < route.length; i++) {
            result[i] = new double[] {
                    route[i][1],
                    route[i][0]
            };
        }
        return result;
    }

    private static void printRoute(double[][] route) {
        for (double[] coords: route) {
            System.out.println(coords[0] + " " + coords[1]);
        }
    }

    private static class RouteCalculator {
        private double photoHeight, photoWidth, fieldHeight, fieldWidth;
        private ArrayList<double[]> route = new ArrayList<>();
        private double x, y;

        public RouteCalculator(double photoHeight, double photoWidth, double fieldHeight, double fieldWidth) {
            this.photoHeight = photoHeight;
            this.photoWidth = photoWidth;
            this.fieldHeight = fieldHeight;
            this.fieldWidth = fieldWidth;
            x = y = 0;
            checkBounds();
            saveCurrentCoords();
        }

        private void saveCurrentCoords() {
            route.add(new double[] {x, y});
        }

        private void checkBounds() {
            double[][] bounds = getBounds();
            y = Math.max(
                    Math.min(y, bounds[1][0]),
                    bounds[0][0]
            );
            x = Math.max(
                    Math.min(x, bounds[1][1]),
                    bounds[0][1]
            );
        }

        private double[][] getBounds() {
            return new double[][] {
                    new double[] {
                            photoHeight / 2, photoWidth / 2
                    },
                    new double[] {
                            fieldHeight - photoHeight / 2, fieldWidth - photoWidth / 2
                    },
            };
        }

        public void moveRight() {
            x += photoWidth;
            checkBounds();
            saveCurrentCoords();
        }

        public void moveLeft() {
            x -= photoWidth;
            checkBounds();
            saveCurrentCoords();
        }

        public void moveUp() {
            y += photoHeight;
            checkBounds();
            saveCurrentCoords();
        }

        public void moveDown() {
            y -= photoHeight;
            checkBounds();
            saveCurrentCoords();
        }

        public double[][] getRoute() {
            double[][] result = new double[route.size()][];
            for (int i = 0; i < route.size(); i++) result[i] = route.get(i);
            return result;
        }

        public double getPhotoHeight() {
            return photoHeight;
        }

        public double getPhotoWidth() {
            return photoWidth;
        }

        public double getFieldHeight() {
            return fieldHeight;
        }

        public double getFieldWidth() {
            return fieldWidth;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public int getWidth() {
            return (int) Math.ceil(fieldWidth / photoWidth);
        }

        public int getHeight() {
            return (int) Math.ceil(fieldHeight / photoHeight);
        }

        public void end() {
            x = 0;
            y = 0;
            checkBounds();
            saveCurrentCoords();
        }
    }

    private void invokeMapWindow(
            double[][] route, double height, double width
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("mapwindow.fxml")
            );
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load(), 800, 600);
            stage.setScene(scene);
            MapWindowController controller = loader.getController();

            stage.show();

            controller.getMapView().init(
                    new MapView.LatLng(50.475175, 31.222136),
                    new MapView.LatLngBounds(
                            new MapView.LatLng(50.466977, 31.211438),
                            new MapView.LatLng(50.476175, 31.219136)
                    ),
                    17
            );

            controller.getMapView().onload(() -> {
                controller.getMapView().setLineWidth(5);
                controller.getMapView().strokeRect(
                        new MapView.Coords(0, 0),
                        new MapView.Bounds(1, 1)
                );


                for (int i = 0; i < route.length; i++) {
                    double[] coords = route[i];
                    double[] unitCoords = new double[] {
                            coords[0] / width, 1 - coords[1] / height
                    };
                    MapView.Coords realCoords = new MapView.Coords(
                            unitCoords[0],
                            unitCoords[1]
                    );
                    if (i == 0) {
                        controller.getMapView().beginPath();
                        controller.getMapView().moveTo(realCoords);
                        controller.getMapView().arcPathPixel(
                                controller.getMapView().unitCoordsToPixels(realCoords),
                                10, 0,2 * Math.PI);
                        controller.getMapView().closePath();
                        controller.getMapView().fillPath();

                        controller.getMapView().setFillPattern(
                                new MapView.ColoredPattern(
                                        new Color(
                                                255, 0, 0
                                        )
                                )
                        );

                        controller.getMapView().beginPath();
                        controller.getMapView().moveTo(realCoords);
                    } else {
                        controller.getMapView().lineTo(realCoords);
                        controller.getMapView().strokePath();

                        if (i != route.length - 1) {
                            controller.getMapView().beginPath();
                            controller.getMapView().moveTo(realCoords);
                            controller.getMapView().arcPathPixel(
                                    controller.getMapView().unitCoordsToPixels(realCoords),
                                    10, 0, 2 * Math.PI);
                            controller.getMapView().closePath();
                            controller.getMapView().fillPath();
                        }

                        controller.getMapView().beginPath();
                        controller.getMapView().moveTo(realCoords);
                    }

                }
                controller.getMapView().closePath();
                controller.getMapView().strokePath();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
