package controllers;

import io.IO;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import objects.MyTab;
import objects.TableViewObject;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import utils.FontUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static utils.GUIUtils.runSafe;

public class AdvancedSearchTableController implements Initializable {

    @FXML
    private GridPane gridPane;

    @FXML
    private TableView<TableViewObject> tableView;

    private ObservableList<TableViewObject> tableData = FXCollections.observableArrayList();

    private TableColumn<TableViewObject, Number> colId = new TableColumn<>("#");

    private TableColumn<TableViewObject, String> colDomain = new TableColumn<>("Domain");

    private TableColumn<TableViewObject, String> colTitle = new TableColumn<>("Title");

    private TableColumn<TableViewObject, Number> colMatches = new TableColumn<>();

    @FXML
    private Button backButton;

    @FXML
    private Button openButton;

    @FXML
    private Label label;

    private SearchManager searchManager;

    private TreeView<String> primaryTreeView;
    private TabPane primaryTabPane;
    private File initFile;

    /**
     * initialize is responsible for setting property values on the stage itself at initialization
     * is executed before initController
     */
    @Override
    public void initialize(URL location, ResourceBundle resourceBundle) {
        initFile = IO.readInitFile();
        if (initFile == null || !initFile.exists()) { System.err.println("initFile == null"); System.exit(0);}

        gridPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        Stage stage = (Stage) newWindow;

                        stage.setWidth(stage.getWidth() + 100);
                        stage.setMinHeight(stage.getHeight());
                        stage.setMaxHeight(stage.getHeight());
                        stage.setResizable(true);

                        stage.setOnCloseRequest(e -> {
                            if (searchManager != null) {
                                searchManager.shutdown();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * initController is responsible for setting the content of the scene
     * executes after initialize
     */
    void initController(final TreeView<String> treeView, final TabPane tabPane,
                        final String text, final ObservableList<TreeItem<String>> list) {
        this.primaryTreeView = treeView;
        this.primaryTabPane = tabPane;

        /*
        INIT backButton, openButton, tableColumns and table
         */
        backButton.setGraphic(FontUtils.createView("back-button-icon"));
        backButton.getGraphic().setOnMouseClicked((mouseEvent) -> loadTree());

        openButton.setGraphic(FontUtils.createView("open-button-icon"));

        // opens up a new tab
        openButton.getGraphic().setOnMouseClicked((mouseEvent) -> {
            TableViewObject obj;
            if ((obj = tableView.getSelectionModel().getSelectedItem()) != null) {
                File inFile = new File(obj.getFile().getAbsolutePath());
                WebView webView;
                MyTab tab = new MyTab(obj.getTitle(), obj.getTreeItem());
                tab.setTooltip(new Tooltip(tab.getText()));
                tab.getTooltip().setShowDelay(Duration.millis(500));

                tabPane.getTabs().add(tab);
                tab.setOnClosed(e -> {
                    MyTab nTab;
                    Node node;
                    if ((nTab = (MyTab) tabPane.getSelectionModel().getSelectedItem()) != null && (node = nTab.getContent()) != null) {
                        node.requestFocus();
                    }
                });

                tabPane.getTabs().get(tabPane.getTabs().size() - 1).setContent(webView = new WebView());
                tabPane.getSelectionModel().selectLast();
                tabPane.getScene().getWindow().requestFocus();
                tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();
                webView.getEngine().load(inFile.toURI().toString());
            }
        });

        colId.prefWidthProperty().bind(tableView.widthProperty().divide(16.67));
        colTitle.prefWidthProperty().bind(tableView.widthProperty().divide(1.80));
        colDomain.prefWidthProperty().bind(tableView.widthProperty().divide(3.3));
        colMatches.prefWidthProperty().bind(tableView.widthProperty().divide(16.67));
        colMatches.setGraphic(FontUtils.createView("column-matches-icon"));

        colId.setCellValueFactory(cellData -> cellData.getValue().cellTableCountProperty());
        colDomain.setCellValueFactory(cellData -> cellData.getValue().dirProperty());
        colTitle.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        colMatches.setCellValueFactory(cellData -> cellData.getValue().matchesProperty());

        tableView.getColumns().add(colId);
        tableView.getColumns().add(colTitle);
        tableView.getColumns().add(colDomain);
        tableView.getColumns().add(colMatches);

        tableView.setItems(tableData);

        // search items for occurrence of text
        search(text, list);
    }


    /**
     * loads the tree scene
     */
    private void loadTree() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/advanced-search-tree.fxml"));
            Parent node = loader.load();
            AdvancedSearchTreeController controller = loader.getController();
            controller.initController(primaryTreeView, primaryTabPane);

            Stage stage = (Stage) gridPane.getScene().getWindow();
            Scene scene = new Scene(node);

            stage.setScene(scene);
            stage.show();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void search(final String text, final ObservableList<TreeItem<String>> list) {
        label.setText("searching for '" + text + "' ...");
        TableViewObject.resetCountId();
        ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
        singleThreadPool.execute(searchManager = new SearchManager(text, list));
    }

    private class SearchManager implements Runnable {
        private final String text;
        private final ObservableList<TreeItem<String>> treeItemList;
        private ExecutorService threadPool = Executors.newFixedThreadPool(10);

        private SearchManager(String text, ObservableList<TreeItem<String>> treeItemList) {
            this.text = text;
            this.treeItemList = treeItemList;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();

            for (TreeItem<String> treeItem : treeItemList) {
                if (treeItem.isLeaf()) {
                    threadPool.submit(new SearchDoc(text, treeItem));
                }
            }

            threadPool.shutdown();

            try {
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
                runSafe(() -> label.setText("done, exe time: " + Math.round((System.nanoTime() - startTime) * Math.pow(10, -9)) + " s"));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private void shutdown() {
            threadPool.shutdownNow();
        }

        private class SearchDoc implements Runnable {
            private String textToSearchFor;
            private final TreeItem<String> treeItem;
            private File searchDir;

            private SearchDoc(String textToSearchFor, TreeItem<String> treeItem) {
                this.textToSearchFor = textToSearchFor;
                this.treeItem = treeItem;
                this.searchDir = new File(getPath(treeItem) + "\\html");
            }

            @Override
            public void run() {
                try {
                    File[] listFiles = searchDir.listFiles();

                    assert listFiles != null;
                    for (File currentFile : listFiles) {
                        Document document = Jsoup.parse(currentFile, "UTF-8");
                        int matches;
                        if ((matches = occurrences(document.text(), textToSearchFor)) != 0) {
                            runSafe(() -> tableData.add(new TableViewObject(document, getDomainName(treeItem), matches,
                                    treeItem, currentFile)));
                        }

                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private int occurrences(String document, @NotNull String text) {
            document = document.toLowerCase();
            int count = 0;

            while (document.contains(text.toLowerCase())) {
                document = document.replaceFirst(text, "");
                count++;
            }

            return count;
        }

        @NotNull
        private String getDomainName(@NotNull TreeItem<String> treeItem) {
            String labelText = treeItem.getValue();
            while ((treeItem = treeItem.getParent()).getParent() != null) {
                labelText = treeItem.getValue() + "/" + labelText;
            }
            return labelText.toUpperCase();
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

}
