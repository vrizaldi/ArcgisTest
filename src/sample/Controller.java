package sample;

import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.ModelSceneSymbol;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML private SceneView view;
    @FXML private TextField altitude_field;
    @FXML private TextField longitude_field;
    @FXML private TextField latitude_field;

    private Graphic drone;

    @Override
    public void initialize(URL location, ResourceBundle resource) {
       initScene();
       initCam();
    }

    private void initScene() {
        // initialise arcgis scene
        ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
        view.setArcGISScene(scene);
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
        double longitude = Double.valueOf(longitude_field.getText());
        double latitude = Double.valueOf(latitude_field.getText());
        double altitude = Double.valueOf(altitude_field.getText());
        Point initialCoord = new Point(longitude, latitude, altitude, SpatialReferences.getWgs84());
        drone = new Graphic(initialCoord, droneSymbol);


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

        drone.getAttributes().put("HEADING", 0);
        drone.getAttributes().put("PITCH", 0);
        drone.getAttributes().put("ROLL", 0);

        view.getGraphicsOverlays().add(droneOverlay);      // add graphic overlay to scene
    }

    private void initCam() {
        initDrone();
        // set camera controller to orbit around drone
        OrbitGeoElementCameraController cameraController =
                new OrbitGeoElementCameraController(drone, 2);
        cameraController.setCameraDistanceInteractive(false);
        cameraController.setAutoHeadingEnabled(false);
        cameraController.setAutoPitchEnabled(false);
        cameraController.setAutoRollEnabled(false);
        view.setCameraController(cameraController);
    }

    public void dispose() {
        view.dispose();
    }

    public void handleCoorChange(ActionEvent actionEvent) {
        double longitude = Double.valueOf(longitude_field.getText());
        double latitude = Double.valueOf(latitude_field.getText());
        double altitude = Double.valueOf(altitude_field.getText());
        drone.setGeometry(new Point(longitude, latitude, altitude,SpatialReferences.getWgs84()));
    }
}
