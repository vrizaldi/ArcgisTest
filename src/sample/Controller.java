package sample;

import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import javax.swing.Timer;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML private SceneView view;
    @FXML private TextField altitude_field;
    @FXML private TextField longitude_field;
    @FXML private TextField latitude_field;
    @FXML private TextField heading_field;
    @FXML private TextField pitch_field;
    @FXML private TextField roll_field;
    @FXML private Button interactive_toggler;

    private static final double DRONE_VELOCITY = 0.00001;

    private Graphic drone;
    private boolean isInteractive;
    private OrbitGeoElementCameraController cameraController;
    private Timer interactiveUpdateTimer;

    @Override
    public void initialize(URL location, ResourceBundle resource) {
       initScene();
       initCam();
    }

    private void initScene() {
        // initialise arcgis scene
        ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
        this.view.setArcGISScene(scene);
        scene.getBaseSurface().getElevationSources().add(new ArcGISTiledElevationSource(
                "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"));
    }

    private void initDrone() {
        // load drone model from resource
        String modelURI = new File(
                "resource\\drone.3ds")
                .getAbsolutePath();
        ModelSceneSymbol droneSymbol = new ModelSceneSymbol(modelURI, 30);
        droneSymbol.loadAsync();
        droneSymbol.setHeading(180);

        // initialise drone as graphics
        this.drone = new Graphic(getCurPos(), droneSymbol);


        // create renderer for drone
        // set which attributes used for heading, pitch and roll
        SimpleRenderer droneRenderer = new SimpleRenderer();
        droneRenderer.getSceneProperties().setHeadingExpression("[HEADING]");
        droneRenderer.getSceneProperties().setPitchExpression("[PITCH]");
        droneRenderer.getSceneProperties().setRollExpression("[ROLL]");

        // create graphic overlay for drone
        GraphicsOverlay droneOverlay = new GraphicsOverlay();
        droneOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
        droneOverlay.getGraphics().add(drone);              // add drone to graphic overlay
        droneOverlay.setRenderer(droneRenderer);

        updateDroneAtt();

        this.view.getGraphicsOverlays().add(droneOverlay);      // add graphic overlay to scene

        // set initial interactive mode accordingly
        this.isInteractive = this.interactive_toggler.getText().split(" ")[2].compareTo("ON") == 0 ? true : false;
    }

    private void initCam() {
        initDrone();
        // set camera controller to orbit around drone
        this.cameraController =
                new OrbitGeoElementCameraController(drone, 2);
        this.cameraController.setCameraDistanceInteractive(false);
        this.cameraController.setAutoHeadingEnabled(false);
        this.cameraController.setAutoPitchEnabled(false);
        this.cameraController.setAutoRollEnabled(false);
        this.view.setCameraController(this.cameraController);
    }

    public void handleChange() {
        // handle change in the drone's current state
        this.drone.setGeometry(getCurPos());
        updateDroneAtt();
    }

    private Point getCurPos() {
        // return a Point as a representation of the drone's current position
        double longitude = Double.valueOf(this.longitude_field.getText());
        double latitude = Double.valueOf(this.latitude_field.getText());
        double altitude = Double.valueOf(this.altitude_field.getText());
        return new Point(longitude, latitude, altitude,SpatialReferences.getWgs84());
    }

    private void updateCurState(Point point, double heading, double pitch, double roll) {
        this.latitude_field.setText(String.valueOf(point.getY()));
        this.longitude_field.setText(String.valueOf(point.getX()));
        this.altitude_field.setText(String.valueOf(point.getZ()));
        this.heading_field.setText(String.valueOf(heading));
        this.pitch_field.setText(String.valueOf(pitch));
        this.roll_field.setText(String.valueOf(roll));
    }

    private void updateDroneAtt() {
        // update the drone's heading, pitch and roll
        double heading = this.getHeading();
        double pitch = this.getPitch();
        double roll = this.getRoll();
        this.drone.getAttributes().put("HEADING", heading);
        this.drone.getAttributes().put("PITCH", pitch);
        this.drone.getAttributes().put("ROLL", roll);
    }

    private double getHeading() {
        return Double.valueOf(this.heading_field.getText());
    }

    private double getPitch() {
        return Double.valueOf(this.pitch_field.getText());
    }

    private double getRoll() {
        return Double.valueOf(this.roll_field.getText());
    }


    public void dispose() {
       this.view.dispose();
    }

    public void toggleInteractive() {
        this.isInteractive = !this.isInteractive;

        // toggle text on the button
        this.interactive_toggler.setText("Interactive mode: " + (this.isInteractive ? "ON" : "OFF"));

        if(isInteractive) {
            // if interactive mode is turned on
            // set cam to follow heading, pitch, and roll of the drone
            this.cameraController.setCameraHeadingOffsetInteractive(false);
            this.cameraController.setCameraPitchOffsetInteractive(false);
            this.cameraController.setCameraPitchOffset(90);
            this.cameraController.setAutoHeadingEnabled(true);
            this.cameraController.setAutoPitchEnabled(true);
            this.cameraController.setAutoRollEnabled(true);

            // set timer to update drone state 1/30 second
            this.interactiveUpdateTimer = new Timer(1000/30,
                    (e) -> {
                        Point curPos = getCurPos();
                        Point newPos = new Point(
                                curPos.getX() + calcXVelocity(this.getHeading(), this.getPitch()),
                                curPos.getY() + calcYVelocity(this.getHeading(), this.getPitch()),
                                curPos.getZ() + calcZVelocity(this.getPitch()),
                                SpatialReferences.getWgs84());
                        drone.setGeometry(newPos);
                        updateCurState(newPos, this.getHeading(), this.getPitch(), this.getRoll());
                    });
            this.interactiveUpdateTimer.setRepeats(true);
            this.interactiveUpdateTimer.start();

        } else {
            // if interactive mode is turned on
            // stop moving plane / update
            if(this.interactiveUpdateTimer != null) this.interactiveUpdateTimer.stop();

            // allow camera to move around
            this.cameraController.setCameraHeadingOffsetInteractive(true);
            this.cameraController.setCameraPitchOffsetInteractive(true);
            this.cameraController.setAutoHeadingEnabled(false);
            this.cameraController.setAutoPitchEnabled(false);
            this.cameraController.setAutoRollEnabled(false);
        }
    }

    private static double calcXVelocity(double heading, double pitch) {
        double headingRad = Math.toRadians(heading);
        double pitchRad = Math.toRadians(pitch);
        System.out.println("X velocity: " + DRONE_VELOCITY + Math.sin(headingRad));
        return DRONE_VELOCITY * Math.cos(pitchRad) * Math.sin(headingRad);
    }

    private static double calcYVelocity(double heading, double pitch) {
        double headingRad = Math.toRadians(heading);
        double pitchRad = Math.toRadians(pitch);
        System.out.println("Y velocity: " + DRONE_VELOCITY + Math.cos(headingRad));
        return DRONE_VELOCITY * Math.cos(pitchRad) * Math.cos(headingRad);
    }

    private static double calcZVelocity(double pitch) {
        double pitchRad = Math.toRadians(pitch);
        return DRONE_VELOCITY * Math.sin(pitchRad);
    }
}
