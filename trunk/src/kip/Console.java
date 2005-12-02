package kip;
import bsh.Interpreter;


public class Console {
	public static void main(String[] args) {
		javax.swing.JFrame frame = new javax.swing.JFrame("BeanShell Console");
		frame.setSize(600, 400);
		bsh.util.JConsole console = new bsh.util.JConsole();
		frame.add(console);
		frame.setVisible(true);
		frame.repaint();
		bsh.Interpreter interpreter = new bsh.Interpreter(console);
		new Thread(interpreter).start();
		// try { interpreter.eval("this.caller.show();"); } catch (Exception e) {}
	}
}
