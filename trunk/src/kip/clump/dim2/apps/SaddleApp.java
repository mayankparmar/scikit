package kip.clump.dim2.apps;

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
		params.add("L/R", 10.0);
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
		
		clump.useFixedBoundaryConditions(!params.sget("Periodic").equals("Yes"));
		
		clump.readParams(params);
		if (params.sget("Zoom").equals("Yes"))
			grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		else
			grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained(), 0, 2);
		
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
		boolean periodic = params.sget("Periodic").equals("Yes");
		
		clump = new FieldClump2D(params);
		clump.useNoiselessDynamics(true);
		clump.useNaturalDynamics(true);
		clump.initializeFieldWithSeed();
		Job.animate();
		
		while (true) {
			double var1 = clump.phiVariance();
			clump.simulate();
			double var2 = clump.phiVariance();
			double scale = var1/var2;
			clump.scaleField(scale);
			
			if (periodic) {
				clump.R -= 20*clump.dFdensity_dR();
			}
			Job.animate();
		}
	}
}
