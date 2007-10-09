package kip.clump.radial;

import static scikit.util.Utilities.*;

import java.awt.Color;

import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;

public class ClumpRadialApp extends Simulation {
	Plot plot = new Plot("");
	ClumpRadial clump;
	
	public static void main(String[] args) {
		new Control(new ClumpRadialApp(), "Radial Clump");
	}
	
	public ClumpRadialApp() {
		frame(plot);
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.addm("Dimension", new ChoiceValue("2", "3"));
		params.addm("T", 0.09);
		params.addm("dt", 0.1);
		params.addm("R", 1000.);
		params.add("L", 4000.);
		params.add("dx", 20.);
		params.add("time");
	}
	
	public void animate() {
		clump.readParams(params);
		clump.convolveWithRange(clump.phi, clump.phibar, clump.R);
		plot.registerLines("", new PointSet(0, clump.dx, clump.phi), Color.BLACK);
		plot.registerLines("ba", new PointSet(0, clump.dx, clump.phibar), Color.RED);
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
