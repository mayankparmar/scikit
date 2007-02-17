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
		frame(new Control(new Clump2DApp()), "Clump Model");
	}

	public Clump2DApp() {
		params.add("R", 12.0);
		params.add("L/R", 16.0);
		params.add("dx", 3.0);
		params.addm("T", 0.15);
		params.addm("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		clump.getParams(params);
        double binWidth = clump.shiftBinWidth(params.fget("kR bin-width"));
		sf.getAccumulator().setBinWidth(binWidth);
	}
	
	public void run() {
		clump = new Clump2D(params);
        grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());

        double binWidth = clump.shiftBinWidth(params.fget("kR bin-width"));
		sf = new StructureFactor((int)(2*clump.L), clump.L, clump.R, binWidth);
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
			long t = System.currentTimeMillis();
			for (int i = 0; i < clump.numPts/2; i++)
				clump.mcsTrial();
			System.out.println(System.currentTimeMillis() - t);
			if (equilibrating && clump.time() >= 15) {
				equilibrating = false;
				sf.getAccumulator().clear();
			}
			sf.accumulate(clump.ptsX, clump.ptsY);
			yield();
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
