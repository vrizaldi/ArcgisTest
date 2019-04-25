package sample;

import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

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
    private static final double Z_VELOCITY_MULT = 100000;
    private static final double PITCH_CHANGE = 1;
    private static final double ROLL_CHANGE = 4;
    private static final double CAM_DISTANCE = 2;
    private static final float CAM_MOVEMENT_DUR = 1;

    private Scene fxscene;

    private Graphic drone;

    private double longitude;
    private double latitude;
    private double altitude;
    private double heading;
    private double pitch;
    private double roll;

    private OrbitGeoElementCameraController cameraController;
    private boolean isInteractive;
    private Timer interactiveUpdateTimer;
    private Timer camNormaliserTimer;

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
        // initialise drone initial state
        this.longitude = -7.797068;
        this.latitude = 110.370529;
        this.altitude = 200;
        this.heading = this.pitch = this.roll = 0;

        // load drone model from resource
        String modelURI = new File(
                "resource\\drone.3ds")
                .getAbsolutePath();
        ModelSceneSymbol droneSymbol = new ModelSceneSymbol(modelURI, 30);
        droneSymbol.loadAsync();
        droneSymbol.setHeading(180);    // model heading is inverted

        // initialise drone as graphics
        this.drone = new Graphic(new Point(0,0,0), droneSymbol);

        // create renderer for drone
        // set which attributes used for heading, pitch and roll
        SimpleRenderer droneRenderer = new SimpleRenderer();
        droneRenderer.getSceneProperties().setHeadingExpression("[HEADING]");
        droneRenderer.getSceneProperties().setPitchExpression("[PITCH]");
        droneRenderer.getSceneProperties().setRollExpression("[ROLL]");

        // create graphic overlay for drone
        GraphicsOverlay droneOverlay = new GraphicsOverlay();
        droneOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
        droneOverlay.getGraphics().add(this.drone);              // add drone to graphic overlay
        droneOverlay.setRenderer(droneRenderer);

        this.updateDisplayFromState();   // update the display according to data
        this.updateModelFromState();

        this.view.getGraphicsOverlays().add(droneOverlay);      // add graphic overlay to scene

        // set initial interactive mode accordingly
        this.isInteractive = this.interactive_toggler.getText().split(" ")[2].compareTo("ON") == 0 ? true : false;
    }

    private void initCam() {
        initDrone();
        // set camera controller to orbit around drone
        this.cameraController =
                new OrbitGeoElementCameraController(drone, CAM_DISTANCE);
        this.view.setCameraController(this.cameraController);

        this.camNormaliserTimer = new Timer(1000/30,
                (e) -> {
                    double curHeading = this.cameraController.getCameraHeadingOffset();
                    double curPitch = this.cameraController.getCameraPitchOffset();

                    Platform.runLater(() -> {
                        if(curHeading - this.heading > 5) {
                            this.cameraController.setCameraHeadingOffset(curHeading - 0.4);
                        } else if(curHeading - this.heading < -5) {
                            this.cameraController.setCameraHeadingOffset(curHeading + 0.4);
                        }

                        if(curPitch - 90 - this.pitch > 5) {
                            this.cameraController.setCameraPitchOffset(curPitch - 0.5);
                        } else if(curPitch - 90 - this.pitch < -5) {
                             this.cameraController.setCameraPitchOffset(curPitch + 0.5);
                        }
                    });
                }
            );

        // set auto heading, pitch, and roll
        this.cameraController.setAutoHeadingEnabled(false);
        this.cameraController.setAutoPitchEnabled(false);
        this.cameraController.setAutoRollEnabled(false);
        setInteractivity(false);
    }

    public void handleModified() {
        // handle change in the drone's current state
        this.updateStateFromDisplay();
        this.updateModelFromState();
    }

    private void updateStateFromDisplay() {
        // update the drone's state according to the displayed data
        this.longitude = this.getDisplayLongitude();
        this.latitude = this.getDisplayLatitude();
        this.altitude = this.getDisplayAltitude();
        this.heading = this.getDisplayHeading();
        this.pitch = this.getDisplayPitch();
        this.roll = this.getDisplayRoll();
    }

    private void updateDisplayFromState() {
        // update the data displayed according to the state
        this.latitude_field.setText(String.valueOf((float)this.latitude));
        this.longitude_field.setText(String.valueOf((float)this.longitude));
        this.altitude_field.setText(String.valueOf((float)this.altitude));
        this.heading_field.setText(String.valueOf((float)this.heading));
        this.pitch_field.setText(String.valueOf((float)this.pitch));
        this.roll_field.setText(String.valueOf((float)this.roll));
    }

    private void updateModelFromState() {
        // update the drone model according to the state
        this.drone.getAttributes().put("HEADING", this.heading);
        this.drone.getAttributes().put("PITCH", this.pitch);
        this.drone.getAttributes().put("ROLL", this.roll);
        Point newPos = new Point(this.latitude, this.longitude, this.altitude, SpatialReferences.getWgs84());
        System.out.println(newPos.toString() + "\nheading: " + heading + "\npitch: " + pitch + "\nroll: " + roll);
        this.drone.setGeometry(newPos);
    }

    private double getDisplayLongitude() {
        return Double.valueOf(this.longitude_field.getText());
    }

    private double getDisplayLatitude() {
        return Double.valueOf(this.latitude_field.getText());
    }

    private double getDisplayAltitude() {
        return Double.valueOf(this.altitude_field.getText());
    }

    private double getDisplayHeading() {
        return Double.valueOf(this.heading_field.getText());
    }

    private double getDisplayPitch() {
        return Double.valueOf(this.pitch_field.getText());
    }

    private double getDisplayRoll() {
        return Double.valueOf(this.roll_field.getText());
    }


    public void toggleInteractive() {
        this.isInteractive = !this.isInteractive;

        // toggle text on the button
        this.interactive_toggler.setText("Interactive mode: " + (this.isInteractive ? "ON" : "OFF"));

        if(isInteractive) {
            // if interactive mode is turned on
            // set timer to update drone state 1/30 second
            setInteractivity(true);
            this.interactiveUpdateTimer = new Timer(1000/30,
                    (e) -> {
                        // UI changes must be done on main thread
                        Platform.runLater(()->{
                            this.latitude += calcXVelocity(this.heading, this.pitch);
                            this.longitude += calcYVelocity(this.heading, this.pitch);
                            this.altitude += calcZVelocity(this.pitch);
                            this.updateDisplayFromState();
                            this.updateModelFromState();
                        });
                    });
            this.interactiveUpdateTimer.setRepeats(true);
            this.interactiveUpdateTimer.start();

        } else {
            // if interactive mode is turned on
            // stop moving plane / update
            if(this.interactiveUpdateTimer != null) this.interactiveUpdateTimer.stop();

            setInteractivity(false);
        }
    }

    private void setInteractivity(boolean isInteractive) {
        // toggle control for interactive / non interactive mode
        this.cameraController.setCameraHeadingOffsetInteractive(!isInteractive);
        this.cameraController.setCameraPitchOffsetInteractive(!isInteractive);
        if(isInteractive) {
            this.camNormaliserTimer.start();
        } else {
            this.camNormaliserTimer.stop();
        }

        enableDroneControl(isInteractive);
    }

    private void enableDroneControl(boolean isEnabled) {
        if(isEnabled) {
            fxscene.addEventFilter(KeyEvent.KEY_PRESSED,
                (e) -> {
                    double pitchChange = calcPitchChange(roll);
                    double headingChange = calcHeadingChange(roll);
                    switch(e.getCode()) {
                    case W:
                        this.pitch -= pitchChange;
                        this.heading -= headingChange;
                        break;
                    case A:
                        this.roll -= ROLL_CHANGE;
                        break;
                    case S:
                        this.pitch += pitchChange;
                        this.heading += headingChange;
                        break;
                    case D:
                        this.roll += ROLL_CHANGE;
                        break;
                    }

                    this.updateModelFromState();
                }
            );
        } else {
        }
    }

    private static double calcXVelocity(double heading, double pitch) {
        double headingRad = Math.toRadians(heading);
        double pitchRad = Math.toRadians(pitch);
        return DRONE_VELOCITY * Math.cos(pitchRad) * Math.sin(headingRad);
    }

    private static double calcYVelocity(double heading, double pitch) {
        double headingRad = Math.toRadians(heading);
        double pitchRad = Math.toRadians(pitch);
        return DRONE_VELOCITY * Math.cos(pitchRad) * Math.cos(headingRad);
    }

    private static double calcZVelocity(double pitch) {
        double pitchRad = Math.toRadians(pitch);
        return Z_VELOCITY_MULT * DRONE_VELOCITY * Math.sin(pitchRad);
    }

    private static double calcPitchChange(double roll) {
        double rollRad = Math.toRadians(roll);
        return PITCH_CHANGE * Math.cos(rollRad);
    }

    private static double calcHeadingChange(double roll) {
        double rollRad = Math.toRadians(roll);
        return PITCH_CHANGE * Math.sin(rollRad);
    }


    /**
     * to be called by main
     */
    public void setFXScene(Scene fxscene) {
        this.fxscene = fxscene;
    }

    public void dispose() {
       this.view.dispose();
       if(this.interactiveUpdateTimer != null) this.interactiveUpdateTimer.stop();
       if(this.camNormaliserTimer != null) this.camNormaliserTimer.stop();
    }
}
