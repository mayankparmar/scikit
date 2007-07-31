package scikit.jobs;

import javax.swing.SwingUtilities;


public class Cooperation {
	private volatile boolean triggered = false;
	
	/**
	 * Schedules the GUI thread to call pass(). This signals the beginning of the
	 * processing loop. The GUI thread will resume control upon completion of the
	 * processing loop.
	 * In the case that this event has already been triggered, does nothing.
	 */
	synchronized public void triggerProcessingLoop() {
		if (!triggered) {
			triggered = true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					synchronized(Cooperation.this) {
						triggered = false;
						pass();
					}
				}
			});
		}
	}
	
	/**
	 * Adds this thread to the processing loop.
	 */
	synchronized public void register() {
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
	 * Passes control to the next thread in the processing loop, and waits until
	 * control returns.
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
	
	/**
	 * Causes the currently executing thread to sleep while allowing the processing
	 * loop to continue.
	 */
	public void sleep(long ms) {
		unregister();
		try { Thread.sleep(ms); }
		catch (InterruptedException e) {}
		register();
	}
}
