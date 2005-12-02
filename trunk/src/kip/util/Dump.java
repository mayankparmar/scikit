package kip.util;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;


public class Dump {

    public static void doubleArray(String filename, double[] data) {
        try {
            File file = new File(filename);
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = pw = new PrintWriter(fw);
            pw.println("# " + filename);
            for (int i = 0; i < data.length; i++) {
                pw.println(i + " " + data[i]);
            }
            pw.close();
        } catch(IOException e) {
            System.err.println("An error occurred saving file `" + filename + "`");
        }
    }
}
