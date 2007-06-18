package kip.dyn1D.apps;

import scikit.params.ChoiceValue;
import scikit.dataset.*;
import scikit.plot.*;
import scikit.jobs.*;
import kip.dyn1D.*;
import static java.lang.Math.*;


public class RelaxationApp extends Simulation {
	Histogram magnetHist = new Histogram("Magnetization", 0, true);
	Plot magnetDeriv = new Plot("dM/dt", true);
	
	AbstractIsing sim;
	int numSteps = 100;
	
	public static void main(String[] args) {
		new Control(new RelaxationApp(), "Ising Magnetization Relaxation");
	}

	public RelaxationApp() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis"));
		params.add("Simulation type", new ChoiceValue("Ising", "Langevin"));
		params.add("Random seed", 0);
		params.add("N", 1<<20);
		params.add("R", 1<<16);
		params.add("T", 0.75);
		params.add("dt", 0.2);
		params.add("dx", 1<<10);
		params.add("Initial magnetization", 0.95);
		params.add("time");
		params.add("cnt");
	}
	
	public void animate() {
		sim.setParameters(params);
		params.set("time", sim.time());
	}
	
	
	public void run() {
		String type = params.sget("Simulation type");
		sim = type.equals("Ising") ? new Ising(params) : new FieldIsing(params);
		
		magnetHist.setBinWidth(0, sim.dt);
		magnetHist.setAveraging(0, true);
		Job.addDisplay(magnetHist);
		
		Derivative deriv = new Derivative(magnetHist.getAccumulator(0));
		deriv.invertDependentParameter = true;
		magnetDeriv.setDataSet(0, deriv);
		magnetDeriv.setDataSet(1, new Function(0, 0.95) {
			public double eval(double m) {
				double bm = m/sim.T;
				switch (sim.dynamics) {
				case GLAUBER:
					return tanh(bm) - m;
				case METROPOLIS:
					return 2 * exp(-abs(bm)) * (sinh(bm) - m*cosh(bm));
				default:
					return Double.NaN;
				}
			}
		});
		Job.addDisplay(magnetDeriv);
		
		params.set("cnt", 0);
		for (int cnt = 0; cnt < 2000; cnt++) {
			sim.initialize(params);
			sim.randomizeField(params.fget("Initial magnetization"));
			
			for (int i = 0; i < numSteps; i++) {
				magnetHist.accum(0, sim.time(), sim.magnetization());
				Job.animate();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);
			params.set("cnt", cnt+1);
		}
	}
}
