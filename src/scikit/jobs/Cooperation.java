package scikit.jobs;

import javax.swing.SwingUtilities;


public class Cooperation {
	private volatile boolean triggered = false;
	
	public void triggerProcessingLoop() {
		if (!triggered) {
			triggered = true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					triggered = false;
					pass();
				}
			});
		}
	}
	
	public void register() {
		pass();
	}
	
	synchronized public void unregister() {
		notify();
	}
	
	synchronized public void pass() {
		notify();
		try {
			wait();
		}
		catch (InterruptedException e) {
			System.err.println("Thread Interrupted.");
		}
	}
}
