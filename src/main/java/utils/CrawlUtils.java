package utils;

public class CrawlUtils {

    public static String convertSrcSetToUrl(String srcSet){
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

    public static String getExtensionType(String url){
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
