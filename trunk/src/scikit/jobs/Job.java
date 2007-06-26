package scikit.jobs;

import java.util.Vector;
import scikit.params.Parameters;


public class Job {
	private static Job current;
	
	private Simulation sim;
	private Thread thread;
	private Cooperation coop = new Cooperation();
	
	private enum State {STEP, RUN, STOP, KILL};
	private State state;
	
	private long lastYield, yieldDelay = 10;
	private long lastAnimate, animateDelay = 50;	
	
	private Vector<Display> displays = new Vector<Display>();

	
	public Job(Simulation sim) {
		this.sim = sim;
		current = this;
		addDisplay(sim);
	}
	
	public void step() {
		state = State.STEP;
		if (thread == null)
			createThread();
		else
			coop.triggerProcessingLoop();
	}
	
	public void start() {
		state = State.RUN;
		if (thread == null)
			createThread();
		else
			coop.triggerProcessingLoop();
	}
	
	public void stop() {
		state = State.STOP;
	}
	
	public void kill() {
		state = State.KILL;
		if (thread != null)
			coop.triggerProcessingLoop();
	}
	
	public Parameters params() {
		return sim.params;
	}
	
	
	public static void addDisplay(Display disp) {
		current()._addDisplay(disp);
	}
	private void _addDisplay(Display disp) {
		if (!displays.contains(disp)) {
			displays.add(disp);
		}
	}
	
	
	// to be called by simulation thread
	public static void animate() {
		current()._animate();
	}
	private void _animate() {
		if (Thread.currentThread() != thread) {
			throw new IllegalThreadStateException("Job.animate() must be called from simulation thread.");
		}
		switch (state) {
		case STEP:
		case STOP:
			state = State.STOP;
			do {
				animateDisplays();
				coop.pass();
			} while (state == State.STOP);
			break;
			
		case RUN:
			if (System.currentTimeMillis() - lastAnimate > animateDelay) {
				animateDisplays();
				lastAnimate = System.currentTimeMillis();
			}
			_yield();
			break;
			
		case KILL:
			throw new ThreadDeath();
		}
	}
	
	// to be called by simulation thread
	public static void yield() {
		current()._yield();
	}
	private void _yield() {
		if (Thread.currentThread() != thread) {
			throw new IllegalThreadStateException("Job.yield() must be called from simulation thread.");
		}
		if (System.currentTimeMillis() - lastYield > yieldDelay) {
			coop.triggerProcessingLoop();
			coop.pass();
			lastYield = System.currentTimeMillis();
		}
	}


	private void animateDisplays() {
		for (Display disp : displays) {
			disp.animate();
		}
	}
	
	private void clearDisplays() {
		for (Display disp : displays) {
			disp.clear();
		}
	}

	private static Job current() {
		return current;
	}
	
	private void createThread() {
		assert (thread == null);
		thread = new Thread(new Runnable() {
			public void run() {
				try {
					coop.register();
					sim.run();
				}
				finally {
					clearDisplays();
					coop.unregister();
					thread = null;
				}
			}
		});
		thread.start();
	}
}
