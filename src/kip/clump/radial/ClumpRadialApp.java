package kip.clump.radial;

import static scikit.util.Utilities.frame;

import java.awt.Color;

import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;

public class ClumpRadialApp extends Simulation {
	Plot plot = new Plot("");
	ClumpRadial clump;
	
	public ClumpRadialApp() {
		frame(plot);
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.9);
		params.addm("dt", 0.1);
		params.addm("R", 1000.);
		params.add("L", 4000);
		params.add("dx", 100);
	}
	
	public void animate() {
		plot.registerLines("", new PointSet(0, clump.dx, clump.phi), Color.BLACK);
	}
	
	public void clear() {
		plot.clear();
	}
	
	public void run() {
		clump = new ClumpRadial(params);
	}
}
