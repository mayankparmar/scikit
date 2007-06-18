package scikit.jobs;

import scikit.params.Parameters;

public abstract class Simulation implements Display {
	// parameters to be read and written by simulation
	public Parameters params = new Parameters();
	
	// main entry point for simulation.  no other threads can run during
	// simulation exception, except during calls to Job.current().yield().
	abstract public void run();
	
	// called periodically (during Job.yield) to output data to user
	public void animate() {}
	
	// called after thread has been killed
	public void clear() {}
}
