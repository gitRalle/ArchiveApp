package GUI;

import IO.IO;
import com.github.lalyos.jfiglet.FigletFont;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.*;
import logic.ArchiveLogic;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.Comparator;
import java.util.Objects;


import static IO.IO.readRootUrl;


public class ArchiveAppController {
    // GUI controls defined in FXML and used by the controller's code

    @FXML
    private GridPane gridPane;

    @FXML
    private TextField searchField;

    @FXML
    private Label path;

    @FXML
    private Button hideButton;

    @FXML
    private Button consoleButton;

    @FXML
    private SplitPane splitPane;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane tabPane;

    private Node treeViewPane;
    private double dividerPosition;

    private Console console;
    private final int CONSOLE_WIDTH  = 550;
    private final int CONSOLE_HEIGTH = 350;
    private boolean consoleIsShowing;

    private ArchiveLogic logic;
    private File initFile;


    public void initialize() {
        if ((initFile = IO.readInitFile()) == null) { System.err.println("InitFile == null"); System.exit(0); }

        // Todo: INIT TREEVIEW
        File rootFile = new File(initFile.getAbsolutePath() + "\\" + "domains"); // Files/domains
        if (!rootFile.exists() && !rootFile.mkdir()) { System.exit(0); }

        TreeItem<String> rootTreeItem = new TreeItem<>(rootFile.getName().toUpperCase());

        for (File currentDomainFile : Objects.requireNonNull(rootFile.listFiles())) {
            TreeItem<String> domainTreeItem = new TreeItem<>(currentDomainFile.getName());

            for (File currentYearFile : Objects.requireNonNull(currentDomainFile.listFiles())) {
                TreeItem<String> yearTreeItem = new TreeItem<>(currentYearFile.getName());
                domainTreeItem.getChildren().add(yearTreeItem);

                for (File currentMonthFile : Objects.requireNonNull(currentYearFile.listFiles())) {
                    TreeItem<String> monthTreeItem = new TreeItem<>(currentMonthFile.getName());
                    yearTreeItem.getChildren().add(monthTreeItem);

                    for (File currentDayFile : Objects.requireNonNull(currentMonthFile.listFiles())) {
                        TreeItem<String> dayTreeItem = new TreeItem<>(currentDayFile.getName());
                        monthTreeItem.getChildren().add(dayTreeItem);
                    }

                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("TH", 2)[0])));
                }

                yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
            }

            domainTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
            rootTreeItem.getChildren().add(domainTreeItem);
        }

        rootTreeItem.getChildren().sort(Comparator.comparing(TreeItem::getValue));
        treeView.setRoot(rootTreeItem);

        // make a copy of treeView
        treeViewPane = splitPane.getItems().get(0);

        // add listener to selectionModel for treeView
        treeView.getSelectionModel().
                selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setLabel(newValue);
                    }
                });

        // add listener to focusedProperty for treeView
        treeView.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && treeView.getSelectionModel().getSelectedItem() != null) {
                setLabel(treeView.getSelectionModel().getSelectedItem());
            }
        });
        treeView.getStyleClass().add("tree-view");

        // init console & logic
        logic = new ArchiveLogic(console = new Console(), treeView);
        console.getStyleClass().add("console");
        console.setPrefWidth(CONSOLE_WIDTH);
        console.setPrefHeight(CONSOLE_HEIGTH);
        console.setOnMessageReceivedHandler(s -> logic.runCommand(s));

        // Todo: INIT TABPANE

        // add listener on selectionModel for tabPane
        tabPane.getSelectionModel().
                selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        MyTab myTab = (MyTab) newValue;
                        path.setText(myTab.getHiddenInfo().replace(initFile.getAbsolutePath() + "\\", "").replace("\\", " > "));
                    }
                    else
                        path.setText("");
                });

        // add mouseEvent on tabPane to transfer focus when clicked
        tabPane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (tabPane.getSelectionModel().getSelectedItem() != null && tabPane.getSelectionModel().getSelectedItem().getContent() != null) {
                tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();
            }
        });

        // add listener to focusedProperty for tabPane
        tabPane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // if focus gained
            if (newValue) {
                MyTab myTab;
                myTab = (MyTab) tabPane.getSelectionModel().getSelectedItem();
                if (myTab != null) {
                    path.setText(myTab.getHiddenInfo().replace(initFile.getAbsolutePath() + "\\", "").replace("\\", " > "));
                }
            }
        });

        tabPane.getTabs().add(new MyTab("Welcome", "Welcome"));

        tabPane.getTabs().get(0).setContent(new AnchorPane());
        tabPane.getTabs().get(0).getContent().requestFocus();

        FontAwesomeIcon icon = FontAwesomeIcon.TOGGLE_ON;
        hideButton.setGraphic(new FontAwesomeIconView(icon));

        
    }

    @FXML
    private void handleAnyEventOnTreeView(Event event) {
        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED && ((KeyEvent) event).getCode() == KeyCode.ENTER) {
            TreeItem<String> selectedItem;

            if ((selectedItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                if (!selectedItem.isLeaf()) {
                    selectedItem.setExpanded(!selectedItem.isExpanded());
                }

                else if (selectedItem.getValue().contains("TH")) {
                    String filePath = initFile.getAbsolutePath() + "\\" + path.getText().replace(" > ", "\\");
                    openNewTab(filePath);
                }
            }
        }


        else if (event instanceof MouseEvent && event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            Node node = ((MouseEvent) event).getPickResult().getIntersectedNode();
            TreeItem<String> selectedItem;

            if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {

                if ((selectedItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                    if (selectedItem.isLeaf() && selectedItem.getValue().contains("TH")) {
                        String filePath = initFile.getAbsolutePath() + "\\" + path.getText().replace(" > ", "\\");
                        openNewTab(filePath);
                    }
                }
            }

        }

    }

    @FXML
    private void hideTreeViewClicked() {
        // Remove left splitPane node
        if (splitPane.getItems().contains(treeViewPane)) {
            dividerPosition = splitPane.getDividers().get(0).getPosition();
            splitPane.getItems().remove(treeViewPane);
        }

        // Add left splitPane node
        else {
            splitPane.getItems().add(0, treeViewPane);
            splitPane.setDividerPosition(0, dividerPosition);
            treeView.requestFocus();
        }
    }

    @FXML
    private void handleKeyEventOnGridPane(@NotNull KeyEvent event) {
        if (event.getCode() == KeyCode.F1) {
            showConsole();
        } else if (event.getCode() == KeyCode.H) {
            hideButton.fire();
        }
    }


    @FXML
    private void handleKeyEventOnTabPane(@NotNull KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            // Closes selected tab if its content currently has focus
            if (tabPane.getSelectionModel().getSelectedItem() != null) {
                if (tabPane.getSelectionModel().getSelectedItem().getContent() != null) {
                    if (tabPane.getSelectionModel().getSelectedItem().getContent().isFocused()) {
                        closeTab(tabPane.getSelectionModel().getSelectedItem());

                    }
                }
            }

        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
            if (tabPane.getSelectionModel().getSelectedItem() != null) {
                if (tabPane.getSelectionModel().getSelectedItem().getContent() != null) {
                    searchPopup((WebView) tabPane.getSelectionModel().getSelectedItem().getContent());
                }
            }
        }
    }

    private void openNewTab(String filePath) {
        // hashCode in indexFile
        int hashCode = readRootUrl(new File(filePath + "\\" + "url.txt")).hashCode();

        File fileToBeLoaded = new File(
                filePath + "\\html\\" + hashCode + ".html"); // Todo: is abs

        String title = "";

        try { title = Jsoup.parse(fileToBeLoaded, "UTF-8").title(); } catch (IOException ex) { ex.printStackTrace(); }

        WebView webView;
        MyTab tab;

        // Add new Tab to TabPane and set onClosed
        tabPane.getTabs().add(tabPane.getTabs().size(), tab = new MyTab(title, filePath));
        tab.setOnClosed(e -> {
            if (tabPane.getSelectionModel().getSelectedItem() != null && tabPane.getSelectionModel().getSelectedItem().getContent() != null) {
                tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();
            }
        });
        // Add WebView to newly created Tab
        tabPane.getTabs().get(tabPane.getTabs().size() - 1).setContent(webView = new WebView());
        // Select newly created Tab
        tabPane.getSelectionModel().clearAndSelect(tabPane.getTabs().size() - 1);
        // Request focus for newly created WebView
        tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();
        // Load selected html doc into engine
        webView.getEngine().load(fileToBeLoaded.toURI().toString());


    }

    private void closeTab(@NotNull Tab tab) {
        EventHandler<Event> handler = tab.getOnClosed();
        if (handler != null) {
            tab.getTabPane().getTabs().remove(tab);
            handler.handle(null);
        }
        else {
            tab.getTabPane().getTabs().remove(tab);
        }
    }

    private void setLabel(@NotNull TreeItem<String> treeItem) {
        String labelText = treeItem.getValue();
        while ((treeItem = treeItem.getParent()) != null) {
            labelText = treeItem.getValue() + " > " + labelText;
        }
        path.setText(labelText);
    }

    private void addChangeListenerOnWebEngine(@NotNull WebView webView) {
        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                if (newValue == Worker.State.SUCCEEDED) {
                    System.out.println("newValue!");
                }

            }
        });
    }

    private int count = 0;

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


    private void searchPopup(WebView webView) {
        final Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initOwner(gridPane.getScene().getWindow());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setY(120);
        stage.setX(1030);

        TextField searchField = new TextField();

        Button backButton = new Button("UP");
        backButton.setMinWidth(38);
        backButton.setPrefWidth(38);

        Button searchButton = new Button("DW");
        searchButton.setDefaultButton(true);

        TextField countField = new TextField();
        countField.setEditable(false);
        countField.setMaxWidth(50);

        Button exitButton = new Button("X");

        HBox hBox = new HBox(searchField, countField, backButton, searchButton, exitButton);
        hBox.setPadding(new Insets(8, 8, 8, 8));
        hBox.setSpacing(2.5);

        Scene scene = new Scene(hBox, 335, 35); // 265

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() != 0 && getOccurrences(webView.getEngine(), newValue) != 0) {
                count = 0;
                searchButton.fire();
            }
            else {
                countField.setText("0/0");
            }
        });

        searchButton.setOnAction(e -> {
            if (webView.getEngine().getDocument() != null && searchField.getText() != null && searchField.getText().length() != 0) {
                if (count == getOccurrences(webView.getEngine(), searchField.getText())) {
                    count = 0;
                }
                countField.setText(String.valueOf(find(
                        webView.getEngine(),
                        searchField.getText()
                )) + "/" + String.valueOf(getOccurrences(
                        webView.getEngine(),
                        searchField.getText()
                )));

            }

        });

        backButton.setOnAction((ActionEvent e) -> {
            if (webView.getEngine().getDocument() != null && searchField.getText() != null && searchField.getText().length() != 0) {
                if (count == 1) {
                    count = getOccurrences(webView.getEngine(), searchField.getText()) + 1;
                }
                countField.setText(String.valueOf(findBack(
                        webView.getEngine(),
                        searchField.getText()
                )) + "/" + String.valueOf(getOccurrences(
                        webView.getEngine(),
                        searchField.getText()
                )));
            }

        });

        exitButton.setOnAction((ActionEvent event) -> {
            stage.close();
        });

        EventHandler<KeyEvent> handleKeyEvent = (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                exitButton.fire();
            }
        };
        stage.addEventHandler(KeyEvent.KEY_PRESSED, handleKeyEvent);

        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                exitButton.fire();
            }
        });

        stage.setScene(scene);
        stage.show();

    }


    public void showConsole() {
        if (!consoleIsShowing) {

            final Stage stage = new Stage();
            stage.initOwner(gridPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setX(gridPane.getScene().getWindow().getX() + 8);
            stage.setY(gridPane.getScene().getWindow().getY() + 1.3);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setResizable(false);

            AnchorPane anchorPane = new AnchorPane(console);
            anchorPane.getStyleClass().add("root");

            Scene scene = new Scene(anchorPane, CONSOLE_WIDTH, CONSOLE_HEIGTH);
            scene.getStylesheets().add(
                    getClass().getResource("console.css").toExternalForm()
            );

            EventHandler<KeyEvent> handleKeyEvent = (KeyEvent event) -> {
                if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.F1) {
                    stage.close();
                    stage.getOnCloseRequest().handle(null);
                }
            };
            stage.addEventHandler(KeyEvent.KEY_PRESSED, handleKeyEvent);

            stage.setOnShowing(event ->      consoleIsShowing = true);
            stage.setOnCloseRequest(event -> consoleIsShowing = false);

            stage.setScene(scene);
            stage.show();
            stage.getOnShowing().handle(null);
            console.requestFocus();

        }

    }

}

