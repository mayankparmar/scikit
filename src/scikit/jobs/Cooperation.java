package scikit.jobs;

import javax.swing.SwingUtilities;


public class Cooperation {
	private volatile boolean triggered = false;
	
	/**
	 * Adds an event to the GUI event queue which, when called back, will
	 * call pass() from the GUI thread. pass() will hang the GUI thread
	 * and allow the simulation thread to run. When the simulation thread
	 * calls pass(), the GUI thread will back awakened, and this callback
	 * will return.
	 * In summary, schedules one processing loop while the GUI thread waits.
	 */
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
	
	/**
	 * Adds this thread to the processing loop.
	 */
	public void register() {
		// make sure that this thread is being run cooperatively with the GUI thread
		// (the GUI thread should hang while this thread is being processing)
		triggerProcessingLoop();
		pass();
	}
	
	/**
	 * Removes this thread from the processing loop.
	 */
	synchronized public void unregister() {
		notify();
	}
	
	/**
	 * Cooperatively allows the other thread in the processing loop to run.
	 */
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
