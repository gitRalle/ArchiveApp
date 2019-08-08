package controllers;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.textfield.CustomTextField;
import utils.FontUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class AdvancedSearchTreeController implements Initializable {

    @FXML
    private GridPane gridPane;

    @FXML
    private Button exitButton;

    private CheckTreeView<String> checkTreeView = new CheckTreeView<String>();

    private CustomTextField searchField = new CustomTextField();

    private Button searchButton = new Button();

    private final int STAGE_WIDTH = 616;

    private TreeView<String> primaryTreeView;
    private TabPane primaryTabPane;

    /**
     * initialize is responsible for setting property values on the stage itself
     * is executed before initController
     */
    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        gridPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {

                        Stage stage = (Stage) newWindow;

                        if (!stage.isShowing()) {
                            // first time stage is set
                            stage.setTitle("Advanced Search");
                            stage.initStyle(StageStyle.DECORATED);
                            stage.initModality(Modality.WINDOW_MODAL);
                            stage.setResizable(false);
                        } else {
                            // when/if table -> tree
                            stage.setWidth(STAGE_WIDTH);
                            stage.setResizable(false);
                        }

                    }

                });
            }
        });
    }

    /**
     * initController is responsible for setting the content of the scene
     * executes after initialize
     */
    void initController(final TreeView<String> treeView, final TabPane tabPane) throws Exception {
        this.primaryTreeView = treeView;
        this.primaryTabPane = tabPane;

        /*
        INIT checkTreeView
         */
        CheckBoxTreeItem<String> root = new CheckBoxTreeItem<String>(treeView.getRoot().getValue());

        for (TreeItem<String> domainTreeItem : treeView.getRoot().getChildren()) {
            CheckBoxTreeItem<String> domainCheckItem = new CheckBoxTreeItem<>(domainTreeItem.getValue());
            domainCheckItem.setExpanded(true);
            root.getChildren().add(domainCheckItem);

            for (TreeItem<String> yearTreeItem : domainTreeItem.getChildren()) {
                CheckBoxTreeItem<String> yearCheckItem = new CheckBoxTreeItem<>(yearTreeItem.getValue());
                yearCheckItem.setExpanded(true);
                domainCheckItem.getChildren().add(yearCheckItem);

                for (TreeItem<String> monthTreeItem : yearTreeItem.getChildren()) {
                    CheckBoxTreeItem<String> monthCheckItem = new CheckBoxTreeItem<>(monthTreeItem.getValue());
                    monthCheckItem.setExpanded(true);
                    yearCheckItem.getChildren().add(monthCheckItem);

                    for (TreeItem<String> dayTreeItem : monthTreeItem.getChildren()) {
                        CheckBoxTreeItem<String> dayCheckItem = new CheckBoxTreeItem<>(dayTreeItem.getValue());
                        dayCheckItem.setExpanded(true);
                        monthCheckItem.getChildren().add(dayCheckItem);
                    }
                }
            }
        }

        root.setExpanded(true);
        checkTreeView.setRoot(root);
        checkTreeView.setPadding(new Insets(4, 4, 4, 4));
        checkTreeView.setMinWidth(Double.NEGATIVE_INFINITY);
        checkTreeView.setMaxWidth(Double.NEGATIVE_INFINITY);
        checkTreeView.setPrefWidth(STAGE_WIDTH / 2);
        checkTreeView.getStyleClass().add("check-tree-view");
        gridPane.add(checkTreeView, 0, 1);
        gridPane.add(FontUtils.createView("check-tree-view-icon"), 1, 1);

        /*
        INIT searchButton, searchField and exitButton
         */
        searchButton.setMinWidth(Double.NEGATIVE_INFINITY);
        searchButton.setMaxWidth(Double.NEGATIVE_INFINITY);
        searchButton.setPrefWidth(20);
        searchButton.setDefaultButton(true);
        searchButton.setGraphic(FontUtils.createView("search-button-icon"));
        searchButton.setOnAction(e -> {
            if (checkTreeView.getCheckModel().getCheckedItems().size() != 0 &&
            searchField.getText().length() != 0) {
                loadTable();
            }
        });
        searchButton.getStyleClass().add("search-button");

        searchField.setRight(searchButton);
        searchField.getStyleClass().add("search-text-field");

        gridPane.add(searchField, 1, 2);

        exitButton.setGraphic(FontUtils.createView("exit-button-icon"));
        exitButton.getGraphic().setOnMouseClicked(e -> {
            Stage stage = (Stage) gridPane.getScene().getWindow();
            if (stage.getOnCloseRequest() != null) {
                stage.getOnCloseRequest().handle(null);
            }
            stage.close();
        });

    }

    /**
     * loads the table stage
     */
    private void loadTable() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/advanced-search-table.fxml"));
            Parent pane = loader.load();
            AdvancedSearchTableController controller = loader.getController();
            controller.initController(primaryTreeView, primaryTabPane,
                    searchField.getText(), checkTreeView.getCheckModel().getCheckedItems());

            Stage stage = (Stage) gridPane.getScene().getWindow();
            Scene scene = new Scene(pane);

            stage.setScene(scene);
            stage.show();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
