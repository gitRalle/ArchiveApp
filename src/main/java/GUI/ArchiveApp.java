package GUI;

import IO.IO;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ArchiveApp extends Application {

    private boolean m_stageShowing = false;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception {
        if (IO.readInitFile() == null)
            startArchiveInit(stage);
        else
            startArchiveApp(new Stage());

    }

    private void startArchiveInit(@NotNull Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("archiveappinit.fxml"));

        Scene scene = new Scene(root);

        stage.setTitle("Archive App Init");

        stage.setScene(scene);

        ObservableList<Node> list = root.getChildrenUnmodifiable();

        Button selectButton = (Button) list.get(1);
        selectButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File("Files"));

            File dir = directoryChooser.showDialog(stage);

            if (dir != null && dir.exists() && dir.canExecute() && !dir.isHidden()) {
                IO.writeInitFile(dir);
                System.out.println(dir.getAbsolutePath());
                stage.close();
                try { Thread.sleep(1000); start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        Button exitButton = (Button) list.get(2);
        exitButton.setOnAction(e -> stage.close());

        stage.setMinWidth(500);
        stage.setMinHeight(305);
        stage.setMaxWidth(500);
        stage.setMaxHeight(305);

        stage.show();
    }

    private void startArchiveApp(@NotNull Stage stage) throws Exception {
        /* load fxml file (GUI) and return reference to the scene graph's root node
         * also instantiates the controller class specified in the fxml file */
        Parent root = FXMLLoader.load(getClass().getResource("archiveapp.fxml"));

        // attach scene graph to scene
        Scene scene = new Scene(root);

        stage.setTitle("Archive App");

        // attach scene to stage
        stage.setScene(scene);

        SplitPane splitPane = (SplitPane) root.getChildrenUnmodifiable().get(0);
        ChangeListener<Number> changeListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                splitPane.setDividerPositions(0.2);
                if (m_stageShowing) {
                    observable.removeListener(this);
                }
            }
        };
        splitPane.widthProperty().addListener(changeListener);
        splitPane.heightProperty().addListener(changeListener);

        stage.setMaximized(true);

        // display the stage
        stage.show();
        m_stageShowing = true;

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

}
