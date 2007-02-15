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
		frame(new Control(new ClumpCollectionApp()), "Nucleation");
	}

	public ClumpCollectionApp() {
		params.add("Output directory", "/Users/kbarros/Desktop/output/");
		params.add("R", 16);
		params.add("L/R", 16);
		params.add("R/dx", 8);
		params.add("T min", 0.15);
		params.add("T max", 0.25);
		params.add("T iterations", 10);
		params.add("Equilibration time", 10.);
		params.add("Stop time", 50.);
		params.add("kR bin-width", 0.025);
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
    		final Clump2DLattice clump = new Clump2DLattice(params);
            grid.setData(clump.Lp, clump.Lp, clump.qt.rawElements);
            
    		sf = new StructureFactor(clump.Lp, clump.L, clump.R, params.fget("kR bin-width"));
    		sf.setBounds(0.1, 12);
            plot.setDataSet(0, sf.getAccumulator());
            plot.setDataSet(1, new Function(sf.kRmin, sf.kRmax) {
            	public double eval(double kR) {
            		double V = 2*j1(kR)/kR;
            		return 1/(V/clump.T+1);
            	}
            });
            
            double eqTime = params.fget("Equilibration time");
            while (clump.time() < eqTime) {
            	params.set("Time", clump.time());
            	for (int j = 0; j < clump.numPts/4.; j++) {
            		clump.mcsTrial();
            		yield();
            	}
            }
            double stopTime = params.fget("Stop time");
            while (clump.time() < stopTime) {
            	sf.accumulate(clump.qt.rawElements);
            	params.set("Time", clump.time());
            	for (int j = 0; j < clump.numPts/4.; j++) {
            		clump.mcsTrial();
            		yield();
            	}
            }
            
            String filename = params.sget("Output directory")+
                "/R="+clump.R+",T="+format(clump.T)+"" +
                ",ts="+eqTime+",tf="+stopTime+".txt";
            scikit.util.Dump.doubleArray(filename, sf.acc.copyData(), 2);
            
            params.set("T", format(clump.T+dT));
        }
	}
}
