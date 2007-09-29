package kip.clump.dim3.apps;

import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.format;
import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim3.FieldClump3D;
import kip.clump.dim3.StructureFactor3D;
import scikit.dataset.Accumulator;
import scikit.graphics.dim2.Plot;
import scikit.graphics.dim3.Grid3D;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;

public class Saddle3DApp extends Simulation {
	Grid3D grid = new Grid3D("Grid");
    Plot plot = new Plot("Structure factor");
    Plot energy = new Plot("Saddle Pt. Free Energy vs Temperature");
    Accumulator energyAcc;
	FieldClump3D clump;
	boolean periodic;
	boolean findSaddle;
    StructureFactor3D sf;
    
	double kRmin = 0.1;
	double kRmax = 14;
	double kRwidth = 0.1;

	
	public static void main(String[] args) {
		new Control(new Saddle3DApp(), "3D Clump Saddle Profile");
	}

	public Saddle3DApp() {
		frame(grid, plot, energy);
		params.addm("Saddle", new ChoiceValue("Yes", "No"));
		params.addm("Periodic", new ChoiceValue("Yes", "No"));
		params.addm("T", new DoubleValue(0.09, 0.0, 0.15).withSlider());
		params.addm("dt", 1.0);
		params.add("Seed", new ChoiceValue("BCC", "Noise"));
		params.add("R", 1000.0);
		params.add("L", 2000.0);
		params.add("dx", 100.0);
		params.add("Random seed", 0);
		params.add("Time");
		params.add("F density");
		params.add("dF/dphi");
		params.add("Valid profile");
		flags.add("Res up");
		flags.add("Res down");
		flags.add("Calc S.F.");
		flags.add("Add Pt.");
	}
	
	public void animate() {
		if (flags.contains("Res up")) {
			clump.doubleResolution();
		}
		if (flags.contains("Res down")) {
			clump.halveResolution();
		}
		if (flags.contains("Calc S.F.")) {
	        sf = clump.newStructureFactor(kRwidth);
			sf.setBounds(kRmin, kRmax);
			clump.accumulateIntoStructureFactor(sf);
		}
		if (flags.contains("Add Pt.")) {
			energyAcc.accum(clump.T, clump.freeEnergyDensity);
		}
		flags.clear();
		
		findSaddle = params.sget("Saddle").equals("Yes");
		periodic = params.sget("Periodic").equals("Yes");
		
		clump.useNoiselessDynamics(findSaddle);
		clump.useFixedBoundaryConditions(!periodic);		
		clump.readParams(params);

		int Lp = clump.numColumns();
		grid.registerData(Lp, Lp, Lp, clump.coarseGrained());
		
		plot.registerLines("Structure data", sf.getAccumulator(), Color.BLACK);
		energy.registerLines("Data", energyAcc, Color.BLACK);
		
		params.set("dx", clump.dx);
		params.set("R", format(clump.R));
		params.set("Time", format(clump.time()));
		params.set("F density", format(clump.freeEnergyDensity));
		params.set("dF/dphi", format(clump.rms_dF_dphi));
		params.set("Valid profile", !clump.rescaleClipped);
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
		energy.clear();
	}
	
	public void run() {
		clump = new FieldClump3D(params);
		clump.initializeFieldWithSeed(params.sget("Seed"));
        sf = clump.newStructureFactor(kRwidth);
		energyAcc = new Accumulator(0.001);
        
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
