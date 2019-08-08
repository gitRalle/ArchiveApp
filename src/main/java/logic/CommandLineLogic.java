// Todo: put in controllers?

package logic;

import javafx.scene.control.TreeView;
import objects.CommandPrompt;
import objects.CrawlType;
import objects.Execute;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

public class CommandLineLogic {
    private final CommandPrompt prompt;
    private final TreeView<String> treeView;
    private final HashMap<String, Execute> commands = initMap();
    private Crawler crawler = null;

    public CommandLineLogic(final CommandPrompt prompt,
                            final TreeView<String> treeView) {
        this.prompt = prompt;
        this.treeView = treeView;
    }

    public void execute(String inputText) {

        for (String str : commands.keySet()) {
            if (Pattern.matches(str, inputText)) {
                commands.get(str).execute(inputText);
                return;
            }
        }
        prompt.println("'" + inputText + "' is not recognized as an internal command\n", Collections.singletonList("syntax-error"));
    }

    private HashMap<String, Execute> initMap() {
        return new HashMap<>() {
            {
                put("^./crawl .* use: \\d{1,2}+, \\d{1,5}+, (true|false)$",              (input) -> crawl(input, Keyword.NONE));
                put("^./crawl .* use: \\d{1,2}+, \\d{1,5}+, (true|false) --stacktrace$", (input) -> crawl(input, Keyword.STACKTRACE));
                put("^./crawl .* use: \\d{1,2}+, \\d{1,5}+, (true|false) --as [^\\s]*$", (input) -> crawl(input, Keyword.AS));
                put("^./crawl init$", (input) -> {
                    if (crawler != null) {
                        crawler.init();
                    } else {
                        printNullReferenceMsg();
                    }
                });
                put("^./crawl data$", (input) -> {
                    if (crawler != null) {
                        crawler.printData(null);
                    } else {
                        printNullReferenceMsg();
                    }
                });
                put("^./crawl data --html$", (input) -> {
                   if (crawler != null) {
                       crawler.printData(CrawlType.HTML);
                   }  else {
                       printNullReferenceMsg();
                   }
                });
                put("^./crawl data --image$", (input) -> {
                   if (crawler != null) {
                       crawler.printData(CrawlType.IMAGE);
                   } else {
                       printNullReferenceMsg();
                   }
                });
                put("^./crawl shutdown$", (input) -> {
                    if (crawler != null) {
                        crawler.shutdown();
                    } else {
                        printNullReferenceMsg();
                    }
                });
                put("^clear$",                (input) -> prompt.clear());
                put("^date$",                 (input) -> printDate());
                put("^date [^-]*$",           (input) -> printDate(input, Keyword.NONE));
                put("^date .* --as [^\\s]*$", (input) -> printDate(input, Keyword.AS));
            }
        };
    }

    private void crawl(String input, Keyword keyword) {
        boolean stacktrace = false;
        String var = null;

        switch (keyword) {
            case STACKTRACE:
                stacktrace = true;
                input = input.split(" --stacktrace")[0];
                break;
            case AS:
                var = input.split(" --as ")[1];
                input = input.split(" --as ")[0];
                break;
            case NONE:
                break;
        }

        try {

            input = input.replace("./crawl ", "");
            final String URL_TO_CRAWL    = input.split(" use: ")[0];
            final int NUMBER_OF_CRAWLERS = Integer.parseInt(input.split(" use: ")[1].split(", ")[0]);
            final int NUMBER_OF_URLS     = Integer.parseInt(input.split(" use: ")[1].split(", ")[1]);
            final boolean IMAGES         = Boolean.parseBoolean(input.split(" use: ")[1].split(", ")[2]);


            if (keyword == Keyword.NONE || keyword == Keyword.STACKTRACE) {
                crawler = new Crawler(URL_TO_CRAWL, NUMBER_OF_CRAWLERS, NUMBER_OF_URLS, IMAGES,
                        stacktrace, prompt, treeView);
            }

            else if (keyword == Keyword.AS) {

                if (!commands.containsKey("^./" + var + " init$")) {

                    Crawler varCrawler = new Crawler(URL_TO_CRAWL, NUMBER_OF_CRAWLERS, NUMBER_OF_URLS, IMAGES,
                            false, prompt, treeView);

                    commands.putIfAbsent("^./" + var + " init$",         (text) -> varCrawler.init());
                    commands.putIfAbsent("^./" + var + " data$",         (text) -> varCrawler.printData(null));
                    commands.putIfAbsent("^./" + var + " data --html$",  (text) -> varCrawler.printData(CrawlType.HTML));
                    commands.putIfAbsent("^./" + var + " data --image$", (text) -> varCrawler.printData(CrawlType.IMAGE));
                    commands.putIfAbsent("^./" + var + " shutdown$",     (text) -> varCrawler.shutdown());
                }

                else {
                    prompt.println("var '" + var + "' is already in use\n", Collections.singletonList("syntax-warning"));
                }
            }
        }
        catch (IllegalArgumentException ex) {
            prompt.println(ex.getMessage() + "\n", Collections.singletonList("syntax-warning"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void printDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
        String now = LocalDateTime.now().format(formatter);

        prompt.println(now + "\n", Collections.singletonList("syntax-output"));

    }

    private void printDate(String input, Keyword keyword) {
        String pattern = null;
        String var = null;

        if (keyword == Keyword.AS) {
            var   = input.split(" --as ")[1];
            input = input.split(" --as ")[0];
        }

        try {

            pattern = input.split("^date ")[1];
            String args = pattern;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String now = LocalDateTime.now().format(formatter);

            if (keyword == Keyword.NONE) {

                prompt.println(now + "\n", Collections.singletonList("syntax-output"));

            }
            else if (keyword == Keyword.AS) {

                String regex = "^" + var + "$";

                if (!commands.containsKey(regex)) {

                    prompt.println(now + "\n", Collections.singletonList("syntax-output"));
                    commands.putIfAbsent(regex, (text) -> printDate("date " + args, Keyword.NONE));
                }
                else {
                    prompt.println("var '" + var + "' is already in use\n", Collections.singletonList("syntax-warning"));
                }
            }

        }

        catch (IllegalArgumentException ex) {
            prompt.println("'" + pattern + "' threw an IllegalArgumentException\n", Collections.singletonList("syntax-warning"));
        }
    }



    private void printNullReferenceMsg() {
        prompt.println("there are currently no active crawlers -> ./crawl --help\n", Collections.singletonList("syntax-warning"),
                42, 56, Collections.singletonList("syntax-reference"));
    }

    public enum Keyword {
        NONE,
        AS,
        SAVE_AS,
        STACKTRACE
    }
}
