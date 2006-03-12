package scikit.util;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.awt.FileDialog;

public class Dump {
	
	public static void saveDialog(java.awt.Component comp, String str, double[] data, int cols) {
		for (; comp != null; comp = comp.getParent()) {
			if (comp instanceof java.awt.Frame) {
				FileDialog d = new FileDialog((java.awt.Frame)comp, "Save", FileDialog.SAVE);
				d.setFile(str);
				d.show();
				String file  = d.getDirectory() + d.getFile();
				doubleArray(file, data, cols);
			}
		}
	}
	
	
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
