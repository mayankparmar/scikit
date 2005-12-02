package scikit.jobs;


import scikit.plot.Display;
import java.util.Vector;
import javax.swing.JFrame;


public abstract class Job implements Runnable {
	private Thread _thread = null;
	private long timerDelay = 50, updateThrottle = 0;
	private long lastTimer, lastUpdate;
	
	private Vector<Display> displays = new Vector<Display>();
	
	volatile private boolean stopRequested = false;
	volatile private boolean stepRequested = false;
	volatile private boolean killRequested = false;
	
	public Parameters params = new Parameters();
	
	
	public void start() {
		stopRequested = false;
		if (_thread == null) {
			// System.out.println("new");
			_thread = new Thread(this);
			_thread.start();
		}
		else {
			wakeProcess();
		}
	}
	
	public void stop() {
		stopRequested = true;
	}
	
	public void step() {
		stepRequested = true;
		start();
	}
	
	public void kill() {
		if (_thread != null) {
			killRequested = true;
			stopRequested = false;
			wakeProcess();
			try { _thread.join(1000); } catch (InterruptedException e) {}
			if (_thread.isAlive()) 
				_thread.stop();
		}
		_thread = null;
		
		clearDisplays();
	}
	
	
	public void addDisplay(Display disp) {
		if (!displays.contains(disp))
			displays.add(disp);
	}
	
	
	public String toString() {
		return getClass() + " : " + _thread;
	}
	
	synchronized private void wakeProcess() {
		// System.out.println("notifying");
		notify();
	}
	
	
	private void clearDisplays() {
		for (Display disp : displays) {
			disp.clear();
			disp.animate();
		}
	}
	
	
	private void animateDisplays() {
		animate();
		for (Display disp : displays) {
			disp.animate();
		}
	}
	
	
	//
	// Called by subclass
	//
	
	synchronized protected void yield() {
		long time = System.currentTimeMillis();
		
		if (time - lastUpdate < updateThrottle) {
			long delay = updateThrottle - (time - lastUpdate);
			try {Thread.sleep(delay); } catch (InterruptedException e) {}
			time += delay;
		}
		lastUpdate = time;
		
		if (time - lastTimer > timerDelay) {
			animateDisplays();
			Thread.yield();
			lastTimer = time;
		}
		
		if (stepRequested) {
			stepRequested = false;
			stopRequested = true;
		}
		
		while (stopRequested && !killRequested) {
			try {
				// System.out.println("sleep");
				animateDisplays();
				wait();
				// System.out.println("wake");
			} catch (InterruptedException e) {
				System.out.println("interrupted");
			}
		}
		
		if (killRequested) {
			killRequested = false;
			throw new ThreadDeath();
		}
	}
	
	
	//
	// To be implemented by subclass
	//	
	
	public void animate() {
	}

	public void dispose() {
	}
}
