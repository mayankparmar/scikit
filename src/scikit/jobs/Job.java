package scikit.jobs;

import java.util.Vector;


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
	
	/**
	 * Performs one step of the simulation. The GUI thread will wait while
	 * the simulation thread runs. The step is completed when the simulation thread calls
	 * the static method Job.animate().
	 */
	public void step() {
		state = State.STEP;
		if (thread == null)
			createThread();
		else
			coop.triggerProcessingLoop();
	}
	
	/**
	 * Runs the simulation until it is explicitly stopped. The GUI thread will wait while
	 * the simulation thread runs.
	 */
	public void start() {
		state = State.RUN;
		if (thread == null)
			createThread();
		else
			coop.triggerProcessingLoop();
	}
	
	/**
	 * Stops the simulation execution after the completion of this "simulation step",
	 * when the simulation thread calls the static method Job.animate(). 
	 */
	public void stop() {
		state = State.STOP;
	}
	
	/**
	 * Schedules the thread to be killed after the completion of this "simulation step".
	 * This will cause a ThreadDeath exception to be thrown in the simulation thread. 
	 */
	public void kill() {
		state = State.KILL;
		if (thread != null)
			coop.triggerProcessingLoop();
	}
	
	/**
	 * Wakes the simulation thread if it is stopped, in order that it can animate. This
	 * can be useful if, for example, an external parameter has been changed and the displays
	 * need to be updated.
	 */
	public void wake() {
		if (thread != null)
			coop.triggerProcessingLoop();		
	}

	/**
	 * Returns the underlying Simulation object for this Job. 
	 */
	public Simulation sim() {
		return sim;
	}
	
	
	public static void addDisplay(Display disp) {
		current()._addDisplay(disp);
	}
	private void _addDisplay(Display disp) {
		if (!displays.contains(disp)) {
			displays.add(disp);
		}
	}
	
	/**
	 * To be called from the simulation thread. Registers that the simulation thread has
	 * completed a step. Calls the <code>animate</code> method of the simulation.
	 * Wakes the GUI thread, which has been locked during execution
	 * of the simulation thread. 
	 * The simulation thread either continues running or stops based on
	 * the Job state.
	 */
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
	
	/**
	 * To be called from the simulation thread. Wakes the GUI thread, which has
	 * been locked during execution of the simulation thread. It is necessary to periodically call either
	 * <code>Job.animate()</code> or <code>Job.yield()</code> to ensure that the GUI does not hang.
	 */
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
					// registering causes the GUI thread to wait during simulation execution
					coop.register();
					// perform the simulation, periodically yielding to the GUI thread 
					sim.run();
					// when simulation has finished, yield control to the GUI thread, 
					// and animate when the GUI thread requests it
					while (true) {
						coop.pass();
						Job.animate();
					}
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
