package kip.clump.apps;

import static kip.util.MathPlus.j1;
import kip.clump.*;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.Function;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;


public class Clump2DApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor sf;
    FieldClump2D clump;
	
	
	public static void main(String[] args) {
		frame(new Control(new Clump2DApp()), "Clump Model");
	}

	public Clump2DApp() {
		params.addm("T", 0.15);
		params.add("R", 12.0);
		params.add("L/R", 16.0);
		params.add("dx", 3.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		clump.readParams(params);
	}
	
	public void run() {
		clump = new FieldClump2D(params, false);
        grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
        grid.setBounds(0, 2*Clump2D.DENSITY);
        
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
			clump.simulate(1);
			if (equilibrating && clump.time() >= 15) {
				equilibrating = false;
				sf.getAccumulator().clear();
			}
			clump.accumulateIntoStructureFactor(sf);
			yield();
		}
 	}
}
