package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.input.*;
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

    private final CommandPrompt prompt = new CommandPrompt();
    private final int CONSOLE_WIDTH = 600;
    private final int CONSOLE_HEIGHT = 350;
    private boolean consoleIsShowing;

    private CommandLineLogic commandLogic;
    private File initFile;

    private ContextMenu contextMenu = new ContextMenu();

    public void initialize() {
        if ((initFile = IO.readInitFile()) == null) {
            System.err.println("InitFile == null");
            System.exit(0);
        }

        /*
         * INIT treeView
         * */

        File rootFile = new File(initFile.getAbsolutePath() + "\\" + "domains"); // Files/domains
        if (!rootFile.exists() && !rootFile.mkdir()) {
            System.exit(0);
        }

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

        treeView.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {

                Node node = e.getPickResult().getIntersectedNode();

                if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {

                    TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();

                    if (selectedItem != null && selectedItem.isLeaf()) {

                        double x = e.getScreenX();
                        double y = e.getScreenY();

                        contextMenu.show(gridPane.getScene().getWindow(), x, y);
                    } else {
                        contextMenu.hide();

                    }
                }
            }
        });

        /*
         * INIT contextMenu
         */

        MenuItem aboutMenuItem = new MenuItem("About");
        aboutMenuItem.setGraphic(FontUtils.createView("about-menu-item-icon"));
        aboutMenuItem.setOnAction(e -> {
            TreeItem<String> selectedItem;

            if ((selectedItem = treeView.getSelectionModel().getSelectedItem()) != null) {
                showAboutAlert(selectedItem);
            }

        });
        MenuItem searchMenuItem = new MenuItem("Search");
        searchMenuItem.setGraphic(FontUtils.createView("search-icon"));
        searchMenuItem.setOnAction(e -> {

        });
        Menu codeMenu = new Menu("View code");
        codeMenu.setGraphic(FontUtils.createView("code-menu-icon"));
        MenuItem htmlMenuItem = new MenuItem("HTML");
        htmlMenuItem.setGraphic(FontUtils.createView("html-menu-item-icon"));
        htmlMenuItem.setOnAction(e -> {

        });
        MenuItem cssMenuItem = new MenuItem("CSS");
        cssMenuItem.setGraphic(FontUtils.createView("css-menu-item-icon"));
        cssMenuItem.setOnAction(e -> {

        });
        MenuItem jsMenuItem = new MenuItem("JavaScript");
        jsMenuItem.setGraphic(FontUtils.createView("js-menu-item-icon"));
        jsMenuItem.setOnAction(e -> {

        });
        codeMenu.getItems().addAll(htmlMenuItem, cssMenuItem, jsMenuItem);
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setGraphic(FontUtils.createView("delete-menu-item-icon"));
        deleteMenuItem.setOnAction(e -> {

        });

        contextMenu.getItems().addAll(aboutMenuItem, searchMenuItem, new SeparatorMenuItem(), codeMenu, new SeparatorMenuItem(), deleteMenuItem);
        contextMenu.getStyleClass().add("context-menu");

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
                    } else {
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
        * INIT logic / prompt
        * */
        commandLogic = new CommandLineLogic(prompt, treeView);
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
                ((KeyEvent) event).getCode() == KeyCode.ENTER) {
            TreeItem<String> selectedTreeItem;

            if ((selectedTreeItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                if (!selectedTreeItem.isLeaf()) {
                    selectedTreeItem.setExpanded(!selectedTreeItem.isExpanded());
                } else {
                    openNewTab(selectedTreeItem);
                }
            }
        } else if (event instanceof MouseEvent && event.getEventType() == MouseEvent.MOUSE_PRESSED &&
                ((MouseEvent) event).getButton() != MouseButton.SECONDARY) {

            Node node = ((MouseEvent) event).getPickResult().getIntersectedNode();

            if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {
                TreeItem<String> selectedTreeItem;

                if ((selectedTreeItem = treeView.getSelectionModel().getSelectedItem()) != null) {

                    if (selectedTreeItem.isLeaf()) {
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
        String path = getPath(treeItem);

        int hashCode = readRootUrl(path).hashCode();

        File fileToBeLoaded = new File(
                path + "\\html\\" + hashCode + ".html");

        String title = "";

        try {
            title = Jsoup.parse(
                    fileToBeLoaded, "UTF-8").title();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

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
        } else {
            tab.getTabPane().getTabs().remove(tab);
        }
    }


    @NotNull
    private String getPath(@NotNull TreeItem<String> treeItem) {
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
    private void showAdvancedSearchStage() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/advanced-search-tree.fxml"));
        Parent root = loader.load();
        AdvancedSearchTreeController controller = loader.getController();
        controller.initController(treeView, tabPane);

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
                stage.setX(gridPane.getScene().getWindow().getX() + gridPane.getScene().getWindow().getWidth() - stageWidth);
            });

            stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                double stageHeight = newValue.doubleValue();
                stage.setY(gridPane.getScene().getWindow().getY() + gridPane.getScene().getWindow().getHeight() - stageHeight);
            });

            Scene scene = new Scene(new BorderPane(prompt), CONSOLE_WIDTH, CONSOLE_HEIGHT);
            scene.getStylesheets().add(
                    getClass().getResource("/css/purple_and_white/command-prompt.css").toExternalForm()
            );

            EventHandler<KeyEvent> handleKeyEvent = (KeyEvent event) -> {
                if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.F1) {
                    stage.close();
                    stage.getOnCloseRequest().handle(null);
                }
            };
            stage.addEventHandler(KeyEvent.KEY_PRESSED, handleKeyEvent);

            stage.setOnShowing(event -> consoleIsShowing = true);
            stage.setOnCloseRequest(event -> consoleIsShowing = false);

            stage.setScene(scene);
            stage.show();
            stage.getOnShowing().handle(null);
            prompt.requestFocus();
        }
    }

    private void showAboutAlert(TreeItem<String> treeItem) {
        String path = getPath(treeItem);
        File pathFile = new File(path);

        // double checks selected treeItem's path (to homeFolder)
        if (!pathFile.exists()) {
            return;
        }

        List<String> lines = IO.readLog(pathFile.getAbsolutePath());

        // checks if log.file exists
        if (lines == null || lines.size() != 4) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setGraphic(FontUtils.createView("about-icon"));
        alert.setTitle("About");
        alert.setHeaderText("Information");
        alert.getDialogPane().getStylesheets().addAll(
                getClass().getResource("/css/purple_and_white/about.css").toExternalForm(),
                getClass().getResource("/css/purple_and_white/font.css").toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("dialog-pane");

        alert.setOnCloseRequest(e -> alert.hide());
        alert.getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                alert.hide();
            }
        });

        Label firstRowLabel = new Label("Links to: " + lines.get(0) + " pages", FontUtils.createView("first-row-label-icon"));
        Label secondRowLabel = new Label("Includes: " + lines.get(1) + " images", FontUtils.createView("second-row-label-icon"));
        Label thirdRowLabel = new Label("Runtime: " + lines.get(2), FontUtils.createView("third-row-label-icon"));
        Label fourthRowLabel = new Label(lines.get(3), FontUtils.createView("fourth-row-label-icon"));

        TextFlow textFlow = new TextFlow();
        textFlow.getChildren().addAll(firstRowLabel, new Text("\n"), secondRowLabel, new Text("\n"),
                thirdRowLabel, new Text("\n"), fourthRowLabel);
        textFlow.getStyleClass().add("text-flow");

        Button button = ((Button) alert.getDialogPane().lookupButton(ButtonType.OK));
        button.setText(null);
        button.setGraphic(FontUtils.createView("about-button-icon"));
        button.getStyleClass().add("about-button");

        alert.getDialogPane().setContent(textFlow);
        alert.getDialogPane().setPrefWidth(275);
        alert.setX(treeItem.getGraphic().getScene().getWindow().getX() + treeItem.getGraphic().getLayoutX());
        alert.setY(treeItem.getGraphic().getScene().getWindow().getY() + treeItem.getGraphic().getLayoutY());

        alert.show();

    }


}

