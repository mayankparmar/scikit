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
		createThread();
		state = State.STEP;
		coop.triggerProcessingLoop();
	}
	
	public void start() {
		createThread();
		state = State.RUN;
		coop.triggerProcessingLoop();
	}
	
	public void stop() {
		assert (thread != null);
		state = State.STOP;
	}
	
	public void kill() {
		assert (thread != null);
		state = State.KILL;
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
		assert (Thread.currentThread() == thread);		
		switch (state) {
		case STEP:
		case STOP:
			state = State.STOP;
			animateDisplays();
			coop.pass();
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
		assert (Thread.currentThread() == thread);
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
		if (thread != null)
			return;
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
