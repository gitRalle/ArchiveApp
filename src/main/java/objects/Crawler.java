package objects;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import GUI.Console;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static IO.IO.writeRootUrl;
import static logic.ArchiveLogic.convertSrcSetToUrl;
import static logic.ArchiveLogic.getExtensionType;

/**
 * Crawler class holds meta data needed for the nestled Crawl and Write classes to run properly
 * also holds all references for Crawl and Write threads.
 */
public class Crawler {
    // 'Crawl' Objects
    private final String rootPage;
    private Set<String> visitedUrls;
    private UniqueConcurrentArrayList<String> imgList;
    private final int NUMBER_OF_CRAWLERS;
    private final int NUMBER_OF_URLS;
    private final boolean DOWNLOAD_IMAGES;
    private final ExecutorService crawlPool;
    private final ExecutorService writePool;
    private final Directory dir;

    // GUI components TODO: look into moving there elsewhere?
    private final Console console;
    private final TreeView<String> treeView;

    // Objects used to track iterations and time elapsed.
    private final ConcurrentDataTracker dataTracker;
    private long startTime;
    private long imageStartTime;
    private long timePassed;
    private long imageTimePassed = 0;
    private AtomicInteger iteratorCount;
    private AtomicBoolean threadsTerminated;
    private AtomicBoolean imageThreadsTerminated;

    public Crawler(String rootPage, int nrOfCrawlers, int nrOfUrls, boolean downloadImages,
                   Console console, TreeView<String> treeView) throws IOException, IllegalArgumentException {
        this.rootPage = rootPage;
        visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>(10000)); // initialCapacity: Math.round(1.25f * nrOfUrls) / threadsTerminated crawlers at max?
        imgList = new UniqueConcurrentArrayList<String>();
        if (nrOfCrawlers > 10)
            throw new IllegalArgumentException(
                    "max 10 threads / threadPool"
            );
        NUMBER_OF_CRAWLERS = nrOfCrawlers;
        NUMBER_OF_URLS = nrOfUrls;
        DOWNLOAD_IMAGES = downloadImages;
        crawlPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        writePool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
        iteratorCount = new AtomicInteger(0);
        threadsTerminated = new AtomicBoolean(false);
        imageThreadsTerminated = null;
        dir = new Directory(rootPage); // can throw exceptions
        dataTracker = new ConcurrentDataTracker();
        this.console = console;
        this.treeView = treeView;

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
                // connect to url through instance variable
                Document doc =
                        Jsoup.connect(urlToCrawl).
                                timeout(60 * 1000).
                                userAgent("Mozilla/10.0 (Windows NT 10.0) AppleWebKit/538.36 (KHTML, like Gecko) Chrome/69.420 Safari/537.36").
                      //          ignoreHttpErrors(true).
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
                    if (visitedUrls.add(nextUrl)) {
                        crawlPool.submit(new Crawl(nextUrl));
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

                // submit modified document as a Runnable to be written to local drive
                writePool.submit(new Write(doc));
            }

