package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class RelaxationApp extends Job {
	Histogram magnetHist = new Histogram("Magnetization", 0, true);
	
	Dynamics1D sim;
	Structure structure;
	int numSteps = 100;
	
	public static void main(String[] args) {
		frame(new Control(new RelaxationApp()), "Ising Magnetization Relaxation");
	}

	public RelaxationApp() {
		params.add("Dynamics", true, "Ising Glauber", "Ising Metropolis");
		params.add("Random seed", 0, true);
		
//		params.add("N", 1<<9, true);
//		params.add("R", 1<<3, true);
		params.add("N", 1<<15, true);
		params.add("R", 1<<9, true);
//		params.add("N", 1<<21, true);
//		params.add("R", 1<<15, true);
		params.add("T", 1000.0, false);
		params.add("dt", 0.02, true);
		outputs.add("time");
	}
	
	public void animate() {
		sim.setParameters(params);
		outputs.set("time", sim.time());
	}
	
	
	public void run() {
		String dyn = params.sget("Dynamics");
		if (dyn.equals("Ising Glauber"))
			sim = new Ising(params, Ising.Dynamics.GLAUBER);
		else if (dyn.equals("Ising Metropolis"))
			sim = new Ising(params, Ising.Dynamics.METROPOLIS);
		
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
