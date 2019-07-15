package objects;

import IO.IO;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class Directory {
    private final String name;
    private final String year;
    private final String month;
    private final String day;
    private final LinkedHashMap<String, File> dirLinkedMap;

    Directory(String url) throws IOException {
        LocalDate now = LocalDate.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.valueOf(now));

        name = parseUrl(url).toUpperCase();
        year  = String.valueOf(now.getYear());
        month = String.valueOf(now.getMonth());
        day = now.getDayOfMonth() + getOrdinalNumber(now.getDayOfMonth()).toUpperCase() + ", " + now.getDayOfWeek().
                getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase();
        dirLinkedMap = initMap();
        initDir();

    }

    private LinkedHashMap<String, File> initMap() {
        File initFile;
        if ((initFile = IO.readInitFile()) == null || !initFile.canExecute()) { System.err.println("initFile == null"); System.exit(0); }

        return new LinkedHashMap<>(9, 0.75f) {
            {
                put("nameDir",   new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name));
                put("yearDir",   new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year));
                put("monthDir",  new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month));
                put("dayDir",    new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month + "\\" + day));
                put("htmlDir",   new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month + "\\" + day + "\\" + "html"));
                put("assetsDir", new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month + "\\" + day + "\\" + "assets"));
                put("cssDir",    new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month + "\\" + day + "\\" + "assets" + "\\" + "css"));
                put("imagesDir", new File(initFile.getAbsolutePath() + "\\" +  "domains" + "\\" + name + "\\" + year + "\\" + month + "\\" + day + "\\" + "assets" + "\\" + "images"));
            }
        };
    }

    private void initDir() throws IOException
    {
        for (File currentFile : dirLinkedMap.values()) {
            if (!currentFile.exists()) {
                if (!currentFile.mkdir()) {
                    throw new IOException(
                            "Failed to init dir");
                }
            }
        }
    }

    /** getters ->
     */
    protected String getName()  { return name;  }

    protected String getYear()  { return year;  }

    protected String getMonth() { return month; }

    protected String getDay()   { return day;   }

    protected File get(String key)   { return dirLinkedMap.get(key);         }

    protected File getHomeFolder()   { return dirLinkedMap.get("dayDir");    }

    protected File getHtmlFolder()   { return dirLinkedMap.get("htmlDir");   }

    protected File getAssetsFolder() { return dirLinkedMap.get("assetsDir"); }

    protected File getCssFolder()    { return dirLinkedMap.get("cssDir");    }

    protected File getImagesFolder() { return dirLinkedMap.get("imagesDir"); }

    @Override
    public String toString() {
        return String.format("%s%n%s%n%s%n%s%n",
                getName(), getYear(), getMonth(), getDay());
    }


    private String parseUrl(String rootUrl) throws MalformedURLException {
        StringBuilder rootName = new StringBuilder();

        /* check for Malformed URL */
        new URL(rootUrl);

        if (!rootUrl.contains("http") && !rootUrl.contains("https") || !rootUrl.contains("www")) {
            throw new MalformedURLException(
                    "url must always contain 'www' and either contain 'http' or 'https'"
            );
        }

        /* ex: https://www.aftonbladet.se */
        char[] url = rootUrl.toCharArray();
        int dots = 0;

        // check URL for appearance of two dots
        for (int i = 0; i < url.length; i++) {
            if (url[i] == '.')
                dots++;
        }

        if (dots < 2)
            throw new MalformedURLException(
                    "url must consist of at least two dots "
            );

        // parse URL into rootName
        for (int i = 0; i < url.length; i++) {
            if (url[i] == '.') {
                i++;

                while (url[i] != '.') {
                    rootName.append(url[i]);
                    i++;
                }
                break;
            }
        }

        return new String(rootName);
    }

    private String getOrdinalNumber(int dayOfMonth) {

        switch (dayOfMonth) {
            case 1:
            case 21:
            case 31:
                return "st";

            case 2:
            case 22:
                return "nd";

            case 3:
            case 23:
                return "rd";

            default:
                return "th";
        }
    }
}
