package scikit.util;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;


public class Dump {

    public static void doubleArray(String filename, double[] data, int cols) {
        try {
            File file = new File(filename);
            FileWriter fw = new FileWriter(file);
            PrintWriter pw = pw = new PrintWriter(fw);
			cols = Math.max(cols, 1);
			int i = 0;
            while (i+cols <= data.length) {
				pw.print(cols == 1 ? i : data[i++]);
				for (int c = 1; c < cols; c++) {
					pw.print("\t" + data[i++]);
				}
				pw.println();
            }
            pw.close();
        } catch(IOException e) {
            System.err.println("An error occurred saving file `" + filename + "`");
        }
    }
}
