package logic;

import GUI.Console;
import javafx.scene.control.TreeView;
import objects.Crawler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/** ArchiveLogic's main area of responsibility is acting as to go-between, between the GUI class and the Crawler class.
 */
public class ArchiveLogic {
    private final Console console; // GUI component
    private final TreeView<String> treeView; // GUI component
    private Crawler crawler;
    private ArrayList<String> args = new ArrayList<>();

    public ArchiveLogic(Console console, TreeView<String> treeView) {
        this.console = console;
        this.treeView = treeView;
    }


    private String checkForArgs(String input) {
        String returnVal = input;


        if (!input.equals("crawl status") && !input.equals("crawl help") && !input.equals("crawl shutdown") &&
        !input.equals("crawl status --html") && !input.equals("crawl status --image")) {
            if (input.length() >= 6) {
                if (input.substring(0, 6).equals("crawl ")) {
                    String afterCrawl = input.substring(6);
                    StringBuilder arg = new StringBuilder();
                    /* url crawlers urls */

                    for (int i = 0; i < afterCrawl.length(); i++) {
                        if (afterCrawl.charAt(i) != ' ') {
                            arg.append(afterCrawl.charAt(i));
                        }
                        else {
                            args.add(new String(arg));
                            arg = new StringBuilder();
                        }
                    }
                    args.add(new String(arg));

                    returnVal = "crawl";
                }
            }
        }

        if (input.equals("crawl")) {
            returnVal = "crawl help";
        }


        return returnVal;
    }

    public void runCommand(String input) {
        args.clear();
        input = checkForArgs(input);


        switch (input) {
            case "crawl":
               crawl();
                break;
            case "crawl shutdown":
                crawlShutdown(); // TODO: needs to include image shutdown
                break;
            case "crawl status":
                crawlStatus();
                break;
            case "crawl status --html":
                crawlStatusHtml();
                break;
            case "crawl status --image":
                crawlStatusImage();
                break;
            case "crawl help":
                crawlHelp();
                break;
            case "time":
                time();
                break;
            case "date":
                date();
                break;
            case "clear":
                console.clear();
                break;
            default:
                console.println("is not a valid command, type help for more information on acceptable commands");
        }

    }

    /** initiates a crawl if args.size() == 4 and args content is parsed successfully
     * @catches NumberFormatException if args content cannot be parsed to int
     * @catches MalformedURLException from Directory_version_1 -> Crawler -> ArchiveLogic.crawl() if urls doesn't adhere to required format.
     * See Directory_version_1 Class for url format requirements.
     * @catches IllegalArgumentException from Directory_version_1 -> Crawler -> ArchiveLogic.crawl() if Directory_version_1 files fails to init.
     */
    private void crawl() {
        if (args.size() != 4) {
            runCommand("default");
        }

        else {
            try {

                final String  URL            = args.get(0);
                final int     NR_OF_CRAWLERS = Integer.parseInt(args.get(1));
                final int     NR_OF_URLS     = Integer.parseInt(args.get(2));
                final boolean IMAGES         = Boolean.parseBoolean(args.get(3));

                crawler = new Crawler(URL, NR_OF_CRAWLERS, NR_OF_URLS, IMAGES, console, treeView);
                crawler.init();

            } catch (NumberFormatException ex) {
                runCommand("crawl help");
            }

            catch (IOException ex) {
                console.println(ex.getMessage());
            }
        }
    }

    private void crawlShutdown() {
        if (crawler != null) {
            crawler.shutdown();
        }

        else {
            console.println("no currently active crawlers");
        }
    }

    private void crawlStatus() {
        if (crawler != null) {
            if (crawler.isDOWNLOAD_IMAGES()) {
                crawler.printlnData();
                crawler.printlnImageData();
                console.println("...........................................................................");
            }
            else {
                crawler.printlnData();
            }
        }
        else {
            console.println("no currently active crawlers");
        }
    }

    private void crawlStatusHtml() {
        if (crawler != null) {
            crawler.printlnData();
            console.println("...........................................................................");
        }
        else {
            console.println("no currently active crawlers");
        }
    }

    private void crawlStatusImage() {
        if (crawler != null) {
            if (crawler.isDOWNLOAD_IMAGES()) {
                crawler.printlnImageData();
                console.println("...........................................................................");
            }
            else {
                console.println("no currently active image crawlers");
            }
        }
        else {
            console.println("no currently active crawlers");
        }
    }


    private void crawlHelp() {
        console.println("to initiate a crawl: crawl <url> <nr_of_crawlers> <nr_of_urls> <incl_images>");
    }

    private void time() {
        console.println(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void date() {
        console.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }


    public static String convertSrcSetToUrl (String srcSet){
        char[] src = srcSet.toCharArray();
        StringBuilder url = new StringBuilder();

        for (int i = 0; i < src.length; i++) {
            if (src[i] != ' ')
                url.append(src[i]);
            else
                break;
        }

        return new String(url);
    }

    public static String getExtensionType (String url){
        char[] src = url.toCharArray();
        StringBuilder fileExtension = new StringBuilder();
        int count = 0;

        for (int i = 0; i < src.length; i++) {
            fileExtension.append(src[i]);
            count++;

            if (src[i] == '.') {
                fileExtension = new StringBuilder();
                count = 0;
            }

            if (src[i] == ' ')
                break;
        }

        // jpg, png, gif, jpeg, svg
        if (count != 3 && count != 4 || new String(fileExtension).equals("svg")) {
            fileExtension = new StringBuilder("noContentType");
        }

        return new String(fileExtension);
    }
}



