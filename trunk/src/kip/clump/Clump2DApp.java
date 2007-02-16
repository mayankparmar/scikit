package kip.clump;

import static kip.util.MathPlus.*;

import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;
import scikit.plot.Function;


public class Clump2DApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor sf;
    Clump2D clump;
	
	
	public static void main(String[] args) {
		frame(new Control(new Clump2DApp()), "Nucleation");
	}

	public Clump2DApp() {
		params.add("R", 12);
		params.add("L/R", 16);
		params.add("dx", 2);
		params.addm("T", 0.15);
		params.addm("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		clump.getParams(params);
		sf.getAccumulator().setBinWidth(params.fget("kR bin-width"));
	}
	
	public void run() {
		clump = new Clump2D(params);
        grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
        
		sf = new StructureFactor((int)(2*clump.L), clump.L, clump.R, params.fget("kR bin-width"));
		sf.setBounds(0.1, 14);
        plot.setDataSet(0, sf.getAccumulator());
        plot.setDataSet(1, new Function(sf.kRmin, sf.kRmax) {
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
			for (int i = 0; i < clump.numPts/2.; i++) {
				clump.mcsTrial();
			}
			if (equilibrating && clump.time() >= 15) {
				equilibrating = false;
				sf.clear(params.fget("kR bin-width"));
				plot.setDataSet(0, sf.getAccumulator());				
			}
			sf.accumulate(clump.ptsX, clump.ptsY);
			yield();
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
