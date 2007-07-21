package rachele.ising.dim2.apps;

/* This is basically the same as kip.clump.dim2.FiledClump2D.java */

//import static kip.util.MathPlus.j1;

import static java.lang.Math.*;
import rachele.ising.dim2.*;
import kip.clump.dim2.StructureFactor;
//import scikit.dataset.Function;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class IsingField2DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    StructureFactor sf;
    IsingField2D ising;

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Clump Model");
	}
	
	public IsingField2DApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.1);
		params.addm("dt", 0.01);
		params.add("R", 1000);
		params.add("L/R", 16.0);
		params.add("dx", 125.0);
		params.add("J", -2.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(-1, 1);
        //grid.setData(ising.numColumns(), ising.numColumns(), ising.coarseGrained());
	}
	
	public void run() {
		ising = new IsingField2D(params);
		double [] tester;
		tester = new double [ising.Lp*ising.Lp];
		for (int i = 0; i < ising.Lp*ising.Lp; i++)
			tester[i] = 0.0; 
        grid.setData(ising.numColumns(), ising.numColumns(), ising.phi);
        
        double binWidth = params.fget("kR bin-width");
        binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth);
		sf.setBounds(0.1, 14);
        plot.setDataSet(0, sf.getAccumulator());
        //plot.setDataSet(1, new Function(sf.kRmin(), sf.kRmax()) {
        //	public double eval(double kR) {
        //		double V = 2*j1(kR)/kR;
        //		return 1/(V/ising.T+1);
        //	}
        //});
        Job.addDisplay(grid);
        Job.addDisplay(plot);
        
        boolean equilibrating = true;
        while (true) {
			params.set("Time", ising.time());
			ising.simulate();
			if (equilibrating && ising.time() >= 15) {
				equilibrating = false;
				sf.getAccumulator().clear();
			}
			for (int i = 0; i < ising.Lp*ising.Lp; i++)
				tester[i] += (double)(i)/(ising.Lp*ising.Lp); 
			//ising.accumulateIntoStructureFactor(sf);
			sf.accumulate(ising.phi);
			Job.animate();
		}
 	}
}
