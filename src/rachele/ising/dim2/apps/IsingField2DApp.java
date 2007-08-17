package rachele.ising.dim2.apps;

/* This is basically the same as kip.clump.dim2.FiledClump2D.java */

//import static kip.util.MathPlus.j1;

import static java.lang.Math.*;
import rachele.ising.dim2.*;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.params.DoubleValue;
import scikit.plot.FieldDisplay;
import scikit.plot.Plot;


public class IsingField2DApp extends Simulation {
    FieldDisplay grid = new FieldDisplay("Phi(x)", true);
    FieldDisplay sfGrid = new FieldDisplay("S(k)", true);
    Plot hSlice = new Plot("Horizontal Slice", true);
    Plot vSlice = new Plot("Vertical Slice", true);
	Plot structurePeak = new Plot("Structure Peak vs Time", true);
    StructureFactor sf;
    IsingField2D ising;

    
	public static void main(String[] args) {
		new Control(new IsingField2DApp(), "Ising Field");
	}
	
	public IsingField2DApp() {
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Approx", new ChoiceValue("Linear", "Exact", "Phi4"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("T", 0.03);
		params.addm("dt", 1.0);
		params.addm("R", 1000000);
		params.add("L/R", 8.0);
		params.add("R/dx", 8.0);
		params.add("J", -1.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.0);
		params.add("Time");
		params.add("Mean Phi");
		params.add("Lp");
		
		flags.add("Clear S.F.");
	}
	
	public void animate() {
		
		ising.readParams(params);
		
		if (params.sget("Zoom").equals("Yes"))
			grid.setAutoScale();
		else
			grid.setScale(-1.0, 1.0);

		sfGrid.setAutoScale();
				
		hSlice.clear();
		vSlice.clear();
		
		hSlice.setDataSet(0, ising.getHslice());
		vSlice.setDataSet(0, ising.getVslice());

        structurePeak.setDataSet(3, sf.getPeakV());
        structurePeak.setDataSet(4, sf.getPeakH());
        structurePeak.setDataSet(5, sf.getPeakC());
        
		if (flags.contains("Clear S.F.")) {
			sf.getAccumulatorCA().clear();
			sf.getAccumulatorHA().clear();
			sf.getAccumulatorVA().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void run() {
		Job.addDisplay(grid);
		Job.addDisplay(sfGrid);
		Job.addDisplay(hSlice);
		Job.addDisplay(vSlice);
		Job.addDisplay(structurePeak);

		ising = new IsingField2D(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
		grid.setData(ising.Lp,ising.Lp,ising.phi);
		sfGrid.setData(ising.Lp, ising.Lp,sf.sFactor);
		
        boolean equilibrating = true;
        while (true) {
			params.set("Time", ising.time());
			params.set("Mean Phi", ising.mean(ising.phi));
			ising.simulate();
			if (equilibrating && ising.time() >= .5) {
			equilibrating = false;
			}
			sf.accumulateAll(ising.time(), ising.coarseGrained());
			Job.animate();
		}
 	}
}
