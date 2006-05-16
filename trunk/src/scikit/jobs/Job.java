package scikit.jobs;


import scikit.plot.Display;
import java.util.Vector;


public abstract class Job implements Runnable {
	class JobThread extends Thread {
		public Job job;
		public JobThread(Job _job) { super(_job); job = _job; }
	}

	private Thread _thread = null;
	private long timerDelay = 50, updateThrottle = 0;
	private long lastTimer, lastUpdate;
	
	private Vector<Display> displays = new Vector<Display>();
	
	volatile private boolean stopRequested = false;
	volatile private boolean stepRequested = false;
	volatile private boolean killRequested = false;
	
	public Parameters params = new Parameters(this);
	public Parameters outputs = new Parameters(this);
	
	
	public void start() {
		stopRequested = false;
		if (_thread == null) {
			_thread = new JobThread(this);
			_thread.start();
			params.setLocked(true);
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
		params.setLocked(false);
		
		clearDisplays();
	}
	
	
	synchronized public void wakeProcess() {
		notify();
	}
	
	
	public void addDisplay(Display disp) {
		if (!displays.contains(disp))
			displays.add(disp);
	}
	
	
	public String toString() {
		return getClass() + " : " + _thread;
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
	
	synchronized private void _yield() {
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
				animateDisplays();
				wait();
			} catch (InterruptedException e) {
				System.out.println("interrupted");
			}
		}
		
		if (killRequested) {
			killRequested = false;
			throw new ThreadDeath();
		}
	}
	
	
	public static void yield() {
		current()._yield();
	}
	
	
	// utility method for quickly viewing data.
	private scikit.plot.Plot debugPlot;
	public static void plot(int i, double[] data) {
		Job j = current();
		if (j.debugPlot == null) {
			j.debugPlot = new scikit.plot.Plot("Debug : " + j.toString(), true);
			j.addDisplay(j.debugPlot);
		}
		j.debugPlot.setDataSet(i, new scikit.plot.PointSet(0, 1, data));
		// try to force an immediate repaint
		j.debugPlot.animate();
		Thread.yield();
	}
	
	
	public static Job current() {
		Thread t = Thread.currentThread();
		if (t instanceof JobThread)
			return ((JobThread)t).job;
		else
			return null;
	}
	
	
	static int _frameStagger = 100;	
	public static javax.swing.JFrame frame(javax.swing.JComponent comp, String title) {
		javax.swing.JFrame frame = new javax.swing.JFrame();
		frame.getContentPane().add(comp);
		frame.setTitle(title);
		frame.setLocation(_frameStagger, _frameStagger);		
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		_frameStagger += 60;		
		return frame;
	}
	
	
	//
	// To be implemented by subclass
	//
	
	public static Job initApplet(javax.swing.JApplet applet) {
		return null;
	}
	
	public void animate() {
	}

	public void dispose() {
	}
}
