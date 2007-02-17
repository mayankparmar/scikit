package kip.clump;

import static kip.util.MathPlus.*;
import static scikit.jobs.DoubleValue.format;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;
import scikit.plot.Function;


public class ClumpCollectionApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor sf;
	
	
	public static void main(String[] args) {
		frame(new Control(new ClumpCollectionApp()), "Clump Model -- S(k) Collection");
	}

	public ClumpCollectionApp() {
		params.add("Output directory", "/Users/kbarros/Desktop/output/");
		params.add("R", 12);
		params.add("L/R", 16);
		params.add("dx", 3);
		params.add("T min", 0.15);
		params.add("T max", 0.25);
		params.add("T iterations", 5);
		params.add("Equilibration time", 50.);
		params.add("Stop time", 1000.);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("T");
		params.add("Time");
	}
	
	public void run() {
        addDisplay(grid);
        addDisplay(plot);
        
        int iters = params.iget("T iterations");
        double dT = (params.fget("T max") - params.fget("T min")) / iters;
        params.set("T", params.fget("T min"));
        
        for (int i = 0; i < iters; i++) {
            params.set("Random seed", params.iget("Random seed")+1);
    		final Clump2D clump = new Clump2D(params);
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
            
            double eqTime = params.fget("Equilibration time");
            while (clump.time() < eqTime) {
            	sf.accumulate(clump.ptsX, clump.ptsY);
            	params.set("Time", clump.time());
            	for (int j = 0; j < clump.numPts/2; j++)
            		clump.mcsTrial();
        		yield();
            }
            sf.getAccumulator().clear();
            double stopTime = params.fget("Stop time");
            while (clump.time() < stopTime) {
            	sf.accumulate(clump.ptsX, clump.ptsY);
            	params.set("Time", clump.time());
            	for (int j = 0; j < clump.numPts/2; j++)
            		clump.mcsTrial();
        		yield();
            }
            
            String filename = params.sget("Output directory")+
                "/R="+clump.R+",T="+format(clump.T)+"" +
                ",ts="+eqTime+",tf="+stopTime+".txt";
            scikit.util.Dump.doubleArray(filename, sf.acc.copyData(), 2);
            
            params.set("T", format(clump.T+dT));
        }
	}
}
