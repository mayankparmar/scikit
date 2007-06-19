package kip.clump3d.apps;

import static scikit.util.Utilities.format;
import kip.clump3d.Clump3D;
import kip.clump3d.StructureFactor3D;
import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class ClumpCollection3DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor3D sf;
	
	
	public static void main(String[] args) {
		new Control(new ClumpCollection3DApp(), "Clump Model -- S(k) Collection");
	}

	public ClumpCollection3DApp() {
		params.add("Output directory", "/Users/kbarros/Desktop/output/");
		params.add("R", 6.0);
		params.add("L/R", 6.0);
		params.add("dx", 2.0);
		params.add("dt", 1.0);
		params.add("T min", 0.1);
		params.add("T max", 0.2);
		params.add("T iterations", 10);
		params.add("Equilibration time", 50.);
		params.add("Stop time", 1000.);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("T");
		params.add("Time");
	}
	
	public void run() {
        Job.addDisplay(grid);
        Job.addDisplay(plot);
        
        int iters = params.iget("T iterations");
        double dT = (params.fget("T max") - params.fget("T min")) / iters;
        params.set("T", params.fget("T min"));
        
        for (int i = 0; i < iters; i++) {
            params.set("Random seed", params.iget("Random seed")+1);
    		final Clump3D clump = new Clump3D(params);
            grid.setData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
            
            sf = clump.newStructureFactor(params.fget("kR bin-width"));
    		sf.setBounds(0.1, 14);
            plot.setDataSet(0, sf.getAccumulator());
            plot.setDataSet(1, new Function(sf.kRmin(), sf.kRmax()) {
            	public double eval(double kR) {
            		return 1/(clump.potential(kR)/clump.T+1);
            	}
            });
            
            double eqTime = params.fget("Equilibration time");
            while (clump.time() < eqTime) {
            	clump.accumulateIntoStructureFactor(sf);
            	params.set("Time", clump.time());
            	clump.simulate();
        		Job.animate();
            }
            sf.getAccumulator().clear();
            double stopTime = params.fget("Stop time");
            while (clump.time() < stopTime) {
            	clump.accumulateIntoStructureFactor(sf);
            	params.set("Time", clump.time());
            	clump.simulate();
        		Job.animate();
            }
            
            String filename = params.sget("Output directory")+
                "/R="+clump.R+",T="+format(clump.T)+"" +
                ",ts="+eqTime+",tf="+stopTime+".txt";
            scikit.util.Dump.dumpColumns(filename, sf.getAccumulator().copyData(), 2);
            
            params.set("T", format(clump.T+dT));
        }
	}
}
