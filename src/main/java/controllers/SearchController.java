package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.ResourceBundle;
import utils.FontUtils;

public class SearchController implements Initializable {

    @FXML
    private HBox hBox;

    @FXML
    private TextField searchField;

    @FXML
    private TextField countField;

    @FXML
    private Button upButton;

    @FXML
    private Button downButton;

    @FXML
    private Button exitButton;

    private WebView webView;

    void setWebView(WebView webView) { this.webView = webView; }

    private int count = 0;

    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        upButton.setGraphic(FontUtils.createView("up-button-icon"));
        downButton.setGraphic(FontUtils.createView("down-button-icon"));
        exitButton.setGraphic(FontUtils.createView("exit-button-icon"));

        hBox.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                // scene is set for the first time. Now its the time to listen stage changes.
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        // stage is set. now is the right time to do whatever we need to the stage in the controller.
                        Stage stage = (Stage) newWindow;
                        stage.initOwner(webView.getScene().getWindow());
                        stage.initModality(Modality.NONE);
                        stage.initStyle(StageStyle.UNDECORATED);
                        stage.setResizable(false);
                        stage.setY(120);
                        stage.setX(1030);

                        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                            if (newValue.length() != 0 && getOccurrences(webView.getEngine(), newValue) != 0) {
                                count = 0;
                                downButton.fire();
                            }
                            else {
                                countField.setText("0/0");
                            }
                        });

                        downButton.setOnAction(e -> {
                            if (webView.getEngine().getDocument() != null && searchField.getText().length() != 0) {
                                if (count == getOccurrences(webView.getEngine(), searchField.getText())) {
                                    count = 0;
                                }
                                countField.setText(find(
                                        webView.getEngine(),
                                        searchField.getText()
                                ) + "/" + getOccurrences(
                                        webView.getEngine(),
                                        searchField.getText()
                                ));
                            }
                        });

                        upButton.setOnAction(e -> {
                            if (webView.getEngine().getDocument() != null && searchField.getText().length() != 0) {
                                if (count == 1) {
                                    count = getOccurrences(webView.getEngine(), searchField.getText()) + 1;
                                }
                                countField.setText(findBack(
                                        webView.getEngine(),
                                        searchField.getText()
                                ) + "/" + getOccurrences(
                                        webView.getEngine(),
                                        searchField.getText()
                                ));
                            }
                        });

                        exitButton.setOnAction(e -> stage.close());

                        stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                            if (e.getCode() == KeyCode.ENTER) {
                                downButton.fire();
                            }
                            else if (e.getCode() == KeyCode.UNDO && !searchField.isFocused()) {
                                upButton.fire();
                            }
                            else if (e.getCode() == KeyCode.ESCAPE) {
                                exitButton.fire();
                            }
                        });

                        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
                            if (!newValue) {
                                exitButton.fire();
                                webView.getScene().getWindow().requestFocus();
                                webView.requestFocus();
                            }
                        });
                    }

                });
            }
        });
    }

    private int find(@NotNull WebEngine engine, String text) {
        /* window.find(aString, aCaseSensitive, aBackwards, aWrapAround, aWholeWord, aSearchInFrames, aShowDialog) */
        engine.executeScript("window.find('" + text + "', false, false, true, false, true, false)");

        return ++count;
    }

    private int findBack(@NotNull WebEngine engine, String text) {
        /* window.find(aString, aCaseSensitive, aBackwards, aWrapAround, aWholeWord, aSearchInFrames, aShowDialog) */
        engine.executeScript("window.find('" + text + "', false, true, true, false, true, false)");

        return --count;
    }

    // Todo: document.body.innerText and or window.find() includes/excludes å, ä, ö -> fix / account for!
    private int getOccurrences(@NotNull WebEngine engine, @NotNull String text) {
        Object obj = engine.executeScript("document.body.innerText");

        String str = (String) obj;
        str = str.toLowerCase();
        int count = 0;

        while (str.contains(text.toLowerCase())) {
            str = str.replaceFirst(text, "");
            count++;
        }

        return count;
    }

}
