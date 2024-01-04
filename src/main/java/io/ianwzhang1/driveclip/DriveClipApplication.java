package io.ianwzhang1.driveclip;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DriveClipApplication extends Application {

    public static final String CLIP_NAME = "android_clip";
    public static DriveClipApplication instance;

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        FXMLLoader loader = new FXMLLoader(this.getClass().getClassLoader().getResource("ClipActions.fxml"));
        primaryStage.setTitle("DriveClip");
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.show();
    }

    public static void main(String... args) {
        launch(args);
    }

    public static DriveClipApplication getInstance() {
        return instance;
    }
}