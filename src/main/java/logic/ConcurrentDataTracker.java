package logic;

import objects.CrawlType;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ConcurrentDataTracker {


    String getPrintData(long timePassed, final int NUMBER_OF_URLS, Directory dir, CrawlType crawlType) {
        String type;
        String path;
        int filesWritten;

        if (crawlType == CrawlType.HTML) { path = dir.getHtmlFolder().getAbsolutePath(); type = "urls"; }
        else { path = dir.getImagesFolder().getAbsolutePath(); type = "imgs"; }

        try {
            filesWritten = Objects.requireNonNull(new File(path).listFiles()).length;
        }
        catch (NullPointerException ex) {
            filesWritten = 0;
        }

        Duration duration = Duration.ofNanos(timePassed);

        return String.format("crawled for: %02d:%02d:%02d | written: %d/%d | avgSpeed: %.1f " + type + "/s",
                duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(),
                filesWritten, NUMBER_OF_URLS, filesWritten / (timePassed * Math.pow(10, -9)));
    }

    List<String> getLogData(long timePassed, int numberOfImages, String initDate, String path) {
        int filesWritten = Objects.requireNonNull(new File(path).listFiles()).length;
        Duration duration = Duration.ofNanos(timePassed);

        return new ArrayList<>() {
            {
                add(String.format("%d", filesWritten));
                add(String.format("%d", numberOfImages));
                add(String.format("%02d:%02d:%02d",
                        duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart()));
                add(initDate);
            }
        };


    }





}
