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
import scikit.params.DoubleValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class IsingField2DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    Plot hSlice = new Plot("Horizontal Slice", true);
    Plot vSlice = new Plot("Vertical Slice", true);
    StructureFactor sf;
    IsingField2D ising;

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Ising Field");
	}
	
	public IsingField2DApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("T", 0.1);
		params.addm("dt", 0.01);
		params.addm("R", 1000);
		params.add("L/R", 8.0);
		params.add("R/dx", 16.0);
		params.add("J", -10.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Density", 0.0);
		params.add("Time");
		params.add("Mean Phi");
	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(-1.0, 1.0);
		hSlice.clear();
		vSlice.clear();
		hSlice.setDataSet(0, ising.getHslice());
		vSlice.setDataSet(0, ising.getVslice());
		//for (int i=0; i<ising.Lp; i++){
        //	slice.append(0, i, ising.phi[i]);
        //}
		//grid.setData(ising.numColumns(), ising.numColumns(), ising.coarseGrained());
	}
	
	public void run() {
		ising = new IsingField2D(params);
		grid.setData(ising.Lp,ising.Lp,ising.phi);
		
        double binWidth = params.fget("kR bin-width");
        binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth);
		sf.setBounds(0.1, 14);
        plot.setDataSet(0, sf.getAccumulator());
        //for (int i=0; i<ising.Lp; i++){
        //	slice.append(0, ising.phi[i],(double)i);
        //}
        //plot.setDataSet(1, new Function(sf.kRmin(), sf.kRmax()) {
        //	public double eval(double kR) {
        //		double V = 2*j1(kR)/kR;
        //		return 1/(V/ising.T+1);
        //	}
        //});
        Job.addDisplay(grid);
        Job.addDisplay(plot);
        Job.addDisplay(hSlice);
        Job.addDisplay(vSlice);
        boolean equilibrating = true;
        while (true) {
			params.set("Time", ising.time());
			params.set("Mean Phi", ising.mean(ising.phi));
			ising.simulate();
			if (equilibrating && ising.time() >= .5) {
			equilibrating = false;
				sf.getAccumulator().clear();
			}
			//ising.accumulateIntoStructureFactor(sf);
			sf.accumulate(ising.phi);
			Job.animate();
		}
 	}
}
