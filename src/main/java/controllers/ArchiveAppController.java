package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import logic.CommandLineLogic;
import objects.CommandPrompt;
import objects.MyTab;
import io.IO;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import utils.FontUtils;

import java.io.File;
import java.io.IOException;
import java.time.Month;
import java.util.*;


import static io.IO.readRootUrl;

public class ArchiveAppController {

    @FXML
    private GridPane gridPane;

    @FXML
    private TextFlow textFlow;

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private Button hideButton;

    @FXML
    private Button advancedSearchButton;

    @FXML
    private Button promptButton;

    @FXML
    private TextField searchField;

    @FXML
    private SplitPane splitPane;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane tabPane;

    private Node treeViewPane;
    private double dividerPosition;

    private CommandPrompt prompt = new CommandPrompt();
    private final int CONSOLE_WIDTH  = 600;
    private final int CONSOLE_HEIGHT = 350;
    private boolean consoleIsShowing;

    private CommandLineLogic commandLogic = new CommandLineLogic(prompt, treeView);
    private File initFile;

    public void initialize() {
        if ((initFile = IO.readInitFile()) == null) { System.err.println("InitFile == null"); System.exit(0); }

        /*
        * INIT treeView
        * */

        File rootFile = new File(initFile.getAbsolutePath() + "\\" + "domains"); // Files/domains
        if (!rootFile.exists() && !rootFile.mkdir()) { System.exit(0); }

        TreeItem<String> rootTreeItem = new TreeItem<>(rootFile.getName().toUpperCase());
        rootTreeItem.setGraphic(FontUtils.createRootView());

        for (File currentDomainFile : Objects.requireNonNull(rootFile.listFiles())) {
            TreeItem<String> domainTreeItem = new TreeItem<>(currentDomainFile.getName());
            domainTreeItem.setGraphic(FontUtils.createBranchView());

            for (File currentYearFile : Objects.requireNonNull(currentDomainFile.listFiles())) {
                TreeItem<String> yearTreeItem = new TreeItem<>(currentYearFile.getName());
                yearTreeItem.setGraphic(FontUtils.createBranchView());
                domainTreeItem.getChildren().add(yearTreeItem);

                for (File currentMonthFile : Objects.requireNonNull(currentYearFile.listFiles())) {
                    TreeItem<String> monthTreeItem = new TreeItem<>(currentMonthFile.getName());
                    monthTreeItem.setGraphic(FontUtils.createBranchView());
                    yearTreeItem.getChildren().add(monthTreeItem);

                    for (File currentDayFile : Objects.requireNonNull(currentMonthFile.listFiles())) {
                        TreeItem<String> dayTreeItem = new TreeItem<>(currentDayFile.getName());
                        dayTreeItem.setGraphic(FontUtils.createLeafView());
                        monthTreeItem.getChildren().add(dayTreeItem);
                    }

                    monthTreeItem.getChildren().sort(
                            Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("(ST|ND|RD|TH)", 2)[0])));
                }

                yearTreeItem.getChildren().sort(
                        Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
            }

            domainTreeItem.getChildren().sort(
                    Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
            rootTreeItem.getChildren().add(domainTreeItem);
        }

        rootTreeItem.getChildren().sort(Comparator.comparing(TreeItem::getValue));
        treeView.setRoot(rootTreeItem);

        // make a copy of treeView
        treeViewPane = splitPane.getItems().get(0);

        treeView.getSelectionModel().selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setTextFlow(newValue);
                    }
                });

        treeView.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && treeView.getSelectionModel().getSelectedItem() != null) {
                setTextFlow(treeView.getSelectionModel().getSelectedItem());
            }
        });

        treeView.getStyleClass().add("tree-view");
        treeView.getRoot().setExpanded(true);
        treeView.getSelectionModel().selectFirst();

        /*
        ----------------------------------------------------------------------------------------------------------------
        * INIT tabPane
        * */

        tabPane.getSelectionModel().selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setTextFlow(((MyTab) newValue).getHiddenTreeItem());
                    }
                    else {
                        textFlow.getChildren().clear();
                    }
                });

        tabPane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            MyTab tab;
            Node node;

            if ((tab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null && (node = tab.getContent()) != null) {
                node.requestFocus();
            }
        });

        tabPane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            MyTab tab;
            if (newValue && (tab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null) {
                setTextFlow(tab.getHiddenTreeItem());
            }
        });

        /*
        ----------------------------------------------------------------------------------------------------------------
        * INIT prompt / prompt
        * */
        prompt.setOnMessageReceivedHandler(s -> commandLogic.execute(s));
        prompt.setPrefWidth(CONSOLE_WIDTH);
        prompt.setPrefHeight(CONSOLE_HEIGHT);

        initButtons();

        treeView.requestFocus();

    }

    private void initButtons() {
        FontAwesomeIconView hideButtonIcon = new FontAwesomeIconView();
        hideButtonIcon.setStyleClass("hide-button-icon");
        hideButton.setGraphic(hideButtonIcon);

        FontAwesomeIconView advancedSearchButtonIcon = new FontAwesomeIconView();
        advancedSearchButtonIcon.setStyleClass("advanced-search-button-icon");
        advancedSearchButton.setGraphic(advancedSearchButtonIcon);

        FontAwesomeIconView consoleButtonIcon = new FontAwesomeIconView();
        consoleButtonIcon.setStyleClass("prompt-button-icon");
        promptButton.setGraphic(consoleButtonIcon);

    }

    @FXML
    private void handleAnyEventOnTreeView(Event event) {
        if (event instanceof KeyEvent && event.getEventType() == KeyEvent.KEY_PRESSED &&
                ((KeyEvent) event).getCode() == KeyCode.ENTER)
        {
            TreeItem<String> selectedTreeItem;

            if ((selectedTreeItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                if (!selectedTreeItem.isLeaf()) {
                    selectedTreeItem.setExpanded(!selectedTreeItem.isExpanded());
                }

                else if (selectedTreeItem.getValue().contains("TH")) {
                    openNewTab(selectedTreeItem);
                }
            }
        }

        else if (event instanceof MouseEvent && event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            Node node = ((MouseEvent) event).getPickResult().getIntersectedNode();
            TreeItem<String> selectedTreeItem;

            if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {

                if ((selectedTreeItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                    // Todo: TH -> fix!
                    if (selectedTreeItem.isLeaf() && selectedTreeItem.getValue().contains("TH")) {
                        openNewTab(selectedTreeItem);
                    }
                }
            }
        }
    }

    @FXML
    private void hideButtonClicked() {
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
            promptButton.fire();
        } else if (event.getCode() == KeyCode.H) {
            hideButton.fire();
        }
    }


    @FXML
    private void handleKeyEventOnTabPane(@NotNull KeyEvent event) throws Exception {
        MyTab tab;
        Node node;

        if (event.getCode() == KeyCode.ESCAPE) {
            if ((tab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null && (node = tab.getContent()) != null) {
                if (node.isFocused()) {
                    closeTab(tab);
                }
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
            if ((tab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null && (node = tab.getContent()) != null) {
                if (node instanceof WebView) {
                    showSearchStage((WebView) node);
                }
            }
        }
    }

    private void openNewTab(TreeItem<String> treeItem) {
        String filePath = getFilePath(treeItem);

        int hashCode = readRootUrl(new File(filePath + "\\" + "url.txt")).hashCode();

        File fileToBeLoaded = new File(
                filePath + "\\html\\" + hashCode + ".html");

        String title = "";

        try { title = Jsoup.parse(
                fileToBeLoaded, "UTF-8").title(); } catch (IOException ex) { ex.printStackTrace(); }

        WebView webView;
        MyTab tab = new MyTab(title, treeItem);
        tab.setTooltip(new Tooltip(title));
        tab.getTooltip().setShowDelay(Duration.millis(500));

        tabPane.getTabs().add(tab);
        tab.setOnClosed(e -> {
            MyTab nTab;
            Node node;
            // tabPane.getTabs().remove(tab) handled in closeTab method ->
            if ((nTab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null && (node = nTab.getContent()) != null) {
                node.requestFocus();
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


    private String getFilePath(@NotNull TreeItem<String> treeItem) {
        String labelText = treeItem.getValue();
        while ((treeItem = treeItem.getParent()) != null) {
            labelText = treeItem.getValue() + "\\" + labelText;
        }
        return initFile.getAbsolutePath() + "\\" + labelText.toLowerCase();
    }

    private void setTextFlow(@NotNull TreeItem<String> treeItem) {
        textFlow.getChildren().clear();

        Label label = FontUtils.createLabel(treeItem);
        label.getStyleClass().add("text-flow-label");

        TextFlow temp = new TextFlow(label);

        while ((treeItem = treeItem.getParent()) != null) {
            label = FontUtils.createLabel(treeItem);
            label.getStyleClass().add("text-flow-label");
            temp = new TextFlow(label, temp);
        }

        textFlow.getChildren().add(temp);
    }

    private void showSearchStage(WebView webView) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/search.fxml"));
        Parent root = (Parent) loader.load();
        SearchController controller = loader.getController();
        controller.setWebView(webView);

        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void showPromptStage() {
        if (!consoleIsShowing) {

            Stage stage = new Stage();
            stage.setTitle("Archive App Command Prompt");
            stage.getIcons().add(new Image(
                    getClass().getResource("/images/terminal.png").toExternalForm()
            ));

            stage.initOwner(gridPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.DECORATED);
            stage.setResizable(true);
            stage.widthProperty().addListener((observable, oldValue, newValue) -> {
                double stageWidth = newValue.doubleValue();
                stage.setX(gridPane.getScene().getWindow().getX() + gridPane.getScene().getWindow().getWidth() / 2 - stageWidth / 2);
            });

            stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                double stageHeight = newValue.doubleValue();
                stage.setY(gridPane.getScene().getWindow().getY() + gridPane.getScene().getWindow().getHeight() / 2 - stageHeight / 2);
            });

            Scene scene = new Scene(new BorderPane(prompt), CONSOLE_WIDTH, CONSOLE_HEIGHT);
            scene.getStylesheets().add(
                    getClass().getResource("/css/command-prompt.css").toExternalForm()
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
            prompt.requestFocus();
        }
    }

}

