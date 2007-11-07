package kip.clump.dim2.apps;

import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim2.FieldClump2D;
import scikit.dataset.PointSet;
import scikit.graphics.GrayScale;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;


public class SaddleApp extends Simulation {
	Grid grid = new Grid("Grid");
	FieldClump2D clump;
	boolean periodic;
	Plot plot = new Plot("");
	
	public static void main(String[] args) {
		new Control(new SaddleApp(), "Clump Model Saddle Profile");
	}

	public SaddleApp() {
		frame(grid, plot);
		params.addm("Periodic", new ChoiceValue("Yes", "No"));
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.addm("Circular", new ChoiceValue("No", "Yes"));
		params.addm("T", 0.135);
		params.addm("dt", 1.0);
		params.add("R", 1000.0);
		params.add("L", 30000.0);
		params.add("dx", 100.0);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		params.add("Valid profile");
		flags.add("Res up");
		flags.add("Res down");
	}
	
	public void animate() {
		if (flags.contains("Res up"))
			clump.doubleResolution();
		if (flags.contains("Res down"))
			clump.halveResolution();
		flags.clear();
		
		periodic = params.sget("Periodic").equals("Yes");
		clump.useFixedBoundaryConditions(!periodic);
		clump.readParams(params);
		
		grid.setColors(new GrayScale());
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0.2, 4);
		grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		
		int Lp = clump.numColumns();
		double[] section = new double[Lp];
		System.arraycopy(clump.coarseGrained(), Lp*(Lp/2), section, 0, Lp);
		plot.registerLines("", new PointSet(0, 1, section), Color.BLUE);
		
		params.set("dx", clump.dx);
		params.set("R", clump.R);
		params.set("Time", format(clump.time()));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		params.set("Valid profile", !clump.rescaleClipped);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
	}
	
	public void run() {
		clump = new FieldClump2D(params);
		clump.useNoiselessDynamics(true);
		clump.initializeFieldWithSeed();
		Job.animate();
		
		while (true) {
			double var1 = clump.phiVariance();
			clump.simulate();
			double var2 = clump.phiVariance();
			double scale = var1/var2;
			
			if (params.sget("Saddle").equals("Yes")) {
				clump.scaleField(scale);
			}
			if (params.sget("Circular").equals("Yes")) {
				clump.circularAverage();
			}
			if (periodic) {
				clump.R -= sqr(clump.R)*clump.dFdensity_dR();
			}
			Job.animate();
		}
	}
}
