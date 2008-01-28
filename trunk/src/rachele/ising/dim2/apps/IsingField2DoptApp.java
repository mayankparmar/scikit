package rachele.ising.dim2.apps;


import static java.lang.Math.floor;
import static scikit.util.Utilities.*;
import rachele.ising.dim2.IsingField2D;
import rachele.ising.dim2.StructureFactor;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;

public class IsingField2DoptApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
    Grid sfGrid = new Grid("S(k)");
	StructureFactor sf;
    IsingField2D ising;

    public static void main(String[] args) {
		new Control(new IsingField2DoptApp(), "Ising Field");
	}
	
	public IsingField2DoptApp(){
		frameTogether("Grids", grid, sfGrid);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Square", "Circle"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Langevin Conserve M","Langevin No M Convervation"));
		params.addm("Plot FEvT", new ChoiceValue("Off", "On"));
		params.addm("Horizontal Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
		params.addm("Vertical Slice", new DoubleValue(0.5, 0, 0.9999).withSlider());
//		params.addm("kR", new DoubleValue(5.135622302, 0.0, 6.0).withSlider());
		params.addm("T", 0.09);
		params.addm("H", 0.0);
		params.addm("tolerance", 0.0001);
		params.addm("dt", 1.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 5.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.7);
		params.add("Time");
		params.add("Lp");

		flags.add("Write Config");
		flags.add("Clear");
		flags.add("SF");
	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes")) 
			grid.setAutoScale();
		else 
			grid.setScale(-1, 1);
		sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
	}
	
	public void clear() {
	}
	
	public void run() {
		ising = new IsingField2D(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingField2D.KR_SP / floor(IsingField2D.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		
        while (true) {
        	ising.readParams(params);
			params.set("Time", ising.time());
			ising.simulate();
			sf.accumulateAll(ising.time(), ising.coarseGrained());
			Job.animate();
		}
 	}
	
}
