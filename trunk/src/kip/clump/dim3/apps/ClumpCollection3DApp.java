package kip.clump.dim3.apps;

import static scikit.util.Utilities.format;

import java.awt.Color;

import kip.clump.dim3.Clump3D;
import kip.clump.dim3.StructureFactor3D;
import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;


public class ClumpCollection3DApp extends Simulation {
    Grid grid = new Grid("Grid");
    Plot plot = new Plot("Structure factor");
    StructureFactor3D sf;
    Clump3D clump;
    
	
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
	
	public void animate() {
       	params.set("Time", clump.time());
       	grid.registerData(clump.numColumns(), clump.numColumns(), clump.coarseGrained());
        plot.registerLines("Structure factor", sf.getAccumulator(), Color.BLACK);
        plot.registerLines("Structure data", new Function(sf.kRmin(), sf.kRmax()) {
        	public double eval(double kR) {
        		return 1/(clump.potential(kR)/clump.T+1);
        	}
        }, Color.BLUE);		
	}
	
	public void clear() {
		grid.clear();
		plot.clear();
	}
	
	public void run() {
        int iters = params.iget("T iterations");
        double dT = (params.fget("T max") - params.fget("T min")) / iters;
        params.set("T", params.fget("T min"));
        
        for (int i = 0; i < iters; i++) {
            params.set("Random seed", params.iget("Random seed")+1);
    		clump = new Clump3D(params);
            
            sf = clump.newStructureFactor(params.fget("kR bin-width"));
    		sf.setBounds(0.1, 14);
            
            double eqTime = params.fget("Equilibration time");
            while (clump.time() < eqTime) {
            	clump.accumulateIntoStructureFactor(sf);
            	clump.simulate();
        		Job.animate();
            }
            sf.getAccumulator().clear();
            double stopTime = params.fget("Stop time");
            while (clump.time() < stopTime) {
            	clump.accumulateIntoStructureFactor(sf);
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
