package logic;

import objects.CrawlType;


class ConcurrentDataTracker {

    String getCrawlData(long timePassed, int iterations, final int NUMBER_OF_URLS, CrawlType crawlType) {
        String type;

        switch (crawlType) {
            case IMAGE:
                type = "imgs";
                break;
            default:
                type = "urls";
                break;
        }

        double percentage = (double) (iterations / NUMBER_OF_URLS) * 100;
        int roundDown = iterations;
        if (iterations > NUMBER_OF_URLS) {
            roundDown = NUMBER_OF_URLS;
        }
        double eta = getEta(timePassed, roundDown, NUMBER_OF_URLS);

        return String.format("{timeElapsed: %.2f s} {avgSpeed: %.1f " + type + "/s} {downloaded: %d/%d [%.1f%s] {eta: %.1f s}}",
                timePassed * Math.pow(10, -9), iterations / (timePassed * Math.pow(10, -9)), iterations, NUMBER_OF_URLS, percentage, "%", eta);
    }


    private double getEta(long timePassed, int iterations, int setSize) {
        long diff = setSize - iterations;
        long average = timePassed / iterations;
        return (diff * average * Math.pow(10, -9));
    }


}
