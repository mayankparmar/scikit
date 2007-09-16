package scikit.jobs;

import java.util.Set;
import java.util.TreeSet;

import scikit.params.Parameters;

public abstract class Simulation {
	// parameters to be read and written by simulation
	public Parameters params = new Parameters();
	
	// simulation fills 'flags' in the constructor to indicate what flags
	// it will respond to. later, when a button is pressed, the corresponding
	// string will be added to 'flags'.
	public Set<String> flags = new TreeSet<String>();  
	
	// main entry point for simulation.  while running, the GUI event thread will
	// be blocked except during calls to yield() and animate() in Job.current().
	// using this form of cooperative multitasking the implementor needn't worry about 
	// thread conflicts.
	abstract public void run();
	
	// called periodically to output visual data to the user and read simulation
	// parameters
	abstract public void animate();
	
	// called after thread has been killed
	abstract public void clear();
}
