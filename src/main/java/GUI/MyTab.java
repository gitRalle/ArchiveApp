package GUI;

import javafx.scene.control.Tab;

public class MyTab extends Tab {
    private final String hiddenInfo;

    public MyTab(String text, String hiddenInfo) {
        super(text);
        this.hiddenInfo = hiddenInfo;
    }

    public String getHiddenInfo() {
        return hiddenInfo;
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
        return super.equals(myTab) && myTab.hiddenInfo.equals(this.hiddenInfo);

    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash * 31 * super.hashCode() + hiddenInfo.hashCode();
    }
}
