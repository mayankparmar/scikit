package kip.clump.dim3.apps;

import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim3.FieldClump3D;
import scikit.dataset.Accumulator;
import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;

public class Saddle3DApp extends Simulation {
	Grid3D grid = new Grid3D("Grid");
    Plot plot = new Plot("Structure factor");
    Plot slice = new Plot("Slice");
	FieldClump3D clump;
	boolean periodic;
	boolean findSaddle;
    Accumulator sf;
    
	double kRmin = 0.1;
	double kRmax = 14;
	double kRwidth = 0.1;

	
	public static void main(String[] args) {
		new Control(new Saddle3DApp(), "3D Clump Saddle Profile");
	}

	public Saddle3DApp() {
		frame(grid, plot, slice);
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.addm("Periodic", new ChoiceValue("Yes", "No"));
		params.addm("T", new DoubleValue(0.09, 0.0, 0.15).withSlider());
		params.addm("dt", 1.0);
		params.add("Seed", new ChoiceValue("BCC", "Triangle", "Noise"));
		params.add("R", 1300.0);
		params.add("L", 4000.0);
		params.add("dx", 100.0);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		params.add("Valid profile");
		params.add("Rx");
		params.add("Ry");
		params.add("Rz");
		flags.add("Res up");
		flags.add("Res down");
		flags.add("Calc S.F.");
		flags.add("Load config.");
	}
	
	public void animate() {
		if (flags.contains("Res up")) {
			clump.doubleResolution();
		}
		if (flags.contains("Res down")) {
			clump.halveResolution();
		}
		if (flags.contains("Calc S.F.")) {
	        sf = clump.newStructureAccumulator(kRwidth);
			clump.accumulateStructure(sf);
		}
		if (flags.contains("Load config.")) {
			grid.extractData(clump.coarseGrained());
		}
		flags.clear();
		
		findSaddle = params.sget("Saddle").equals("Yes");
		periodic = params.sget("Periodic").equals("Yes");
		
		clump.useNoiselessDynamics(findSaddle);
		clump.useFixedBoundaryConditions(!periodic);		
		clump.readParams(params);

		int Lp = clump.numColumns();
		grid.registerData(Lp, Lp, Lp, clump.coarseGrained());
		
		plot.registerLines("Structure data", sf, Color.BLACK);
		
		double[] sliceData = new double[Lp];
		System.arraycopy(clump.coarseGrained(), (Lp*Lp)*(Lp/2)+Lp*(Lp/2), sliceData, 0, Lp);
		slice.registerLines("Slice", new PointSet(0, 1, sliceData), Color.BLACK);
		
		params.set("dx", clump.dx);
		params.set("Rx", format(clump.R));
		params.set("Time", format(clump.time()));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		params.set("Valid profile", !clump.rescaleClipped);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
		slice.clear();
	}
	
	public void run() {
		clump = new FieldClump3D(params);
		clump.initializeFieldWithSeed(params.sget("Seed"));
        
		Job.animate();
		
		while (true) {
			double var1 = clump.phiVariance();
			clump.simulate();
			double var2 = clump.phiVariance();
			double scale = var1/var2;
			if (findSaddle)
				clump.scaleField(scale);
			if (periodic)
				clump.R -= sqr(clump.R)*clump.dFdensity_dR();
			Job.animate();
		}
	}
}
