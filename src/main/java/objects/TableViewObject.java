package objects;

import javafx.beans.property.*;

import javafx.scene.control.TreeItem;
import org.jsoup.nodes.Document;

import java.io.File;


public class TableViewObject {

    private static int idCount = 1;
    private final IntegerProperty cellTableCount = new SimpleIntegerProperty(idCount++);
    private final ObjectProperty<Document> document;
    private final StringProperty title;
    private final StringProperty dir;
    private final IntegerProperty matches;
    private final TreeItem<String> treeItem;
    private final File file;

    public TableViewObject(Document document, String dir, int matches, TreeItem<String> treeItem, File file) {
        this.document = new SimpleObjectProperty<Document>(document);
        this.title = new SimpleStringProperty(document.title());
        this.matches = new SimpleIntegerProperty(matches);
        this.dir = new SimpleStringProperty(dir);
        this.treeItem = treeItem;
        this.file = file;
    }


    public Document getDocument() {
        return document.get();
    }

    public ObjectProperty<Document> documentProperty() {
        return document;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public int getMatches() {
        return matches.get();
    }

    public IntegerProperty matchesProperty() {
        return matches;
    }

    public int getCellTableCount() {
        return cellTableCount.get();
    }

    public IntegerProperty cellTableCountProperty() {
        return cellTableCount;
    }

    public String getDir() {
        return dir.get();
    }

    public StringProperty dirProperty() {
        return dir;
    }

    public static void resetCountId() {
        idCount = 1;
    }

    public TreeItem<String> getTreeItem() {
        return treeItem;
    }

    public File getFile() {
        return file;
    }
}
