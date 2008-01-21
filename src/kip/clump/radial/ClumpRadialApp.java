package kip.clump.radial;

import static scikit.util.Utilities.*;

import java.awt.Color;

import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;

public class ClumpRadialApp extends Simulation {
	Plot plot = new Plot("");
	ClumpRadial clump;
	
	public static void main(String[] args) {
		new Control(new ClumpRadialApp(), "Radial Clump");
	}
	
	public ClumpRadialApp() {
		frame(plot);
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.add("Dimension", new ChoiceValue("3", "2"));
		params.addm("T", 0.09);
		params.addm("dt", 0.5);
		params.add("R", 1000.);
		params.add("L", 40000.);
		params.add("dx", 20.);
		params.add("time");
	}
	
	public void animate() {
		clump.readParams(params);
		plot.registerLines("phi", new PointSet(0, clump.dx, clump.phi), Color.BLACK);
		plot.registerLines("bar", new PointSet(0, clump.dx, clump.phibar), Color.RED);
		
		// clump.convolveWithRange(clump.phi, clump.phibar, clump.R);
		// double[] temp = new double[clump.phi.length];
		// clump.convolveWithRangeSlow(clump.phi, temp, clump.R);
		// plot.registerLines("bar2", new PointSet(0, clump.dx, temp), Color.BLUE);
		
		params.set("time", format(clump.t));
	}
	
	public void clear() {
		plot.clear();
	}
	
	public void run() {
		clump = new ClumpRadial(params);
		Job.animate();
		while (true) {
			double var1 = clump.rvariance(clump.phi);
			clump.simulate();
			double var2 = clump.rvariance(clump.phi);
			double scale = var1/var2;
			if (params.sget("Saddle").equals("Yes")) {
				clump.scaleField(scale);
			}
			Job.animate();
		}
	}
}
