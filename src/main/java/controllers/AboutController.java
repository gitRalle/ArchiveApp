package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private BorderPane rootBorderPane;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab tabOne;

    @FXML
    private Tab tabTwo;

    @FXML
    private BorderPane tabOneBorderPane;

    @FXML
    private BorderPane tabTwoBorderPane;

    @FXML
    private TextFlow tabOneTextFlow;

    @FXML
    private TextFlow tabTwoTextFlow;

    private List<String> list;

    public void setList(List<String> list) { this.list = list; }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
