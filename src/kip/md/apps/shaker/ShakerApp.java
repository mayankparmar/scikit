package kip.md.apps.shaker;

import scikit.util.Terminal;


public class ShakerApp extends Terminal {
	@SuppressWarnings("unused")
	Object cmds = new Object() {
		public void hi() {
			interpreter().println("hello");
		}
	};
	
	public static void main(String[] args) {
		ShakerApp term = new ShakerApp();
		term.importObject(term.cmds);
		term.runApplication();
	}
}
