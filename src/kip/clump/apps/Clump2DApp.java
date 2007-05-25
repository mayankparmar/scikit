package kip.clump.apps;

import static kip.util.MathPlus.j1;
import kip.clump.*;
import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class Clump2DApp extends Job {
    FieldDisplay grid = new FieldDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor sf;
    AbstractClump2D clump;
    boolean fieldDynamics = false;
	
	public static void main(String[] args) {
		frame(new Control(new Clump2DApp()), "Clump Model");
	}
	
	public Clump2DApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.15);
		params.addm("dt", 1.0);
		if (fieldDynamics) {
			params.add("R", 1000);
			params.add("L/R", 16.0);
			params.add("dx", 125.0);
		}
		else {
			params.add("R", 12.0);
			params.add("L/R", 16.0);
			params.add("dx", 3.0);
		}
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
		if (fieldDynamics)
			clump = new FieldClump2D(params);
		else
			clump = new Clump2D(params);
		
        grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
        
        sf = clump.newStructureFactor(params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
        plot.setDataSet(0, sf.getAccumulator());
        plot.setDataSet(1, new Function(sf.kRmin(), sf.kRmax()) {
        	public double eval(double kR) {
        		double V = 2*j1(kR)/kR;
        		return 1/(V/clump.T+1);
        	}
        });
        addDisplay(grid);
        addDisplay(plot);
        
        boolean equilibrating = true;
        while (true) {
			params.set("Time", clump.time());
			clump.simulate();
			if (equilibrating && clump.time() >= 15) {
				equilibrating = false;
				sf.getAccumulator().clear();
			}
			clump.accumulateIntoStructureFactor(sf);
			yield();
		}
 	}
}
