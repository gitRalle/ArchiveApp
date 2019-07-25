package objects;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import java.util.*;
import java.util.function.Consumer;

import static javafx.scene.input.KeyCode.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static utils.GUIUtils.runSafe;


public class CommandPrompt extends BorderPane {
    private final StyleClassedTextArea area = new StyleClassedTextArea();

    private final List<String> history = new ArrayList<>();
    private int historyPointer = 0;

    private Consumer<String> onMessageReceivedHandler;

    public CommandPrompt() {
        VirtualizedScrollPane<StyleClassedTextArea> scrollPane = new VirtualizedScrollPane<>(area);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        setCenter(scrollPane);
        area.setEditable(true);
        area.setWrapText(false);

        Nodes.addInputMap(area, InputMap.consume(keyPressed(ENTER), e -> {
            String paragraphText = area.getText(area.getCurrentParagraph());
            if (paragraphText != null && !paragraphText.equals("")) {
                history.add(paragraphText);
                historyPointer++;
            }
            area.appendText(System.lineSeparator());
            if (onMessageReceivedHandler != null && paragraphText != null && !paragraphText.equals("")) {
                onMessageReceivedHandler.accept(paragraphText);
            }
        }));

        Nodes.addInputMap(area, InputMap.consume(keyPressed(BACK_SPACE), e -> {
            int minor = area.offsetToPosition(area.getCaretPosition(), Bias.Backward).getMinor();

            if (minor != 0) {
                area.deletePreviousChar();
            }
        }));

        Nodes.addInputMap(area, InputMap.consume(keyPressed(LEFT), e -> {
            int major = area.offsetToPosition(area.getCaretPosition(), Bias.Backward).getMajor();
            int minor = area.offsetToPosition(area.getCaretPosition(), Bias.Backward).getMinor();

            if (minor != 0) {
                area.moveTo(major, minor - 1);
            }
        }));

        Nodes.addInputMap(area, InputMap.consume(keyPressed(RIGHT), e -> {
            int major = area.offsetToPosition(area.getCaretPosition(), Bias.Forward).getMajor();
            int minor = area.offsetToPosition(area.getCaretPosition(), Bias.Forward).getMinor();

            if (minor != area.getParagraphLength(major)) {
                area.moveTo(major, minor + 1);
            }
        }));

        Nodes.addInputMap(area, InputMap.consume(keyPressed(UP), e -> {
            if (historyPointer == 0) {
                return;
            }
            historyPointer--;
            runSafe(() -> area.replaceText(area.getCurrentParagraph(), 0, area.getCurrentParagraph(),
                    area.getParagraphLength(area.getCurrentParagraph()), history.get(historyPointer)));
        }));

        Nodes.addInputMap(area, InputMap.consume(keyPressed(DOWN), e -> {
            if (historyPointer >= history.size() - 1) {
                return;
            }
            historyPointer++;
            runSafe(() -> area.replaceText(area.getCurrentParagraph(), 0, area.getCurrentParagraph(),
                    area.getParagraphLength(area.getCurrentParagraph()), history.get(historyPointer)));
        }));

        Nodes.addInputMap(area, InputMap.ignore(mouseClicked()));

        Nodes.addInputMap(area, InputMap.ignore(mouseReleased()));

        Nodes.addInputMap(area, InputMap.ignore(mousePressed()));

        Nodes.addInputMap(area, InputMap.ignore(mouseDragged()));

    }


    @Override
    public void requestFocus() {
        super.requestFocus();
        area.requestFocus();
    }

    public void setOnMessageReceivedHandler(final Consumer<String> onMessageReceivedHandler) {
        this.onMessageReceivedHandler = onMessageReceivedHandler;
    }

    public void println(String text, List<String> style) {
        runSafe(() -> {
            area.deleteText(area.getCurrentParagraph(), 0, area.getCurrentParagraph(), area.getParagraphLength(area.getCurrentParagraph()));
            area.setStyle(area.getCurrentParagraph(), style);
            area.appendText(text + System.lineSeparator());
            area.clearStyle(area.getCurrentParagraph());
        });
    }

    public void println(String text, List<String> style, int from, int to, List<String> styleFromTo) {
        runSafe(() -> {
            int paragraph = area.getCurrentParagraph();
            area.deleteText(paragraph, 0, paragraph, area.getParagraphLength(paragraph));
            area.appendText(text + System.lineSeparator());
            area.setStyle(paragraph, style);
            area.setStyle(paragraph, from, to, styleFromTo);
            area.clearStyle(area.getCurrentParagraph());
        });
    }

    public void println(String text, List<String> defaultStyle,
                        int a, int b, List<String> styleA,
                        int c, int d, List<String> styleB) {
        runSafe(() -> {
            area.deleteText(area.getCurrentParagraph(), 0, area.getCurrentParagraph(), area.getParagraphLength(area.getCurrentParagraph()));
            area.appendText(text);
            area.setStyle(area.getCurrentParagraph(), defaultStyle); // default style
            area.setStyle(area.getCurrentParagraph(), a, b, styleA); // style a
            area.setStyle(area.getCurrentParagraph(), c, d, styleB); //style b
            area.appendText(System.lineSeparator());
            area.clearStyle(area.getCurrentParagraph());
        });
    }

    public void lineSeparator() {
        runSafe(() -> area.appendText(System.lineSeparator()));
    }

    public void clear() {
        runSafe(area::clear);
    }

}
