package sample;

import javafx.concurrent.Worker;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapView extends Region {
    private WebView webView;
    private LatLng center;
    private double zoom;
    private LatLngBounds bounds;
    private boolean initialized;
    private boolean loaded = false;
    private Pattern strokePattern = null;
    private Pattern fillPattern = null;
    private long lineWidth = 1;

    public Pattern getStrokePattern() {
        return strokePattern;
    }

    public void setStrokePattern(Pattern strokePattern) {
        checkLoaded();
        this.strokePattern = strokePattern;
        getWebEngine().executeScript(
                String.format(Locale.US, 
                        "%s = %s",
                        "window.MapOverlay.CanvasOverlay.getContext().strokeStyle",
                        strokePattern.toJS()
                )
        );
    }

    public Pattern getFillPattern() {
        return fillPattern;
    }

    public void setFillPattern(Pattern fillPattern) {
        checkLoaded();
        this.fillPattern = fillPattern;
        getWebEngine().executeScript(
                String.format(Locale.US, 
                        "%s = %s",
                        "window.MapOverlay.CanvasOverlay.getContext().fillStyle",
                        fillPattern.toJS()
                )
        );
    }

    public long getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(long lineWidth) {
        checkLoaded();
        this.lineWidth = lineWidth;
        getWebEngine().executeScript(buildAssignment(
                "window.MapOverlay.CanvasOverlay.getContext().lineWidth",
                lineWidth
        ));
    }

    public static final boolean dimaPidor = true;

    private List<Runnable> onLoad = new ArrayList<>();
    { onLoad.add(() -> loaded = true); }

    private void checkLoaded() {
        if (!loaded) throw new RuntimeException("The page has not loaded yet! Please await by using instance.onload(() -> {...})");
    }

    private void checkInit() {
        if (!initialized) throw new RuntimeException("This instance is not initialized yet! Please initialize it by using instance.init(...)");
    }

    private static String colorToHex(Color color) {
        return String.format(Locale.US, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public void onload(Runnable callback) {
        onLoad.add(callback);
    }

    public void init(LatLng center, LatLngBounds bounds, double zoom) {
        if (!checkInternetConnection()) throw new RuntimeException("Internet connection required");

        initialized = true;
        this.center = center;
        this.zoom = zoom;
        this.bounds = bounds;

        webView = new WebView();
        getWebEngine().setOnAlert(event -> showAlert(event.getData()));
        getWebEngine().load(getClass().getResource(
                "/sample/mapviewlayout.html" // TODO change resource name!
        ).toString());
        getWebEngine().executeScript(String.format(Locale.US, 
                "window.COORDS = {\n" +
                        "            center: {\n" +
                        "                lat: %f, lng: %f\n" +
                        "            },\n" +
                        "            bound1: {\n" +
                        "                lat: %f, lng: %f\n" +
                        "            },\n" +
                        "            bound2: {\n" +
                        "                lat: %f, lng: %f\n" +
                        "            },\n" +
                        "            ZOOM: %f\n" +
                        "        };",
                center.getLatitude(), center.getLongitude(),
                bounds.getFirstBound().getLatitude(), bounds.getFirstBound().getLongitude(),
                bounds.getSecondBound().getLatitude(), bounds.getSecondBound().getLongitude(),
                zoom
        ));
        getChildren().add(webView);
        getWebEngine().getLoadWorker().stateProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != Worker.State.SUCCEEDED) {
                        return;
                    }

                    onLoad.forEach(Runnable::run);
                    onLoad = null;
                }
        );
    }

    public double getCanvasWidth() {
        return ((Number) getWebEngine().executeScript("window.MapOverlay.CanvasOverlay.getWidth()")).doubleValue();
    }

    public double getCanvasHeight() {
        return ((Number) getWebEngine().executeScript("window.MapOverlay.CanvasOverlay.getHeight()")).doubleValue();
    }

    public long verticalUnitsToPixels(double units) {
        return Math.round(units * getCanvasHeight());
    }

    public long horizontalUnitsToPixels(double units) {
        return Math.round(units * getCanvasWidth());
    }

    public Bounds unitBoundsToPixels(Bounds units) {
        return new Bounds(
                verticalUnitsToPixels(units.height),
                horizontalUnitsToPixels(units.width)
        );
    }

    private static boolean checkInternetConnection() {
        try {
            Process process = Runtime.getRuntime().exec("ping www.google.com");
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Coords unitCoordsToPixels(Coords units) {
        return new Coords(
                horizontalUnitsToPixels(units.x),
                verticalUnitsToPixels(units.y)
        );
    }

    private void update() {
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.draw"
        ));
    }

    private static void showAlert(String message) {
        Dialog<Void> alert = new Dialog<>();
        alert.getDialogPane().setContentText(message);
        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.showAndWait();
    }

    private static String buildFunctionCall(String function, Object... params) {
        StringBuilder format = new StringBuilder(function + "(");
        for (int i = 0; i < params.length; i++) {
            format.append(params[i] != null ? patternFromClass(params[i].getClass()) : "%s");
            if (i != params.length - 1) format.append(",");
        }
        format.append(")");
        return String.format(Locale.US, format.toString(), params);
    }

    private static String buildAssignment(String lvalue, Object value) {
        return String.format(Locale.US, 
                "%s = " + (value != null ? patternFromClass(value.getClass()) : "%s"),
                lvalue,
                value
        );
    }

    private static String patternFromClass(Class clazz) {
        if (clazz == String.class) {
            return "\"%s\"";
        } else if (clazz == Long.class || clazz == Integer.class) {
            return "%d";
        } else if (Number.class.isAssignableFrom(clazz)) {
            return "%f";
        } else if (clazz == Boolean.class) {
            return "%b";
        } else throw new RuntimeException("Invalid argument class: " + clazz);
    }

    public void fillRectPixel(Coords coords, Bounds bounds) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().fillRect",
                coords.getX(), coords.getY(),
                bounds.getWidth(), bounds.getHeight()
        ));
        update();
    }

    public void fillRect(Coords coords, Bounds bounds) {
        fillRectPixel(unitCoordsToPixels(coords), unitBoundsToPixels(bounds));
    }

    public void clearRectPixel(Coords coords, Bounds bounds) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().clearRect",
                coords.getX(), coords.getY(),
                bounds.getWidth(), bounds.getHeight()
        ));
        update();
    }

    public void clearRect(Coords coords, Bounds bounds) {
        clearRectPixel(unitCoordsToPixels(coords), unitBoundsToPixels(bounds));
    }

    public void strokeRectPixel(Coords coords, Bounds bounds) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().strokeRect",
                coords.getX(), coords.getY(),
                bounds.getWidth(), bounds.getHeight()
        ));
        update();
    }

    public void strokeTextPixel(CharSequence text, Coords coords, long maxWidth) {
        checkLoaded();
        Object[] args = maxWidth < 0 ? new Object[] {text.toString(), coords.x, coords.y} : new Object[] {text.toString(), coords.x, coords.y, maxWidth};
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().strokeText", args
        ));
    }

    public void strokeTextPixel(CharSequence text, Coords coords) {
        strokeTextPixel(text, coords, -1);
    }

    public void strokeText(CharSequence text, Coords coords, double maxWidth) {
        strokeTextPixel(text, unitCoordsToPixels(coords), horizontalUnitsToPixels(maxWidth));
    }

    public void strokeText(CharSequence text, Coords coords) {
        strokeTextPixel(text, unitCoordsToPixels(coords), -1);
    }

    public void fillTextPixel(CharSequence text, Coords coords, long maxWidth) {
        checkLoaded();
        Object[] args = maxWidth < 0 ? new Object[] {text.toString(), coords.x, coords.y} : new Object[] {text.toString(), coords.x, coords.y, maxWidth};
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().fillText", args
        ));
    }

    public void fillTextPixel(CharSequence text, Coords coords) {
        fillTextPixel(text, coords, -1);
    }

    public void fillText(CharSequence text, Coords coords, double maxWidth) {
        fillTextPixel(text, unitCoordsToPixels(coords), horizontalUnitsToPixels(maxWidth));
    }

    public void fillText(CharSequence text, Coords coords) {
        fillTextPixel(text, unitCoordsToPixels(coords), -1);
    }

    public void strokeRect(Coords coords, Bounds bounds) {
        strokeRectPixel(unitCoordsToPixels(coords), unitBoundsToPixels(bounds));
    }

    public void beginPath() {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().beginPath"
        ));
    }

    public void closePath() {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().closePath"
        ));
    }

    public void fillPath() {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().fill"
        ));
        update();
    }

    public void strokePath() {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().stroke"
        ));
        update();
    }

    public void moveToPixel(Coords coords) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().moveTo",
                coords.getX(), coords.getY()
        ));
    }

    public void lineToPixel(Coords coords) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().lineTo",
                coords.getX(), coords.getY()
        ));
    }

    public void moveTo(Coords coords) {
        moveToPixel(unitCoordsToPixels(coords));
    }

    public void lineTo(Coords coords) {
        lineToPixel(unitCoordsToPixels(coords));
        moveTo(coords);
    }

    public void arcPathPixel(Coords coords, long r, double startAngle, double endAngle, boolean counterClockwise) {
        checkLoaded();
        getWebEngine().executeScript(buildFunctionCall(
                "window.MapOverlay.CanvasOverlay.getContext().arc",
                coords.x, coords.y,
                r,
                startAngle, endAngle,
                counterClockwise
        ));
    }

    public void arcPathPixel(Coords coords, long r, double startAngle, double endAngle) {
        arcPathPixel(coords, r, startAngle, endAngle, false);
    }

    public void arcPath(Coords coords, double r, double startAngle, double endAngle, boolean counterClockwise, boolean heightRelated) {
        arcPathPixel(
                unitCoordsToPixels(coords),
                heightRelated ? verticalUnitsToPixels(r) : horizontalUnitsToPixels(r),
                startAngle, endAngle,
                counterClockwise
        );
    }

    public void arcPath(Coords coords, double r, double startAngle, double endAngle, boolean heightRelated) {
        arcPathPixel(
                unitCoordsToPixels(coords),
                heightRelated ? verticalUnitsToPixels(r) : horizontalUnitsToPixels(r),
                startAngle, endAngle,
                false
        );
    }


    public WebEngine getWebEngine() {
        checkInit();
        return webView.getEngine();
    }

    public static class LatLng {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static class LatLngBounds {
        private LatLng firstBound;
        private LatLng secondBound;

        public LatLng getFirstBound() {
            return firstBound;
        }

        public void setFirstBound(LatLng firstBound) {
            this.firstBound = firstBound;
        }

        public LatLng getSecondBound() {
            return secondBound;
        }

        public void setSecondBound(LatLng secondBound) {
            this.secondBound = secondBound;
        }

        public LatLngBounds(LatLng firstBound, LatLng secondBound) {
            this.firstBound = firstBound;
            this.secondBound = secondBound;
        }
    }

    public static class Coords {
        private double x;
        private double y;

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public Coords(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Coords{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    public static interface Pattern {
        String toJS();
    }

    public static class ColoredPattern implements Pattern {
        private Color color;

        public ColoredPattern(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public String toJS() {
            return "\"" + colorToHex(color) + "\"";
        }
    }

    public static class Bounds {
        private double height;
        private double width;

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }

        public Bounds(double height, double width) {
            this.height = height;
            this.width = width;
        }

        @Override
        public String toString() {
            return "Bounds{" +
                    "height=" + height +
                    ", width=" + width +
                    '}';
        }
    }
}
