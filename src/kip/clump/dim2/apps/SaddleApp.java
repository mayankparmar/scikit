package kip.clump.dim2.apps;

import kip.clump.dim2.*;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;


public class SaddleApp extends Simulation {
	FieldDisplay grid = new FieldDisplay("Grid", true);
	FieldClump2D clump;

	public static void main(String[] args) {
		new Control(new SaddleApp(), "Clump Model Saddle Profile");
	}

	public SaddleApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.135);
		params.addm("dt", 1.0);
		params.add("R", 1000);
		params.add("L/R", 32.0);
		params.add("dx", 125.0);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("dF/dphi");
		params.add("Valid profile");
	}

	public void animate() {
		clump.readParams(params);
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0, 2);
		
		params.set("Time", clump.time());
		params.set("dF/dphi", clump.rms_dF_dphi);
		params.set("Valid profile", !clump.rescaleClipped);
	}
	
	public void run() {
		clump = new FieldClump2D(params);
		clump.useNoiselessDynamics();
		clump.useFixedBoundaryConditions();
		clump.initializeFieldWithSeed();
		
		grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		grid.setScale(0.0, 2);
		grid.setDeltaX(clump.dx/clump.R);
		Job.addDisplay(grid);
		Job.animate();
		
		while (true) {
			
			double var1 = clump.phiVariance();
			clump.simulate();
			double var2 = clump.phiVariance();
			double scale = var1/var2;
			clump.scaleField(scale);
			
			Job.animate();
		}
	}
}