package rachele.ising.dim2.apps;


import static java.lang.Math.floor;
import static scikit.util.Utilities.*;
import rachele.ising.dim2.IsingField2Dopt;
import rachele.util.FileUtil;
//import rachele.ising.dim2.StructureFactor;
//import rachele.util.FileUtil;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.dataset.Accumulator;
import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DoubleValue;
import scikit.graphics.dim2.Plot;
import java.awt.Color;

public class IsingField2DoptApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
    Grid delPhiGrid = new Grid("delPhi(x)");
    Grid sfGrid = new Grid("S(k)");
    Plot fePlot = new Plot("Free Energy");
	//StructureFactor sf;
    IsingField2Dopt ising;
	boolean initFile = false;
	Accumulator freeEnergy;
    
    public static void main(String[] args) {
		new Control(new IsingField2DoptApp(), "Ising Field");
	}
	
	public IsingField2DoptApp(){
		frameTogether("Grids", grid, sfGrid, delPhiGrid, fePlot);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Square", "Circle"));
		params.addm("Theory", new ChoiceValue("Exact", "Slow Near Edge"));
		params.addm("Dynamics?", new ChoiceValue("Langevin No M Convervation", "Langevin Conserve M"));
		params.addm("Noise", new DoubleValue(0, 0, 1.0).withSlider());
		params.addm("T", 0.03);
		params.addm("H", 0.7);
		params.addm("Rx", 400.0);
		params.addm("Ry", 400.0);
		params.add("L", 1000.0);
		params.add("dx", 10.0);
		params.add("Random seed", 0);
		params.add("Magnetization", 0.0);
		params.addm("range change", 0.01);
		params.add("Time");
		params.add("Free Energy");
		params.add("Pot");
		params.add("Ent");
		flags.add("Write Config");
		flags.add("Record FE");
		flags.add("Clear");

	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes"))grid.setAutoScale();
		else grid.setScale(-1, 1);
		fePlot.setAutoScale(true);
		freeEnergy.accum(ising.t, ising.freeEnergy);
		fePlot.registerLines("FE", freeEnergy, Color.RED);
		//sfGrid.registerData(ising.Lp, ising.Lp, sf.sFactor);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		delPhiGrid.registerData(ising.Lp, ising.Lp, ising.phiVector);
		params.set("Rx", ising.Rx);
		params.set("Ry", ising.Ry);
		params.set("Free Energy", ising.freeEnergy);
		params.set("Pot", ising.potAccum);
		params.set("Ent", ising.entAccum);
		if(flags.contains("Clear")) freeEnergy.clear();
		if(flags.contains("Record FE")) recordTvsFE();
		flags.clear();
	}
	
	public void clear() {
	}
	
	public void run() {
		ising = new IsingField2Dopt(params);
		freeEnergy = new Accumulator(1.0);
		double binWidth = 0.1;//params.fget("kR bin-width");
		binWidth = IsingField2Dopt.KR_SP / floor(IsingField2Dopt.KR_SP/binWidth);
        //sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		//sf.setBounds(0.1, 14);
		
        while (true) {
        	ising.readParams(params);
			params.set("Time", ising.time());
			ising.simulate();
			ising.adjustRanges();
			//writeDataToFile();
			//sf.accumulateAll(ising.time(), ising.coarseGrained());
			Job.animate();
		}
 	}
	
	public void recordTvsFE(){
		String file = "../../../research/javaData/feData/fe";
		FileUtil.printlnToFile(file, ising.T, ising.freeEnergy);
		System.out.println("Wrote to file: T = " + ising.T + " FE = " + ising.freeEnergy);
	}
}
