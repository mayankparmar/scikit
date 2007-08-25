package kip.ising.dim1.apps;

import static java.lang.Math.*;
import static scikit.util.Utilities.*;

import java.awt.Color;

import scikit.params.ChoiceValue;
import scikit.dataset.*;
import scikit.graphics.dim2.Plot;
import scikit.jobs.*;
import kip.ising.dim1.AbstractIsing;
import kip.ising.dim1.FieldIsing;
import kip.ising.dim1.Ising;


public class RelaxationApp extends Simulation {
	Plot magnetPlot = new Plot("Magnetization");
	Plot derivPlot = new Plot("dM/dt");
	Accumulator magnetization;
	
	AbstractIsing sim;
	int numSteps = 100;
	
	public static void main(String[] args) {
		new Control(new RelaxationApp(), "Ising Magnetization Relaxation");
	}

	public RelaxationApp() {
		frame(magnetPlot, derivPlot);
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
		params.set("time", format(sim.time()));
		
		magnetPlot.registerLines("m", magnetization, Color.BLACK);
		
		Derivative deriv = new Derivative(magnetization);
		deriv.invertDependentParameter = true;
		derivPlot.registerLines("deriv data", deriv, Color.BLACK);
		derivPlot.registerLines("deriv theory", new Function(0, 0.95) {
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
		}, Color.BLUE);
	}
	
	public void clear() {
		magnetPlot.clear();
		derivPlot.clear();
	}
	
	public void run() {
		String type = params.sget("Simulation type");
		sim = type.equals("Ising") ? new Ising(params) : new FieldIsing(params);
		
		magnetization = new Accumulator(sim.dt);
		magnetization.setAveraging(true);
		
		params.set("cnt", 0);
		for (int cnt = 0; cnt < 2000; cnt++) {
			sim.initialize(params);
			sim.randomizeField(params.fget("Initial magnetization"));
			
			for (int i = 0; i < numSteps; i++) {
				magnetization.accum(sim.time(), sim.magnetization());
				Job.animate();
				sim.step();
			}
			
			params.set("Random seed", params.iget("Random seed")+1);
			params.set("cnt", cnt+1);
		}
	}
}
