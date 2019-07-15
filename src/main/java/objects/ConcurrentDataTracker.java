package objects;

import java.io.File;
import java.util.Objects;

/** Class responsible for gathering and returning data from file system to Crawler class
 */
public class ConcurrentDataTracker {


    public String getData(long timePassed, final int NUMBER_OF_URLS, String pathToFolder) {
        int filesWritten = Objects.requireNonNull(new File(pathToFolder).listFiles()).length;
        double percentage = ((double) filesWritten / NUMBER_OF_URLS) * 100.0;

        return String.format("...........................................................................%n" +
                             ".....HTML status:%n" +
                             ".....totalTime:      %.2f s%n" +
                             ".....downloaded: %d/%d [%.1f%s]%n" +
                             ".....speed:            %.1f urls/s",
                timePassed * Math.pow(10, -9),
                filesWritten, NUMBER_OF_URLS, percentage, "%",
                filesWritten / (timePassed * Math.pow(10, -9)));
    }

    public String getImageData(long timePassed, int urlsFound, String pathToFolder) {
        int filesWritten = Objects.requireNonNull(new File(pathToFolder).listFiles()).length;
        double percentage = ((double) filesWritten / urlsFound) * 100.0;

        return String.format("...........................................................................%n" +
                             ".....IMAGE status:%n" +
                             ".....totalTime:      %.2f s%n" +
                             ".....downloaded: %d/%d [%.1f%s]%n" +
                             ".....speed:            %.1f imgs/s",
                timePassed * Math.pow(10, -9),
                filesWritten, urlsFound, percentage, "%",
                filesWritten / (timePassed * Math.pow(10, -9)));
    }





}
