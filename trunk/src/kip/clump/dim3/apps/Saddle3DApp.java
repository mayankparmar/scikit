package kip.clump.dim3.apps;

import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;
import kip.clump.dim3.FieldClump3D;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;

public class Saddle3DApp extends Simulation {
	Grid grid = new Grid("Grid");
	FieldClump3D clump;
	boolean periodic;
	
	public static void main(String[] args) {
		new Control(new Saddle3DApp(), "3D Clump Saddle Profile");
	}

	public Saddle3DApp() {
		frame(grid);
		params.addm("Slice", new DoubleValue(0, 0, 0.999).withSlider());
		params.addm("Periodic", new ChoiceValue("Yes", "No"));
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.09);
		params.addm("dt", 1.0);
		params.add("R", 1000.0);
		params.add("L/R", 2.0);
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
		int Lp = clump.numColumns();
		double[] slice = new double[Lp*Lp];
		int z = (int)(params.fget("Slice")*Lp);
		System.out.println(z);
		System.arraycopy(clump.coarseGrained(), Lp*Lp*z, slice, 0, Lp*Lp);
		grid.registerData(Lp, Lp, slice);
		
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
		clump = new FieldClump3D(params);
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
			
			if (periodic)
				clump.R -= 100*clump.dFdensity_dR();
			Job.animate();
		}
	}
}
