package scikit.util;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.awt.Frame;
import java.awt.Component;
import java.awt.FileDialog;

public class Dump {
	
	public static PrintWriter pwFromDialog(Component comp, String fname) throws IOException {
		for (; comp != null; comp = comp.getParent()) {
			if (comp instanceof Frame) {
				FileDialog d = new FileDialog((Frame)comp, "Save", FileDialog.SAVE);
				d.setFile(fname);
				d.setVisible(true);
				return pwFromString(d.getDirectory()+d.getFile());
			}
		}
		throw new IOException();
	}
	
	public static PrintWriter pwFromString(String fname) throws IOException {
		File file = new File(fname);
		FileWriter fw = new FileWriter(file);
		return new PrintWriter(fw);				
	}
	
	
	public static void writeOctaveGrid(PrintWriter pw, double[] data, int cols, double dx) throws IOException {
    	if (cols < 1)
    		throw new IllegalArgumentException();
		pw.println("#name: dx\n#type: scalar");
		pw.println(dx);
		pw.println("#name: grid\n#type: matrix");
		pw.println("#rows: "+data.length/cols);
		pw.println("#columns: "+cols);
		for (int i = 0; i < data.length; i++) {
			pw.print(data[i]);
			if ((i+1) % cols == 0)
				pw.println();
			else
				pw.print(' ');
		}
		pw.close();
	}
	
	
    public static void writeColumns(PrintWriter pw, double[] data, int cols) throws IOException {
    	if (cols < 1)
    		throw new IllegalArgumentException();
		for (int i = 0; i < data.length; i++) {
			pw.print(data[i]);
			if ((i+1) % cols == 0)
				pw.println();
			else
				pw.print(' ');
		}
		pw.close();
    }
    
    
    public static void dumpString(String fname, String str) {
    	try {
    		PrintWriter pw = pwFromString(fname);
    		pw.write(str);
    		pw.close();
    	}
    	catch (IOException e) {}
    }
    
    public static void dumpColumns(String fname, double[] data, int cols) {
    	try { writeColumns(pwFromString(fname), data, cols); }
    	catch (IOException e) {}
    }
}
