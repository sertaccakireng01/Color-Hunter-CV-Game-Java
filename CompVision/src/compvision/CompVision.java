package compvision;

import org.opencv.core.Core;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Initializes the UI and loads the OpenCV native library.
 */
public class CompVision extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Load FXML layout
            FXMLLoader fxmlLoader = new FXMLLoader(CompVision.class.getResource("FXMLDocument.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            // Access controller to handle safe camera shutdown
            FXMLDocumentController controller = fxmlLoader.getController();
            
            stage.setOnCloseRequest(event -> {
                System.out.println("Closing application...");
                controller.stopAcquisition(); // Release camera hardware resources
                Platform.exit();
                System.exit(0);
            });

            // Set window properties and display
            stage.setTitle("CompVision - Color Hunter");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Error: FXML file not found or failed to load.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Load OpenCV native library - Essential for OpenCV functions
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
}
