package utils;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;

public class FontUtils {

    public static Label createLabel(@NotNull TreeItem<String> treeItem) {
        Label label;

        if (treeItem.getParent() == null) {
            label = new Label(treeItem.getValue() + " > ", createRootView());
        }
        else if (!treeItem.isLeaf()) {
            label = new Label(treeItem.getValue() + " > ", createBranchView());
        }
        else {
            label = new Label(treeItem.getValue(), createLeafView());
        }

        return label;
    }

    public static FontAwesomeIconView createRootView() {
        FontAwesomeIconView rootIcon = new FontAwesomeIconView();
        rootIcon.setStyleClass("root-icon");
        return rootIcon;
    }

    public static FontAwesomeIconView createBranchView() {
        FontAwesomeIconView branchIcon = new FontAwesomeIconView();
        branchIcon.setStyleClass("branch-icon");
        return branchIcon;
    }

    public static FontAwesomeIconView createLeafView() {
        FontAwesomeIconView leafIcon = new FontAwesomeIconView();
        leafIcon.setStyleClass("leaf-icon");
        return leafIcon;
    }

    public static FontAwesomeIconView createView(String styleClass) {
        FontAwesomeIconView view = new FontAwesomeIconView();
        view.setStyleClass(styleClass);
        return view;
    }

}
