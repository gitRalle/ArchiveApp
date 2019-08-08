package io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class IO {
    private final static String initPath = "Files\\init.data";
    private final static File   initFile = new File(initPath);

    public static void writeRootUrl(String rootPage, String pathToFolder) {
        String extension = "\\url.data";
        File outFile = new File(pathToFolder + extension);

        try (ObjectOutputStream oOut = new ObjectOutputStream(new FileOutputStream(outFile)))
        {
            oOut.writeObject(rootPage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static String readRootUrl(String pathToFolder) {
        String extension = "\\url.data";
        File inFile = new File(pathToFolder + extension);
        String line = null;

        try (ObjectInputStream oIn = new ObjectInputStream(new FileInputStream(inFile)))
        {
            line = (String) oIn.readObject();
        }
        catch (EOFException | ClassNotFoundException ex) {
            // end of stream
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

    public static boolean writeLog(String path, @NotNull List<String> log) {
        String extension = "\\log.data";
        File outFile = new File(path + extension);

        try (ObjectOutputStream oOut = new ObjectOutputStream(new FileOutputStream(outFile)))
        {
            for (String line : log)
                oOut.writeObject(line);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    @Nullable
    public static List<String> readLog(String path) {
        String extension = "\\log.data";
        File inFile = new File(path + extension);
        if (!inFile.exists()) {
            return null;
        }
        String line;
        List<String> lines = new ArrayList<>();

        try (ObjectInputStream oIn = new ObjectInputStream(new FileInputStream(inFile)))
        {
            while ((line = (String) oIn.readObject()) != null)
                lines.add(line);
        }

        catch (EOFException | ClassNotFoundException ex) {
            // end of stream
        }

        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return lines;
    }

}