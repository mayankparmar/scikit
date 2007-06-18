package kip.clump3d.apps;

import kip.clump3d.Clump3D;
import kip.clump3d.StructureFactor3D;
import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class Clump3DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor3D sf;
    Clump3D clump;
 	
	public static void main(String[] args) {
		new Control(new Clump3DApp(), "Clump Model");
	}
	
	public Clump3DApp() {
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
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(0, 2);
	}
	
	public void run() {
		clump = new Clump3D(params);
		
		// one 2D slice is presented in field display
        grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
        
        sf = clump.newStructureFactor(params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
		plot.setDataSet(0, sf.getAccumulator());
		plot.setDataSet(1, new Function(sf.kRmin(), sf.kRmax()) {
        	public double eval(double kR) {
        		return 1/(clump.potential(kR)/clump.T+1);
	        }
		});
        Job.addDisplay(grid);
        Job.addDisplay(plot);
        
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