            catch (IOException ex) {
                System.err.println(ex.getMessage() + ": " + urlToCrawl);
            }
        }
    }

    /**
     * Write class implements interface Runnable and will be instantiated and executed for every unique url found
     */
    private class Write implements Runnable {
        private final Document document;

        private Write(Document document) {
            this.document = document;
        }

        @Override
        public void run() {

            if (iteratorCount.get() <= NUMBER_OF_URLS) {

                try {

                    if (DOWNLOAD_IMAGES) {

                        Elements images = document.select("picture source[srcSet], img[srcSet]");
                        for (Element e : images) {
                            String url;

                            if (e.attr("src").equals("") && !e.attr("srcSet").equals("")) {
                                url = convertSrcSetToUrl(e.attr("srcSet"));
                            } else if (!e.attr("src").equals("") && e.attr("srcSet").equals("")) {
                                url = e.attr("src");
                            } else {
                                url = convertSrcSetToUrl(e.attr("srcSet"));
                            }

                            e.parent().append("<img src=" + '"' + url + '"' + ">");
                            e.remove();

                        }

                        images = document.select("img[src]");
                        for (Element img : images) {
                            String url = img.absUrl("src");

                            String extensionType = getExtensionType(url);

                            if (!extensionType.equals("noContentType")) {
                                img.attr("src", "../assets/images/" + url.hashCode() + "." + extensionType);
                                imgList.add(url);
                            }
                            else {
                                img.attr("alt", "noContentType found");
                            }

                        }
                    } // end of if (DOWNLOAD_IMAGES)

                    FileWriter writer = new FileWriter(new File(dir.getHtmlFolder().getAbsolutePath() + "\\" + document.location().hashCode() + ".html"));
                    writer.write(document.html());
                    writer.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                finally {
                    iteratorCount.incrementAndGet();
                }

            } else { // end of iteratorCount <= NUMBER_OF_URLS

                if (!threadsTerminated.get()) {
                    shutdownExecutorService(); // is synchronized
                }
            }
        }
    }

    /**
     */
    private class ImageDownloader implements Runnable {
        private final String url;

        public ImageDownloader(String url) {
            this.url = url;
        }

        // needs to call shutdown when done
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

                String fileName = dir.getImagesFolder().getAbsolutePath() + "\\" + url.hashCode() + "." + getExtensionType(url);

                String[] fileNameParts = fileName.split("\\.");
                String format = fileNameParts[fileNameParts.length - 1];

                File file = new File(fileName);
                BufferedImage bufferedImage;

                InputStream in = new ByteArrayInputStream(buffer.array());
                bufferedImage = ImageIO.read(in);
                ImageIO.write(bufferedImage, format, file);

            }
            catch (IOException | IllegalArgumentException ex) {
                System.err.println(ex.getMessage() + ": " + url + " [." + getExtensionType(url) + "]");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

        }

    }

    /**
     * downloads all stylesheets found in rootPage, initiated by init method.
     */
    private void downloadStylesheet() {
        try {

            Document doc = Jsoup.
                    connect(rootPage).
                    timeout(30 * 1000).
                    userAgent("Mozilla/10.0 (Windows NT 10.0) AppleWebKit/538.36 (KHTML, like Gecko) Chrome/69.420 Safari/537.36").
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
     * shuts down the crawling process - called by Runnable Writer or by user if called through GUI
     */
    private synchronized void shutdownExecutorService() {
        threadsTerminated.set(true);
        crawlPool.shutdownNow();
        writePool.shutdownNow();
        setTimePassed(CrawlType.HTML);
        /* not ran by fxApplicationThread */

        if (DOWNLOAD_IMAGES) {
            // wonky solution attempt ->
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.execute(() -> {
                ExecutorService imageThreadPool = Executors.newFixedThreadPool(NUMBER_OF_CRAWLERS);
                imageThreadsTerminated = new AtomicBoolean(false);
                imageStartTime = System.nanoTime();

                for (String url : imgList) {
                    imageThreadPool.submit(new ImageDownloader(url));
                }

                imageThreadPool.shutdown();

                try {
                    imageThreadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.HOURS);
                    imageThreadsTerminated.set(true);
                    setTimePassed(CrawlType.IMAGE);
                    console.println(String.format("crawl executed in %.2f s", (System.nanoTime() - startTime) * Math.pow(10, -9)));
                    updateControllerTreeView();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            executorService.shutdown();

        } // end of DOWNLOAD_IMAGES

        else {
            console.println(String.format("crawl executed in %.2fs", (System.nanoTime() - startTime) * Math.pow(10, -9)));
            updateControllerTreeView();
        }
    }

    /**
     * updates the GUI's treeView to display updated domains, runs after crawl finishes.
     */
    private synchronized void updateControllerTreeView() {
        // block-head solution ->
        Platform.runLater(() -> {

            String action = "domain + year + month + day";
            TreeItem<String> domainTreeItem = new TreeItem<>(dir.getName());
            TreeItem<String> yearTreeItem   = new TreeItem<>(dir.getYear());
            TreeItem<String> monthTreeItem  = new TreeItem<>(dir.getMonth());
            TreeItem<String> dayTreeItem    = new TreeItem<>(dir.getDay());

            for (TreeItem<String> currentDomainItem : treeView.getRoot().getChildren()) {
                if (currentDomainItem.getValue().equals(dir.getName())) {
                    // Todo: DOMAIN ALREADY EXISTS
                    action = "year + month + day";
                    domainTreeItem = currentDomainItem;

                    for (TreeItem<String> currentYearItem : currentDomainItem.getChildren()) {
                        if (currentYearItem.getValue().equals(dir.getYear())) {
                            // Todo: YEAR ALREADY EXISTS
                            action = "month + day";
                            yearTreeItem = currentYearItem;

                            for (TreeItem<String> currentMonthItem : currentYearItem.getChildren()) {
                                if (currentMonthItem.getValue().equals(dir.getMonth())) {
                                    // Todo: MONTH ALREADY EXISTS
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
        writeRootUrl(rootPage, dir.getHomeFolder().getAbsolutePath());
        downloadStylesheet();
        console.println("crawl initiated");
        startTime = System.nanoTime();
        crawlPool.execute(new Crawl(rootPage));
    }

    /**
     * shuts down the crawling process if is not already threadsTerminated - called by fxApplicationThread
     */
    public void shutdown() {
        if (!threadsTerminated.get()) {
            shutdownExecutorService();
        } else {
            console.println("shutdownNow already invoked");
        }
    }

    /**
     * appends the console component with data on current or most recent crawl
     */
    public void printlnData() {
        if (!threadsTerminated.get()) { // == Thread is running
            setTimePassed(CrawlType.HTML);
        }

        console.println(dataTracker.getData(timePassed, NUMBER_OF_URLS, dir.getHtmlFolder().getAbsolutePath()));
    }

    public void printData() {
        if (!threadsTerminated.get()) { // == Thread is running
            setTimePassed(CrawlType.HTML);
        }

        console.print(dataTracker.getData(timePassed, NUMBER_OF_URLS, dir.getHtmlFolder().getAbsolutePath()));
    }

    public void printlnImageData() {
        if (imageThreadsTerminated != null) {
            if (!imageThreadsTerminated.get()) { // Thread is running
                setTimePassed(CrawlType.IMAGE);
            }
        }

        console.println(dataTracker.getImageData(imageTimePassed, imgList.size(), dir.getImagesFolder().getAbsolutePath()));
    }

    public void printImageData() {
        if (imageThreadsTerminated != null) {
            if (!imageThreadsTerminated.get()) { // Thread is running
                setTimePassed(CrawlType.IMAGE);
            }
        }

        console.print(dataTracker.getImageData(imageTimePassed, imgList.size(), dir.getImagesFolder().getAbsolutePath()));
    }

    public boolean isDOWNLOAD_IMAGES() {
        return DOWNLOAD_IMAGES;
    }

}


