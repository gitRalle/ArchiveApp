package objects;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;

public class MyTab extends Tab {
    private final TreeItem<String> hiddenTreeItem;

    public MyTab(String text, TreeItem<String> hiddenTreeItem) {
        super(text);
        this.hiddenTreeItem = hiddenTreeItem;
    }

    public TreeItem<String> getHiddenTreeItem() {
        return hiddenTreeItem;
    }

    @Override
    public boolean equals(Object obj ) {
        if (!(obj instanceof MyTab)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        MyTab myTab = (MyTab) obj;
        return super.equals(myTab) && myTab.hiddenTreeItem.equals(this.hiddenTreeItem);

    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash * 31 * super.hashCode() + hiddenTreeItem.hashCode();
    }
}
