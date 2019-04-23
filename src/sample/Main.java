package sample;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.stage.Stage;


public class Main extends Application {
    @FXML
    private MapView view;

    private Controller controller;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader =  new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 350));
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        controller.dispose();
    }

    public static void main(String[] args) {
        ArcGISRuntimeEnvironment.setInstallDirectory("D:\\Docs\\1.Ilkomp UGM\\Organisasi dan Kepanitiaan\\Gamaforce\\ArcgisTest\\lib\\arcgis");

        launch(args);
    }
}
