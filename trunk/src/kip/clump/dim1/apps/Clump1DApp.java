package kip.clump.dim1.apps;

import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim1.AbstractClump1D;
import kip.clump.dim1.FieldClump1D;
import kip.clump.dim1.StructureFactor1D;
import scikit.dataset.Function;
import scikit.dataset.PointSet;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;


public class Clump1DApp extends Simulation {
    Plot plot = new Plot("Clump density");
    Plot sfplot = new Plot("Structure factor");
    
    StructureFactor1D sf;
    FieldClump1D clump;

	public static void main(String[] args) {
		new Control(new Clump1DApp(), "Clump Model");
	}
	
	public Clump1DApp() {
		frame(plot, sfplot);
		params.addm("Noisy", new ChoiceValue("Yes", "No"));
		params.addm("T", new DoubleValue(0.09, 0, 0.3).withSlider());
		params.addm("dt", 1.0);
		params.add("R", 10000.0);
		params.add("L", 100000.0);
		params.add("dx", 100.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
		flags.add("Clear S.F.");
	}
	
	public void animate() {		
		if (flags.contains("Clear S.F."))
			sf.getAccumulator().clear();
		flags.clear();
		
		clump.readParams(params);
		clump.useNoiselessDynamics(!params.sget("Noisy").equals("Yes"));
		plot.registerLines("Clump data", new PointSet(0, clump.dx, clump.coarseGrained()), Color.BLACK);
		
		sfplot.clear();
		sfplot.registerLines("Structure data", sf.getAccumulator(), Color.BLACK);
		if (clump.T > AbstractClump1D.T_SP) {
			sfplot.registerLines("Structure theory", new Function(sf.kRmin(), sf.kRmax()) {
				public double eval(double kR) {
					return 1/(clump.potential(kR)/clump.T+1);
				}
			}, Color.BLUE);
		}
	}
	
	public void clear() {
		plot.clear();
	}
	
	public void run() {
		clump = new FieldClump1D(params);
        sf = clump.newStructureFactor(params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
        
        while (true) {
			params.set("Time", clump.time());
			clump.simulate();
			clump.accumulateIntoStructureFactor(sf);
			Job.animate();
		}
 	}
}

