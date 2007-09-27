package kip.clump.dim2.apps;

import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;
import kip.clump.dim2.FieldClump2D;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;


public class SaddleApp extends Simulation {
	Grid grid = new Grid("Grid");
	FieldClump2D clump;
	boolean periodic;
	
	public static void main(String[] args) {
		new Control(new SaddleApp(), "Clump Model Saddle Profile");
	}

	public SaddleApp() {
		frame(grid);
		params.addm("Periodic", new ChoiceValue("Yes", "No"));
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.135);
		params.addm("dt", 1.0);
		params.add("R", 1000.0);
		params.add("L", 10000.0);
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
		params.set("dx", clump.dx);
		
		periodic = params.sget("Periodic").equals("Yes");
		clump.useFixedBoundaryConditions(!periodic);
		
		clump.readParams(params);
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0, 2);
		grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		
		params.set("R", clump.R);
		params.set("Time", format(clump.time()));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		params.set("Valid profile", !clump.rescaleClipped);
	}
	
	public void clear() {
		grid.clear();
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
			clump.scaleField(scale);
			
			if (periodic) {
				clump.R -= sqr(clump.R)*clump.dFdensity_dR();
			}
			Job.animate();
		}
	}
}
