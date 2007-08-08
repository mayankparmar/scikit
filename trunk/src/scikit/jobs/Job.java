package scikit.jobs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;

import javax.swing.JOptionPane;


public class Job {
	private static Job current;
	
	private Simulation sim;
	private Thread thread;
	private Cooperation coop = new Cooperation();
	
	private enum State {STEP, RUN, STOP, KILL};
	private State state;
	
	private long lastYield, yieldDelay = 10;
	private long lastAnimate, animateDelay = 50;	
	private boolean throttleAnimation = false;
	
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
	 * Force a delay of 50 ms between each simulation step 
	 * @param b
	 */
	public void throttleAnimation(boolean b) {
		throttleAnimation = b;
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
		case RUN:
			long timeUntilAnimate = lastAnimate + animateDelay - System.currentTimeMillis();
			if (throttleAnimation && timeUntilAnimate > 0) {
				coop.sleep(timeUntilAnimate);
			}
			if (throttleAnimation || timeUntilAnimate < 0) {
				animateDisplays();
				lastAnimate = System.currentTimeMillis();
			}
			_yield();
			break;
		}
		
		// during sleep() or yield() the user might have stopped the simulation. therefore
		// we enter a new switch statement.
		
		switch (state) {
		case STEP:
		case STOP:
			state = State.STOP;
			do {
				animateDisplays();
				coop.pass();
			} while (state == State.STOP);
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
	
	private String detailedErrorMessage(Exception e) {
		StringWriter stringWriter = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(stringWriter);
	    e.printStackTrace(printWriter);
	    return stringWriter.getBuffer().toString();
	}
	
	private void createThread() {
		assert (thread == null);
		thread = new Thread(new Runnable() {
			String errMsg = null;
			public void run() {
				try {
					// registering causes the GUI thread to wait during simulation execution
					coop.register();
					// perform the simulation, periodically yielding to the GUI thread. if
					// the simulation is externally killed a thread death error will be
					// thrown.
					sim.run();
					// simulation has finished. "pass" to yield control to the GUI thread.
					// if the GUI thread returns control, then perform an animation, and again
					// "pass".
					while (true) {
						coop.pass();
						Job.animate();
					}
				}
				catch (Exception e) {
					errMsg = detailedErrorMessage(e);
				}
				finally {
					System.out.println("finishing");
					// we could reach here due to a bug in the simulation (an Exception)
					// or because the user killed the job (ThreadDeath error). in either case,
					// we must now return the Job to its initial state.
					clearDisplays();
					System.out.println("unregistering");
					coop.unregister();
					thread = null;
					// display possible execution exception in full detail for debugging
					if (errMsg != null) {
						System.err.println(errMsg);
						JOptionPane.showMessageDialog(null, errMsg, "Error Occurred in Simulation", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		thread.start();
	}
}
