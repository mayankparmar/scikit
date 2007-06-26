package kip.ising.dim1.apps;

import scikit.params.ChoiceValue;
import scikit.params.Parameters;
import scikit.plot.*;
import scikit.dataset.Accumulator;
import scikit.dataset.Function;
import scikit.jobs.*;
import static scikit.util.Utilities.*;
import kip.ising.dim1.AbstractIsing;
import kip.ising.dim1.Ising;
import static java.lang.Math.*;

// purpose of this program: to determine breakdown time of linear theory as 
// a function of R.

public class Ordering2App extends Simulation {
	Plot structurePlot = new Plot("Magnetization", true);
	AbstractIsing sim;
	double[] field;
	Accumulator[] structures;
	double tmax;
	
	public static void main(String[] args) {
		new Control(new Ordering2App(), "Phase Ordering Linear Breakdown");
	}
	
	public Ordering2App() {
		params.add("Dynamics", new ChoiceValue("Ising Glauber", "Ising Metropolis"));
		params.add("N/R", 1<<5);
		params.add("dx/R", 0.2);
		params.add("R low", 1<<8);
		params.add("R high", 1<<10);
		params.add("R count", 4);
		params.add("dt", 0.05);
		params.add("t high", 4.0);
		params.add("T", 4.0/9.0);
		params.add("time");
		params.add("iterations");
	}
	
	public void animate() {
		params.set("time", format(sim.time()));
	}
	
	public void run() {
		int N_R = params.iget("N/R");
		double dx_R = params.fget("dx/R");
		double dt = params.fget("dt");
		tmax = params.fget("t high");
		
		int rlo = params.iget("R low");
		int rhi = params.iget("R high");
		int rcnt = params.iget("R count");
		
		Parameters params2 = new Parameters(
			"Dynamics", params.sget("Dynamics"),
			"N", 0,
			"R", 0,
			"dx", 0,
			"Random seed", 0,
			"T", params.fget("T"),
			"dt", dt
		);
		
		sim = new Ising(params2);
		final int mobility = sim.dynamics == AbstractIsing.DynType.GLAUBER ? 2 : 4;
		
		structures = new Accumulator[rcnt];
		for (int i = 0; i < rcnt; i++) {
			structures[i] = new Accumulator(dt);
			structures[i].setAveraging(true);
		}

		structurePlot.setDataSet(0, new Function(0, tmax) {
			public double eval(double t) {
				double D = sim.J/sim.T-1;
				return exp(mobility*D*t)*(1 + 1/D) - 1/D;
			}
		});
		structurePlot.setDataSet(1, structures[0]);
		structurePlot.setDataSet(2, structures[1]);
		structurePlot.setDataSet(3, structures[2]);
		structurePlot.setDataSet(4, structures[3]);
		Job.addDisplay(structurePlot);
		
		params.set("iterations", 0);
		for (int iterations = 0; ; iterations++) {
			for (int i = 0; i < rcnt; i++) {
				int R = (rhi-rlo)*i/rcnt + rlo;
				params2.set("Random seed", iterations*rcnt+i);
				params2.set("R", R);
				params2.set("N", N_R*R);
				params2.set("dx", (int)(dx_R*R));
				sim.initialize(params2);
				sim.randomizeField(0);
				
				while (sim.time() < tmax) {
					double m = sim.magnetization();
					// System.out.println("time " + sim.time() + "Accumulating " + sim.N*m);
					structures[i].accum(sim.time(), sim.N*m*m);
					Job.animate();
					sim.step();
				}
			}
			params.set("iterations", iterations);
		}
	}
}
