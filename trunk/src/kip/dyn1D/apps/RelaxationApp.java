package kip.dyn1D.apps;

import scikit.plot.*;
import scikit.jobs.*;
import kip.dyn1D.*;


public class RelaxationApp extends Job {
	Histogram magnetHist = new Histogram("Magnetization", 0, true);
	
	AbstractIsing sim;
	Structure structure;
	int numSteps = 100;
	
	public static void main(String[] args) {
		frame(new Control(new RelaxationApp()), "Ising Magnetization Relaxation");
	}

	public RelaxationApp() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis"));
		params.add("Random seed", 0);
		params.add("N", 1<<15);
		params.add("R", 1<<9);
		params.add("T", 1000.0);
		params.add("dt", 0.02);
		params.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		params.set("time", sim.time());
	}
	
	
	public void run() {
		sim = new Ising(params);
		
		magnetHist.setBinWidth(0, sim.dt);
		magnetHist.setAveraging(0, true);
		addDisplay(magnetHist);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeField(1);
			
			for (int i = 0; i < numSteps; i++) {
				magnetHist.accum(0, sim.time(), sim.magnetization());
				yield();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
