package sample;

import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSceneSymbol;
import com.esri.arcgisruntime.geometry.Point;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private SceneView view;

    private Graphic drone;

    @Override
    public void initialize(URL location, ResourceBundle resource) {
       initScene();
       initCam();
    }

    private void initScene() {
        // initialise arcgis scene
        ArcGISScene scene = new ArcGISScene(Basemap.createImageryWithLabels());
        view.setArcGISScene(scene);
        scene.getBaseSurface().getElevationSources().add(new ArcGISTiledElevationSource(
                "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"));
    }

    private void initDrone() {
        // represent drone on map as a geometry
        SimpleMarkerSceneSymbol droneSymbol = new SimpleMarkerSceneSymbol(
                SimpleMarkerSceneSymbol.Style.CONE,
                0x55000000, 10, 20, 10,
                SceneSymbol.AnchorPosition.CENTER);
        Point initialCoord = new Point(-118.24368, 34.05293, 200, SpatialReferences.getWgs84());
        drone = new Graphic(initialCoord, droneSymbol);
        GraphicsOverlay objectOverlay = new GraphicsOverlay();
        objectOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.ABSOLUTE);
        objectOverlay.getGraphics().add(drone);
        view.getGraphicsOverlays().add(objectOverlay);
    }

    private void initCam() {
        initDrone();
        // set camera controller to orbit around drone
        OrbitGeoElementCameraController cameraController =
                new OrbitGeoElementCameraController(drone, 500);
        view.setCameraController(cameraController);
    }

    public void dispose() {
        view.dispose();
    }
}
