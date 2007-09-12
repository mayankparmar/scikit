package kip.clump.dim3.apps;

import static scikit.util.Utilities.frame;

import java.awt.Color;

import kip.clump.dim3.Clump3D;
import kip.clump.dim3.StructureFactor3D;
import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;


public class Clump3DApp extends Simulation {
    Grid grid = new Grid("Grid");
    Plot plot = new Plot("Structure factor");
    StructureFactor3D sf;
    Clump3D clump;
 	
	public static void main(String[] args) {
		new Control(new Clump3DApp(), "Clump Model");
	}
	
	public Clump3DApp() {
		frame(grid, plot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.15);
		params.addm("dt", 1.0);
		params.add("R", 4.0);
		params.add("L/R", 4.0);
		params.add("dx", 2.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		clump.readParams(params);
		
		plot.registerLines("Structure data", sf.getAccumulator(), Color.BLACK);
		plot.registerLines("Structure theory", new Function(sf.kRmin(), sf.kRmax()) {
        	public double eval(double kR) {
        		return 1/(clump.potential(kR)/clump.T+1);
	        }
		}, Color.BLUE);
		
		// one 2D slice is presented in field display
		if (params.sget("Zoom").equals("Yes"))
			grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
		else
			grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained(), 0, 2);
	}
	
	public void clear() {
		plot.clear();
		grid.clear();
	}
	
	public void run() {
		clump = new Clump3D(params);
        
        sf = clump.newStructureFactor(params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
        
        boolean equilibrating = true;
        while (true) {
			params.set("Time", clump.time());
			clump.simulate();
			if (equilibrating && clump.time() >= 15) {
				equilibrating = false;
				sf.getAccumulator().clear();
			}
			clump.accumulateIntoStructureFactor(sf);
			Job.animate();
		}
 	}

}