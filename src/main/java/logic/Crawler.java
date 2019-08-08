package logic;

import io.IO;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import objects.CommandPrompt;
import objects.CrawlType;
import objects.UniqueConcurrentArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.IO.writeRootUrl;

import utils.CrawlUtils;
import utils.FontUtils;

/**
 * Crawler class holds meta data needed for the nestled Crawl and Write classes to run properly
 * also holds all references for Crawl and Write threads.
 */
    class Crawler {
    // crawl objects
    private final String rootPage;
    private Set<String> urlsToCrawl;
    private UniqueConcurrentArrayList<String> uniqueImageUrls;
    private final int NUMBER_OF_CRAWLERS;
    private final int NUMBER_OF_URLS;
    private final boolean DOWNLOAD_IMAGES;
    private final ExecutorService cThreadPool;
    private final ExecutorService wThreadPool;
    private ExecutorService iThreadPool = null;
    private final Directory dir;
    private final boolean STACKTRACE;
    private boolean initialized = false;
    private final String userAgent = "Mozilla/10.0 (Windows NT 10.0) AppleWebKit/538.36 (KHTML, like Gecko) Chrome/69.420 Safari/537.36";

    // gui components TODO: look into moving there elsewhere?
    private final CommandPrompt prompt;
    private final TreeView<String> treeView;

    // Objects used to track iterations and time elapsed.
    private final ConcurrentDataTracker dataTracker;
    private long startTime;
    private long imageStartTime;
    private long timePassed;
    private long imageTimePassed = 0;
    private AtomicBoolean coreThreadsAlive = null;
    private AtomicBoolean imageThreadsAlive = null;
    private AtomicInteger iteratorCount = new AtomicInteger(0);
    private AtomicInteger exceptionCount = new AtomicInteger(0);
    private AtomicInteger imageIteratorCount = new AtomicInteger(0);
    private AtomicInteger imageExceptionCount = new AtomicInteger(0);
    private String crawlInitDate;
    private String crawlExeDate = null;

    Crawler(String rootPage, int nrOfCrawlers, int nrOfUrls, boolean downloadImages, final boolean stacktrace,
            CommandPrompt prompt, TreeView<String> treeView) throws IOException, IllegalArgumentException {
        this.rootPage = rootPage;
        urlsToCrawl = Collections.newSetFromMap(new ConcurrentHashMap<>(10000)); // initialCapacity: Math.round(1.25f * nrOfUrls) / threadsTerminated crawlers at max?
        uniqueImageUrls = new UniqueConcurrentArrayList<>();
        if (nrOfCrawlers > 10)
            throw new IllegalArgumentException(
                    "max 10 threads / threadPool"
            );
        NUMBER_OF_CRAWLERS = nrOfCrawlers;
        NUMBER_OF_URLS = nrOfUrls;
        DOWNLOAD_IMAGES = downloadImages;
        cThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        wThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        STACKTRACE = stacktrace;

        dir = new Directory(rootPage); // can throw exceptions
        dataTracker = new ConcurrentDataTracker();
        this.prompt = prompt;
        this.treeView = treeView;

        this.prompt.println("ready -> ./<crawl> init\n", Collections.singletonList("syntax-output"),
                9, 23, Collections.singletonList("syntax-reference"));
    }

    /**
     * Crawl class implements interface Runnable and will be instantiated for every unique url found
     */
    private class Crawl implements Runnable {
        private final String urlToCrawl;

        private Crawl(String urlToCrawl) {
            this.urlToCrawl = urlToCrawl;
        }

        @Override
        public void run() {
            try {

                Document doc =
                        Jsoup.connect(urlToCrawl).
                                timeout(60 * 1000).
                                userAgent(userAgent).
                                get();

                // get all a[href]
                Elements anchors = doc.select("a[href]");
                // loop over anchor collection
                for (Element currentAnchor : anchors) {
                    // get absolute url from currentAnchor
                    String nextUrl = currentAnchor.absUrl("href");
                    // set href value to point towards future local html file
                    currentAnchor.attr("href", nextUrl.hashCode() + ".html");
                    // ignore certain values
                    if (nextUrl.startsWith("#") || !nextUrl.contains(rootPage)) {
                        continue;
                    }
                    // if url hasn't already been saved -> submit a new Runnable
                    if (urlsToCrawl.add(nextUrl)) {
                        cThreadPool.submit(new Crawl(nextUrl));
                    }
                }
                // get all link[rel=stylesheet]
                Elements links = doc.select("link[rel=stylesheet]");
                // loop over link collection
                for (Element currentLink : links) {
                    // get absolute url from currentLink
                    String absUrl = currentLink.absUrl("href");
                    // set href value to point towards local css file
                    currentLink.attr("href", "../assets/css/" + absUrl.hashCode() + ".css");
                }

                wThreadPool.submit(new Write(doc));

            } catch (IOException ex) {
                if (STACKTRACE) {
                    prompt.println(ex.getMessage() + ": " + urlToCrawl + "\n", Collections.singletonList("syntax-error"));
                }
            }
        }
    }

    /**
     * Write class is responsible for writing a crawled JSoup Document to file
     */
    private class Write implements Runnable {
        private final Document document;

        private Write(Document document) {
            this.document = document;
        }

        @Override
        public void run() {
            if (iteratorCount.get() <= (NUMBER_OF_URLS - NUMBER_OF_CRAWLERS)) {

                if (DOWNLOAD_IMAGES) {

                    Elements images = document.select("picture source[srcSet], img[srcSet]");
                    for (Element e : images) {
                        String url;

                        if (e.attr("src").equals("") && !e.attr("srcSet").equals("")) {
                            url = CrawlUtils.convertSrcSetToUrl(e.attr("srcSet"));
                        } else if (!e.attr("src").equals("") && e.attr("srcSet").equals("")) {
                            url = e.attr("src");
                        } else {
                            url = CrawlUtils.convertSrcSetToUrl(e.attr("srcSet"));
                        }

                        e.parent().append("<img src=" + '"' + url + '"' + ">");
                        e.remove();
                    }

                    images = document.select("img[src]");
                    for (Element img : images) {
                        String url = img.absUrl("src");

                        String extensionType = CrawlUtils.getExtensionType(url);

                        if (!extensionType.equals("noContentType")) {
                            img.attr("src", "../assets/images/" + url.hashCode() + "." + extensionType);
                            uniqueImageUrls.add(url);
                        } else {
                            img.attr("alt", "noContentType found");
                        }

                    }
                } // end of if (DOWNLOAD_IMAGES)

                try (FileWriter writer = new FileWriter(
                        new File(dir.getHtmlFolder().getAbsolutePath() + "\\" + document.location().hashCode() + ".html"))) {
                    writer.write(document.html());
                }
                catch (IOException ex) {
                    exceptionCount.incrementAndGet();
                    ex.printStackTrace();
                }
                finally {
                    iteratorCount.incrementAndGet();
                }

            } else { // end of iteratorCount <= NUMBER_OF_URLS

                if (coreThreadsAlive.get()) {
                    shutdownExecutorService(); // is synchronized
                }
            }
        }
    }

    /**
     * ImageDownloader is responsible for downloading an Image from a URL
     */
    private class ImageDownloader implements Runnable {
        private final String url;

        private ImageDownloader(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {

                byte[] bytes = Jsoup.connect(url).
                        timeout(60 * 1000).
                        userAgent("Mozilla").
                        ignoreContentType(true).
                        execute().
                        bodyAsBytes();

                ByteBuffer buffer = ByteBuffer.wrap(bytes);

                String fileName = dir.getImagesFolder().getAbsolutePath() + "\\" + url.hashCode() + "." + CrawlUtils.getExtensionType(url);

                String[] fileNameParts = fileName.split("\\.");
                String format = fileNameParts[fileNameParts.length - 1];

                File file = new File(fileName);
                BufferedImage bufferedImage;

                InputStream in = new ByteArrayInputStream(buffer.array());
                bufferedImage = ImageIO.read(in);
                ImageIO.write(bufferedImage, format, file);

            } catch (IOException | IllegalArgumentException ex) {
                imageExceptionCount.incrementAndGet();
                if (STACKTRACE) {
                    prompt.println(
                            ex.getMessage() + ": " + url + "[." + CrawlUtils.getExtensionType(url) + "]\n",
                            Collections.singletonList("syntax-error"));
                }
            } finally {
                imageIteratorCount.incrementAndGet();
            }
        }
    }

    /** downloads all stylesheets found in rootPage, called by this::init prior to crawl start.
     */
    private void downloadStylesheets() {
        try {

            Document doc = Jsoup.
                    connect(rootPage).
                    timeout(30 * 1000).
                    userAgent(userAgent).
                    get();

            Elements links = doc.select("link[rel=stylesheet]");

            for (Element currentLink : links) {
                String absUrl = currentLink.absUrl("href");
                File outputFile = new File(dir.getCssFolder().getAbsolutePath() + "\\" + absUrl.hashCode() + ".css");
                BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(absUrl).openStream()));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                }
                reader.close();
                writer.close();
            }
        }
        catch (UnknownHostException ex) {

        }

        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * shuts down the crawling process - called by Runnable Writer or by user if called through gui
     */
    private synchronized void shutdownExecutorService() {
        cThreadPool.shutdownNow();
        wThreadPool.shutdownNow();
        coreThreadsAlive.set(false);
        setTimePassed(CrawlType.HTML);

        if (DOWNLOAD_IMAGES) {
            // wonky solution attempt ->
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.execute(() -> {
                imageStartTime = System.nanoTime();
                iThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
                imageThreadsAlive = new AtomicBoolean(true);

                for (String url : uniqueImageUrls) {
                    iThreadPool.submit(new ImageDownloader(url));
                }
                iThreadPool.shutdown();

                try {
                    iThreadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
                    imageThreadsAlive.set(false);
                    setTimePassed(CrawlType.IMAGE);
                    crawlExeDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    updateControllerTreeView();
                    log();
                    printExeMsg(timePassed + imageTimePassed);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            executorService.shutdown();

        } // end of DOWNLOAD_IMAGES

        else {
            crawlExeDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            updateControllerTreeView();
            log();
            printExeMsg(timePassed);
        }
    }

    /**
     * updates the gui's treeView to display updated domains, runs after crawl finishes.
     */
    private synchronized void updateControllerTreeView() {
        // block-head solution ->
        Platform.runLater(() -> {

            String action = "domain + year + month + day";
            TreeItem<String> domainTreeItem = new TreeItem<>(dir.getName());
            TreeItem<String> yearTreeItem = new TreeItem<>(dir.getYear());
            TreeItem<String> monthTreeItem = new TreeItem<>(dir.getMonth());
            TreeItem<String> dayTreeItem = new TreeItem<>(dir.getDay());

            for (TreeItem<String> currentDomainItem : treeView.getRoot().getChildren()) {
                if (currentDomainItem.getValue().equals(dir.getName())) {
                    // domain treeItem already exists
                    action = "year + month + day";
                    domainTreeItem = currentDomainItem;

                    for (TreeItem<String> currentYearItem : currentDomainItem.getChildren()) {
                        if (currentYearItem.getValue().equals(dir.getYear())) {
                            // year treeItem already exists
                            action = "month + day";
                            yearTreeItem = currentYearItem;

                            for (TreeItem<String> currentMonthItem : currentYearItem.getChildren()) {
                                if (currentMonthItem.getValue().equals(dir.getMonth())) {
                                    // month threeItem already exists ->
                                    // ADD ONLY DAY
                                    action = "day";
                                    monthTreeItem = currentMonthItem;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            switch (action) {
                case "day":
                    dayTreeItem.setGraphic(FontUtils.createLeafView());
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("(ST|ND|RD|TH)", 2)[0])));
                    break;
                case "month + day":
                    dayTreeItem.setGraphic(FontUtils.createLeafView());
                    monthTreeItem.setGraphic(FontUtils.createBranchView());
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("(ST|ND|RD|TH)", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    break;
                case "year + month + day":
                    dayTreeItem.setGraphic(FontUtils.createLeafView());
                    monthTreeItem.setGraphic(FontUtils.createBranchView());
                    yearTreeItem.setGraphic(FontUtils.createBranchView());
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("(ST|ND|RD|TH)", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    domainTreeItem.getChildren().add(yearTreeItem);
                    domainTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
                    break;
                case "domain + year + month + day":
                    dayTreeItem.setGraphic(FontUtils.createLeafView());
                    monthTreeItem.setGraphic(FontUtils.createBranchView());
                    yearTreeItem.setGraphic(FontUtils.createBranchView());
                    domainTreeItem.setGraphic(FontUtils.createRootView());
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("(ST|ND|RD|TH)", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    domainTreeItem.getChildren().add(yearTreeItem);
                    domainTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
                    treeView.getRoot().getChildren().add(domainTreeItem);
                    treeView.getRoot().getChildren().sort(Comparator.comparing(TreeItem::getValue));
                    break;
                default:
                    prompt.println("failed to update the TreeView\n", Collections.singletonList("syntax-error"));
            }

        });
    }

    private void log() {
        if (initialized) {
            if (crawlExeDate != null) {
                boolean success = IO.writeLog(dir.getHomeFolder().getAbsolutePath(),
                        dataTracker.getLogData(
                                timePassed + imageTimePassed,
                                imageIteratorCount.get() - imageExceptionCount.get(),
                                dir.getDirInitDate(), dir.getHtmlFolder().getAbsolutePath()));
                if (success) {
                } else {
                    prompt.println("failed to log crawl\n", Collections.singletonList("syntax-error"));
                }
            }
            else {
            }
        }
        else {
            printInitMsg();
        }
    }

    /** used for keeping track of time elapsed, since this::init called
     */
    private void setTimePassed(CrawlType crawlType) {
        if (crawlType == CrawlType.HTML)
            timePassed = System.nanoTime() - startTime;

        else if (crawlType == CrawlType.IMAGE)
            imageTimePassed = System.nanoTime() - imageStartTime;
    }

    /* public methods ->
    ---------------------------------------------------------------------------------------------------*/

    /** initiates and starts a crawl
     */
    void init() {
        initialized = true;
        crawlInitDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        writeRootUrl(rootPage, dir.getHomeFolder().getAbsolutePath());
        downloadStylesheets();

        startTime = System.nanoTime();
        prompt.println("root dependencies setup finished -> crawl initiated\n", Collections.singletonList("syntax-output"));
        coreThreadsAlive = new AtomicBoolean(true);
        cThreadPool.execute(new Crawl(rootPage));
    }

    /** invokes a shutdown of all non image threads
     */
    void shutdown() {
        if (initialized) {
            if (coreThreadsAlive.get() || (imageThreadsAlive != null && imageThreadsAlive.get())) {
                prompt.println("shutdown invoked\n", Collections.singletonList("syntax-output"));
                shutdownExecutorService();
            } else {
                prompt.println("crawler is already shutdown\n", Collections.singletonList("syntax-warning"));
            }
        } else {
            printInitMsg();
        }
    }

    void printData(CrawlType crawlType) {

        if (crawlType == CrawlType.HTML) {

            if (initialized) {
                if (coreThreadsAlive.get()) {
                    setTimePassed(CrawlType.HTML);
                }
                prompt.println(dataTracker.getPrintData(
                        timePassed, NUMBER_OF_URLS, dir, CrawlType.HTML) + "\n",
                        Collections.singletonList("syntax-output"));


            } else {
                printInitMsg();
            }
        }

        else if (crawlType == CrawlType.IMAGE) {

            if (initialized) {
                if (imageThreadsAlive != null && imageThreadsAlive.get()) {
                    setTimePassed(CrawlType.IMAGE);
                }
                if (DOWNLOAD_IMAGES) {
                    prompt.println(dataTracker.getPrintData(
                            imageTimePassed, uniqueImageUrls.size(), dir, CrawlType.IMAGE) + "\n",
                            Collections.singletonList("syntax-output"));
                } else {
                    prompt.println("crawler has not been instructed to crawl for images\n", Collections.singletonList("syntax-warning"));
                }
            } else {
                printInitMsg();
            }
        }

        else {

            if (initialized) {
                String str = "\n";

                if (coreThreadsAlive.get()) {
                    setTimePassed(CrawlType.HTML);
                }

                if (DOWNLOAD_IMAGES) {
                    str = "";
                }

                prompt.println(dataTracker.getPrintData(
                        timePassed, NUMBER_OF_URLS, dir, CrawlType.HTML) + str,
                        Collections.singletonList("syntax-output"));

                if (imageThreadsAlive != null && imageThreadsAlive.get()) {
                    setTimePassed(CrawlType.IMAGE);
                }

                if (DOWNLOAD_IMAGES) {
                    prompt.println(dataTracker.getPrintData(
                            imageTimePassed, uniqueImageUrls.size(), dir, CrawlType.IMAGE) + "\n",
                            Collections.singletonList("syntax-output"));
                }
            } else {
                printInitMsg();
            }
        }
    }

    private void printInitMsg() {
        prompt.println("crawler has not been initialized\n", Collections.singletonList("syntax-warning"));
    }

    private void printExeMsg(long timePassed) {
        String promptOutput = String.format("crawl executed in %.1f s -> ./crawl data", (timePassed * Math.pow(10, -9)));
        prompt.println(promptOutput,
                Collections.singletonList("syntax-filler"),
                0, 14, Collections.singletonList("syntax-execution"),
                promptOutput.length() - 12, promptOutput.length(), Collections.singletonList("syntax-reference"));
        prompt.lineSeparator();
    }

}


