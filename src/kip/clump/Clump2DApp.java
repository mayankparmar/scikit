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
		params.add("R", 2);
		params.add("L/R", 2);
		params.add("R/dx", 8);
		params.addm("T", 1.0);
		params.addm("kR bin-width", 0.025);
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
        addDisplay(grid);
        
		sf = new StructureFactor(clump.numColumns(), clump.L, clump.R, params.fget("kR bin-width"));
		sf.setBounds(2, 10);
        plot.setDataSet(0, sf.getAccumulator());
        plot.setDataSet(1, new Function(sf.kRmin, sf.kRmax) {
        	public double eval(double kR) {
        		double V = 2*j1(kR)/kR;
        		return 1/(V/clump.T+1);
        	}
        });
        addDisplay(plot);
        
        while (true) {
			params.set("Time", clump.time());
			for (int i = 0; i < clump.numPts/4.; i++) {
				clump.mcsTrial();
				yield();
			}
			sf.accumulate(clump.coarseGrained());
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
