package scikit.util;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import bsh.Capabilities;
import bsh.Interpreter;
import bsh.Capabilities.Unavailable;
import bsh.util.JConsole;

public class ConsoleApp implements Runnable {
	public void run() {
		final JConsole console = new JConsole();
		console.setPreferredSize(new Dimension(400, 400));
		Interpreter interpreter = new Interpreter(console);
		interpreter.setShowResults(true);
		interpreter.getNameSpace().importStatic(Commands.class);
		new Thread(interpreter).start();
		try {
			Capabilities.setAccessibility(true);
		} catch (Unavailable exc) {
			System.err.print("Beanshell reflection not available.");
		}

		JFrame frame = new JFrame("Console");
		Utilities.staggerFrame(frame);
		frame.getContentPane().add(console);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new ConsoleApp());
	}
}
