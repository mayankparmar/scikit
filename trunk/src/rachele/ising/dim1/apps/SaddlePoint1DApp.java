package rachele.ising.dim1.apps;

import static java.lang.Math.floor;
//import static scikit.util.Utilities.format;
import rachele.ising.dim1.PathSample1D;
import rachele.ising.dim1.StructureFactor1D;
//import scikit.dataset.PointSet;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.plot.Plot;
import scikit.plot.FieldDisplay;

public class SaddlePoint1DApp extends Simulation{

	FieldDisplay grid = new FieldDisplay("Time vs Coarse Grained Field", true);
    Plot SFPlot = new Plot("Structure factor", true);
    PathSample1D sim;
    StructureFactor1D sf;
	
	
	public static void main(String[] args) {
		new Control(new SaddlePoint1DApp(), "Saddle Point 1D");
	}

	public SaddlePoint1DApp(){
		//params.addm("T", 0.2);
		//params.addm("J", -2.5);
		//params.addm("dt", 0.01);
		//params.add("R", 10000);
		//params.add("L/R", 16.0);
		//params.add("R/dx", 125.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		//params.add("Density", 0.0);
		//params.add("Time");
	}
	
	public void animate() {
		//params.set("Time", format(sim.t));
		//sim.readParams(params);
		
		//SFPlot.setDataSet(0, sf.getAccumulator());
	
		if (flags.contains("Clear S.F.")) {
			sf.getAccumulator().clear();
			System.out.println("clicked");
		}
		flags.clear();
	}
	
	public void run(){
		sim = new PathSample1D(params);
		double KR_SP = PathSample1D.KR_SP;
		double binWidth = KR_SP / floor(KR_SP/params.fget("kR bin-width"));
		sf = new StructureFactor1D(sim.Lp, sim.L, sim.R, binWidth);
		Job.addDisplay(grid);
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
