package rachele.ising.dim1.apps;

import static java.lang.Math.floor;
import static scikit.util.Utilities.format;
import rachele.ising.dim1.PathSample1D;
import rachele.ising.dim1.StructureFactor1D;
//import scikit.dataset.PointSet;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.params.ChoiceValue;
import scikit.plot.Plot;
import scikit.plot.FieldDisplay;

public class SaddlePoint1DApp extends Simulation{

	FieldDisplay grid = new FieldDisplay("Time vs Coarse Grained Field", true);
    //Plot SFPlot = new Plot("Structure factor", true);
	Plot timeSlice = new Plot("Configuration at t_f/2", true);
	Plot spaceSlice = new Plot("Path at Lp/2", true);
	PathSample1D sim;
    StructureFactor1D sf;
	
	
	public static void main(String[] args) {
		new Control(new SaddlePoint1DApp(), "Saddle Point 1D");
	}

	public SaddlePoint1DApp(){
		params.addm("Sampling Noise", new ChoiceValue("On", "Off"));
		params.addm("T", 0.86);
		params.addm("J", -2.0);
		params.addm("dt", 0.1);
		params.add("R", 3000);
		params.addm("H", 0.07);
		params.add("L/R", 32.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Density", -0.3);
		params.addm("du", 0.005);
		params.add("Time Interval", 200);
		params.add("u");
	}
	
	public void animate() {
		params.set("u", format(sim.u));
		
		timeSlice.setDataSet(0, sim.getTimeSlice());
		spaceSlice.setDataSet(0, sim.getSpaceSlice());
        grid.setData(sim.Lp, sim.t_f, sim.copyField());
		sim.readParams(params);
		
		//SFPlot.setDataSet(0, sf.getAccumulator());
	
		//if (flags.contains("Clear S.F.")) {
		//	sf.getAccumulator().clear();
		//	System.out.println("clicked");
		//}
		//flags.clear();
	}
	
	public void run(){

		sim = new PathSample1D(params);
		double KR_SP = PathSample1D.KR_SP;
		double binWidth = KR_SP / floor(KR_SP/params.fget("kR bin-width"));
		sf = new StructureFactor1D(sim.Lp, sim.L, sim.R, binWidth);
		Job.addDisplay(grid);
		Job.addDisplay(timeSlice);
		Job.addDisplay(spaceSlice);
		timeSlice.setDataSet(0, sim.getTimeSlice());
		spaceSlice.setDataSet(0, sim.getSpaceSlice());
        grid.setData(sim.Lp, sim.t_f, sim.copyField());
		//fieldPlot.setYRange(-1, 1);
		//Job.addDisplay(fieldPlot);
		//Job.addDisplay(SFPlot);
		//sf.getAccumulator().clear();
		
		while (true) {
			sim.simulate();
			//sf.accumulate(sim.phi);
			//avStructH.accum(structure.getAccumulatorH());
			Job.animate();
			//SFPlot.setDataSet(0, sf.getAccumulator());
			//fieldPlot.setDataSet(0, new PointSet(0, sim.dx, sim.phi));
		}
	}
	
}
