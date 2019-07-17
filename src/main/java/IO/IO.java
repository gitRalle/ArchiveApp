package IO;

import org.jetbrains.annotations.Nullable;
import java.io.*;

public class IO {
    private final static String initPath = "Files\\init.data";
    private final static File   initFile = new File(initPath);

    public static void writeRootUrl(String rootPage, String pathToFolder) {
        File fileToBeWritten = new File(pathToFolder + "\\" + "url.txt");

        try (FileWriter writer = new FileWriter(fileToBeWritten))
        {
            writer.write(rootPage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static String readRootUrl(File urlTxtFile) {
        String line = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(urlTxtFile)))
        {
            line = reader.readLine();

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return line;
    }


    @Nullable
    public static File readInitFile() {

        try (ObjectInputStream oIn = new ObjectInputStream(new FileInputStream(initFile)))
        {
           return (File) oIn.readObject();
        }

        catch (EOFException | ClassNotFoundException ex) {
            // end of stream
        }

        catch (FileNotFoundException ex) {
            return null;
        }

        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return null;

    }

    public static void writeInitFile(File path) {

        try (ObjectOutputStream oOut = new ObjectOutputStream(new FileOutputStream(initFile)))
        {
            oOut.writeObject(path);
            initFile.setReadOnly();
        }

        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}