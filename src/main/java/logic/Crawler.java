package logic;

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
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.IO.writeRootUrl;

import utils.CrawlUtils;


/**
 * Crawler class holds meta data needed for the nestled Crawl and Write classes to run properly
 * also holds all references for Crawl and Write threads.
 */
public class Crawler {
    // crawl objects
    private final String rootPage;
    private Set<String> urlsToCrawl;
    private UniqueConcurrentArrayList<String> uniqueImageUrls;
    private final int NUMBER_OF_CRAWLERS;
    private final int NUMBER_OF_URLS;
    private final boolean DOWNLOAD_IMAGES;
    private final ExecutorService crawlThreadPool;
    private final ExecutorService writeThreadPool;
    private ExecutorService imageThreadPool = null;
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
    private AtomicInteger iteratorCount = new AtomicInteger(0);
    private AtomicInteger imageIteratorCount = new AtomicInteger(0);
    private final String crawlInitDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private String crawlExeDate = null;

    public Crawler(String rootPage, int nrOfCrawlers, int nrOfUrls, boolean downloadImages, final boolean stacktrace,
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
        crawlThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        writeThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        STACKTRACE = stacktrace;

        dir = new Directory(rootPage); // can throw exceptions
        dataTracker = new ConcurrentDataTracker();
        this.prompt = prompt;
        this.treeView = treeView;

        prompt.println("ready -> ./<crawl> init\n", Collections.singletonList("yellow-green-text"),
                9, 23, Collections.singletonList("sky-blue-text"));
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
                        crawlThreadPool.submit(new Crawl(nextUrl));
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

                if (DOWNLOAD_IMAGES) {

                    Elements images = doc.select("picture source[srcSet], img[srcSet]");
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

                    images = doc.select("img[src]");
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

                writeThreadPool.submit(new Write(doc));
            } catch (IOException ex) {
                if (STACKTRACE) {
                    prompt.println(ex.getMessage() + ": " + urlToCrawl + "\n", Collections.singletonList("red-text"));
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

                try {
                    FileWriter writer = new FileWriter(new File(dir.getHtmlFolder().getAbsolutePath() + "\\" + document.location().hashCode() + ".html"));
                    writer.write(document.html());
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    iteratorCount.incrementAndGet();
                }

            } else { // end of iteratorCount <= NUMBER_OF_URLS

                if (!writeThreadPool.isShutdown()) {
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

        public ImageDownloader(String url) {
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
                if (STACKTRACE) {
                    prompt.println(
                            ex.getMessage() + ": " + url + "[." + CrawlUtils.getExtensionType(url) + "]\n",
                            Collections.singletonList("red-text"));
                }
            } finally {
                imageIteratorCount.incrementAndGet();
            }
        }
    }

    /**
     * downloads all stylesheets found in rootPage, called by this::init prior to crawl start.
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * shuts down the crawling process - called by Runnable Writer or by user if called through gui
     */
    private synchronized void shutdownExecutorService() {
        crawlThreadPool.shutdownNow();
        writeThreadPool.shutdownNow();
        setTimePassed(CrawlType.HTML);

        if (DOWNLOAD_IMAGES) {
            // wonky solution attempt ->
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.execute(() -> {
                imageStartTime = System.nanoTime();
                imageThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);

                for (String url : uniqueImageUrls) {
                    imageThreadPool.submit(new ImageDownloader(url));
                }
                imageThreadPool.shutdown();

                try {
                    imageThreadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
                    setTimePassed(CrawlType.IMAGE);
                    String timeElapsed = String.valueOf(Math.round((System.nanoTime() - startTime) * Math.pow(10, -9)));
                    String promptOutput = "crawl executed in " + timeElapsed + " s -> ./crawl status";
                    prompt.println(promptOutput,
                            Collections.singletonList("gray-text"),
                            0, 14, Arrays.asList("yellow-text", "big-font"),
                            promptOutput.length() - 14, promptOutput.length(), Collections.singletonList("sky-blue-text"));
                    prompt.lineSeparator();
                  //  updateControllerTreeView();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            executorService.shutdown();

        } // end of DOWNLOAD_IMAGES

        else {
            String timeElapsed = String.valueOf(Math.round((System.nanoTime() - startTime) * Math.pow(10, -9)));
            String promptOutput = "crawl executed in " + timeElapsed + " s -> ./crawl status";
            prompt.println(promptOutput,
                    Collections.singletonList("gray-text"),
                    0, 14, Arrays.asList("yellow-text", "big-font"),
                    promptOutput.length() - 14, promptOutput.length(), Collections.singletonList("sky-blue-text"));
            prompt.lineSeparator();
        }
        crawlExeDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      //  updateControllerTreeView();
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

            // Todo: -> Throws NullPointerException! fix!
            for (TreeItem<String> currentDomainItem : treeView.getRoot().getChildren()) {
                if (currentDomainItem.getValue().equals(dir.getName())) {
                    action = "year + month + day";
                    domainTreeItem = currentDomainItem;

                    for (TreeItem<String> currentYearItem : currentDomainItem.getChildren()) {
                        if (currentYearItem.getValue().equals(dir.getYear())) {
                            action = "month + day";
                            yearTreeItem = currentYearItem;

                            for (TreeItem<String> currentMonthItem : currentYearItem.getChildren()) {
                                if (currentMonthItem.getValue().equals(dir.getMonth())) {
                                    // ADD ONLY DAY
                                    action = "day";
                                    monthTreeItem = currentMonthItem;
                                    dayTreeItem = new TreeItem<>(dir.getDay());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            switch (action) {
                case "day":
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("TH", 2)[0])));
                    break;
                case "month + day":
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("TH", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    break;
                case "year + month + day":
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("TH", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    domainTreeItem.getChildren().add(yearTreeItem);
                    domainTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
                    break;
                case "domain + year + month + day":
                    monthTreeItem.getChildren().add(dayTreeItem);
                    monthTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue().split("TH", 2)[0])));
                    yearTreeItem.getChildren().add(monthTreeItem);
                    yearTreeItem.getChildren().sort(Comparator.comparingInt(o -> Month.valueOf(o.getValue()).getValue()));
                    domainTreeItem.getChildren().add(yearTreeItem);
                    domainTreeItem.getChildren().sort(Comparator.comparingInt(o -> Integer.parseInt(o.getValue())));
                    treeView.getRoot().getChildren().add(domainTreeItem);
                    treeView.getRoot().getChildren().sort(Comparator.comparing(TreeItem::getValue));
                    break;
                default:
                    System.err.println("error updating controllerTreeView");
            }

        });
    }


    /**
     * private helper method, sets time elapsed since start of param operation.
     */
    private void setTimePassed(CrawlType crawlType) {
        if (crawlType == CrawlType.HTML)
            timePassed = System.nanoTime() - startTime;

        else if (crawlType == CrawlType.IMAGE)
            imageTimePassed = System.nanoTime() - imageStartTime;
    }

    /* public methods ->
    ---------------------------------------------------------------------------------------------------*/

    /**
     * starts the crawling process
     */
    public void init() {
        initialized = true;

        writeRootUrl(rootPage, dir.getHomeFolder().getAbsolutePath());
        downloadStylesheets();

        startTime = System.nanoTime();
        prompt.println("root dependencies setup finished -> crawl initiated\n", Collections.singletonList("yellow-green-text"));
        crawlThreadPool.submit(new Crawl(rootPage));
    }

    /**
     * shuts down the crawling process if is not already threadsTerminated - called by fxApplicationThread
     */
    public void shutdown() {
        if (initialized) {
            if (!crawlThreadPool.isTerminated() || (imageThreadPool != null && !imageThreadPool.isTerminated())) {
                shutdownExecutorService();
            } else {
                prompt.println("crawler is already shutdown\n", Collections.singletonList("orange-text"));
            }
        }
        else {
            prompt.println("crawler has not been initialized\n", Collections.singletonList("orange-text"));
        }
    }

    /**
     * appends the console component with data on current or most recent crawl
     */
    public void printCrawlData() {
        if (initialized) {
            if (!crawlThreadPool.isTerminated()) {
                setTimePassed(CrawlType.HTML);
            }
            prompt.println(dataTracker.getCrawlData(
                    timePassed, iteratorCount.get(), NUMBER_OF_URLS, CrawlType.HTML) + "\n", Collections.singletonList("yellow-green-text"));
        }
        else {
            prompt.println("crawler has not been initialized\n", Collections.singletonList("orange-text"));
        }

    }

    public void printImageData() {
        if (initialized) {
            if (imageThreadPool != null && !imageThreadPool.isTerminated()) {
                setTimePassed(CrawlType.IMAGE);
            }
            if (DOWNLOAD_IMAGES) {
                prompt.println(dataTracker.getCrawlData(
                        imageTimePassed, imageIteratorCount.get(), uniqueImageUrls.size(), CrawlType.IMAGE) + "\n", Collections.singletonList("yellow-green-text"));
            } else {
                prompt.println("crawler has not been instructed to crawl for images\n", Collections.singletonList("orange-text"));
            }
        }
        else {
            prompt.println("crawler has not been initialized\n", Collections.singletonList("orange-text"));
        }

    }

    public void printData() {
        if (initialized) {
            String str = "\n";

            if (!crawlThreadPool.isTerminated()) {
                setTimePassed(CrawlType.HTML);
            }

            if (DOWNLOAD_IMAGES) {
                str = "";
            }

            prompt.println(dataTracker.getCrawlData(
                    timePassed, iteratorCount.get(), NUMBER_OF_URLS, CrawlType.HTML) + str, Collections.singletonList("yellow-green-text"));

            if (imageThreadPool != null && !imageThreadPool.isTerminated()) {
                setTimePassed(CrawlType.IMAGE);
            }

            if (DOWNLOAD_IMAGES) {
                prompt.println(dataTracker.getCrawlData(
                        imageTimePassed, imageIteratorCount.get(), uniqueImageUrls.size(), CrawlType.IMAGE) + "\n", Collections.singletonList("yellow-green-text"));
            }
        }
        else {
            prompt.println("crawler has not been initialized\n", Collections.singletonList("orange-text"));
        }
    }

    public void printStatus() {
        if (initialized) {
            prompt.println(rootPage + ", " + NUMBER_OF_CRAWLERS + ", " + NUMBER_OF_URLS + ", " + DOWNLOAD_IMAGES,
                    Collections.singletonList("yellow-green-text"));
            prompt.println("dir setup on: " + dir.getDirInitDate(), Collections.singletonList("sky-blue-text"));
            prompt.println("crawl init on: " + crawlInitDate, Collections.singletonList("sky-blue-text"));
            if (crawlExeDate != null) {
                prompt.println("crawl exe on: " + crawlExeDate + "\n", Collections.singletonList("yellow-text"));
            }
            else {
                prompt.lineSeparator();
            }
        }
        else {
            prompt.println("crawler has not been initialized\n", Collections.singletonList("orange-text"));
        }
    }

}


