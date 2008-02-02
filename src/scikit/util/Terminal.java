package scikit.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import bsh.Capabilities;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Capabilities.Unavailable;
import bsh.util.JConsole;

public class Terminal {
	private Interpreter interpreter;
	private JConsole console;
	private Object banner = new Object() {
		@SuppressWarnings("unused")
		public void printBanner() {
			console.print(
					"SciKit",
					new Font("SansSerif", Font.BOLD, 12), 
					new Color(20,100,20));
			console.print(" scikit.googlecode.com\n",
					new Color(20,20,100));
			console.print(greeting());
		}
	};
	
	protected String greeting() {
		return "";
	}
	
	protected void importObject(Object o) {
		interpreter.getNameSpace().importObject(o);
	}
	
	public Terminal() {
		console = new JConsole();
		console.setPreferredSize(new Dimension(400, 400));
		interpreter = new Interpreter(console);
		interpreter.setShowResults(true);
		NameSpace namespace = interpreter.getNameSpace();
		namespace.importStatic(Commands.class);
		namespace.importObject(banner);
		new Thread(interpreter).start();
		try {
			Capabilities.setAccessibility(true);
		} catch (Unavailable exc) {
			System.err.print("Beanshell reflection not available.");
		}
	}
	
	public JConsole console() {
		return console;
	}
	
	public Interpreter interpreter() {
		return interpreter;
	}
	
	public void runApplication() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = Utilities.frame(console, "Console");
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			}
		});
	}

	public static void main(String[] args) {
		new Terminal().runApplication();
	}
}
