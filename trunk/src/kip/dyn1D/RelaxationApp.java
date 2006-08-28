package kip.dyn1D;

import scikit.plot.*;
import scikit.jobs.*;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


public class RelaxationApp extends Job {
	Plot fieldPlot = new Plot("Fields", true);
	Histogram magnetHist = new Histogram("Magnetization", 0, true);
	
	Dynamics1D sim;
	Structure structure;
	double[] field;
	int numSteps = 100;
	
	public static void main(String[] args) {
		frame(new Control(new RelaxationApp()), "Ising Magnetization Relaxation");
	}

	public RelaxationApp() {
		params.add("Dynamics", true, "Ising Glauber", "Ising Metropolis");
		params.add("Random seed", 0, true);
		params.add("N", 1<<16, true);
		params.add("R", 512, true);
		params.add("T", 5.0, false);
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
		
		field = sim.copyField(null);
		fieldPlot.setDataSet(0, new Coarsened(field, 0, sim.N, sim.N/100.0));
		fieldPlot.setYRange(-1, 1);
		
		magnetHist.setBinWidth(0, sim.dt);
		magnetHist.setAveraging(0, true);
		
		addDisplay(fieldPlot);
		addDisplay(magnetHist);
		
		while (true) {
			sim.initialize(params);
			sim.randomizeField(1);
			
			for (int i = 0; i < numSteps; i++) {
				magnetHist.accum(0, sim.time(), sim.magnetization());
				sim.copyField(field);
				yield();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);			
		}
	}
}
